package com.spingbootinit.model.dto.user;

import lombok.Data;

@Data
public class Location {
    // 省
    private String province;
    // 市
    private String city;
    // 区
    private String district;
}
