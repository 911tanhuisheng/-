package com.spingbootinit.model.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BlogPostAddRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 256, message = "标题过长")
    private String title;

    @Size(max = 512, message = "摘要过长")
    private String summary;

    @NotBlank(message = "正文不能为空")
    @Size(min = 1, max = 200_000, message = "正文过长")
    private String content;

    @Size(max = 1024, message = "封面地址过长")
    private String coverUrl;

    private List<@Size(max = 32) String> tags;

    /**
     * 0 草稿 1 发布
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /**
     * 自定义 slug，可选；留空则根据标题自动生成
     */
    @Size(max = 180, message = "slug 过长")
    private String slug;
}
