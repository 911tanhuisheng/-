package com.spingbootinit.model.dto.contest;

import com.spingbootinit.model.dto.question.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理员赛事分页
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContestAdminPageRequest extends PageRequest {

    /**
     * 标题关键字（模糊）
     */
    private String title;
}
