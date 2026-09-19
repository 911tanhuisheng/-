package com.spingbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spingbootinit.mapper.UserInAppNotificationMapper;
import com.spingbootinit.mapper.UserMapper;
import com.spingbootinit.model.entity.User;
import com.spingbootinit.model.entity.UserInAppNotification;
import com.spingbootinit.model.vo.notification.InAppNotificationItemVO;
import com.spingbootinit.model.vo.notification.NotificationSummaryVO;
import com.spingbootinit.service.InAppNotificationService;
import com.spingbootinit.service.SiteAnnouncementService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InAppNotificationServiceImpl extends ServiceImpl<UserInAppNotificationMapper, UserInAppNotification>
        implements InAppNotificationService {

    @Resource
    private SiteAnnouncementService siteAnnouncementService;

    @Resource
    private UserMapper userMapper;

    @Override
    public NotificationSummaryVO summary(long userId) {
        NotificationSummaryVO vo = new NotificationSummaryVO();
        LambdaQueryWrapper<UserInAppNotification> w = new LambdaQueryWrapper<>();
        w.eq(UserInAppNotification::getUserId, userId)
                .eq(UserInAppNotification::getIsDelete, 0)
                .isNull(UserInAppNotification::getReadAt);
        vo.setInAppUnread(this.count(w));
        User me = userMapper.selectById(userId);
        long lr = me == null || me.getLastReadAnnouncementId() == null ? 0L : me.getLastReadAnnouncementId();
        vo.setAnnouncementUnread(siteAnnouncementService.countUnreadForUser(lr));
        return vo;
    }

    @Override
    public Page<InAppNotificationItemVO> pageMine(long userId, long current, long pageSize) {
        long pageNo = current <= 0 ? 1 : current;
        long size = pageSize <= 0 ? 20 : Math.min(pageSize, 50);
        Page<UserInAppNotification> p = new Page<>(pageNo, size);
        LambdaQueryWrapper<UserInAppNotification> w = new LambdaQueryWrapper<>();
        w.eq(UserInAppNotification::getUserId, userId)
                .eq(UserInAppNotification::getIsDelete, 0)
                .orderByDesc(UserInAppNotification::getCreateTime);
        Page<UserInAppNotification> raw = this.page(p, w);
        Page<InAppNotificationItemVO> out = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        List<InAppNotificationItemVO> rows = raw.getRecords().stream().map(this::toItem).collect(Collectors.toList());
        out.setRecords(rows);
        return out;
    }

    private InAppNotificationItemVO toItem(UserInAppNotification n) {
        InAppNotificationItemVO v = new InAppNotificationItemVO();
        v.setId(n.getId());
        v.setType(n.getType());
        v.setTitle(n.getTitle());
        v.setBody(n.getBody());
        v.setLinkKind(n.getLinkKind());
        v.setLinkRef(n.getLinkRef());
        v.setRead(n.getReadAt() != null);
        v.setCreateTime(n.getCreateTime());
        return v;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(long userId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Date now = new Date();
        LambdaUpdateWrapper<UserInAppNotification> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserInAppNotification::getUserId, userId)
                .in(UserInAppNotification::getId, ids)
                .isNull(UserInAppNotification::getReadAt)
                .set(UserInAppNotification::getReadAt, now);
        this.update(uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(long userId) {
        Date now = new Date();
        LambdaUpdateWrapper<UserInAppNotification> uw = new LambdaUpdateWrapper<>();
        uw.eq(UserInAppNotification::getUserId, userId)
                .isNull(UserInAppNotification::getReadAt)
                .set(UserInAppNotification::getReadAt, now);
        this.update(uw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(long recipientUserId, int type, String title, String body, String linkKind, String linkRef, String bizKey) {
        if (recipientUserId <= 0) {
            return;
        }
        if (bizKey != null && !bizKey.isEmpty()) {
            LambdaQueryWrapper<UserInAppNotification> ex = new LambdaQueryWrapper<>();
            ex.eq(UserInAppNotification::getUserId, recipientUserId).eq(UserInAppNotification::getBizKey, bizKey);
            if (this.count(ex) > 0) {
                return;
            }
        }
        UserInAppNotification n = new UserInAppNotification();
        n.setUserId(recipientUserId);
        n.setType(type);
        n.setTitle(title);
        n.setBody(body);
        n.setLinkKind(linkKind);
        n.setLinkRef(linkRef);
        n.setBizKey(bizKey);
        this.save(n);
    }
}
