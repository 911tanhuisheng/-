package com.spingbootinit.model.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BlogPostUpdateRequest {

    /** 雪花 ID，前端以 JSON 字符串传递避免精度丢失 */
    @NotBlank(message = "文章 id 不能为空")
    private String id;

    @Size(max = 256, message = "标题过长")
    private String title;

    @Size(max = 512, message = "摘要过长")
    private String summary;

    @Size(min = 1, max = 200_000, message = "正文过长")
    private String content;

    @Size(max = 1024, message = "封面地址过长")
    private String coverUrl;

    private List<@Size(max = 32) String> tags;

    /**
     * 0 草稿 1 发布
     */
    private Integer status;

    @Size(max = 180, message = "slug 过长")
    private String slug;
}
