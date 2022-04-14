package io.harness.ng.trialsignup;

import static io.harness.k8s.KubernetesConvention.getAccountIdentifier;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;

import io.harness.ng.NextGenConfiguration;
import io.harness.ng.core.delegate.client.DelegateNgManagerCgManagerClient;
import io.harness.rest.RestResponse;

import com.google.inject.Inject;
import java.io.IOException;
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

  private static final String GENERATE_SAMPLE_DELEGATE_CURL_COMMAND_FORMAT_STRING =
      "curl -s -X POST -H 'content-type: application/json' "
      + "--url https://app.harness.io/gateway/api/webhooks/WLwBdpY6scP0G9oNsGcX2BHrY4xH44W7r7HWYC94 "
      + "-d '{\"application\":\"4qPkwP5dQI2JduECqGZpcg\","
      + "\"parameters\":{\"Environment\":\"%s\",\"delegate\":\"delegate-ci\","
      + "\"account_id\":\"%s\",\"account_id_short\":\"%s\",\"account_secret\":\"%s\"}}'";

  public ProvisionResponse.Status provisionCIResources(String accountId) {
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
      tokenRequest = delegateTokenNgClient.getDelegateTokenValue(accountId, null, null, "default_token").execute();
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
}
