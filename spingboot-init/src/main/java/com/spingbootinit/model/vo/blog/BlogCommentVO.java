package com.spingbootinit.model.vo.blog;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class BlogCommentVO {

    private String id;
    private String postId;
    private String parentId;
    private String userId;
    private String userName;
    private String userAvatar;
    private String content;
    private Integer likeCount;
    private Boolean liked;
    private Date createTime;

    /**
     * 一级评论下的回复（最多后端裁剪）
     */
    private List<BlogCommentVO> replies = new ArrayList<>();

    /**
     * 点赞数 ≥10 的一级评论：展示热评徽章（前端）
     */
    private Boolean hot;
}
