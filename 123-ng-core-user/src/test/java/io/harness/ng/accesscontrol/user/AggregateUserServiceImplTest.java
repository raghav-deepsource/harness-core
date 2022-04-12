/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.accesscontrol.user;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.user.remote.dto.UserAggregateDTO;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PL)
public class AggregateUserServiceImplTest extends CategoryTest {
  @Mock NgUserService ngUserService;
  @Inject @InjectMocks private AggregateUserServiceImpl aggregateUserService;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetAggregatedUser_userNotAtScope() {
    String userId = "userId";
    Scope scope = Scope.builder()
                      .accountIdentifier("accountIdentifier")
                      .orgIdentifier("orgIdentifier")
                      .projectIdentifier("projectIdentifier")
                      .build();
    doReturn(false).when(ngUserService).isUserAtScope(userId, scope);
    Optional<UserAggregateDTO> userAggregateDTO = aggregateUserService.getAggregatedUser(scope, userId);
    assertThat(userAggregateDTO.isPresent()).isFalse();
    verify(ngUserService, times(1)).isUserAtScope(userId, scope);
    verify(ngUserService, times(0)).getUserMetadata(userId);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetAggregatedUser_userDataNotPresent() {
    String userId = "userId";
    Scope scope = Scope.builder()
                      .accountIdentifier("accountIdentifier")
                      .orgIdentifier("orgIdentifier")
                      .projectIdentifier("projectIdentifier")
                      .build();
    doReturn(true).when(ngUserService).isUserAtScope(userId, scope);
    doReturn(Optional.empty()).when(ngUserService).getUserMetadata(userId);
    Optional<UserAggregateDTO> userAggregateDTO = aggregateUserService.getAggregatedUser(scope, userId);
    assertThat(userAggregateDTO.isPresent()).isFalse();
    verify(ngUserService, times(1)).isUserAtScope(userId, scope);
    verify(ngUserService, times(1)).getUserMetadata(userId);
  }
}
