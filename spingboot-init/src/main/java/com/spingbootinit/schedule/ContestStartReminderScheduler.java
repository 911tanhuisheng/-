package com.spingbootinit.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.spingbootinit.mapper.ContestMapper;
import com.spingbootinit.mapper.ContestUserMapper;
import com.spingbootinit.model.entity.Contest;
import com.spingbootinit.model.entity.ContestUser;
import com.spingbootinit.model.entity.UserInAppNotification;
import com.spingbootinit.service.InAppNotificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 对已报名用户：开赛前约 30 分钟内发送一次「开赛提醒」（幂等 bizKey）
 */
@Component
@Slf4j
public class ContestStartReminderScheduler {

    private static final long THIRTY_MIN_MS = 30L * 60 * 1000;

    @Resource
    private ContestMapper contestMapper;

    @Resource
    private ContestUserMapper contestUserMapper;

    @Resource
    private InAppNotificationService inAppNotificationService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void remindUpcomingContests() {
        Date now = new Date();
        Date horizon = new Date(now.getTime() + THIRTY_MIN_MS);
        LambdaQueryWrapper<Contest> cw = new LambdaQueryWrapper<>();
        cw.eq(Contest::getIsDelete, 0)
                .gt(Contest::getStartTime, now)
                .le(Contest::getStartTime, horizon);
        List<Contest> contests = contestMapper.selectList(cw);
        if (contests.isEmpty()) {
            return;
        }
        for (Contest c : contests) {
            LambdaQueryWrapper<ContestUser> uw = new LambdaQueryWrapper<>();
            uw.eq(ContestUser::getContestId, c.getId()).eq(ContestUser::getIsDelete, 0);
            List<ContestUser> users = contestUserMapper.selectList(uw);
            String title = "比赛即将开始";
            String body = "「" + (c.getTitle() == null ? "竞赛" : c.getTitle()) + "」将在 30 分钟内开赛，请提前进入赛场。";
            for (ContestUser cu : users) {
                if (cu.getUserId() == null) {
                    continue;
                }
                String biz = "CONTEST_SOON_" + c.getId() + "_" + cu.getUserId();
                try {
                    inAppNotificationService.send(
                            cu.getUserId(),
                            UserInAppNotification.TYPE_CONTEST_SOON,
                            title,
                            body,
                            "CONTEST",
                            String.valueOf(c.getId()),
                            biz);
                } catch (Exception e) {
                    log.warn("contest soon notify skip user {} contest {}: {}", cu.getUserId(), c.getId(), e.getMessage());
                }
            }
        }
    }
}
