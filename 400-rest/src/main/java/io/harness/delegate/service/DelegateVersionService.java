/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.beans.FeatureName.USE_IMMUTABLE_DELEGATE;
import static io.harness.delegate.beans.DelegateType.CE_KUBERNETES;
import static io.harness.delegate.beans.DelegateType.KUBERNETES;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.beans.AccountVersionOverride;
import io.harness.delegate.beans.AccountVersionOverride.AccountVersionOverrideKeys;
import io.harness.delegate.service.intfc.DelegateRingService;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;

import software.wings.app.MainConfiguration;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections.CollectionUtils;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DelegateVersionService {
  public static final String DEFAULT_DELEGATE_IMAGE_TAG = "harness/delegate:latest";
  public static final String DEFAULT_UPGRADER_IMAGE_TAG = "harness/upgrader:latest";
  private final DelegateRingService delegateRingService;
  private final FeatureFlagService featureFlagService;
  private final MainConfiguration mainConfiguration;
  private final HPersistence persistence;

  public String getDelegateImageTag(final String accountId, final String delegateType) {
    final AccountVersionOverride accountVersionOverride = getVersionOverride(accountId);
    if (accountVersionOverride != null && isNotBlank(accountVersionOverride.getDelegateImageTag())) {
      return accountVersionOverride.getDelegateImageTag();
    }

    final String ringImage = delegateRingService.getDelegateImageTag(accountId);
    if (isImmutableDelegate(accountId, delegateType) && isNotBlank(ringImage)) {
      return ringImage;
    }

    final String managerConfigImage = mainConfiguration.getPortal().getDelegateDockerImage();
    if (isNotBlank(managerConfigImage)) {
      return managerConfigImage;
    }
    return DEFAULT_DELEGATE_IMAGE_TAG;
  }

  public String getUpgraderImageTag(final String accountId, final String delegateType) {
    final AccountVersionOverride accountVersionOverride = getVersionOverride(accountId);
    if (accountVersionOverride != null && isNotBlank(accountVersionOverride.getUpgraderImageTag())) {
      return accountVersionOverride.getUpgraderImageTag();
    }

    final String ringImage = delegateRingService.getUpgraderImageTag(accountId);
    if (isImmutableDelegate(accountId, delegateType) && isNotBlank(ringImage)) {
      return ringImage;
    }

    if (isNotBlank(mainConfiguration.getPortal().getUpgraderDockerImage())) {
      return mainConfiguration.getPortal().getUpgraderDockerImage();
    }
    return DEFAULT_UPGRADER_IMAGE_TAG;
  }

  public List<String> getDelegateJarVersions(final String accountId) {
    final AccountVersionOverride accountVersionOverride = getVersionOverride(accountId);
    if (accountVersionOverride != null && !CollectionUtils.isEmpty(accountVersionOverride.getDelegateJarVersions())) {
      return accountVersionOverride.getDelegateJarVersions();
    }

    final List<String> ringVersion = delegateRingService.getDelegateVersions(accountId);
    if (!CollectionUtils.isEmpty(ringVersion)) {
      return ringVersion;
    }

    return Collections.emptyList();
  }

  public List<String> getWatcherJarVersions(final String accountId) {
    final AccountVersionOverride accountVersionOverride = getVersionOverride(accountId);
    if (accountVersionOverride != null && !CollectionUtils.isEmpty(accountVersionOverride.getWatcherJarVersions())) {
      return accountVersionOverride.getWatcherJarVersions();
    }

    final List<String> ringVersion = delegateRingService.getWatcherVersions(accountId);
    if (!CollectionUtils.isEmpty(ringVersion)) {
      return ringVersion;
    }

    return Collections.emptyList();
  }

  private AccountVersionOverride getVersionOverride(final String accountId) {
    return persistence.createQuery(AccountVersionOverride.class)
        .filter(AccountVersionOverrideKeys.accountId, accountId)
        .get();
  }

  private boolean isImmutableDelegate(final String accountId, final String delegateType) {
    return featureFlagService.isEnabled(USE_IMMUTABLE_DELEGATE, accountId)
        && (KUBERNETES.equals(delegateType) || CE_KUBERNETES.equals(delegateType));
  }
}
