package com.spingbootinit.model.vo.blog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlogLikeToggleVO {
    private boolean liked;
    private int likeCount;
}
