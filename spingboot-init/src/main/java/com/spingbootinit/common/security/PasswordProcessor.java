package com.spingbootinit.common.security;

import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;


/**
 * 密码处理器(用于SHA-256+BCrypt) 双重加密
 */
@Component
@RequiredArgsConstructor
public class PasswordProcessor {

    @Resource
    private PasswordEncoder passwordEncoder;

    @Value("${app.security.pepper}")
    private String pepper;

    /**
     * 先加pepper进行SHA-256，再用BCrypt加密
     * rawpassword 用户输入的密码
     */
    public String encode(String rawPassword) {
        String hashed = sha256WithPepper(rawPassword);
        return passwordEncoder.encode(hashed);
    }


    /**
     * 验证密码是否匹配  验证密码：对用户输入做相同处理，再与存储的BCrypt哈希比对
     * rawpassword 用户输入的密码
     * encodedPassword 数据库中的加密密码
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        String hashed = sha256WithPepper(rawPassword);
        return passwordEncoder.matches(hashed, encodedPassword);
    }

    /**
     * 先加pepper进行SHA-256
     * rawpassword 用户输入的密码
     * 使用 Apache Commons Codec 安全地生成 SHA-256 哈希（自动处理 UTF-8 和十六进制）
     */
    private String sha256WithPepper(String rawPassword) {
        return DigestUtils.sha256Hex(rawPassword + pepper);
    }


}
