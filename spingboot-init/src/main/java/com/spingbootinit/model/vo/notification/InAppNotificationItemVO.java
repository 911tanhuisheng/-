package com.spingbootinit.model.vo.notification;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class InAppNotificationItemVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Integer type;

    private String title;

    private String body;

    private String linkKind;

    private String linkRef;

    private Boolean read;

    private Date createTime;
}
