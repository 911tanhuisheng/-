package com.spingbootinit.model.vo.blog;

import lombok.Data;

@Data
public class BlogCommentLikeToggleVO {

    private String commentId;
    private Boolean liked;
    private Integer likeCount;
}
