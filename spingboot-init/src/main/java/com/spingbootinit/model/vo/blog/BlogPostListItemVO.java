package com.spingbootinit.model.vo.blog;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BlogPostListItemVO {
    private String id;
    private String userId;
    private String authorName;
    private String authorAvatar;
    private String title;
    private String slug;
    private String summary;
    private String coverUrl;
    private List<String> tags;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Date publishedAt;
    private Date createTime;
}
