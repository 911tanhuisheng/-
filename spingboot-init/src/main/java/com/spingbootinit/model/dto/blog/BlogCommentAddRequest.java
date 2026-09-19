package com.spingbootinit.model.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlogCommentAddRequest {

    @NotBlank(message = "文章 id 不能为空")
    private String postId;

    /**
     * 回复的一级评论 id；不传表示发一级评论
     */
    private String parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论过长")
    private String content;
}
