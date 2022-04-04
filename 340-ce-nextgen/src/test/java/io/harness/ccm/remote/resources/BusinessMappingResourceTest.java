/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.remote.resources;

import static io.harness.rule.OwnerRule.SAHILDEEP;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.views.businessMapping.entities.BusinessMapping;
import io.harness.ccm.views.businessMapping.entities.CostTarget;
import io.harness.ccm.views.businessMapping.entities.SharedCost;
import io.harness.ccm.views.businessMapping.entities.SharedCostSplit;
import io.harness.ccm.views.businessMapping.entities.SharingStrategy;
import io.harness.ccm.views.businessMapping.service.intf.BusinessMappingService;
import io.harness.ccm.views.entities.ViewCondition;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BusinessMappingResourceTest extends CategoryTest {
  @Mock private BusinessMappingService businessMappingService;
  @InjectMocks private BusinessMappingResource businessMappingResource;
  private BusinessMapping businessMapping;

  @Before
  public void setUp() {
    businessMapping = BusinessMappingHelper.getBusinessMapping(UUID.randomUUID().toString());
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testSave() {
    when(businessMappingService.save(any(BusinessMapping.class))).thenReturn(businessMapping);
    final RestResponse<BusinessMapping> response =
        businessMappingResource.save(BusinessMappingHelper.TEST_ACCOUNT_ID, businessMapping);
    verify(businessMappingService).save(businessMapping);
    assertThat(response.getResource()).isEqualTo(businessMapping);
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testList() {
    final List<BusinessMapping> businessMappings = Collections.singletonList(businessMapping);
    when(businessMappingService.list(BusinessMappingHelper.TEST_ACCOUNT_ID)).thenReturn(businessMappings);
    final RestResponse<List<BusinessMapping>> response =
        businessMappingResource.list(BusinessMappingHelper.TEST_ACCOUNT_ID);
    verify(businessMappingService).list(BusinessMappingHelper.TEST_ACCOUNT_ID);
    assertThat(response.getResource()).isEqualTo(businessMappings);
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testGet() {
    when(businessMappingService.get(BusinessMappingHelper.TEST_ID, BusinessMappingHelper.TEST_ACCOUNT_ID))
        .thenReturn(businessMapping);
    final RestResponse<BusinessMapping> response =
        businessMappingResource.get(BusinessMappingHelper.TEST_ACCOUNT_ID, BusinessMappingHelper.TEST_ID);
    verify(businessMappingService).get(BusinessMappingHelper.TEST_ID, BusinessMappingHelper.TEST_ACCOUNT_ID);
    assertThat(response.getResource()).isEqualTo(businessMapping);
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testUpdate() {
    when(businessMappingService.update(businessMapping)).thenReturn(businessMapping);
    final RestResponse<String> response =
        businessMappingResource.update(BusinessMappingHelper.TEST_ACCOUNT_ID, businessMapping);
    verify(businessMappingService).update(businessMapping);
    assertThat(response.getResource()).isEqualTo("Successfully updated the Business Mapping");
  }

  @Test
  @Owner(developers = SAHILDEEP)
  @Category(UnitTests.class)
  public void testDelete() {
    when(businessMappingService.delete(BusinessMappingHelper.TEST_ID, BusinessMappingHelper.TEST_ACCOUNT_ID))
        .thenReturn(true);
    final RestResponse<String> response =
        businessMappingResource.delete(BusinessMappingHelper.TEST_ACCOUNT_ID, BusinessMappingHelper.TEST_ID);
    verify(businessMappingService).delete(BusinessMappingHelper.TEST_ID, BusinessMappingHelper.TEST_ACCOUNT_ID);
    assertThat(response.getResource()).isEqualTo("Successfully deleted the Business Mapping");
  }

  static final class BusinessMappingHelper {
    public static final String TEST_ID = UUID.randomUUID().toString();
    public static final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID";
    public static final String TEST_NAME_1 = "TEST_NAME_1";
    public static final String TEST_NAME_2 = "TEST_NAME_2";

    private BusinessMappingHelper() {}

    public static BusinessMapping getBusinessMapping(final String uuid) {
      return BusinessMapping.builder()
          .uuid(uuid)
          .accountId(TEST_ACCOUNT_ID)
          .name(TEST_NAME_1)
          .costTargets(getCostTargets())
          .sharedCosts(getSharedCosts())
          .build();
    }

    private static CostTarget getCostTarget(final String name, final List<ViewRule> rules) {
      return CostTarget.builder().name(name).rules(rules).build();
    }

    @NotNull
    private static List<CostTarget> getCostTargets() {
      final List<ViewRule> rules = getRules();
      return Stream.of(getCostTarget(TEST_NAME_1, rules), getCostTarget(TEST_NAME_2, rules))
          .collect(Collectors.toList());
    }

    private static SharedCost getSharedCost(final String name, final List<ViewRule> rules,
        final SharingStrategy strategy, final List<SharedCostSplit> splits) {
      return SharedCost.builder().name(name).rules(rules).strategy(strategy).splits(splits).build();
    }

    @NotNull
    private static List<SharedCost> getSharedCosts() {
      final List<ViewRule> rules = getRules();
      final List<SharedCostSplit> splits = getSharedCostSplits();
      return Stream
          .of(getSharedCost(TEST_NAME_1, rules, SharingStrategy.FIXED, splits),
              getSharedCost(TEST_NAME_2, rules, SharingStrategy.PROPORTIONAL, splits))
          .collect(Collectors.toList());
    }

    private static SharedCostSplit getSharedCostSplit(
        final String costTargetName, final double percentageContribution) {
      return SharedCostSplit.builder()
          .costTargetName(costTargetName)
          .percentageContribution(percentageContribution)
          .build();
    }

    @NotNull
    private static List<SharedCostSplit> getSharedCostSplits() {
      return Stream.of(getSharedCostSplit(TEST_NAME_1, 10.0), getSharedCostSplit(TEST_NAME_2, 90.0))
          .collect(Collectors.toList());
    }

    @NotNull
    private static List<ViewRule> getRules() {
      final List<ViewCondition> viewConditions = Collections.singletonList(ViewIdCondition.builder().build());
      return Collections.singletonList(ViewRule.builder().viewConditions(viewConditions).build());
    }
  }
}
