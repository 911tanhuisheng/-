package com.spingbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 博客评论（纯文本；一级 / 回复）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@TableName("blog_comment")
public class BlogComment extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long postId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 父评论 ID；NULL 表示一级评论
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private String content;

    private Integer likeCount;
}
