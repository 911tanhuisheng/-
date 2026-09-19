package com.spingbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.spingbootinit.mapper.SiteAnnouncementMapper;
import com.spingbootinit.model.dto.notification.AnnouncementPublishRequest;
import com.spingbootinit.model.entity.SiteAnnouncement;
import com.spingbootinit.model.vo.notification.SiteAnnouncementItemVO;
import com.spingbootinit.service.SiteAnnouncementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SiteAnnouncementServiceImpl extends ServiceImpl<SiteAnnouncementMapper, SiteAnnouncement>
        implements SiteAnnouncementService {

    private static final int STATUS_PUBLISHED = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long publish(long publisherUserId, AnnouncementPublishRequest request) {
        SiteAnnouncement a = new SiteAnnouncement();
        a.setTitle(request.getTitle().trim());
        a.setContent(request.getContent().trim());
        a.setPublisherId(publisherUserId);
        a.setStatus(STATUS_PUBLISHED);
        Date now = new Date();
        a.setPublishedAt(now);
        this.save(a);
        return a.getId();
    }

    @Override
    public List<SiteAnnouncementItemVO> listRecentPublic(int limit) {
        int n = limit <= 0 ? 10 : Math.min(limit, 30);
        LambdaQueryWrapper<SiteAnnouncement> w = new LambdaQueryWrapper<>();
        w.eq(SiteAnnouncement::getIsDelete, 0)
                .eq(SiteAnnouncement::getStatus, STATUS_PUBLISHED)
                .orderByDesc(SiteAnnouncement::getId)
                .last("LIMIT " + n);
        return this.list(w).stream().map(this::toVo).collect(Collectors.toList());
    }

    private SiteAnnouncementItemVO toVo(SiteAnnouncement a) {
        SiteAnnouncementItemVO v = new SiteAnnouncementItemVO();
        v.setId(a.getId());
        v.setTitle(a.getTitle());
        v.setContent(a.getContent());
        v.setPublishedAt(a.getPublishedAt());
        return v;
    }

    @Override
    public long countUnreadForUser(long lastReadAnnouncementId) {
        long lr = Math.max(0L, lastReadAnnouncementId);
        LambdaQueryWrapper<SiteAnnouncement> w = new LambdaQueryWrapper<>();
        w.eq(SiteAnnouncement::getIsDelete, 0)
                .eq(SiteAnnouncement::getStatus, STATUS_PUBLISHED)
                .gt(SiteAnnouncement::getId, lr);
        return this.count(w);
    }
}
