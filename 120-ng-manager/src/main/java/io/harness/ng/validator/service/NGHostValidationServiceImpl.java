/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.validator.service;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.NgSetupFields.NG;
import static io.harness.delegate.beans.NgSetupFields.OWNER;
import static io.harness.delegate.task.utils.PhysicalDataCenterConstants.DEFAULT_HOST_VALIDATION_FAILED_MSG;
import static io.harness.delegate.task.utils.PhysicalDataCenterConstants.HOSTS_NUMBER_VALIDATION_LIMIT;
import static io.harness.delegate.task.utils.PhysicalDataCenterConstants.TRUE_STR;
import static io.harness.delegate.task.utils.PhysicalDataCenterUtils.extractHostnameFromHost;
import static io.harness.delegate.task.utils.PhysicalDataCenterUtils.extractPortFromHost;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.validator.dto.HostValidationDTO.HostValidationStatus.FAILED;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.beans.DelegateTaskRequest;
import io.harness.beans.IdentifierRef;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.SSHTaskParams;
import io.harness.delegate.beans.secrets.SSHConfigValidationTaskResponse;
import io.harness.delegate.task.utils.PhysicalDataCenterConstants;
import io.harness.delegate.utils.TaskSetupAbstractionHelper;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretRefHelper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.manage.ManagedExecutorService;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.models.Secret;
import io.harness.ng.validator.dto.HostValidationDTO;
import io.harness.ng.validator.service.api.NGHostValidationService;
import io.harness.pms.utils.CompletableFutures;
import io.harness.secretmanagerclient.SecretType;
import io.harness.secretmanagerclient.services.SshKeySpecDTOHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class NGHostValidationServiceImpl implements NGHostValidationService {
  @Inject private SshKeySpecDTOHelper sshKeySpecDTOHelper;
  @Inject private NGSecretServiceV2 ngSecretServiceV2;
  @Inject private TaskSetupAbstractionHelper taskSetupAbstractionHelper;
  @Inject private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  private final Executor executor = new ManagedExecutorService(Executors.newFixedThreadPool(4));

  @Override
  public List<HostValidationDTO> validateSSHHosts(@NotNull List<String> hosts, @Nullable String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier, @NotNull String secretIdentifierWithScope) {
    if (hosts.isEmpty()) {
      return Collections.emptyList();
    }
    if (isBlank(secretIdentifierWithScope)) {
      throw new InvalidArgumentsException("Secret identifier cannot be null or empty", USER_SRE);
    }

    CompletableFutures<HostValidationDTO> validateHostTasks = new CompletableFutures<>(executor);
    for (String hostName : limitHosts(hosts)) {
      validateHostTasks.supplyAsyncExceptionally(()
                                                     -> validateSSHHost(hostName, accountIdentifier, orgIdentifier,
                                                         projectIdentifier, secretIdentifierWithScope),
          ex
          -> HostValidationDTO.builder()
                 .host(hostName)
                 .status(FAILED)
                 .error(buildErrorDetailsWithMsg(ex.getMessage(), hostName))
                 .build());
    }

    CompletableFuture<List<HostValidationDTO>> hostValidationResults = validateHostTasks.allOf();
    try {
      return new ArrayList<>(hostValidationResults.get());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException(ex.getMessage(), USER);
    } catch (ExecutionException ex) {
      throw new InvalidRequestException(ex.getMessage(), USER);
    }
  }

  @Override
  public HostValidationDTO validateSSHHost(@NotNull String host, String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier, @NotNull String secretIdentifierWithScope) {
    if (isBlank(host)) {
      throw new InvalidArgumentsException("SSH host cannot be null or empty", USER_SRE);
    }
    if (isBlank(secretIdentifierWithScope)) {
      throw new InvalidArgumentsException("Secret identifier cannot be null or empty", USER_SRE);
    }

    Optional<Secret> secretOptional =
        findSecret(accountIdentifier, orgIdentifier, projectIdentifier, secretIdentifierWithScope);
    if (!secretOptional.isPresent()) {
      throw new InvalidArgumentsException(
          format("Not found secret for host validation, secret identifier: %s", secretIdentifierWithScope), USER_SRE);
    }
    if (SecretType.SSHKey != secretOptional.get().getType()) {
      throw new InvalidArgumentsException(
          format("Secret is not SSH type, secret identifier: %s", secretIdentifierWithScope), USER_SRE);
    }

    SSHKeySpecDTO secretSpecDTO = (SSHKeySpecDTO) secretOptional.get().getSecretSpec().toDTO();
    List<EncryptedDataDetail> encryptionDetails = sshKeySpecDTOHelper.getSSHKeyEncryptionDetails(
        secretSpecDTO, getBaseNGAccess(accountIdentifier, orgIdentifier, projectIdentifier));
    Optional<Integer> portFromHost = extractPortFromHost(host);
    // if port from host exists it takes precedence over the port from SSH key
    // host is host name and port number
    portFromHost.ifPresent(secretSpecDTO::setPort);

    String hostName = portFromHost.isPresent()
        ? extractHostnameFromHost(host).orElseThrow(
            ()
                -> new InvalidArgumentsException(
                    format("Not found hostName, host: %s, extracted port: %s", host, portFromHost.get()), USER_SRE))
        : host;

    DelegateTaskRequest delegateTaskRequest =
        DelegateTaskRequest.builder()
            .accountId(accountIdentifier)
            .taskType(TaskType.NG_SSH_VALIDATION.name())
            .taskParameters(SSHTaskParams.builder()
                                .host(hostName)
                                .encryptionDetails(encryptionDetails)
                                .sshKeySpec(secretSpecDTO)
                                .build())
            .taskSetupAbstractions(setupTaskAbstractions(accountIdentifier, orgIdentifier, projectIdentifier))
            .executionTimeout(Duration.ofSeconds(PhysicalDataCenterConstants.EXECUTION_TIMEOUT_IN_SECONDS))
            .build();

    log.info("Start validation host:{}, hostName:{}, secretIdent:{}, accountIdent:{}, orgIdent:{}, projIdent:{},", host,
        hostName, secretIdentifierWithScope, accountIdentifier, orgIdentifier, projectIdentifier);
    DelegateResponseData delegateResponseData = this.delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);

    if (delegateResponseData instanceof SSHConfigValidationTaskResponse) {
      SSHConfigValidationTaskResponse responseData = (SSHConfigValidationTaskResponse) delegateResponseData;
      return HostValidationDTO.builder()
          .host(hostName)
          .status(HostValidationDTO.HostValidationStatus.fromBoolean(responseData.isConnectionSuccessful()))
          .error(responseData.isConnectionSuccessful()
                  ? buildEmptyErrorDetails()
                  : buildErrorDetailsWithMsg(responseData.getErrorMessage(), hostName))
          .build();
    }

    return HostValidationDTO.builder()
        .host(hostName)
        .status(FAILED)
        .error(buildErrorDetailsWithMsg(getErrorMessageFromDelegateResponseData(delegateResponseData), hostName))
        .build();
  }

  @NotNull
  private List<String> limitHosts(@NotNull List<String> hosts) {
    int numberOfHosts = hosts.size();
    if (numberOfHosts > HOSTS_NUMBER_VALIDATION_LIMIT) {
      log.warn("Limiting validation hosts to {}", HOSTS_NUMBER_VALIDATION_LIMIT);
    }

    return hosts.subList(0, Math.min(HOSTS_NUMBER_VALIDATION_LIMIT, numberOfHosts));
  }

  private BaseNGAccess getBaseNGAccess(
      final String accountIdentifier, final String orgIdentifier, final String projectIdentifier) {
    return BaseNGAccess.builder()
        .accountIdentifier(accountIdentifier)
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .build();
  }

  private Map<String, String> setupTaskAbstractions(
      final String accountIdIdentifier, final String orgIdentifier, final String projectIdentifier) {
    Map<String, String> abstractions = new HashMap<>(2);
    final String owner = taskSetupAbstractionHelper.getOwner(accountIdIdentifier, orgIdentifier, projectIdentifier);
    if (isNotEmpty(owner)) {
      abstractions.put(OWNER, owner);
    }
    abstractions.put(NG, TRUE_STR);
    return abstractions;
  }

  private String getErrorMessageFromDelegateResponseData(DelegateResponseData delegateResponseData) {
    if (delegateResponseData instanceof ErrorNotifyResponseData) {
      ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
      return errorNotifyResponseData.getErrorMessage();
    }

    return (delegateResponseData instanceof RemoteMethodReturnValueData)
        ? ((RemoteMethodReturnValueData) delegateResponseData).getException().getMessage()
        : DEFAULT_HOST_VALIDATION_FAILED_MSG;
  }

  private ErrorDetail buildErrorDetailsWithMsg(final String message, final String hostName) {
    return ErrorDetail.builder().message(message).reason(format("Validation failed for host: %s", hostName)).build();
  }

  private ErrorDetail buildEmptyErrorDetails() {
    return ErrorDetail.builder().build();
  }

  private Optional<Secret> findSecret(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String secretIdentifierWithScope) {
    SecretRefData secretRefData = SecretRefHelper.createSecretRef(secretIdentifierWithScope);
    IdentifierRef secretIdentifiers = IdentifierRefHelper.getIdentifierRef(
        secretIdentifierWithScope, accountIdentifier, orgIdentifier, projectIdentifier);
    return ngSecretServiceV2.get(secretIdentifiers.getAccountIdentifier(), secretIdentifiers.getOrgIdentifier(),
        secretIdentifiers.getProjectIdentifier(), secretRefData.getIdentifier());
  }
}
