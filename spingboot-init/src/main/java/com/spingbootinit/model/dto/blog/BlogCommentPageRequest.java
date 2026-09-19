package com.spingbootinit.model.dto.blog;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlogCommentPageRequest {

    @NotBlank(message = "文章 id 不能为空")
    private String postId;

    private Long current = 1L;

    private Long pageSize = 12L;

    /**
     * hot：按点赞；latest：按时间
     */
    private String sort = "latest";
}
