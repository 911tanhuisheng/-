package com.spingbootinit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.spingbootinit.model.entity.UserCheckInLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

public interface UserCheckInLogMapper extends BaseMapper<UserCheckInLog> {

    @Select("""
            SELECT check_in_date FROM user_check_in_log
            WHERE user_id = #{userId}
              AND check_in_date >= #{start}
              AND check_in_date <= #{end}
            ORDER BY check_in_date
            """)
    List<LocalDate> selectDatesBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Select("""
            SELECT COUNT(*) FROM user_check_in_log
            WHERE user_id = #{userId}
              AND check_in_date >= #{start}
              AND check_in_date <= #{end}
            """)
    int countBetween(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
