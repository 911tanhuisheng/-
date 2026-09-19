package com.spingbootinit.service;

import com.spingbootinit.model.dto.blog.BlogCommentAddRequest;
import com.spingbootinit.model.dto.blog.BlogCommentPageRequest;
import com.spingbootinit.model.vo.blog.BlogCommentLikeToggleVO;
import com.spingbootinit.model.vo.blog.BlogCommentPageVO;
import com.spingbootinit.model.vo.blog.BlogCommentVO;

public interface BlogCommentService {

    BlogCommentVO addComment(BlogCommentAddRequest request, long userId);

    boolean deleteComment(String idStr, long userId);

    BlogCommentPageVO pageComments(BlogCommentPageRequest request, Long viewerUserId);

    BlogCommentLikeToggleVO toggleLike(String commentIdStr, long userId);
}
