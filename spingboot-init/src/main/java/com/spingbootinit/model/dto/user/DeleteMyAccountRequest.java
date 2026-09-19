package com.spingbootinit.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户自助注销账号（需校验登录密码）
 */
@Data
public class DeleteMyAccountRequest {

    @NotBlank(message = "请输入登录密码以确认注销")
    private String password;
}
