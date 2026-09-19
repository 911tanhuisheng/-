package com.spingbootinit.model.dto.blog;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlogCommentLikeToggleRequest {

    @NotBlank(message = "评论 id 不能为空")
    private String commentId;
}
