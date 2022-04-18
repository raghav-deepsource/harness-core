/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.azure.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.artifact.ArtifactUtilities;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.azure.AzureClient;
import io.harness.azure.client.AcrNGRestClient;
import io.harness.azure.client.AzureManagementRestClient;
import io.harness.azure.client.AzureNgClient;
import io.harness.azure.model.AzureNGConfig;
import io.harness.azure.model.AzureNGInheritDelegateCredentialsConfig;
import io.harness.azure.model.AzureNGManualCredentialsConfig;
import io.harness.azure.response.AcrRegistriesDTO;
import io.harness.azure.response.AcrRegistryDTO;
import io.harness.azure.response.AcrRepositoriesDTO;
import io.harness.azure.response.AcrRepositoryDTO;
import io.harness.azure.response.AzureClusterDTO;
import io.harness.azure.response.AzureClustersDTO;
import io.harness.azure.response.AzureResourceGroupDTO;
import io.harness.azure.response.AzureResourceGroupsDTO;
import io.harness.azure.response.AzureSubscriptionDTO;
import io.harness.azure.response.AzureSubscriptionsDTO;
import io.harness.exception.AzureConfigException;
import io.harness.exception.AzureContainerRegistryException;
import io.harness.exception.NestedExceptionUtils;

import software.wings.helpers.ext.azure.AcrGetRepositoriesResponse;
import software.wings.helpers.ext.azure.AcrGetRepositoryTagsResponse;
import software.wings.helpers.ext.azure.AcrRegistry;
import software.wings.helpers.ext.azure.AcrRegistryListResult;
import software.wings.helpers.ext.azure.AcrRegistryProperties;

