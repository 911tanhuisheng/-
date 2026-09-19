package com.spingbootinit.model.dto.contest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
public class ContestAdminAddRequest implements Serializable {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    /** 前端常用 ISO-8601（如 {@code 2026-05-08T12:05:35.000Z}），用 Instant 解析避免 Date 反序列化失败 */
    @NotNull(message = "开始时间不能为空")
    private Instant startTime;

    @NotNull(message = "结束时间不能为空")
    private Instant endTime;

    /**
     * 赛题列表，可为空（先创建空壳赛场后再维护题目）
     */
    @Valid
    private List<ContestQuestionUpsertItem> questions;
}
