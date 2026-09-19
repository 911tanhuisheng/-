package com.spingbootinit.model.dto.user;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "账号不能为空")
    private String username;   // 用户名/邮箱/手机号

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String code;      // 登录验证码（当前登录流程必填）
}