/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector.mappers.azuremapper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.entities.embedded.azureconnector.AzureConfig;
import io.harness.connector.entities.embedded.azureconnector.AzureManagedIdentityCredential;
import io.harness.connector.entities.embedded.azureconnector.AzureManualCredential;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.azureconnector.AzureAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientKeyCertDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureClientSecretKeyDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureConnectorDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureCredentialType;
import io.harness.delegate.beans.connector.azureconnector.AzureInheritFromDelegateDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthSADTO;
import io.harness.delegate.beans.connector.azureconnector.AzureMSIAuthUADTO;
import io.harness.delegate.beans.connector.azureconnector.AzureManagedIdentityType;
import io.harness.delegate.beans.connector.azureconnector.AzureManualDetailsDTO;
import io.harness.delegate.beans.connector.azureconnector.AzureSecretType;
import io.harness.delegate.beans.connector.azureconnector.AzureUserAssignedMSIAuthDTO;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDP)
@Singleton
public class AzureEntityToDTO implements ConnectorEntityToDTOMapper<AzureConnectorDTO, AzureConfig> {
  @Override
  public AzureConnectorDTO createConnectorDTO(AzureConfig connector) {
    final AzureCredentialType credentialType = connector.getCredentialType();
    switch (credentialType) {
      case INHERIT_FROM_DELEGATE:
        return buildInheritFromDelegate(connector);
      case MANUAL_CREDENTIALS:
        return buildManualCredential(connector);
      default:
        throw new InvalidRequestException("Invalid Credential type.");
    }
  }

  private AzureConnectorDTO buildManualCredential(AzureConfig connector) {
    final AzureManualCredential auth = (AzureManualCredential) connector.getCredential();
    final SecretRefData secretRef = SecretRefHelper.createSecretRef(auth.getSecretKeyRef());
    final AzureAuthDTO azureAuthDTO = buildAzureAuthDTO(secretRef, auth.getAzureSecretType());
    final AzureManualDetailsDTO azureManualDetailsDTO = AzureManualDetailsDTO.builder()
                                                            .clientId(auth.getClientId())
                                                            .tenantId(auth.getTenantId())
                                                            .authDTO(azureAuthDTO)
                                                            .build();
    return AzureConnectorDTO.builder()
        .delegateSelectors(connector.getDelegateSelectors())
        .azureEnvironmentType(connector.getAzureEnvironmentType())
        .credential(AzureCredentialDTO.builder()
                        .azureCredentialType(AzureCredentialType.MANUAL_CREDENTIALS)
                        .config(azureManualDetailsDTO)
                        .build())
        .build();
  }

  private AzureAuthDTO buildAzureAuthDTO(SecretRefData secretRef, AzureSecretType azureSecretType) {
    AzureAuthDTO azureAuthDTO = AzureAuthDTO.builder().azureSecretType(azureSecretType).build();
    switch (azureSecretType) {
      case SECRET_KEY:
        azureAuthDTO.setCredentials(AzureClientSecretKeyDTO.builder().secretKey(secretRef).build());
        break;
      case KEY_CERT:
        azureAuthDTO.setCredentials(AzureClientKeyCertDTO.builder().clientCertRef(secretRef).build());
        break;
      default:
        throw new InvalidRequestException("Invalid secret type.");
    }
    return azureAuthDTO;
  }

  private AzureConnectorDTO buildInheritFromDelegate(AzureConfig connector) {
    final AzureManagedIdentityCredential auth = (AzureManagedIdentityCredential) connector.getCredential();
    final AzureManagedIdentityType azureManagedIdentityType = auth.getAzureManagedIdentityType();

    AzureMSIAuthDTO azureMSIAuthDTO;
    switch (azureManagedIdentityType) {
      case USER_ASSIGNED_MANAGED_IDENTITY: {
        azureMSIAuthDTO = AzureMSIAuthUADTO.builder()
                              .azureManagedIdentityType(azureManagedIdentityType)
                              .credentials(AzureUserAssignedMSIAuthDTO.builder().clientId(auth.getClientId()).build())
                              .build();
        break;
      }
      case SYSTEM_ASSIGNED_MANAGED_IDENTITY: {
        azureMSIAuthDTO = AzureMSIAuthSADTO.builder().azureManagedIdentityType(azureManagedIdentityType).build();
        break;
      }
      default: {
        throw new InvalidRequestException("Invalid ManagedIdentity credentials type.");
      }
    }

    return AzureConnectorDTO.builder()
        .delegateSelectors(connector.getDelegateSelectors())
        .azureEnvironmentType(connector.getAzureEnvironmentType())
        .credential(AzureCredentialDTO.builder()
                        .azureCredentialType(AzureCredentialType.INHERIT_FROM_DELEGATE)
                        .config(AzureInheritFromDelegateDetailsDTO.builder().authDTO(azureMSIAuthDTO).build())
                        .build())
        .build();
  }
}
