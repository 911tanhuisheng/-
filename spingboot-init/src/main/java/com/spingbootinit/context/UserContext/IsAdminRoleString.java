package com.spingbootinit.context.UserContext;

import org.springframework.stereotype.Component;

/**
 * 判断是否是管理员
 * @author: 98050
 * @create: 2021-11-05 21:05
 **/
@Component
public class IsAdminRoleString {
    public  boolean isAdminRoleString(String role) {
        if (role == null || role.isBlank()) {
            return false;
        }
        String r = role.trim();
        return "admin".equalsIgnoreCase(r) || "管理员".equals(r) || r.toUpperCase().contains("ADMIN");
    }
}
