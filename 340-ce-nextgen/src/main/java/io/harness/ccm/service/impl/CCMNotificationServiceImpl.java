package io.harness.ccm.service.impl;

import io.harness.ccm.commons.dao.notifications.CCMNotificationsDao;
import io.harness.ccm.commons.entities.notifications.CCMNotificationSetting;
import io.harness.ccm.commons.entities.notifications.CCMPerspectiveNotificationChannelsDTO;
import io.harness.ccm.service.intf.CCMNotificationService;
import io.harness.ccm.views.service.CEViewService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CCMNotificationServiceImpl implements CCMNotificationService {
  @Inject CCMNotificationsDao notificationsDao;
  @Inject CEViewService viewService;

  @Override
  public CCMNotificationSetting upsert(CCMNotificationSetting notificationSetting) {
    return notificationsDao.upsert(notificationSetting);
  }

  @Override
  public CCMNotificationSetting get(String perspectiveId, String accountId) {
    return notificationsDao.get(perspectiveId, accountId);
  }

  @Override
  public List<CCMPerspectiveNotificationChannelsDTO> list(String accountId) {
    List<CCMNotificationSetting> notificationSettings = notificationsDao.list(accountId);
    List<String> perspectiveIds =
        notificationSettings.stream().map(CCMNotificationSetting::getPerspectiveId).collect(Collectors.toList());
    Map<String, String> perspectiveIdToNameMapping =
        viewService.getPerspectiveIdToNameMapping(accountId, perspectiveIds);
    List<CCMPerspectiveNotificationChannelsDTO> perspectiveNotificationChannels = new ArrayList<>();
    notificationSettings.forEach(notificationSetting
        -> perspectiveNotificationChannels.add(
            CCMPerspectiveNotificationChannelsDTO.builder()
                .perspectiveId(notificationSetting.getPerspectiveId())
                .perspectiveName(perspectiveIdToNameMapping.get(notificationSetting.getPerspectiveId()))
                .channels(notificationSetting.getChannels())
                .build()));
    return perspectiveNotificationChannels;
  }

  @Override
  public boolean delete(String perspectiveId, String accountId) {
    return notificationsDao.delete(perspectiveId, accountId);
  }
}
