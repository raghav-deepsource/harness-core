/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.events;

import static io.harness.audit.ResourceTypeConstants.ENVIRONMENT_GROUP;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.ng.core.ProjectScope;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceScope;
import io.harness.ng.core.events.OutboxEventConstants;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EnvironmentGroupCreateEventTest extends CategoryTest {
  private String ACC_ID = "accId";
  private String ORG_ID = "orgId";
  private String PRO_ID = "proId";
  private String ENV_GROUP_ID = "envGroupId";

  private EnvironmentGroupCreateEvent createEvent;
  @Before
  public void before() {
    createEvent = new EnvironmentGroupCreateEvent(ACC_ID, ORG_ID, PRO_ID,
        EnvironmentGroupEntity.builder()
            .accountId(ACC_ID)
            .orgIdentifier(ORG_ID)
            .projectIdentifier(PRO_ID)
            .identifier(ENV_GROUP_ID)
            .build());
  }
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetResource() {
    Resource resource = createEvent.getResource();
    assertThat(resource.getIdentifier()).isEqualTo(ENV_GROUP_ID);
    assertThat(resource.getType()).isEqualTo(ENVIRONMENT_GROUP);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void getEventType() {
    assertThat(createEvent.getEventType()).isEqualTo(OutboxEventConstants.ENVIRONMENT_GROUP_CREATED);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetResourceScope() {
    ResourceScope resourceScope = createEvent.getResourceScope();
    assertThat(resourceScope.getScope()).isEqualTo("project");

    ProjectScope projectScope = (ProjectScope) resourceScope;
    assertThat(projectScope.getAccountIdentifier()).isEqualTo(ACC_ID);
    assertThat(projectScope.getOrgIdentifier()).isEqualTo(ORG_ID);
    assertThat(projectScope.getProjectIdentifier()).isEqualTo(PRO_ID);
  }
}
