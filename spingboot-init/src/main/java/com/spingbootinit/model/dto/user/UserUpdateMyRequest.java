package com.spingbootinit.model.dto.user;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

/**
 * 更新当前登录用户资料（字段均可选，只更新传入的字段）
 */
@Data
public class UserUpdateMyRequest {

    private String nickname;

    private String phone;

    private String avatar;

    private String introduction;

    private Location locationCodes;

    private List<String> website;

    /** 0-未知，1-男，2-女 */
    private Integer sex;

    private String country;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
}