import com.google.inject.Singleton;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

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

  @Override
  public AzureSubscriptionsDTO getSubscriptionsNG(AzureNGConfig azureConfig) {
    return AzureSubscriptionsDTO.builder()
        .subscriptions(getAzureClientNg(azureConfig, null)
                           .subscriptions()
                           .list()
                           .stream()
                           .map(subscription
                               -> AzureSubscriptionDTO.builder()
                                      .subscriptionName(subscription.displayName())
                                      .subscriptionId(subscription.subscriptionId())
                                      .build())
                           .collect(toList()))
        .build();
  }

  @Override
  public AzureResourceGroupsDTO listResourceGroups(AzureNGConfig azureNGConfig, String subscriptionId) {
    return AzureResourceGroupsDTO.builder()
        .resourceGroups(getAzureClientNg(azureNGConfig, subscriptionId)
                            .resourceGroups()
                            .list()
                            .stream()
                            .map(HasName::name)
                            .map(resourceGroup -> AzureResourceGroupDTO.builder().resourceGroup(resourceGroup).build())
                            .collect(Collectors.toList()))
        .build();
  }

  private List<AcrRegistry> getContainerRegistries(AzureNGConfig azureConfig, String subscriptionId) {
    List<AcrRegistry> acrRegistries = new ArrayList<>();
    try {
      if (azureConfig instanceof AzureNGManualCredentialsConfig) {
        PagedList<Registry> registries = getAzureClientNg(azureConfig, subscriptionId).containerRegistries().list();
        for (Registry registry : registries) {
          acrRegistries.add(
              AcrRegistry.builder()
                  .name(registry.name())
                  .id(registry.id())
                  .properties(AcrRegistryProperties.builder().loginServer(registry.loginServerUrl()).build())
                  .build());
        }
      } else if (azureConfig instanceof AzureNGInheritDelegateCredentialsConfig) {
        AzureManagementRestClient azureManagementRestClient = getAzureManagementRestClient(
            ((AzureNGInheritDelegateCredentialsConfig) azureConfig).getAzureEnvironmentType());
        Response<AcrRegistryListResult> response = null;
        String token = getAzureNGBearerAuthToken(azureConfig);

        response = azureManagementRestClient.listACRRegistries(token, subscriptionId).execute();
        if (response.isSuccessful()) {
          acrRegistries.addAll(response.body().getValue());
        } else {
          String error = null;
          try {
            error = response.errorBody().string();
          } catch (Exception e) {
            error = String.format("Retrieving ACR registries has failed with response code %s", response.code());
          }
          throw new Exception(error);
        }
      }
    } catch (Exception e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Check ACR configuration and the Managed Identity permissions", "Failed to retrieve ACR registries",
          new AzureContainerRegistryException(e.getMessage()));
    }
    return acrRegistries;
  }

  @Override
  public AcrRegistriesDTO listContainerRegistries(AzureNGConfig azureConfig, String subscriptionId) {
    return AcrRegistriesDTO.builder()
        .registries(getContainerRegistries(azureConfig, subscriptionId)
                        .stream()
                        .map(registry -> AcrRegistryDTO.builder().registry(registry.getName()).build())
                        .collect(Collectors.toList()))
        .build();
  }

  @Override
  public AzureClustersDTO listClusters(AzureNGConfig azureNGConfig, String subscriptionId, String resourceGroup) {
    return AzureClustersDTO.builder()
        .clusters(getAzureClientNg(azureNGConfig, subscriptionId)
                      .kubernetesClusters()
                      .listByResourceGroup(resourceGroup)
                      .stream()
                      .map(HasName::name)
                      .map(cluster -> AzureClusterDTO.builder().cluster(cluster).build())
                      .collect(Collectors.toList()))
        .build();
  }

  @Override
  public AcrRepositoriesDTO listRepositories(
      AzureNGConfig azureNGConfig, String subscriptionId, String containerRegistry) {
    try {
      AcrRegistry registry = getContainerRegistryByName(azureNGConfig, subscriptionId, containerRegistry);
      AcrNGRestClient acrRestClient =
          getAzureRestClient(buildRepositoryHostUrl(registry.getProperties().getLoginServer()), AcrNGRestClient.class);
      String credentials = getCredentials(azureNGConfig);
      List<String> allRepositories = new ArrayList<>();
      String last = null;
      List<String> repositories;
      do {
        Response<AcrGetRepositoriesResponse> execute = acrRestClient.listRepositories(credentials, last).execute();
        AcrGetRepositoriesResponse body = execute.body();
        repositories = body.getRepositories();
        if (isNotEmpty(repositories)) {
          allRepositories.addAll(repositories);
          last = repositories.get(repositories.size() - 1);
        }
      } while (isNotEmpty(repositories));
      return AcrRepositoriesDTO.builder()
          .repositories(allRepositories.stream()
                            .distinct()
                            .map(repository -> AcrRepositoryDTO.builder().repository(repository).build())
                            .collect(toList()))
          .build();
    } catch (Exception e) {
      throw NestedExceptionUtils.hintWithExplanationException("Check Service Principal/Managed Identity permissions",
          String.format("Error occurred while getting repositories for subscriptionId/registryName: %s/%s",
              subscriptionId, containerRegistry),
          new AzureContainerRegistryException(e.getMessage()));
    }
  }

  private AcrRegistry getContainerRegistryByName(
      AzureNGConfig azureNGConfig, String subscriptionId, String containerRegistry) {
    return getContainerRegistries(azureNGConfig, subscriptionId)
        .stream()
        .filter(reg -> reg.getName().equalsIgnoreCase(containerRegistry))
        .findFirst()
        .orElseThrow(()
                         -> NestedExceptionUtils.hintWithExplanationException(
                             String.format("Check if the registry exists for the subscription %s", subscriptionId),
                             String.format("Failed to retrieve container registry with name: %s", containerRegistry),
                             new AzureContainerRegistryException("Failed to retrieve container registry")));
  }

  @Override
  public List<BuildDetailsInternal> listBuildDetails(
      AzureNGConfig azureNGConfig, String subscriptionId, String registryName, String repositoryName) {
    try {
      AcrRegistry registry = getContainerRegistryByName(azureNGConfig, subscriptionId, registryName);

      AcrNGRestClient acrRestClient =
          getAzureRestClient(buildRepositoryHostUrl(registry.getProperties().getLoginServer()), AcrNGRestClient.class);
      String credentials = getCredentials(azureNGConfig);

      Response<AcrGetRepositoryTagsResponse> response =
          acrRestClient.listRepositoryTags(credentials, repositoryName).execute();
      if (response.isSuccessful()) {
        List<String> tags = response.body().getTags();

        String repositoryNameNormalized = ArtifactUtilities.trimSlashforwardChars(repositoryName);
        String registryUrl = registry.getProperties().getLoginServer().toLowerCase();
        String imageUrl = registryUrl + "/" + repositoryNameNormalized;

        return tags.stream()
            .map(tag -> {
              Map<String, String> metadata = new HashMap<>();
              metadata.put(ArtifactMetadataKeys.IMAGE, imageUrl + ":" + tag);
              metadata.put(ArtifactMetadataKeys.TAG, tag);
              metadata.put(ArtifactMetadataKeys.REGISTRY_HOSTNAME, registryUrl);
              return BuildDetailsInternal.builder()
                  .number(tag)
                  .buildUrl(imageUrl + ":" + tag)
                  .metadata(metadata)
                  .uiDisplayName("Tag# " + tag)
                  .build();
            })
            .collect(toList());
      } else {
        String error = null;
        try {
          error = response.errorBody().string();
        } catch (Exception e) {
          error = String.format("Request to fetch repository tag has failed with response code %s", response.code());
        }
        throw new Exception(error);
      }
    } catch (Exception e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          String.format("Check if the ACR configuration is proper and if all information is correct."),
          String.format(
              "Error occurred while getting repositories from subscriptionId/registryName/repositoryName: %s/%s/%s",
              subscriptionId, registryName, repositoryName),
          new AzureContainerRegistryException(e.getMessage()));
    }
  }

  private String getCredentials(AzureNGConfig azureNGConfig) {
    if (azureNGConfig instanceof AzureNGManualCredentialsConfig) {
      return getRestAzureCredentials((AzureNGManualCredentialsConfig) azureNGConfig);
    } else {
      return getAzureNGBearerAuthToken(azureNGConfig);
    }
  }
}
