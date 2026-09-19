package com.spingbootinit.model.vo.uservo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

@Data
public class LoginVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String username;
    /** 短期 access token（与 token 字段相同，便于新前端读取） */
    private String accessToken;
    /** 长期 refresh token，仅用于 /user/refresh */
    private String refreshToken;
    /** 兼容旧前端：等同 accessToken */
    private String token;
    private String userRole;
    private String avatar;
    /** 0-禁用 1-正常 */
    private Integer status;

}
