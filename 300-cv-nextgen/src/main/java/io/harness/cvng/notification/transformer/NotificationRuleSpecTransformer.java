/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.notification.transformer;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.notification.beans.NotificationRuleDTO;
import io.harness.cvng.notification.beans.NotificationRuleSpec;
import io.harness.cvng.notification.entities.NotificationRule;

public abstract class NotificationRuleSpecTransformer<E extends NotificationRule, S extends NotificationRuleSpec> {
  public abstract E getEntity(ProjectParams projectParams, NotificationRuleDTO notificationRuleDTO);

  public final NotificationRuleDTO getDto(E notificationRule) {
    return NotificationRuleDTO.builder()
        .orgIdentifier(notificationRule.getOrgIdentifier())
        .projectIdentifier(notificationRule.getProjectIdentifier())
        .identifier(notificationRule.getIdentifier())
        .name(notificationRule.getName())
        .type(notificationRule.getType())
        .spec(getSpec(notificationRule))
        .enabled(notificationRule.isEnabled())
        .build();
  }

  protected abstract S getSpec(E notificationRule);
}
