package com.spingbootinit.model.vo.questionsubmitvo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
public class MySubmitItemVO {

    /**
     * 数据库提交主键；仅管理员列表等场景返回，普通用户为 null（前端用 submitNo 展示序号）。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String id;

    /**
     * 语义随列表接口的 serialPerQuestion 变化；列表「全部提交」下为全站次序。
     */
    private Long submitNo;

    /**
     * 当前用户账号下全局第几次提交（与「我的全部提交」列表中的次序一致），状态轮询接口始终带上便于前端对齐行。
     */
    private Long globalSubmitNo;

    private String language;
    private String questionId;
    private String questionTitle;
    private String userId;

    /**
     * 非竞赛提交为 null；竞赛提交为竞赛 id 字符串。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String contestId;

    private Integer status;
    private String judgeInfo;
    private Date createTime;
}
