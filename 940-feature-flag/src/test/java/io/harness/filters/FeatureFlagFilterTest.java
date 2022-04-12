/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.ff.FeatureFlagService;
import io.harness.ff.filters.EnumFeatureFlagFilter;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class FeatureFlagFilterTest extends CategoryTest {
  @Mock private FeatureFlagService featureFlagService;
  @InjectMocks private EnumFeatureFlagFilter featureFlagFilterService;

  @Before
  public void setUp() throws Exception {
    featureFlagFilterService.put(FeatureName.SSH_NG, Sets.newHashSet(WeekdaysEnum.SATURDAY, WeekdaysEnum.SUNDAY));
  }

  @Test
  @Owner(developers = OwnerRule.BOJAN)
  @Category(UnitTests.class)
  public void testFilterEnumFFEnabled() {
    doReturn(true).when(featureFlagService).isEnabled(any(), any());
    List<WeekdaysEnum> types = Arrays.stream(WeekdaysEnum.values())
                                   .filter(featureFlagFilterService.filter("accountId", FeatureName.SSH_NG))
                                   .collect(Collectors.toList());

    assertThat(types.size()).isEqualTo(7);
  }
  @Test
  @Owner(developers = OwnerRule.BOJAN)
  @Category(UnitTests.class)
  public void testFilterEnumFFDisabled() {
    doReturn(false).when(featureFlagService).isEnabled(any(), any());
    List<WeekdaysEnum> types = Arrays.stream(WeekdaysEnum.values())
                                   .filter(featureFlagFilterService.filter("accountId", FeatureName.SSH_NG))
                                   .collect(Collectors.toList());

    assertThat(types.size()).isEqualTo(5);
  }
}
