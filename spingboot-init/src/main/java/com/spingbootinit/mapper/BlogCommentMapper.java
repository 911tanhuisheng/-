package com.spingbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spingbootinit.model.entity.BlogComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface BlogCommentMapper extends BaseMapper<BlogComment> {

    /**
     * 删除某文章下所有评论对应的点赞（物理删，避免孤儿数据；需在删评论行前执行）。
     */
    @Delete("DELETE cl FROM blog_comment_like cl INNER JOIN blog_comment c ON c.id = cl.comment_id WHERE c.post_id = #{postId}")
    int deleteCommentLikesByPostId(@Param("postId") Long postId);

    /**
     * 删除某文章下全部评论（物理删，含已逻辑删除的行）。
     */
    @Delete("DELETE FROM blog_comment WHERE post_id = #{postId}")
    int deleteCommentsByPostId(@Param("postId") Long postId);
}
