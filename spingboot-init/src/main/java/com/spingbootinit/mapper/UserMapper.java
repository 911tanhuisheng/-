package com.spingbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spingbootinit.model.entity.User;
import com.spingbootinit.model.vo.uservo.UserLeaderboardRowVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    @Select("""
            SELECT u.id AS userId,
                   COALESCE(NULLIF(TRIM(u.nickname), ''), u.username) AS displayName,
                   u.avatar AS avatar,
                   COALESCE(u.points, 0) AS points,
                   COALESCE(sub.ac_cnt, 0) AS solvedCount,
                   COALESCE(sub.submit_cnt, 0) AS submitCount
            FROM `user` u
            LEFT JOIN (
                SELECT qs.user_id AS uid,
                       COUNT(DISTINCT CASE WHEN qs.status = 2 THEN qs.question_id END) AS ac_cnt,
                       COUNT(*) AS submit_cnt
                FROM question_submit qs
                WHERE qs.is_delete = 0
                GROUP BY qs.user_id
            ) sub ON u.id = sub.uid
            WHERE u.is_delete = 0 AND u.status = 1
              AND EXISTS (
                  SELECT 1 FROM contest_user cu
                  WHERE cu.user_id = u.id AND cu.is_delete = 0
              )
            ORDER BY COALESCE(sub.ac_cnt, 0) DESC,
                     COALESCE(u.points, 0) DESC,
                     COALESCE(sub.submit_cnt, 0) ASC,
                     u.id ASC
            LIMIT #{offset}, #{limit}
            """)
    List<UserLeaderboardRowVO> selectUserLeaderboard(@Param("offset") long offset, @Param("limit") long limit);

    /** 同上榜范围；按积分优先，其次 AC 数、提交次数升序。 */
    @Select("""
            SELECT u.id AS userId,
                   COALESCE(NULLIF(TRIM(u.nickname), ''), u.username) AS displayName,
                   u.avatar AS avatar,
                   COALESCE(u.points, 0) AS points,
                   COALESCE(sub.ac_cnt, 0) AS solvedCount,
                   COALESCE(sub.submit_cnt, 0) AS submitCount
            FROM `user` u
            LEFT JOIN (
                SELECT qs.user_id AS uid,
                       COUNT(DISTINCT CASE WHEN qs.status = 2 THEN qs.question_id END) AS ac_cnt,
                       COUNT(*) AS submit_cnt
                FROM question_submit qs
                WHERE qs.is_delete = 0
                GROUP BY qs.user_id
            ) sub ON u.id = sub.uid
            WHERE u.is_delete = 0 AND u.status = 1
              AND EXISTS (
                  SELECT 1 FROM contest_user cu
                  WHERE cu.user_id = u.id AND cu.is_delete = 0
              )
            ORDER BY COALESCE(u.points, 0) DESC,
                     COALESCE(sub.ac_cnt, 0) DESC,
                     COALESCE(sub.submit_cnt, 0) ASC,
                     u.id ASC
            LIMIT #{offset}, #{limit}
            """)
    List<UserLeaderboardRowVO> selectUserLeaderboardByPoints(@Param("offset") long offset, @Param("limit") long limit);

    /** 同上榜范围；按总提交次数（勤奋度）优先，其次 AC、积分。 */
    @Select("""
            SELECT u.id AS userId,
                   COALESCE(NULLIF(TRIM(u.nickname), ''), u.username) AS displayName,
                   u.avatar AS avatar,
                   COALESCE(u.points, 0) AS points,
                   COALESCE(sub.ac_cnt, 0) AS solvedCount,
                   COALESCE(sub.submit_cnt, 0) AS submitCount
            FROM `user` u
            LEFT JOIN (
                SELECT qs.user_id AS uid,
                       COUNT(DISTINCT CASE WHEN qs.status = 2 THEN qs.question_id END) AS ac_cnt,
                       COUNT(*) AS submit_cnt
                FROM question_submit qs
                WHERE qs.is_delete = 0
                GROUP BY qs.user_id
            ) sub ON u.id = sub.uid
            WHERE u.is_delete = 0 AND u.status = 1
              AND EXISTS (
                  SELECT 1 FROM contest_user cu
                  WHERE cu.user_id = u.id AND cu.is_delete = 0
              )
            ORDER BY COALESCE(sub.submit_cnt, 0) DESC,
                     COALESCE(sub.ac_cnt, 0) DESC,
                     COALESCE(u.points, 0) DESC,
                     u.id ASC
            LIMIT #{offset}, #{limit}
            """)
    List<UserLeaderboardRowVO> selectUserLeaderboardBySubmissions(@Param("offset") long offset, @Param("limit") long limit);

    @Select("""
            SELECT COUNT(*) FROM `user` u
            WHERE u.is_delete = 0 AND u.status = 1
              AND EXISTS (
                  SELECT 1 FROM contest_user cu
                  WHERE cu.user_id = u.id AND cu.is_delete = 0
              )
            """)
    long countLeaderboardUsers();
}

