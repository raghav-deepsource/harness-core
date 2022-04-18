/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.artifacts.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.azure.AzureEnvironmentType;
import io.harness.azure.model.AzureNGConfig;
import io.harness.azure.model.AzureNGInheritDelegateCredentialsConfig;
import io.harness.azure.model.AzureNGManualCredentialsConfig;
import io.harness.delegate.beans.connector.azureconnector.AzureClientKeyCertDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientSecretKeyDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureInheritFromDelegateDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthSADTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthUADTO;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureUserAssignedMSIAuthDTO;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.azure.AcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.azure.AcrArtifactDelegateResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CDP)
public class AcrRequestResponseMapper {
  public AzureNGConfig toAzureInternalConfig(
      AcrArtifactDelegateRequest acrArtifactDelegateRequest, SecretDecryptionService secretDecryptionService) {
    AzureCredentialDTO credential = acrArtifactDelegateRequest.getAzureConnectorDTO().getCredential();
    List<EncryptedDataDetail> encryptedDataDetails = acrArtifactDelegateRequest.getEncryptedDataDetails();
    AzureCredentialType azureCredentialType = credential.getAzureCredentialType();
    AzureEnvironmentType azureEnvironmentType =
        acrArtifactDelegateRequest.getAzureConnectorDTO().getAzureEnvironmentType();
    return toAzureInternalConfig(
        credential, encryptedDataDetails, azureCredentialType, azureEnvironmentType, secretDecryptionService);
  }
  public AzureNGConfig toAzureInternalConfig(AzureCredentialDTO credential,
      List<EncryptedDataDetail> encryptedDataDetails, AzureCredentialType azureCredentialType,
      AzureEnvironmentType azureEnvironmentType, SecretDecryptionService secretDecryptionService) {
    AzureNGConfig azureNGConfig = null;
    switch (azureCredentialType) {
      case INHERIT_FROM_DELEGATE: {
        AzureInheritFromDelegateDetailsDTO azureInheritFromDelegateDetailsDTO =
            (AzureInheritFromDelegateDetailsDTO) credential.getConfig();
        AzureMSIAuthDTO azureMSIAuthDTO = azureInheritFromDelegateDetailsDTO.getAuthDTO();

        if (azureMSIAuthDTO instanceof AzureMSIAuthUADTO) {
          AzureUserAssignedMSIAuthDTO azureUserAssignedMSIAuthDTO =
              ((AzureMSIAuthUADTO) azureMSIAuthDTO).getCredentials();
          azureNGConfig = AzureNGInheritDelegateCredentialsConfig.builder()
                              .isUserAssignedManagedIdentity(true)
                              .clientId(azureUserAssignedMSIAuthDTO.getClientId())
                              .azureEnvironmentType(azureEnvironmentType)
                              .build();
        } else if (azureMSIAuthDTO instanceof AzureMSIAuthSADTO) {
          azureNGConfig = AzureNGInheritDelegateCredentialsConfig.builder()
                              .isUserAssignedManagedIdentity(false)
                              .clientId(null)
                              .azureEnvironmentType(azureEnvironmentType)
                              .build();
        } else {
          throw new IllegalStateException(
              "Unexpected ManagedIdentity credentials type : " + azureMSIAuthDTO.getClass().getName());
        }
        break;
      }
      case MANUAL_CREDENTIALS: {
        AzureManualDetailsDTO azureManualDetailsDTO = (AzureManualDetailsDTO) credential.getConfig();
        secretDecryptionService.decrypt(
            ((AzureManualDetailsDTO) credential.getConfig()).getAuthDTO().getCredentials(), encryptedDataDetails);
        azureNGConfig = AzureNGManualCredentialsConfig.builder()
                            .azureEnvironmentType(azureEnvironmentType)
                            .clientId(azureManualDetailsDTO.getClientId())
                            .tenantId(azureManualDetailsDTO.getTenantId())
                            .build();

        switch (azureManualDetailsDTO.getAuthDTO().getAzureSecretType()) {
          case SECRET_KEY:
            AzureClientSecretKeyDTO secretKey =
                (AzureClientSecretKeyDTO) azureManualDetailsDTO.getAuthDTO().getCredentials();
            ((AzureNGManualCredentialsConfig) azureNGConfig).setKey(secretKey.getSecretKey().getDecryptedValue());
            break;
          case KEY_CERT:
            AzureClientKeyCertDTO cert = (AzureClientKeyCertDTO) azureManualDetailsDTO.getAuthDTO().getCredentials();
            ((AzureNGManualCredentialsConfig) ((AzureNGManualCredentialsConfig) azureNGConfig))
                .setCert(String.valueOf(cert.getClientCertRef().getDecryptedValue()).getBytes());
            break;
          default:
            throw new IllegalStateException(
                "Unexpected secret type : " + azureManualDetailsDTO.getAuthDTO().getAzureSecretType());
        }
        break;
      }
      default:
        throw new IllegalStateException("Unexpected azure credential type : " + azureCredentialType);
    }
    return azureNGConfig;
  }

  public AcrArtifactDelegateResponse toAcrResponse(
      BuildDetailsInternal buildDetailsInternal, AcrArtifactDelegateRequest request) {
    return AcrArtifactDelegateResponse.builder()
        .buildDetails(ArtifactBuildDetailsMapper.toBuildDetailsNG(buildDetailsInternal))
        .subscription(request.getSubscription())
        .registry(request.getRegistry())
        .repository(request.getRepository())
        .tag(buildDetailsInternal.getNumber())
        .sourceType(ArtifactSourceType.ACR)
        .build();
  }
}
