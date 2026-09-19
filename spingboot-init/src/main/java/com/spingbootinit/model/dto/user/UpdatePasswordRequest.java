package com.spingbootinit.model.dto.user;

import lombok.Data;

@Data
public class UpdatePasswordRequest {
    // 旧密码
    private String oldPassword;

    // 新密码
    private String newPassword;

    // 确认新密码
    private String confirmNewPassword;
}
