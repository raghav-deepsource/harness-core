/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.k8s.resources.azure.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.azure.AzureHelperService;
import io.harness.delegate.beans.connector.azureconnector.AzureAdditionalParams;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureTaskParams;
import io.harness.delegate.beans.connector.azureconnector.AzureTaskType;
import io.harness.delegate.beans.connector.azureconnector.response.AzureClustersResponse;
import io.harness.delegate.beans.connector.azureconnector.response.AzureResourceGroupsResponse;
import io.harness.delegate.beans.connector.azureconnector.response.AzureSubscriptionsResponse;
import io.harness.ng.core.BaseNGAccess;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@OwnedBy(HarnessTeam.CDP)
public class AzureResourceServiceImpl implements AzureResourceService {
  @Inject AzureHelperService azureHelperService;

  @Override
  public Map<String, String> getSubscriptions(
      IdentifierRef connectorRef, String orgIdentifier, String projectIdentifier) {
    AzureConnectorDTO connector = azureHelperService.getConnector(connectorRef);
    BaseNGAccess baseNGAccess =
        azureHelperService.getBaseNGAccess(connectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails = azureHelperService.getEncryptionDetails(connector, baseNGAccess);
    AzureTaskParams azureTaskParamsTaskParams = AzureTaskParams.builder()
                                                    .azureTaskType(AzureTaskType.LIST_SUBSCRIPTIONS)
                                                    .azureConnector(connector)
                                                    .encryptionDetails(encryptionDetails)
                                                    .delegateSelectors(connector.getDelegateSelectors())
                                                    .build();

    AzureSubscriptionsResponse subscriptionResponse = (AzureSubscriptionsResponse) azureHelperService.executeSyncTask(
        azureTaskParamsTaskParams, baseNGAccess, "Azure list subscriptions task failure due to error");
    return subscriptionResponse.getSubscriptions();
  }

  @Override
  public List<String> getResourceGroups(
      IdentifierRef connectorRef, String orgIdentifier, String projectIdentifier, String subscriptionId) {
    AzureConnectorDTO connector = azureHelperService.getConnector(connectorRef);
    BaseNGAccess baseNGAccess =
        azureHelperService.getBaseNGAccess(connectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails = azureHelperService.getEncryptionDetails(connector, baseNGAccess);

    Map<AzureAdditionalParams, String> additionalParams = new HashMap<>();
    additionalParams.put(AzureAdditionalParams.SUBSCRIPTION_ID, subscriptionId);

    AzureTaskParams azureTaskParamsTaskParams = AzureTaskParams.builder()
                                                    .azureTaskType(AzureTaskType.LIST_RESOURCE_GROUPS)
                                                    .azureConnector(connector)
                                                    .encryptionDetails(encryptionDetails)
                                                    .delegateSelectors(connector.getDelegateSelectors())
                                                    .additionalParams(additionalParams)
                                                    .build();

    AzureResourceGroupsResponse resourceGroupsResponse =
        (AzureResourceGroupsResponse) azureHelperService.executeSyncTask(
            azureTaskParamsTaskParams, baseNGAccess, "Azure list resource groups task failure due to error");
    return resourceGroupsResponse.getResourceGroups();
  }

  @Override
  public List<String> getClusters(IdentifierRef connectorRef, String orgIdentifier, String projectIdentifier,
      String subscriptionId, String resourceGroup) {
    AzureConnectorDTO connector = azureHelperService.getConnector(connectorRef);
    BaseNGAccess baseNGAccess =
        azureHelperService.getBaseNGAccess(connectorRef.getAccountIdentifier(), orgIdentifier, projectIdentifier);
    List<EncryptedDataDetail> encryptionDetails = azureHelperService.getEncryptionDetails(connector, baseNGAccess);

    Map<AzureAdditionalParams, String> additionalParams = new HashMap<>();
    additionalParams.put(AzureAdditionalParams.SUBSCRIPTION_ID, subscriptionId);
    additionalParams.put(AzureAdditionalParams.RESOURCE_GROUP, resourceGroup);
    AzureTaskParams azureTaskParamsTaskParams = AzureTaskParams.builder()
                                                    .azureTaskType(AzureTaskType.LIST_CLUSTERS)
                                                    .azureConnector(connector)
                                                    .encryptionDetails(encryptionDetails)
                                                    .delegateSelectors(connector.getDelegateSelectors())
                                                    .additionalParams(additionalParams)
                                                    .build();

    AzureClustersResponse clustersResponse = (AzureClustersResponse) azureHelperService.executeSyncTask(
        azureTaskParamsTaskParams, baseNGAccess, "Azure list cluster task failure due to error");
    return clustersResponse.getClusters();
  }
}
