package com.spingbootinit.model.dto.blog;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlogPostIdRequest {

    @NotBlank(message = "文章 id 不能为空")
    private String id;
}
