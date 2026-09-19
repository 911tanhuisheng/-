package com.spingbootinit.model.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员解除用户「AI 学习助手违禁」限制。
 */
@Data
public class UserAdminClearAiAssistBanRequest {

    @NotNull(message = "用户 id 不能为空")
    private Long userId;
}
