package com.spingbootinit.model.dto.blog;

import com.spingbootinit.model.dto.question.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BlogPostPageRequest extends PageRequest {

    /** 标题模糊搜索 */
    private String titleKeyword;

    /** 标签精确匹配（在 tags JSON 数组中） */
    private String tag;

    /** 仅「我的文章」分页时：null 全部，0 草稿，1 已发布 */
    private Integer status;
}
