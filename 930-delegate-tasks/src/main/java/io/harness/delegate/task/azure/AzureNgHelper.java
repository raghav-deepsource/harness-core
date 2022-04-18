/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.azure;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.artifact.ArtifactUtilities;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorDescending;
import io.harness.azure.client.AzureNgClient;
import io.harness.azure.model.AzureNGConfig;
import io.harness.azure.model.AzureNGManualCredentialsConfig;
import io.harness.azure.response.AcrRegistriesDTO;
import io.harness.azure.response.AcrRepositoriesDTO;
import io.harness.azure.response.AzureClustersDTO;
import io.harness.azure.response.AzureResourceGroupsDTO;
import io.harness.azure.response.AzureSubscriptionsDTO;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.delegate.beans.azure.response.AzureClustersResponse;
import io.harness.delegate.beans.azure.response.AzureRegistriesResponse;
import io.harness.delegate.beans.azure.response.AzureRepositoriesResponse;
import io.harness.delegate.beans.azure.response.AzureResourceGroupsResponse;
import io.harness.delegate.beans.azure.response.AzureSubscriptionsResponse;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.task.artifacts.mappers.AcrRequestResponseMapper;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.AzureContainerRegistryException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.expression.RegexFunctor;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class AzureNgHelper {
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private AzureNgClient azureNgClient;
  @Inject private NGErrorHelper ngErrorHelper;

  public ConnectorValidationResult getConnectorValidationResult(
      List<EncryptedDataDetail> encryptedDataDetails, AzureConnectorDTO connectorDTO) {
    ConnectorValidationResult connectorValidationResult;
    try {
      AzureNGConfig azureNGConfig = AcrRequestResponseMapper.toAzureInternalConfig(connectorDTO.getCredential(),
          encryptedDataDetails, connectorDTO.getCredential().getAzureCredentialType(),
          connectorDTO.getAzureEnvironmentType(), secretDecryptionService);

      azureNgClient.validateAzureConnection(azureNGConfig);
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .status(ConnectivityStatus.SUCCESS)
                                      .testedAt(System.currentTimeMillis())
                                      .build();
    } catch (Exception e) {
      String errorMessage = e.getMessage();
      connectorValidationResult = ConnectorValidationResult.builder()
                                      .status(ConnectivityStatus.FAILURE)
                                      .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)))
                                      .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                                      .testedAt(System.currentTimeMillis())
                                      .build();
    }
    return connectorValidationResult;
  }

  public AzureSubscriptionsResponse listSubscriptions(
      List<EncryptedDataDetail> encryptionDetails, AzureConnectorDTO azureConnector) {
    AzureNGConfig azureConfig = AcrRequestResponseMapper.toAzureInternalConfig(azureConnector.getCredential(),
        encryptionDetails, azureConnector.getCredential().getAzureCredentialType(),
        azureConnector.getAzureEnvironmentType(), secretDecryptionService);

    AzureSubscriptionsResponse response;
    try {
      AzureSubscriptionsDTO subscriptions = azureNgClient.getSubscriptionsNG(azureConfig);
      response = AzureSubscriptionsResponse.builder()
                     .subscriptions(subscriptions)
                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                     .build();

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      response = AzureSubscriptionsResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                     .build();
    }
    return response;
  }

  public AzureResourceGroupsResponse listResourceGroups(
      List<EncryptedDataDetail> encryptionDetails, AzureConnectorDTO azureConnector, String subscriptionId) {
    AzureNGConfig azureConfig = AcrRequestResponseMapper.toAzureInternalConfig(azureConnector.getCredential(),
        encryptionDetails, azureConnector.getCredential().getAzureCredentialType(),
        azureConnector.getAzureEnvironmentType(), secretDecryptionService);
    AzureResourceGroupsResponse response;
    try {
      AzureResourceGroupsDTO resourceGroups = azureNgClient.listResourceGroups(azureConfig, subscriptionId);
      response = AzureResourceGroupsResponse.builder()
                     .resourceGroups(resourceGroups)
                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                     .build();

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      response = AzureResourceGroupsResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                     .build();
    }
    return response;
  }

  public AzureRegistriesResponse listContainerRegistries(
      List<EncryptedDataDetail> encryptionDetails, AzureConnectorDTO azureConnector, String subscriptionId) {
    AzureNGConfig azureConfig = AcrRequestResponseMapper.toAzureInternalConfig(azureConnector.getCredential(),
        encryptionDetails, azureConnector.getCredential().getAzureCredentialType(),
        azureConnector.getAzureEnvironmentType(), secretDecryptionService);

    AzureRegistriesResponse response;
    try {
      AcrRegistriesDTO containerRegistries = azureNgClient.listContainerRegistries(azureConfig, subscriptionId);
      response = AzureRegistriesResponse.builder()
                     .containerRegistries(containerRegistries)
                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                     .build();

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      response = AzureRegistriesResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                     .build();
    }
    return response;
  }

  public AzureClustersResponse listClusters(List<EncryptedDataDetail> encryptionDetails,
      AzureConnectorDTO azureConnector, String subscriptionId, String resourceGroup) {
    AzureNGConfig azureConfig = AcrRequestResponseMapper.toAzureInternalConfig(azureConnector.getCredential(),
        encryptionDetails, azureConnector.getCredential().getAzureCredentialType(),
        azureConnector.getAzureEnvironmentType(), secretDecryptionService);

    AzureClustersResponse response;
    try {
      AzureClustersDTO clusters = azureNgClient.listClusters(azureConfig, subscriptionId, resourceGroup);
      response = AzureClustersResponse.builder()
                     .clusters(clusters)
                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                     .build();

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      response = AzureClustersResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                     .build();
    }
    return response;
  }

  public AzureRepositoriesResponse listRepositories(List<EncryptedDataDetail> encryptionDetails,
      AzureConnectorDTO azureConnector, String subscriptionId, String containerRegistry) {
    AzureNGConfig azureConfig = AcrRequestResponseMapper.toAzureInternalConfig(azureConnector.getCredential(),
        encryptionDetails, azureConnector.getCredential().getAzureCredentialType(),
        azureConnector.getAzureEnvironmentType(), secretDecryptionService);

    AzureRepositoriesResponse response;
    try {
      AcrRepositoriesDTO repositories = azureNgClient.listRepositories(azureConfig, subscriptionId, containerRegistry);
      response = AzureRepositoriesResponse.builder()
                     .repositories(repositories)
                     .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                     .build();

    } catch (Exception e) {
      String errorMessage = e.getMessage();
      response = AzureRepositoriesResponse.builder()
                     .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                     .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                     .build();
    }
    return response;
  }

  public List<BuildDetailsInternal> getBuilds(
      AzureNGConfig azureNGConfig, String subscriptionId, String containerRegistry, String repository) {
    return azureNgClient.listBuildDetails(azureNGConfig, subscriptionId, containerRegistry, repository);
  }

  public BuildDetailsInternal getLastSuccessfulBuildFromRegex(
      AzureNGConfig azureNGConfig, String subscription, String registry, String repository, String tagRegex) {
    try {
      Pattern.compile(tagRegex);
    } catch (PatternSyntaxException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check tagRegex field in ACR artifact configuration.",
          String.format("TagRegex field contains an invalid regex value '%s'.", tagRegex),
          new AzureContainerRegistryException(e.getMessage()));
    }
    // Temporary check as currently azure client is supported only for secret text type authentication
    if (!(azureNGConfig instanceof AzureNGManualCredentialsConfig
            && ((AzureNGManualCredentialsConfig) azureNGConfig).getKey() != null)) {
      throw NestedExceptionUtils.hintWithExplanationException("Currently not supported",
          String.format(
              "Currently regex field could not be used with connector that uses Certificate or Managed Identity for authentication"),
          new AzureContainerRegistryException(
              String.format("Could not find an artifact tag that matches tagRegex '%s'", tagRegex)));
    }

    List<BuildDetailsInternal> builds = getBuilds(azureNGConfig, subscription, registry, repository);
    builds = builds.stream()
                 .filter(build -> new RegexFunctor().match(tagRegex, build.getNumber()))
                 .sorted(new BuildDetailsInternalComparatorDescending())
                 .collect(Collectors.toList());

    if (builds.isEmpty()) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check tagRegex field in ACR artifact configuration.",
          String.format(
              "Could not find any tags that match regex [%s] for ACR repository [%s] for %s subscription [%s] in registry [%s].",
              tagRegex, repository, subscription, registry),
          new AzureContainerRegistryException(
              String.format("Could not find an artifact tag that matches tagRegex '%s'", tagRegex)));
    }
    return builds.get(0);
  }

  public BuildDetailsInternal verifyBuildNumber(
      AzureNGConfig azureNGConfig, String subscription, String registry, String repository, String tag) {
    // Temporary check as currently azure client is supported only for secret text type authentication
    if (!(azureNGConfig instanceof AzureNGManualCredentialsConfig
            && ((AzureNGManualCredentialsConfig) azureNGConfig).getKey() != null)) {
      return getBuildDetailsInternalWhenCantCallAzure(registry, repository, tag);
    }

    List<BuildDetailsInternal> builds = getBuilds(azureNGConfig, subscription, registry, repository);
    builds = builds.stream().filter(build -> build.getNumber().equals(tag)).collect(Collectors.toList());

    if (builds.size() == 0) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check your ACR repository for artifact tag existence.",
          String.format(
              "Did not find any artifacts for tag [%s] in ACR repository [%s] for %s subscription [%s] in registry [%s].",
              tag, repository, subscription, registry),
          new AzureContainerRegistryException(String.format("Artifact tag '%s' not found.", tag)));
    } else if (builds.size() == 1) {
      return builds.get(0);
    }

    throw NestedExceptionUtils.hintWithExplanationException(
        "Please check your ACR repository for artifacts with same tag.",
        String.format(
            "Found multiple artifacts for tag [%s] in Artifactory repository [%s] for %s subscription [%s] in registry [%s].",
            tag, repository, subscription, registry),
        new AzureContainerRegistryException(
            String.format("Found multiple artifact tags '%s', but expected only one.", tag)));
  }

  private BuildDetailsInternal getBuildDetailsInternalWhenCantCallAzure(
      String registry, String repository, String tag) {
    String repositoryNameNormalized = ArtifactUtilities.trimSlashforwardChars(repository);
    String tagUrl = registry + ".azurecr.io/" + repositoryNameNormalized + "/";
    Map<String, String> metadata = new HashMap<>();
    metadata.put(ArtifactMetadataKeys.IMAGE, tagUrl + ":" + tag);
    metadata.put(ArtifactMetadataKeys.TAG, tag);
    return BuildDetailsInternal.builder()
        .number(tag)
        .buildUrl(tagUrl + ":" + tag)
        .metadata(metadata)
        .uiDisplayName("Tag# " + tag)
        .build();
  }
}
