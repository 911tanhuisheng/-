package com.spingbootinit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.Result;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.context.UserContext.UserContext;
import com.spingbootinit.model.dto.blog.*;
import com.spingbootinit.model.vo.blog.BlogLikeToggleVO;
import com.spingbootinit.model.vo.blog.BlogPostDetailVO;
import com.spingbootinit.model.vo.blog.BlogPostListItemVO;
import com.spingbootinit.service.BlogPostService;
import com.spingbootinit.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 博客文章（公开列表/详情无需登录；写操作需登录）
 */
@RestController
@RequestMapping("/blog/post")
@Slf4j
@Tag(name = "博客文章", description = "博客文章")
public class BlogPostController {

    @Resource
    private BlogPostService blogPostService;

    @Resource
    private JwtUtils jwtUtils;

    private static final String BEARER = "Bearer ";

    /**
     * 从可选 Authorization 解析当前用户 id（公开详情页用于展示是否已点赞）
     */
    private Long tryCurrentUserId(String authorization) {
        if (authorization == null) {
            return null;
        }
        String h = authorization.trim();
        if (h.length() < BEARER.length() || !h.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        String token = h.substring(BEARER.length()).trim();
        if (token.isEmpty() || !jwtUtils.validateToken(token)) {
            return null;
        }
        return jwtUtils.getUserIdFromToken(token);
    }

    @Operation(summary = "新建文章")
    @PostMapping("/add")
    public Result<String> add(@Valid @RequestBody BlogPostAddRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        long id = blogPostService.addPost(request, uid);
        return Result.success(String.valueOf(id));
    }

    @Operation(summary = "更新文章")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody BlogPostUpdateRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(blogPostService.updatePost(request, uid));
    }

    @Operation(summary = "删除文章")
    @PostMapping("/delete")
    public Result<Boolean> delete(@Valid @RequestBody BlogPostDeleteRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(blogPostService.deletePost(request.getId(), uid));
    }

    @Operation(summary = "公开文章分页")
    @PostMapping("/page")
    public Result<Page<BlogPostListItemVO>> pagePublic(@RequestBody BlogPostPageRequest request) {
        return Result.success(blogPostService.pagePublic(request));
    }

    @Operation(summary = "我的文章分页")
    @PostMapping("/my/page")
    public Result<Page<BlogPostListItemVO>> pageMine(@RequestBody BlogPostPageRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(blogPostService.pageMine(request, uid));
    }

    @Operation(summary = "公开文章详情（已发布）；带 Token 时返回是否已点赞")
    @GetMapping("/public/get")
    public Result<BlogPostDetailVO> getPublic(
            @RequestParam("id") String id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long viewer = tryCurrentUserId(authorization);
        return Result.success(blogPostService.getPublicDetail(id, true, viewer));
    }

    @Operation(summary = "我的文章详情（含草稿）")
    @GetMapping("/mine/get")
    public Result<BlogPostDetailVO> getMine(@RequestParam("id") String id) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(blogPostService.getMineDetail(id, uid));
    }

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/like/toggle")
    public Result<BlogLikeToggleVO> toggleLike(@Valid @RequestBody BlogPostLikeToggleRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(blogPostService.toggleLike(request.getPostId(), uid));
    }
}
