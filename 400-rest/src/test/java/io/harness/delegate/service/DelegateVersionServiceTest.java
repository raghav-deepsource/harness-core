/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.beans.FeatureName.USE_IMMUTABLE_DELEGATE;
import static io.harness.delegate.beans.DelegateType.DOCKER;
import static io.harness.delegate.beans.DelegateType.KUBERNETES;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.AccountVersionOverride;
import io.harness.delegate.beans.AccountVersionOverride.AccountVersionOverrideKeys;
import io.harness.delegate.service.intfc.DelegateRingService;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;

import software.wings.app.MainConfiguration;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DelegateVersionServiceTest {
  public static final String ACCOUNT_ID = "accountId";
  public static final String HARNESS_OVERRIDE_TAG = "harness/delegate:override";
  public static final String HARNESS_DELEGATE_RING_IMAGE = "harness/delegate:ring";
  public static final String HARNESS_DELEGATE_PORTAL_IMAGE = "harness/delegate:portal";
  public static final String HARNESS_UPGRADER_RING_IMAGE = "harness/upgrader:ring";
  public static final String HARNESS_UPGRADER_PORTAL_IMAGE = "harness/upgrader:portal";
  public static final String DEFAULT_DELEGATE_IMAGE = "harness/delegate:latest";
  public static final String DEFAULT_UPGRADER_IMAGE = "harness/upgrader:latest";
  public static final List<String> DELEGATE_JAR_RING = ImmutableList.of("delegate1.jar", "delegate2.jar");
  public static final List<String> WATCHER_JAR_RING = ImmutableList.of("watcher1.jar", "watcher2.jar");
  public static final ImmutableList<String> ACCOUNT_DELEGATE_JAR = ImmutableList.of("delegate.jar");
  public static final ImmutableList<String> ACCOUNT_WATCHER_JAR = ImmutableList.of("watcher.jar");

  private DelegateVersionService underTest;
  @Mock private DelegateRingService delegateRingService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private MainConfiguration managerConfig;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private HPersistence persistence;

  @Before
  public void setUp() {
    underTest = spy(new DelegateVersionService(delegateRingService, featureFlagService, managerConfig, persistence));
  }

  @Test
  @Category(UnitTests.class)
  public void whenAllDelegateOverridesThenUseAccount() {
    final AccountVersionOverride override =
        AccountVersionOverride.builder(ACCOUNT_ID).delegateImageTag(HARNESS_OVERRIDE_TAG).build();

    mockDelegateOverrides(override, true, true, true);
    assertOverrides(HARNESS_OVERRIDE_TAG, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenDelegateImageTagAccountOverrideThenUseIt() {
    final AccountVersionOverride override =
        AccountVersionOverride.builder(ACCOUNT_ID).delegateImageTag(HARNESS_OVERRIDE_TAG).build();

    mockDelegateOverrides(override, false, false, false);
    assertOverrides(HARNESS_OVERRIDE_TAG, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenEmptyDelegateImageTagAccountOverrideThenUseDefault() {
    final AccountVersionOverride override = AccountVersionOverride.builder(ACCOUNT_ID).delegateImageTag(EMPTY).build();

    mockDelegateOverrides(override, false, false, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenDelegateImageTagRingOverrideThenUseIt() {
    mockDelegateOverrides(null, true, false, true);
    assertOverrides(HARNESS_DELEGATE_RING_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenDelegateImageTagRingOverrideButNotImmutableThenUseDefault() {
    mockDelegateOverrides(null, true, false, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenDelegateImageTagRingOverrideAndImmutableButNotK8SThenUseDefault() {
    mockDelegateOverrides(null, true, false, true);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), DOCKER);
  }

  @Test
  @Category(UnitTests.class)
  public void whenDelegateImageTagConfigOverrideThenUseIt() {
    mockDelegateOverrides(null, false, true, false);
    assertOverrides(HARNESS_DELEGATE_PORTAL_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenNoDelegateOverrideUseDefault() {
    mockDelegateOverrides(null, false, false, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenAllUpgraderOverridesThenUseAccount() {
    final AccountVersionOverride override =
        AccountVersionOverride.builder(ACCOUNT_ID).upgraderImageTag(HARNESS_OVERRIDE_TAG).build();

    mockUpgraderOverrides(override, true, true, true);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, HARNESS_OVERRIDE_TAG, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenUpgraderImageTagAccountOverrideThenUseIt() {
    final AccountVersionOverride override =
        AccountVersionOverride.builder(ACCOUNT_ID).upgraderImageTag(HARNESS_OVERRIDE_TAG).build();

    mockUpgraderOverrides(override, true, false, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, HARNESS_OVERRIDE_TAG, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenEmptyUpgraderImageTagAccountOverrideThenUseDefault() {
    final AccountVersionOverride override = AccountVersionOverride.builder(ACCOUNT_ID).upgraderImageTag(EMPTY).build();

    mockUpgraderOverrides(override, true, false, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenUpgraderImageTagRingOverrideThenUseIt() {
    mockUpgraderOverrides(null, true, false, true);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, HARNESS_UPGRADER_RING_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenUpgraderImageTagRingOverrideButNotImmutableThenUseDefault() {
    mockUpgraderOverrides(null, true, false, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenUpgraderImageTagRingOverrideAndImmutableButNotK8SThenUseDefault() {
    mockUpgraderOverrides(null, true, false, true);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), DOCKER);
  }

  @Test
  @Category(UnitTests.class)
  public void whenUpgraderImageTagConfigOverrideThenUseIt() {
    mockUpgraderOverrides(null, false, true, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, HARNESS_UPGRADER_PORTAL_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenNoUpgraderOverrideUseDefault() {
    mockUpgraderOverrides(null, false, false, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenAllDelegateJarOverrideThenUseAccount() {
    final AccountVersionOverride override =
        AccountVersionOverride.builder(ACCOUNT_ID).delegateJarVersions(ACCOUNT_DELEGATE_JAR).build();

    mockDelegateJarOverrides(override, true);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, ACCOUNT_DELEGATE_JAR, emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenAccountDelegateJarOverrideThenUseIt() {
    final AccountVersionOverride override =
        AccountVersionOverride.builder(ACCOUNT_ID).delegateJarVersions(ACCOUNT_DELEGATE_JAR).build();

    mockDelegateJarOverrides(override, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, ACCOUNT_DELEGATE_JAR, emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenRingDelegateJarOverrideThenUseIt() {
    mockDelegateJarOverrides(null, true);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, DELEGATE_JAR_RING, emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenNoDelegateJarOverrideThenUseEmpty() {
    mockDelegateJarOverrides(null, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenAllWatcherJarOverrideThenUseAccount() {
    final AccountVersionOverride override =
        AccountVersionOverride.builder(ACCOUNT_ID).watcherJarVersions(ACCOUNT_WATCHER_JAR).build();

    mockWatcherJarOverrides(override, true);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), ACCOUNT_WATCHER_JAR, KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenAccountWatcherJarOverrideThenUseIt() {
    final AccountVersionOverride override =
        AccountVersionOverride.builder(ACCOUNT_ID).watcherJarVersions(ACCOUNT_WATCHER_JAR).build();

    mockWatcherJarOverrides(override, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), ACCOUNT_WATCHER_JAR, KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenRingWatcherJarOverrideThenUseIt() {
    mockWatcherJarOverrides(null, true);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), WATCHER_JAR_RING, KUBERNETES);
  }

  @Test
  @Category(UnitTests.class)
  public void whenNoWatcherJarOverrideThenUseEmpty() {
    mockWatcherJarOverrides(null, false);
    assertOverrides(DEFAULT_DELEGATE_IMAGE, DEFAULT_UPGRADER_IMAGE, emptyList(), emptyList(), KUBERNETES);
  }

  private void mockDelegateOverrides(final AccountVersionOverride override, final boolean isRingDelegateOverride,
      final boolean isPortalDelegateOverride, final boolean isImmutableDelegate) {
    when(persistence.createQuery(AccountVersionOverride.class)
             .filter(AccountVersionOverrideKeys.accountId, ACCOUNT_ID)
             .get())
        .thenReturn(override);

    when(featureFlagService.isEnabled(USE_IMMUTABLE_DELEGATE, ACCOUNT_ID)).thenReturn(isImmutableDelegate);
    if (isRingDelegateOverride) {
      when(delegateRingService.getDelegateImageTag(ACCOUNT_ID)).thenReturn(HARNESS_DELEGATE_RING_IMAGE);
    }

    when(managerConfig.getPortal().getDelegateDockerImage())
        .thenReturn(isPortalDelegateOverride ? HARNESS_DELEGATE_PORTAL_IMAGE : null);
    when(managerConfig.getPortal().getUpgraderDockerImage()).thenReturn(null);
  }

  private void mockUpgraderOverrides(final AccountVersionOverride override, final boolean isRingUpgraderOverride,
      final boolean isPortalUpgraderOverride, final boolean isImmutableDelegate) {
    when(persistence.createQuery(AccountVersionOverride.class)
             .filter(AccountVersionOverrideKeys.accountId, ACCOUNT_ID)
             .get())
        .thenReturn(override);

    when(featureFlagService.isEnabled(USE_IMMUTABLE_DELEGATE, ACCOUNT_ID)).thenReturn(isImmutableDelegate);
    if (isRingUpgraderOverride) {
      when(delegateRingService.getUpgraderImageTag(ACCOUNT_ID)).thenReturn(HARNESS_UPGRADER_RING_IMAGE);
    }

    when(managerConfig.getPortal().getDelegateDockerImage()).thenReturn(null);
    when(managerConfig.getPortal().getUpgraderDockerImage())
        .thenReturn(isPortalUpgraderOverride ? HARNESS_UPGRADER_PORTAL_IMAGE : null);
  }

  private void mockDelegateJarOverrides(final AccountVersionOverride override, final boolean isRingOverride) {
    when(persistence.createQuery(AccountVersionOverride.class)
             .filter(AccountVersionOverrideKeys.accountId, ACCOUNT_ID)
             .get())
        .thenReturn(override);

    when(featureFlagService.isEnabled(USE_IMMUTABLE_DELEGATE, ACCOUNT_ID)).thenReturn(false);

    if (isRingOverride) {
      when(delegateRingService.getDelegateVersions(ACCOUNT_ID)).thenReturn(DELEGATE_JAR_RING);
    }
    when(managerConfig.getPortal().getDelegateDockerImage()).thenReturn(null);
    when(managerConfig.getPortal().getUpgraderDockerImage()).thenReturn(null);
  }

  private void mockWatcherJarOverrides(final AccountVersionOverride override, final boolean isRingOverride) {
    when(persistence.createQuery(AccountVersionOverride.class)
             .filter(AccountVersionOverrideKeys.accountId, ACCOUNT_ID)
             .get())
        .thenReturn(override);

    when(featureFlagService.isEnabled(USE_IMMUTABLE_DELEGATE, ACCOUNT_ID)).thenReturn(false);

    if (isRingOverride) {
      when(delegateRingService.getWatcherVersions(ACCOUNT_ID)).thenReturn(WATCHER_JAR_RING);
    }
    when(managerConfig.getPortal().getDelegateDockerImage()).thenReturn(null);
    when(managerConfig.getPortal().getUpgraderDockerImage()).thenReturn(null);
  }

  private void assertOverrides(final String expectedDelegateTag, final String expectedUpgraderTag,
      final List<String> delegateJars, final List<String> watcherJars, final String delegateType) {
    final String actualDelegate = underTest.getDelegateImageTag(ACCOUNT_ID, delegateType);
    final String actualUpgrader = underTest.getUpgraderImageTag(ACCOUNT_ID, delegateType);
    final List<String> actualDelegateJar = underTest.getDelegateJarVersions(ACCOUNT_ID);
    final List<String> actualWatcherJar = underTest.getWatcherJarVersions(ACCOUNT_ID);

    assertThat(actualDelegate).isEqualTo(expectedDelegateTag);
    assertThat(actualUpgrader).isEqualTo(expectedUpgraderTag);
    assertThat(actualDelegateJar).isEqualTo(delegateJars);
    assertThat(actualWatcherJar).isEqualTo(watcherJars);
  }
}
