package io.harness.ccm.service.intf;

import io.harness.ccm.commons.entities.notifications.CCMNotificationSetting;
import io.harness.ccm.commons.entities.notifications.CCMPerspectiveNotificationChannelsDTO;

import java.util.List;

public interface CCMNotificationService {
  CCMNotificationSetting upsert(CCMNotificationSetting notificationSetting);
  CCMNotificationSetting get(String perspectiveId, String accountId);
  List<CCMPerspectiveNotificationChannelsDTO> list(String accountId);
  boolean delete(String perspectiveId, String accountId);
}
