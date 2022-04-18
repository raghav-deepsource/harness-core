/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.notification.transformer;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.notification.beans.NotificationRuleDTO;
import io.harness.cvng.notification.beans.NotificationRuleType;
import io.harness.cvng.notification.entities.NotificationRule;

import com.google.inject.Inject;
import java.util.Map;

public class NotificationRuleEntityAndDTOTransformer {
  @Inject
  private Map<NotificationRuleType, NotificationRuleSpecTransformer>
      notificationRuleTypeNotificationRuleSpecTransformerMap;

  public NotificationRule getEntity(ProjectParams projectParams, NotificationRuleDTO notificationRuleDTO) {
    NotificationRuleSpecTransformer notificationRuleSpecTransformer =
        notificationRuleTypeNotificationRuleSpecTransformerMap.get(notificationRuleDTO.getType());
    return notificationRuleSpecTransformer.getEntity(projectParams, notificationRuleDTO);
  }

  public NotificationRuleDTO getDto(NotificationRule notificationRule) {
    NotificationRuleSpecTransformer notificationRuleSpecTransformer =
        notificationRuleTypeNotificationRuleSpecTransformerMap.get(notificationRule.getType());
    return notificationRuleSpecTransformer.getDto(notificationRule);
  }
}
