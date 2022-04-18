package io.harness.ng.trialsignup;

import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.k8s.KubernetesConvention.getAccountIdentifier;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;

import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateSetupDetails;
import io.harness.delegate.beans.DelegateSize;
import io.harness.delegate.beans.DelegateType;
import io.harness.delegate.beans.K8sConfigDetails;
import io.harness.delegate.beans.K8sPermissionType;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.k8Connector.KubernetesClusterConfigDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialDTO;
import io.harness.delegate.beans.connector.k8Connector.KubernetesCredentialType;
import io.harness.ng.NextGenConfiguration;
import io.harness.ng.core.delegate.client.DelegateNgManagerCgManagerClient;
import io.harness.rest.RestResponse;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;
import retrofit2.Response;

@Slf4j
public class ProvisionService {
  @Inject DelegateNgManagerCgManagerClient delegateTokenNgClient;
  @Inject NextGenConfiguration configuration;
  @Inject @Named(DEFAULT_CONNECTOR_SERVICE) private ConnectorService connectorService;

  private static final String K8S_CONNECTOR_NAME = "Harness Kubernetes Cluster";
  private static final String K8S_CONNECTOR_DESC =
      "Kubernetes Cluster Connector created by Harness for connecting to Harness Builds environment";
  private static final String K8S_CONNECTOR_IDENTIFIER = "Harness_Kubernetes_Cluster";

  private static final String K8S_DELEGATE_NAME = "Harness Kubernetes Delegate";
  private static final String K8S_DELEGATE_DESC =
      "Kubernetes Delegate created by Harness for communication with Harness Kubernetes Cluster";

  private static final String DEFAULT_TOKEN = "default_token";

  private static final String GENERATE_SAMPLE_DELEGATE_CURL_COMMAND_FORMAT_STRING =
      "curl -s -X POST -H 'content-type: application/json' "
      + "--url https://app.harness.io/gateway/api/webhooks/WLwBdpY6scP0G9oNsGcX2BHrY4xH44W7r7HWYC94 "
      + "-d '{\"application\":\"4qPkwP5dQI2JduECqGZpcg\","
      + "\"parameters\":{\"Environment\":\"%s\",\"delegate\":\"delegate-ci\","
      + "\"account_id\":\"%s\",\"account_id_short\":\"%s\",\"account_secret\":\"%s\"}}'";

  public ProvisionResponse.Status provisionCIResources(String accountId) {
    Boolean delegateUpsertStatus = updateDelegateGroup(accountId);

    if (!delegateUpsertStatus) {
      return ProvisionResponse.Status.DELEGATE_PROVISION_FAILURE;
    }

    Boolean installConnector = installConnector(accountId);

    if (!installConnector) {
      return ProvisionResponse.Status.DELEGATE_PROVISION_FAILURE;
    }
    Boolean delegateInstallStatus = installDelegate(accountId);

    if (delegateInstallStatus) {
      return ProvisionResponse.Status.DELEGATE_PROVISION_FAILURE;
    }

    return ProvisionResponse.Status.SUCCESS;
  }

  private Boolean installDelegate(String accountId) {
    Response<RestResponse<String>> tokenRequest = null;
    String token;
    try {
      tokenRequest = delegateTokenNgClient.getDelegateTokenValue(accountId, null, null, DEFAULT_TOKEN).execute();
    } catch (IOException e) {
      log.error("failed to fetch delegate token from Manager", e);
      return FALSE;
    }

    if (tokenRequest.isSuccessful()) {
      token = tokenRequest.body().getResource();
    } else {
      log.error(format("failed to fetch delegate token from Manager. error is %s", tokenRequest.errorBody()));
      return FALSE;
    }

    // TODO(Aman) assert for trial account

    String script = format(GENERATE_SAMPLE_DELEGATE_CURL_COMMAND_FORMAT_STRING, configuration.getSignupTargetEnv(),
        accountId, getAccountIdentifier(accountId), token);
    Logger scriptLogger = LoggerFactory.getLogger("generate-delegate-" + accountId);
    try {
      ProcessExecutor processExecutor = new ProcessExecutor()
                                            .timeout(10, TimeUnit.MINUTES)
                                            .command("/bin/bash", "-c", script)
                                            .readOutput(true)
                                            .redirectOutput(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                scriptLogger.info(line);
                                              }
                                            })
                                            .redirectError(new LogOutputStream() {
                                              @Override
                                              protected void processLine(String line) {
                                                scriptLogger.error(line);
                                              }
                                            });
      int exitCode = processExecutor.execute().getExitValue();
      if (exitCode == 0) {
        return TRUE;
      }
      log.error("Curl script to generate delegate returned non-zero exit code: {}", exitCode);
    } catch (IOException e) {
      log.error("Error executing generate delegate curl command", e);
    } catch (InterruptedException e) {
      log.info("Interrupted", e);
    } catch (TimeoutException e) {
      log.info("Timed out", e);
    }
    String err = "Failed to provision";
    log.warn(err);
    return FALSE;
  }

  private Boolean installConnector(String accountId) {
    KubernetesCredentialDTO kubernetesCredentialDTO =
        KubernetesCredentialDTO.builder()
            .kubernetesCredentialType(KubernetesCredentialType.INHERIT_FROM_DELEGATE)
            .build();

    KubernetesClusterConfigDTO kubernetesClusterConfigDTO =
        KubernetesClusterConfigDTO.builder()
            .credential(kubernetesCredentialDTO)
            .delegateSelectors(new HashSet<>(Collections.singletonList(K8S_DELEGATE_NAME)))
            .build();

    ConnectorInfoDTO connectorInfoDTO = ConnectorInfoDTO.builder()
                                            .connectorType(ConnectorType.KUBERNETES_CLUSTER)
                                            .identifier(K8S_CONNECTOR_IDENTIFIER)
                                            .name(K8S_CONNECTOR_NAME)
                                            .description(K8S_CONNECTOR_DESC)
                                            .connectorConfig(kubernetesClusterConfigDTO)
                                            .build();

    ConnectorDTO connectorDTO = ConnectorDTO.builder().connectorInfo(connectorInfoDTO).build();

    connectorService.create(connectorDTO, accountId);

    return true;
  }

  Boolean updateDelegateGroup(String accountId) {
    DelegateSetupDetails delegateSetupDetails =
        DelegateSetupDetails.builder()
            .name(K8S_DELEGATE_NAME)
            .description(K8S_DELEGATE_DESC)
            .size(DelegateSize.SMALL)
            .k8sConfigDetails(K8sConfigDetails.builder()
                                  .k8sPermissionType(K8sPermissionType.NAMESPACE_ADMIN)
                                  .namespace(accountId)
                                  .build())
            .delegateType(DelegateType.KUBERNETES)
            .tokenName(DEFAULT_TOKEN)
            .build();

    try {
      Response<RestResponse<DelegateGroup>> delegateGroup =
          delegateTokenNgClient.upsert(K8S_DELEGATE_NAME, accountId, delegateSetupDetails).execute();
      if (delegateGroup == null) {
        log.error("Upserting delegate group failed. Account ID {}", accountId);
        return FALSE;
      }
    } catch (IOException e) {
      log.error("Upserting delegate group failed. Account ID {}. Exception:", accountId, e);
      return FALSE;
    }

    return TRUE;
  }
}
