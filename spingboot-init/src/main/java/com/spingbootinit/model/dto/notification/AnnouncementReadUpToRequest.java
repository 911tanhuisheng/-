package com.spingbootinit.model.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AnnouncementReadUpToRequest {

    @NotBlank(message = "公告 id 不能为空")
    private String id;
}
