package com.spingbootinit.model.vo.uservo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学习打卡日历：某自然年内已签到日期列表（yyyy-MM-dd，上海时区）。
 */
@Data
public class UserCheckInCalendarVO {
    private int year;
    /** 该自然年内签到天数 */
    private int yearCheckInDays;
    /** 当前积分（便于面板展示） */
    private Integer points;
    /** 历史累计签到 */
    private Integer checkInCount;
    private boolean checkedToday;
    /** 已签到日期，如 2026-05-16 */
    private List<String> checkedDates = new ArrayList<>();
}
