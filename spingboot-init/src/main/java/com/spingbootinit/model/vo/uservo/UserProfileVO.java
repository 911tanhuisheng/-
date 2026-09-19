package com.spingbootinit.model.vo.uservo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.spingbootinit.model.dto.user.Location;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 用于前端「基本信息」回显的安全视图对象（避免暴露 password/email/phone 等敏感字段）
 */
@Data
public class UserProfileVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String nickname;

    private String avatar;

    private String introduction;

    private List<String> website;
    /**
     * 所在地
     */
    private Location locationCodes;
    /**
     * 手机号
     */
    private String phone;

    /** 0-未知，1-男，2-女 */
    private Integer sex;

    private String country;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date birthday;

    /** 0-禁用 1-正常 */
    private Integer status;
}

