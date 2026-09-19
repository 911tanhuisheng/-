package com.spingbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.mapper.BlogCommentLikeMapper;
import com.spingbootinit.mapper.BlogCommentMapper;
import com.spingbootinit.mapper.BlogPostMapper;
import com.spingbootinit.model.dto.blog.BlogCommentAddRequest;
import com.spingbootinit.model.dto.blog.BlogCommentPageRequest;
import com.spingbootinit.model.entity.BlogComment;
import com.spingbootinit.model.entity.BlogCommentLike;
import com.spingbootinit.model.entity.BlogPost;
import com.spingbootinit.model.entity.User;
import com.spingbootinit.model.entity.UserInAppNotification;
import com.spingbootinit.model.vo.blog.BlogCommentLikeToggleVO;
import com.spingbootinit.model.vo.blog.BlogCommentPageVO;
import com.spingbootinit.model.vo.blog.BlogCommentVO;
import com.spingbootinit.service.BlogCommentService;
import com.spingbootinit.service.ContentModerationService;
import com.spingbootinit.service.InAppNotificationService;
import com.spingbootinit.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BlogCommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment> implements BlogCommentService {

    private static final int STATUS_PUBLISHED = 1;
    private static final int HOT_MIN_LIKES = 10;
    private static final int HOT_TOP_LIMIT = 10;
    private static final int REPLIES_PER_THREAD = 12;
    private static final int MAX_CONTENT_LEN = 2000;

    @Resource
    private BlogCommentLikeMapper blogCommentLikeMapper;

    @Resource
    private BlogPostMapper blogPostMapper;

    @Resource
    private UserService userService;

    @Resource
    private InAppNotificationService inAppNotificationService;

    @Resource
    private ContentModerationService contentModerationService;

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

    private static String sanitizeContent(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.replace('\r', ' ').trim();
        t = t.replaceAll("<[^>]*>", "");
        if (t.length() > MAX_CONTENT_LEN) {
            t = t.substring(0, MAX_CONTENT_LEN);
        }
        return t;
    }

    private BlogPost requirePublishedPost(long postId) {
        BlogPost post = blogPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "文章不存在");
        }
        if (!Objects.equals(post.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "仅已发布文章可评论");
        }
        return post;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogCommentVO addComment(BlogCommentAddRequest request, long userId) {
        userService.assertCanPostComment(userId);
        long postId = parseId(request.getPostId(), "文章 id");
        requirePublishedPost(postId);
        String content = sanitizeContent(request.getContent());
        if (content.isEmpty()) {
            throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "评论内容不能为空");
        }
        String matchedBadWord = contentModerationService.findMatchedBadWord(content);
        if (matchedBadWord != null) {
            userService.banForProfanityComment(userId, matchedBadWord);
            throw new BusinessException(
                    ResultCode.COMMENT_PROFANITY_BANNED.getCode(),
                    ResultCode.COMMENT_PROFANITY_BANNED.getMessage());
        }
        Long parentId = null;
        if (StringUtils.isNotBlank(request.getParentId())) {
            long pid = parseId(request.getParentId(), "父评论 id");
            BlogComment parent = this.getById(pid);
            if (parent == null) {
                throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "父评论不存在");
            }
            if (!Objects.equals(parent.getPostId(), postId)) {
                throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "父评论不属于该文章");
            }
            if (parent.getParentId() != null) {
                throw new BusinessException(ResultCode.PARAMS_ERROR.getCode(), "暂不支持嵌套回复，请回复一级评论");
            }
            parentId = pid;
        }
        BlogComment c = new BlogComment();
        c.setPostId(postId);
        c.setUserId(userId);
        c.setParentId(parentId);
        c.setContent(content);
        c.setLikeCount(0);
        this.save(c);

        notifyCommentRecipients(postId, parentId, userId, c.getId(), content);

        User u = userService.getById(userId);
        Map<Long, User> um = new HashMap<>();
        if (u != null) {
            um.put(userId, u);
        }
        Set<Long> likedEmpty = Collections.emptySet();
        if (parentId == null) {
            return buildTopLevelVO(c, um, likedEmpty, Collections.emptyList(), userId);
        }
        return buildReplyVO(c, um, likedEmpty);
    }

    private void notifyCommentRecipients(long postId, Long parentId, long actorUserId, long commentId, String rawContent) {
        BlogPost post = blogPostMapper.selectById(postId);
        if (post == null) {
            return;
        }
        User actor = userService.getById(actorUserId);
        String label = actorLabel(actor);
        String pv = preview(rawContent);
        String biz = "CMT_" + commentId;
        if (parentId == null) {
            Long authorId = post.getUserId();
            if (authorId != null && !authorId.equals(actorUserId)) {
                inAppNotificationService.send(
                        authorId,
                        UserInAppNotification.TYPE_COMMENT_ON_POST,
                        label + " 评论了你的文章",
                        pv,
                        "BLOG_POST",
                        String.valueOf(postId),
                        biz);
            }
            return;
        }
        BlogComment parent = this.getById(parentId);
        if (parent == null || parent.getUserId() == null || parent.getUserId().equals(actorUserId)) {
            return;
        }
        inAppNotificationService.send(
                parent.getUserId(),
                UserInAppNotification.TYPE_COMMENT_REPLY,
                label + " 回复了你的评论",
                pv,
                "BLOG_POST",
                String.valueOf(postId),
                biz);
    }

    private static String actorLabel(User u) {
        if (u == null) {
            return "有人";
        }
        if (StringUtils.isNotBlank(u.getNickname())) {
            return u.getNickname().trim();
        }
        return u.getUsername() != null ? u.getUsername() : "有人";
    }

    private static String preview(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= 100) {
            return content;
        }
        return content.substring(0, 100) + "…";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteComment(String idStr, long userId) {
        long id = parseId(idStr, "评论 id");
        BlogComment c = this.getById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "评论不存在");
        }
        if (!Objects.equals(c.getUserId(), userId)) {
            throw new BusinessException(ResultCode.NO_AUTH_ERROR.getCode(), "只能删除自己的评论");
        }

        List<Long> purgeCommentIds = new ArrayList<>();
        purgeCommentIds.add(id);

        // 删除一级评论时，级联软删除其下全部回复，并清理点赞记录
        if (c.getParentId() == null) {
            List<BlogComment> replies = this.list(new LambdaQueryWrapper<BlogComment>().eq(BlogComment::getParentId, id));
            for (BlogComment r : replies) {
                purgeCommentIds.add(r.getId());
            }
            if (!replies.isEmpty()) {
                this.remove(new LambdaQueryWrapper<BlogComment>().eq(BlogComment::getParentId, id));
            }
        }

        if (!purgeCommentIds.isEmpty()) {
            blogCommentLikeMapper.delete(new LambdaQueryWrapper<BlogCommentLike>().in(BlogCommentLike::getCommentId, purgeCommentIds));
        }
        return this.removeById(id);
    }

    @Override
    public BlogCommentPageVO pageComments(BlogCommentPageRequest request, Long viewerUserId) {
        long postId = parseId(request.getPostId(), "文章 id");
        BlogPost post = blogPostMapper.selectById(postId);
        if (post == null || !Objects.equals(post.getStatus(), STATUS_PUBLISHED)) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "文章不存在或未发布");
        }

        String sort = StringUtils.trimToEmpty(request.getSort());
        if (!sort.equalsIgnoreCase("hot") && !sort.equalsIgnoreCase("latest")) {
            sort = "latest";
        }
        long current = request.getCurrent() == null || request.getCurrent() < 1 ? 1 : request.getCurrent();
        long pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 12 : Math.min(request.getPageSize(), 50);

        long totalAll = this.baseMapper.selectCount(new LambdaQueryWrapper<BlogComment>().eq(BlogComment::getPostId, postId));
        long totalTopLevel = this.baseMapper.selectCount(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getPostId, postId)
                .isNull(BlogComment::getParentId));

        LambdaQueryWrapper<BlogComment> hotQ = new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getPostId, postId)
                .isNull(BlogComment::getParentId)
                .ge(BlogComment::getLikeCount, HOT_MIN_LIKES)
                .orderByDesc(BlogComment::getLikeCount)
                .orderByDesc(BlogComment::getCreateTime)
                .last("LIMIT " + HOT_TOP_LIMIT);
        List<BlogComment> hotEntities = this.baseMapper.selectList(hotQ);
        LinkedHashSet<Long> hotIds = hotEntities.stream().map(BlogComment::getId).collect(Collectors.toCollection(LinkedHashSet::new));

        long remainingTop = Math.max(0, totalTopLevel - hotIds.size());

        LambdaQueryWrapper<BlogComment> mq = new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getPostId, postId)
                .isNull(BlogComment::getParentId);
        if (!hotIds.isEmpty()) {
            mq.notIn(BlogComment::getId, hotIds);
        }
        if ("hot".equalsIgnoreCase(sort)) {
            mq.orderByDesc(BlogComment::getLikeCount).orderByDesc(BlogComment::getCreateTime);
        } else {
            mq.orderByDesc(BlogComment::getCreateTime);
        }

        Page<BlogComment> mp = new Page<>(current, pageSize, remainingTop);
        mp.setSearchCount(false);
        Page<BlogComment> page = this.baseMapper.selectPage(mp, mq);
        List<BlogComment> records = page.getRecords();

        BlogCommentPageVO vo = new BlogCommentPageVO();
        vo.setTotal(remainingTop);
        vo.setTotalAll(totalAll);
        vo.setCurrent(current);
        vo.setSize(pageSize);
        vo.setPages(page.getPages());

        LinkedHashSet<Long> topIds = new LinkedHashSet<>();
        hotEntities.forEach(h -> topIds.add(h.getId()));
        records.forEach(r -> topIds.add(r.getId()));

        Map<Long, List<BlogComment>> repliesMap = loadRepliesGrouped(new ArrayList<>(topIds));

        Map<Long, User> userMap = buildUserMap(hotEntities, records, repliesMap);

        Set<Long> allCommentIds = new HashSet<>(topIds);
        repliesMap.values().forEach(list -> list.forEach(r -> allCommentIds.add(r.getId())));
        Set<Long> likedSet = loadViewerLiked(allCommentIds, viewerUserId);

        List<BlogCommentVO> hotVOs = new ArrayList<>();
        for (BlogComment h : hotEntities) {
            hotVOs.add(buildTopLevelVO(h, userMap, likedSet, repliesMap.getOrDefault(h.getId(), Collections.emptyList()), viewerUserId));
        }
        vo.setHotTop(hotVOs);

        List<BlogCommentVO> rowVOs = new ArrayList<>();
        for (BlogComment r : records) {
            rowVOs.add(buildTopLevelVO(r, userMap, likedSet, repliesMap.getOrDefault(r.getId(), Collections.emptyList()), viewerUserId));
        }
        vo.setRecords(rowVOs);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BlogCommentLikeToggleVO toggleLike(String commentIdStr, long userId) {
        long commentId = parseId(commentIdStr, "评论 id");
        BlogComment c = this.getById(commentId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND_ERROR.getCode(), "评论不存在");
        }
        requirePublishedPost(c.getPostId());

        LambdaQueryWrapper<BlogCommentLike> q = new LambdaQueryWrapper<BlogCommentLike>()
                .eq(BlogCommentLike::getCommentId, commentId)
                .eq(BlogCommentLike::getUserId, userId);
        BlogCommentLike existing = blogCommentLikeMapper.selectOne(q);
        if (existing != null) {
            blogCommentLikeMapper.deleteById(existing.getId());
            this.baseMapper.update(null, new LambdaUpdateWrapper<BlogComment>()
                    .setSql("like_count = GREATEST(IFNULL(like_count,0) - 1, 0)")
                    .eq(BlogComment::getId, commentId));
            BlogComment fresh = this.getById(commentId);
            int cnt = fresh != null && fresh.getLikeCount() != null ? fresh.getLikeCount() : 0;
            BlogCommentLikeToggleVO vo = new BlogCommentLikeToggleVO();
            vo.setCommentId(String.valueOf(commentId));
            vo.setLiked(false);
            vo.setLikeCount(cnt);
            return vo;
        }
        BlogCommentLike like = new BlogCommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        like.setCreateTime(new Date());
        blogCommentLikeMapper.insert(like);
        this.baseMapper.update(null, new LambdaUpdateWrapper<BlogComment>()
                .setSql("like_count = IFNULL(like_count,0) + 1")
                .eq(BlogComment::getId, commentId));
        BlogComment fresh = this.getById(commentId);
        int cnt = fresh != null && fresh.getLikeCount() != null ? fresh.getLikeCount() : 1;
        BlogCommentLikeToggleVO vo = new BlogCommentLikeToggleVO();
        vo.setCommentId(String.valueOf(commentId));
        vo.setLiked(true);
        vo.setLikeCount(cnt);
        return vo;
    }

    private Map<Long, List<BlogComment>> loadRepliesGrouped(List<Long> parentIds) {
        Map<Long, List<BlogComment>> map = new HashMap<>();
        if (parentIds == null || parentIds.isEmpty()) {
            return map;
        }
        LambdaQueryWrapper<BlogComment> rq = new LambdaQueryWrapper<BlogComment>()
                .in(BlogComment::getParentId, parentIds)
                .orderByAsc(BlogComment::getCreateTime);
        List<BlogComment> all = this.baseMapper.selectList(rq);
        Map<Long, List<BlogComment>> grouped = all.stream()
                .collect(Collectors.groupingBy(BlogComment::getParentId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<BlogComment>> e : grouped.entrySet()) {
            List<BlogComment> list = e.getValue();
            if (list.size() > REPLIES_PER_THREAD) {
                list = new ArrayList<>(list.subList(0, REPLIES_PER_THREAD));
            }
            map.put(e.getKey(), list);
        }
        return map;
    }

    private Map<Long, User> buildUserMap(List<BlogComment> hot, List<BlogComment> records, Map<Long, List<BlogComment>> repliesMap) {
        Set<Long> uids = new HashSet<>();
        hot.forEach(h -> uids.add(h.getUserId()));
        records.forEach(r -> uids.add(r.getUserId()));
        repliesMap.values().forEach(list -> list.forEach(x -> uids.add(x.getUserId())));
        return loadUsers(uids);
    }

    private Map<Long, User> loadUsers(Set<Long> uids) {
        if (uids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<User> users = userService.listByIds(uids);
        return users.stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private Set<Long> loadViewerLiked(Set<Long> commentIds, Long viewerUserId) {
        if (viewerUserId == null || commentIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<BlogCommentLike> q = new LambdaQueryWrapper<BlogCommentLike>()
                .eq(BlogCommentLike::getUserId, viewerUserId)
                .in(BlogCommentLike::getCommentId, commentIds);
        List<BlogCommentLike> rows = blogCommentLikeMapper.selectList(q);
        return rows.stream().map(BlogCommentLike::getCommentId).collect(Collectors.toSet());
    }

    private BlogCommentVO fillBase(BlogComment c, Map<Long, User> users, Set<Long> likedSet) {
        BlogCommentVO vo = new BlogCommentVO();
        vo.setId(String.valueOf(c.getId()));
        vo.setPostId(String.valueOf(c.getPostId()));
        vo.setParentId(c.getParentId() == null ? null : String.valueOf(c.getParentId()));
        vo.setUserId(String.valueOf(c.getUserId()));
        User u = users.get(c.getUserId());
        vo.setUserName(u != null && StringUtils.isNotBlank(u.getNickname()) ? u.getNickname() : (u != null ? u.getUsername() : "用户"));
        vo.setUserAvatar(u != null ? u.getAvatar() : null);
        vo.setContent(c.getContent());
        vo.setLikeCount(c.getLikeCount() != null ? c.getLikeCount() : 0);
        vo.setLiked(likedSet.contains(c.getId()));
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }

    private BlogCommentVO buildTopLevelVO(BlogComment c, Map<Long, User> users, Set<Long> likedSet,
                                          List<BlogComment> replies, Long viewerUserId) {
        BlogCommentVO vo = fillBase(c, users, likedSet);
        vo.setHot(c.getParentId() == null && vo.getLikeCount() >= HOT_MIN_LIKES);
        if (replies != null && !replies.isEmpty()) {
            Map<Long, User> ru = loadUsers(replies.stream().map(BlogComment::getUserId).collect(Collectors.toSet()));
            Set<Long> rids = replies.stream().map(BlogComment::getId).collect(Collectors.toSet());
            Set<Long> replyLiked = loadViewerLiked(rids, viewerUserId);
            List<BlogCommentVO> children = new ArrayList<>();
            for (BlogComment r : replies) {
                children.add(buildReplyVO(r, ru, replyLiked));
            }
            vo.setReplies(children);
        }
        return vo;
    }

    private BlogCommentVO buildReplyVO(BlogComment c, Map<Long, User> users, Set<Long> likedSet) {
        BlogCommentVO vo = fillBase(c, users, likedSet);
        vo.setHot(false);
        return vo;
    }
}
