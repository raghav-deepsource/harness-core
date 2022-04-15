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
import io.harness.cvng.notification.beans.SLONotificationRuleSpec;
import io.harness.cvng.notification.entities.SLONotificationRule;

public class SLONotificationRuleSpecTransformer
    extends NotificationRuleSpecTransformer<SLONotificationRule, SLONotificationRuleSpec> {
  @Override
  public SLONotificationRule getEntity(ProjectParams projectParams, NotificationRuleDTO notificationRuleDTO) {
    SLONotificationRuleSpec sloNotificationRuleSpec = (SLONotificationRuleSpec) notificationRuleDTO.getSpec();
    return SLONotificationRule.builder()
        .accountId(projectParams.getAccountIdentifier())
        .orgIdentifier(projectParams.getOrgIdentifier())
        .projectIdentifier(projectParams.getProjectIdentifier())
        .identifier(notificationRuleDTO.getIdentifier())
        .name(notificationRuleDTO.getName())
        .enabled(notificationRuleDTO.isEnabled())
        .type(NotificationRuleType.SLO)
        .errorBudgetRemainingPercentageThreshold(sloNotificationRuleSpec.getErrorBudgetRemainingPercentageThreshold())
        .build();
  }

  @Override
  protected SLONotificationRuleSpec getSpec(SLONotificationRule notificationRule) {
    return SLONotificationRuleSpec.builder()
        .errorBudgetRemainingPercentageThreshold(notificationRule.getErrorBudgetRemainingPercentageThreshold())
        .build();
  }
}
