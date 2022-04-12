/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.validator;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.PhysicalDataCenterConnectorValidationResult;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.PhysicalDataCenterConnectorDTO;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.ng.validator.dto.HostValidationDTO;
import io.harness.ng.validator.service.api.NGHostValidationService;

import com.google.inject.Inject;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@OwnedBy(CDP)
public class PhysicalDataCenterConnectorValidator implements ConnectionValidator<PhysicalDataCenterConnectorDTO> {
  @Inject private NGHostValidationService hostValidationService;

  @Override
  public ConnectorValidationResult validate(PhysicalDataCenterConnectorDTO connectorDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    long startTestingAt = System.currentTimeMillis();
    List<HostValidationDTO> hostValidationDTOs = hostValidationService.validateSSHHosts(getHostNames(connectorDTO),
        accountIdentifier, orgIdentifier, projectIdentifier, connectorDTO.getSshKeyRef().toSecretRefStringValue());

    return buildConnectorValidationResult(hostValidationDTOs, startTestingAt);
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }

  @NotNull
  private List<String> getHostNames(PhysicalDataCenterConnectorDTO connectorDTO) {
    return connectorDTO.getHosts().stream().map(HostDTO::getHostName).collect(Collectors.toList());
  }

  private ConnectorValidationResult buildConnectorValidationResult(
      List<HostValidationDTO> hostValidationDTOs, long startTestingAt) {
    ConnectivityStatus connectivityStatus = hostValidationDTOs.stream().anyMatch(isHostValidationStatusFailed())
        ? ConnectivityStatus.FAILURE
        : ConnectivityStatus.SUCCESS;

    return connectivityStatus == ConnectivityStatus.SUCCESS
        ? buildConnectorValidationResultSuccess(hostValidationDTOs, startTestingAt)
        : buildConnectorValidationResultFailure(hostValidationDTOs, startTestingAt);
  }

  private ConnectorValidationResult buildConnectorValidationResultSuccess(
      List<HostValidationDTO> hostValidationDTOS, long startTestingAt) {
    return PhysicalDataCenterConnectorValidationResult.builder()
        .testedAt(startTestingAt)
        .status(ConnectivityStatus.SUCCESS)
        .validationPassedHosts(toHostNames(hostValidationDTOS))
        .build();
  }

  private ConnectorValidationResult buildConnectorValidationResultFailure(
      List<HostValidationDTO> hostValidationDTOS, long startTestingAt) {
    List<HostValidationDTO> validationPassedHosts =
        hostValidationDTOS.stream().filter(isHostValidationStatusSuccess()).collect(Collectors.toList());
    List<HostValidationDTO> validationFailedHosts =
        hostValidationDTOS.stream().filter(isHostValidationStatusFailed()).collect(Collectors.toList());

    List<String> validationPassedHostNames = toHostNames(validationPassedHosts);
    List<String> validationFailedHostNames = toHostNames(validationFailedHosts);

    return PhysicalDataCenterConnectorValidationResult.builder()
        .validationPassedHosts(validationPassedHostNames)
        .errors(getErrorDetails(validationFailedHosts))
        .validationFailedHosts(validationFailedHostNames)
        .errorSummary(getErrorSummary(validationFailedHostNames))
        .testedAt(startTestingAt)
        .status(ConnectivityStatus.FAILURE)
        .build();
  }

  @NotNull
  private List<String> toHostNames(List<HostValidationDTO> hostValidationDTOS) {
    return hostValidationDTOS.stream().map(HostValidationDTO::getHost).collect(Collectors.toList());
  }

  @NotNull
  private List<ErrorDetail> getErrorDetails(List<HostValidationDTO> failedValidationHostResults) {
    return failedValidationHostResults.stream().map(HostValidationDTO::getError).collect(Collectors.toList());
  }

  @NotNull
  private Predicate<HostValidationDTO> isHostValidationStatusFailed() {
    return hostValidationDTO -> hostValidationDTO.getStatus() == HostValidationDTO.HostValidationStatus.FAILED;
  }

  @NotNull
  private Predicate<HostValidationDTO> isHostValidationStatusSuccess() {
    return hostValidationDTO -> hostValidationDTO.getStatus() == HostValidationDTO.HostValidationStatus.SUCCESS;
  }

  private String getErrorSummary(List<String> failedValidationHostNames) {
    return format("Validation failed for hosts: %s", StringUtils.join(failedValidationHostNames, ','));
  }
}
