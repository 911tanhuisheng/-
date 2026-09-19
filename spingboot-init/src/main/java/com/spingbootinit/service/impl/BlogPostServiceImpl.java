package com.spingbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.mapper.BlogCommentMapper;
import com.spingbootinit.mapper.BlogPostLikeMapper;
import com.spingbootinit.mapper.BlogPostMapper;
import com.spingbootinit.model.dto.blog.*;
import com.spingbootinit.model.entity.BlogPost;
import com.spingbootinit.model.entity.BlogPostLike;
import com.spingbootinit.model.entity.User;
import com.spingbootinit.model.vo.blog.BlogLikeToggleVO;
import com.spingbootinit.model.vo.blog.BlogPostDetailVO;
import com.spingbootinit.model.vo.blog.BlogPostListItemVO;
import com.spingbootinit.service.BlogPostService;
import com.spingbootinit.service.UserService;
import com.spingbootinit.utils.BlogSlugUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BlogPostServiceImpl extends ServiceImpl<BlogPostMapper, BlogPost> implements BlogPostService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PUBLISHED = 1;

    @Resource
    private BlogPostLikeMapper blogPostLikeMapper;

    @Resource
    private BlogCommentMapper blogCommentMapper;

    @Resource
    private UserService userService;

    private static long parseId(String raw, String field) {
        if (StringUtils.isBlank(raw)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), field + " 无效");
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), field + " 格式错误");
        }
    }

    private static List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String t : tags) {
            if (t == null) {
                continue;
            }
            String x = t.trim();
            if (x.isEmpty() || x.length() > 32) {
                continue;
            }
            set.add(x);
            if (set.size() >= 12) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    private String buildSummary(String summary, String content) {
        if (StringUtils.isNotBlank(summary)) {
            return summary.trim();
        }
        String c = StringUtils.trimToEmpty(content).replaceAll("\\s+", " ");
        if (c.length() <= 220) {
            return c;
        }
        return c.substring(0, 220) + "…";
    }

    private String ensureUniqueSlug(String desiredBase, Long excludePostId) {
        String base = BlogSlugUtils.slugifyTitle(desiredBase);
        String candidate = base;
        int n = 0;
        while (slugTaken(candidate, excludePostId)) {
            n++;
            candidate = base + "-" + n;
            if (candidate.length() > 180) {
                candidate = BlogSlugUtils.withRandomSuffix(BlogSlugUtils.slugifyTitle(desiredBase + n));
            }
        }
        return candidate;
    }

    private boolean slugTaken(String slug, Long excludePostId) {
        LambdaQueryWrapper<BlogPost> q = new LambdaQueryWrapper<>();
        q.eq(BlogPost::getSlug, slug);
        if (excludePostId != null) {
            q.ne(BlogPost::getId, excludePostId);
        }
        return this.count(q) > 0;
    }

    private void fillAuthors(List<BlogPostListItemVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Set<Long> ids = rows.stream()
                .map(BlogPostListItemVO::getUserId)
                .filter(Objects::nonNull)
                .map(Long::parseLong)
                .collect(Collectors.toSet());
        Map<Long, User> map = new HashMap<>();
        for (Long uid : ids) {
            User u = userService.getById(uid);
            if (u != null) {
                map.put(uid, u);
            }
        }
        for (BlogPostListItemVO vo : rows) {
            if (vo.getUserId() == null) {
                continue;
            }
            User u = map.get(Long.parseLong(vo.getUserId()));
            if (u != null) {
                String nick = StringUtils.isNotBlank(u.getNickname()) ? u.getNickname() : u.getUsername();
                vo.setAuthorName(nick);
                vo.setAuthorAvatar(u.getAvatar());
            } else {
                vo.setAuthorName("用户");
            }
        }
    }

    private BlogPostListItemVO toListVO(BlogPost p) {
        BlogPostListItemVO vo = new BlogPostListItemVO();
        vo.setId(String.valueOf(p.getId()));
        vo.setUserId(String.valueOf(p.getUserId()));
        vo.setTitle(p.getTitle());
        vo.setSlug(p.getSlug());
        vo.setSummary(p.getSummary());
        vo.setCoverUrl(p.getCoverUrl());
        vo.setTags(p.getTags());
        vo.setStatus(p.getStatus());
        vo.setViewCount(p.getViewCount());
        vo.setLikeCount(p.getLikeCount());
        vo.setPublishedAt(p.getPublishedAt());
        vo.setCreateTime(p.getCreateTime());
        return vo;
    }

    private BlogPostDetailVO toDetailVO(BlogPost p, boolean liked) {
        BlogPostDetailVO vo = new BlogPostDetailVO();
        vo.setId(String.valueOf(p.getId()));
        vo.setUserId(String.valueOf(p.getUserId()));
        vo.setTitle(p.getTitle());
        vo.setSlug(p.getSlug());
        vo.setSummary(p.getSummary());
        vo.setContent(p.getContent());
        vo.setCoverUrl(p.getCoverUrl());
        vo.setTags(p.getTags());
        vo.setStatus(p.getStatus());
        vo.setViewCount(p.getViewCount());
        vo.setLikeCount(p.getLikeCount());
        vo.setLiked(liked);
        vo.setPublishedAt(p.getPublishedAt());
        vo.setCreateTime(p.getCreateTime());
        vo.setUpdateTime(p.getUpdateTime());
        User u = userService.getById(p.getUserId());
        if (u != null) {
            String nick = StringUtils.isNotBlank(u.getNickname()) ? u.getNickname() : u.getUsername();
            vo.setAuthorName(nick);
            vo.setAuthorAvatar(u.getAvatar());
        } else {
            vo.setAuthorName("用户");
        }
        return vo;
    }

    @Override
    public long addPost(BlogPostAddRequest request, long userId) {
        String title = StringUtils.trimToEmpty(request.getTitle());
        String content = StringUtils.trimToEmpty(request.getContent());
        if (title.length() < 1 || title.length() > 256) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "标题长度不合法");
        }
        if (content.length() < 10) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "正文至少 10 个字符");
        }
        if (!Objects.equals(request.getStatus(), STATUS_DRAFT) && !Objects.equals(request.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "状态只能为 0 或 1");
        }
        List<String> tags = normalizeTags(request.getTags());
        String summary = buildSummary(request.getSummary(), content);
        String slugBase = StringUtils.isNotBlank(request.getSlug())
                ? BlogSlugUtils.slugifyTitle(request.getSlug())
                : BlogSlugUtils.slugifyTitle(title);
        String slug = ensureUniqueSlug(slugBase, null);

        BlogPost post = new BlogPost();
        post.setUserId(userId);
        post.setTitle(title);
        post.setSlug(slug);
        post.setSummary(summary);
        post.setContent(content);
        post.setCoverUrl(StringUtils.trimToNull(request.getCoverUrl()));
        post.setTags(tags.isEmpty() ? null : tags);
        post.setStatus(request.getStatus());
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setIsDelete(0);
        if (Objects.equals(request.getStatus(), STATUS_PUBLISHED)) {
            post.setPublishedAt(new Date());
        }
        boolean ok = this.save(post);
        if (!ok) {
            throw new BusinessException(ResultCode.OPERATION_ERROR.getCode(), "保存失败");
        }
        return post.getId();
    }

    @Override
    public boolean updatePost(BlogPostUpdateRequest request, long userId) {
        long id = parseId(request.getId(), "文章 id");
        BlogPost old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "文章不存在");
        }
        if (!Objects.equals(old.getUserId(), userId)) {
            throw new BusinessException(ResultCode.NO_AUTH_ERROR.getCode(), "无权修改该文章");
        }
        if (request.getStatus() != null
                && !Objects.equals(request.getStatus(), STATUS_DRAFT)
                && !Objects.equals(request.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "状态只能为 0 或 1");
        }
        if (request.getTitle() != null) {
            String t = request.getTitle().trim();
            if (t.isEmpty() || t.length() > 256) {
                throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "标题长度不合法");
            }
            old.setTitle(t);
        }
        if (request.getContent() != null) {
            String c = request.getContent().trim();
            if (c.length() < 10) {
                throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "正文至少 10 个字符");
            }
            old.setContent(c);
        }
        if (request.getCoverUrl() != null) {
            old.setCoverUrl(StringUtils.trimToNull(request.getCoverUrl()));
        }
        if (request.getTags() != null) {
            List<String> tags = normalizeTags(request.getTags());
            old.setTags(tags.isEmpty() ? null : tags);
        }
        if (request.getStatus() != null) {
            old.setStatus(request.getStatus());
            if (Objects.equals(request.getStatus(), STATUS_PUBLISHED) && old.getPublishedAt() == null) {
                old.setPublishedAt(new Date());
            }
        }
        if (StringUtils.isNotBlank(request.getSlug())) {
            old.setSlug(ensureUniqueSlug(BlogSlugUtils.slugifyTitle(request.getSlug()), id));
        }
        if (request.getSummary() != null) {
            old.setSummary(StringUtils.trimToNull(request.getSummary()));
        }
        if (StringUtils.isBlank(old.getSummary())) {
            old.setSummary(buildSummary(null, old.getContent()));
        }
        return this.updateById(old);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deletePost(String idStr, long userId) {
        long id = parseId(idStr, "文章 id");
        BlogPost old = this.getById(id);
        if (old == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "文章不存在");
        }
        if (!Objects.equals(old.getUserId(), userId)) {
            throw new BusinessException(ResultCode.NO_AUTH_ERROR.getCode(), "无权删除该文章");
        }
        // 文章删除时级联清理评论与评论点赞（物理删除，含此前已软删的评论行）
        blogCommentMapper.deleteCommentLikesByPostId(id);
        blogCommentMapper.deleteCommentsByPostId(id);
        blogPostLikeMapper.delete(new LambdaQueryWrapper<BlogPostLike>().eq(BlogPostLike::getPostId, id));
        return this.removeById(id);
    }

    @Override
    public Page<BlogPostListItemVO> pagePublic(BlogPostPageRequest request) {
        long current = request.getCurrent() < 1 ? 1 : request.getCurrent();
        long size = Math.min(request.getPageSize() < 1 ? 10 : request.getPageSize(), 50);
        LambdaQueryWrapper<BlogPost> qw = new LambdaQueryWrapper<>();
        qw.eq(BlogPost::getStatus, STATUS_PUBLISHED);
        if (StringUtils.isNotBlank(request.getTitleKeyword())) {
            qw.like(BlogPost::getTitle, request.getTitleKeyword().trim());
        }
        if (StringUtils.isNotBlank(request.getTag())) {
            String tag = request.getTag().trim().replace("\"", "").replace("\\", "").replace("'", "");
            if (!tag.isEmpty() && tag.length() <= 32) {
                qw.apply("JSON_CONTAINS(COALESCE(tags, JSON_ARRAY()), CONCAT('\"', {0}, '\"'), '$')", tag);
            }
        }
        qw.orderByDesc(BlogPost::getPublishedAt);
        Page<BlogPost> page = this.page(new Page<>(current, size), qw);
        Page<BlogPostListItemVO> out = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<BlogPostListItemVO> rows = page.getRecords().stream().map(this::toListVO).toList();
        fillAuthors(rows);
        out.setRecords(rows);
        return out;
    }

    @Override
    public Page<BlogPostListItemVO> pageMine(BlogPostPageRequest request, long userId) {
        long current = request.getCurrent() < 1 ? 1 : request.getCurrent();
        long size = Math.min(request.getPageSize() < 1 ? 10 : request.getPageSize(), 50);
        LambdaQueryWrapper<BlogPost> qw = new LambdaQueryWrapper<>();
        qw.eq(BlogPost::getUserId, userId);
        if (request.getStatus() != null) {
            qw.eq(BlogPost::getStatus, request.getStatus());
        }
        if (StringUtils.isNotBlank(request.getTitleKeyword())) {
            qw.like(BlogPost::getTitle, request.getTitleKeyword().trim());
        }
        qw.orderByDesc(BlogPost::getUpdateTime);
        Page<BlogPost> page = this.page(new Page<>(current, size), qw);
        Page<BlogPostListItemVO> out = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<BlogPostListItemVO> rows = page.getRecords().stream().map(this::toListVO).toList();
        fillAuthors(rows);
        out.setRecords(rows);
        return out;
    }

    @Override
    public BlogPostDetailVO getPublicDetail(String idStr, boolean incrementView, Long viewerUserId) {
        long id = parseId(idStr, "文章 id");
        BlogPost p = this.getById(id);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "文章不存在");
        }
        if (!Objects.equals(p.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "文章未发布或不存在");
        }
        if (incrementView) {
            this.baseMapper.update(null, new LambdaUpdateWrapper<BlogPost>()
                    .setSql("view_count = IFNULL(view_count,0) + 1")
                    .eq(BlogPost::getId, id));
            p = this.getById(id);
        }
        boolean liked = false;
        if (viewerUserId != null) {
            liked = blogPostLikeMapper.selectCount(new LambdaQueryWrapper<BlogPostLike>()
                    .eq(BlogPostLike::getPostId, id)
                    .eq(BlogPostLike::getUserId, viewerUserId)) > 0;
        }
        return toDetailVO(p, liked);
    }

    @Override
    public BlogPostDetailVO getMineDetail(String idStr, long userId) {
        long id = parseId(idStr, "文章 id");
        BlogPost p = this.getById(id);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "文章不存在");
        }
        if (!Objects.equals(p.getUserId(), userId)) {
            throw new BusinessException(ResultCode.NO_AUTH_ERROR.getCode(), "无权查看该文章");
        }
        boolean liked = blogPostLikeMapper.selectCount(new LambdaQueryWrapper<BlogPostLike>()
                .eq(BlogPostLike::getPostId, id)
                .eq(BlogPostLike::getUserId, userId)) > 0;
        return toDetailVO(p, liked);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogLikeToggleVO toggleLike(String postIdStr, long userId) {
        long postId = parseId(postIdStr, "文章 id");
        BlogPost post = this.getById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "文章不存在");
        }
        if (!Objects.equals(post.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "仅已发布文章可点赞");
        }
        LambdaQueryWrapper<BlogPostLike> q = new LambdaQueryWrapper<>();
        q.eq(BlogPostLike::getPostId, postId).eq(BlogPostLike::getUserId, userId);
        BlogPostLike existing = blogPostLikeMapper.selectOne(q);
        if (existing != null) {
            blogPostLikeMapper.deleteById(existing.getId());
            this.baseMapper.update(null, new LambdaUpdateWrapper<BlogPost>()
                    .setSql("like_count = GREATEST(IFNULL(like_count,0) - 1, 0)")
                    .eq(BlogPost::getId, postId));
            BlogPost fresh = this.getById(postId);
            return new BlogLikeToggleVO(false, fresh != null && fresh.getLikeCount() != null ? fresh.getLikeCount() : 0);
        }
        BlogPostLike like = new BlogPostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreateTime(new Date());
        blogPostLikeMapper.insert(like);
        this.baseMapper.update(null, new LambdaUpdateWrapper<BlogPost>()
                .setSql("like_count = IFNULL(like_count,0) + 1")
                .eq(BlogPost::getId, postId));
        BlogPost fresh = this.getById(postId);
        return new BlogLikeToggleVO(true, fresh != null && fresh.getLikeCount() != null ? fresh.getLikeCount() : 1);
    }
}
