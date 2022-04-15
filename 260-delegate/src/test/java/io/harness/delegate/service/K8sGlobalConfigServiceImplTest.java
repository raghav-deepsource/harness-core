/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
public class K8sGlobalConfigServiceImplTest {
  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getHelmPath() {
    final K8sGlobalConfigServiceImpl k8sGlobalConfigService = new K8sGlobalConfigServiceImpl();

    assertThat(k8sGlobalConfigService.getHelmPath(null)).isNotNull();
    assertThat(k8sGlobalConfigService.getHelmPath(HelmVersion.V3)).isNotNull();
    assertThat(k8sGlobalConfigService.getHelmPath(HelmVersion.V2)).isNotNull();
  }
}
