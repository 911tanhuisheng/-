package com.spingbootinit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spingbootinit.model.dto.blog.*;
import com.spingbootinit.model.vo.blog.BlogLikeToggleVO;
import com.spingbootinit.model.vo.blog.BlogPostDetailVO;
import com.spingbootinit.model.vo.blog.BlogPostListItemVO;

public interface BlogPostService {

    long addPost(BlogPostAddRequest request, long userId);

    boolean updatePost(BlogPostUpdateRequest request, long userId);

    boolean deletePost(String idStr, long userId);

    Page<BlogPostListItemVO> pagePublic(BlogPostPageRequest request);

    Page<BlogPostListItemVO> pageMine(BlogPostPageRequest request, long userId);

    BlogPostDetailVO getPublicDetail(String idStr, boolean incrementView, Long viewerUserId);

    BlogPostDetailVO getMineDetail(String idStr, long userId);

    BlogLikeToggleVO toggleLike(String postIdStr, long userId);
}
