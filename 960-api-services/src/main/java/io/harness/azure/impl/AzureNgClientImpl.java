/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureClient;
import io.harness.azure.client.AzureNgClient;
import io.harness.azure.model.AzureNGConfig;
import io.harness.azure.model.AzureNGInheritDelegateCredentialsConfig;
import io.harness.azure.model.AzureNGManualCredentialsConfig;
import io.harness.exception.AzureConfigException;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDP)
@Singleton
@Slf4j
public class AzureNgClientImpl extends AzureClient implements AzureNgClient {
  @Override
  public void validateAzureConnection(AzureNGConfig azureNGConfig) {
    try {
      if (azureNGConfig instanceof AzureNGManualCredentialsConfig) {
        AzureNGManualCredentialsConfig azureConfig = (AzureNGManualCredentialsConfig) azureNGConfig;
        getAzureClientNg(azureConfig, null);
        if (log.isDebugEnabled()) {
          log.debug("Azure connection validated for clientId {} ", azureConfig.getClientId());
        }
      } else if (azureNGConfig instanceof AzureNGInheritDelegateCredentialsConfig) {
        AzureNGInheritDelegateCredentialsConfig azureConfig = (AzureNGInheritDelegateCredentialsConfig) azureNGConfig;
        getAzureClientNg(azureConfig, null);
        if (log.isDebugEnabled()) {
          String message = azureConfig.isUserAssignedManagedIdentity()
              ? String.format("UserAssigned MSI [%s]", azureConfig.getClientId())
              : "SystemAssigned MSI";
          log.debug("Azure connection validated for " + message);
        }
      } else {
        throw new AzureConfigException(
            String.format("AzureNGConfig not of expected type [%s]", azureNGConfig.getClass().getName()));
      }
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
  }
}
