package com.spingbootinit.model.dto.contest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
public class ContestAdminUpdateRequest implements Serializable {

    @NotNull(message = "赛事 id 不能为空")
    private Long id;

    @NotBlank(message = "标题不能为空")
    private String title;

    private String description;

    @NotNull(message = "开始时间不能为空")
    private Instant startTime;

    @NotNull(message = "结束时间不能为空")
    private Instant endTime;

    @Valid
    private List<ContestQuestionUpsertItem> questions;
}
