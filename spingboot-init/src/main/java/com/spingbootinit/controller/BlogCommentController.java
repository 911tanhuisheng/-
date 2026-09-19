package com.spingbootinit.controller;

import com.spingbootinit.common.exception.BusinessException;
import com.spingbootinit.common.result.Result;
import com.spingbootinit.common.result.ResultCode;
import com.spingbootinit.context.UserContext.UserContext;
import com.spingbootinit.model.dto.blog.BlogCommentAddRequest;
import com.spingbootinit.model.dto.blog.BlogCommentDeleteRequest;
import com.spingbootinit.model.dto.blog.BlogCommentLikeToggleRequest;
import com.spingbootinit.model.dto.blog.BlogCommentPageRequest;
import com.spingbootinit.model.vo.blog.BlogCommentLikeToggleVO;
import com.spingbootinit.model.vo.blog.BlogCommentPageVO;
import com.spingbootinit.model.vo.blog.BlogCommentVO;
import com.spingbootinit.service.BlogCommentService;
import com.spingbootinit.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 博客评论（列表公开；发表 / 删除 / 点赞需登录）
 */
@RestController
@RequestMapping("/blog/comment")
@Slf4j
@Tag(name = "博客评论", description = "博客评论")
public class BlogCommentController {

    private static final String BEARER = "Bearer ";

    @Resource
    private BlogCommentService blogCommentService;

    @Resource
    private JwtUtils jwtUtils;

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

    @Operation(summary = "发表评论（一级或回复一级）")
    @PostMapping("/add")
    public Result<BlogCommentVO> add(@Valid @RequestBody BlogCommentAddRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(blogCommentService.addComment(request, uid));
    }

    @Operation(summary = "删除我的评论")
    @PostMapping("/delete")
    public Result<Boolean> delete(@Valid @RequestBody BlogCommentDeleteRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(blogCommentService.deleteComment(request.getId(), uid));
    }

    @Operation(summary = "评论分页（含热评置顶区）；带 Token 时返回是否已点赞")
    @PostMapping("/page")
    public Result<BlogCommentPageVO> page(
            @Valid @RequestBody BlogCommentPageRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long viewer = tryCurrentUserId(authorization);
        return Result.success(blogCommentService.pageComments(request, viewer));
    }

    @Operation(summary = "评论点赞开关")
    @PostMapping("/like/toggle")
    public Result<BlogCommentLikeToggleVO> toggleLike(@Valid @RequestBody BlogCommentLikeToggleRequest request) {
        Long uid = UserContext.getCurrentUserId();
        if (uid == null) {
            throw new BusinessException(ResultCode.NOT_LOGIN_ERROR);
        }
        return Result.success(blogCommentService.toggleLike(request.getCommentId(), uid));
    }
}
