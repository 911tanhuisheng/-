package com.spingbootinit.model.dto.contest;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ContestAdminBatchDeleteRequest implements Serializable {

    @NotEmpty(message = "请至少选择一个赛事")
    private List<Long> ids;
}
