package com.spingbootinit.model.vo.blog;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BlogCommentPageVO {

    /**
     * 置顶热评（点赞≥10，一级评论）
     */
    private List<BlogCommentVO> hotTop = new ArrayList<>();

    private List<BlogCommentVO> records = new ArrayList<>();

    private Long total;

    private Long totalAll;

    private Long current;

    private Long size;

    private Long pages;
}
