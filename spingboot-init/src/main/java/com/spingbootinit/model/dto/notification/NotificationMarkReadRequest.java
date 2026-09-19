package com.spingbootinit.model.dto.notification;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class NotificationMarkReadRequest {

    @NotEmpty(message = "ids 不能为空")
    private List<String> ids;
}
