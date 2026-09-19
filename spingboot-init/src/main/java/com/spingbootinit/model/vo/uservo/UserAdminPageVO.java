package com.spingbootinit.model.vo.uservo;

import lombok.Data;

import java.util.List;

@Data
public class UserAdminPageVO {
    private List<UserAdminListItemVO> records;
    private long total;
    // 分页参数
    private long current;
    private long size;
}
