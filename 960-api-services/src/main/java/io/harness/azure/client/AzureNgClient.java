/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.client;

import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.azure.model.AzureNGConfig;
import io.harness.azure.response.AcrRegistriesDTO;
import io.harness.azure.response.AcrRepositoriesDTO;
import io.harness.azure.response.AzureClustersDTO;
import io.harness.azure.response.AzureResourceGroupsDTO;
import io.harness.azure.response.AzureSubscriptionsDTO;

import java.util.List;

public interface AzureNgClient {
  /**
   * Validate azure connection with a provided clientId, tenantId, secret and environment type. Will throw exception if
   *  connection can't be made
   * @param azureNGConfig all information required for Azure connection
   */
  void validateAzureConnection(AzureNGConfig azureNGConfig);

  /**
   * List all subscriptions for clientId and tenantId
   * @param azureNGConfig
   * @return subscriptions
   */
  AzureSubscriptionsDTO getSubscriptionsNG(AzureNGConfig azureNGConfig);

  /**
   * List all resource groups by subscriptionId
   * @param azureNGConfig
   * @param subscriptionId
   * @return list of resource groups
   */
  AzureResourceGroupsDTO listResourceGroups(AzureNGConfig azureNGConfig, String subscriptionId);

  /**
   * List all container registries by subscriptionId
   * @param azureNGConfig
   * @param subscriptionId
   * @return list of container registries
   */
  AcrRegistriesDTO listContainerRegistries(AzureNGConfig azureNGConfig, String subscriptionId);

  /**
   * Returns list of all cluster for a subscription and resource group
   * @param azureNGConfig
   * @param subscriptionId
   * @param resourceGroup
   * @return list of clusters
   */
  AzureClustersDTO listClusters(AzureNGConfig azureNGConfig, String subscriptionId, String resourceGroup);

  /**
   * Returns list of all repositories in container registry
   * @param azureNGConfig
   * @param subscriptionId
   * @param containerRegistry
   * @return list of repositories
   */
  AcrRepositoriesDTO listRepositories(AzureNGConfig azureNGConfig, String subscriptionId, String containerRegistry);

  /**
   * List all build details for specified repository in registry under given subscription
   * @param azureNGConfig
   * @param subscriptionId
   * @param registryName
   * @param repositoryName
   * @return list of build details
   */
  List<BuildDetailsInternal> listBuildDetails(
      AzureNGConfig azureNGConfig, String subscriptionId, String registryName, String repositoryName);
}
