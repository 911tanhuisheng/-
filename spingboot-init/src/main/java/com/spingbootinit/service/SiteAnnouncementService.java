package com.spingbootinit.service;

import com.spingbootinit.model.dto.notification.AnnouncementPublishRequest;
import com.spingbootinit.model.vo.notification.SiteAnnouncementItemVO;

import java.util.List;

public interface SiteAnnouncementService {

    long publish(long publisherUserId, AnnouncementPublishRequest request);

    List<SiteAnnouncementItemVO> listRecentPublic(int limit);

    long countUnreadForUser(long lastReadAnnouncementId);
}
