package com.spingbootinit.model.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AnnouncementPublishRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "正文不能为空")
    @Size(max = 20000)
    private String content;
}
