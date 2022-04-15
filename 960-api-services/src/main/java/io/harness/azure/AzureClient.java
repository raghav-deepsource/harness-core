/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure;

import static io.harness.azure.model.AzureConstants.SUBSCRIPTION_ID_NULL_VALIDATION_MSG;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.getOkHttpClientBuilder;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.azure.client.AzureBlueprintRestClient;
import io.harness.azure.client.AzureContainerRegistryRestClient;
import io.harness.azure.client.AzureManagementRestClient;
import io.harness.azure.context.AzureClientContext;
import io.harness.azure.model.AzureConfig;
import io.harness.azure.model.AzureConstants;
import io.harness.azure.model.AzureNGConfig;
import io.harness.azure.model.AzureNGInheritDelegateCredentialsConfig;
import io.harness.azure.model.AzureNGManualCredentialsConfig;
import io.harness.azure.utility.AzureUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.AzureAuthenticationException;
import io.harness.exception.AzureConfigException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.network.Http;

import com.google.inject.Singleton;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.credentials.MSICredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.rest.LogLevel;
import com.microsoft.rest.ServiceResponseBuilder;
import com.microsoft.rest.serializer.JacksonAdapter;
import java.security.InvalidKeyException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Singleton
@Slf4j
public class AzureClient {
  protected JacksonAdapter azureJacksonAdapter;
  protected ServiceResponseBuilder.Factory serviceResponseFactory;

  public AzureClient() {
    azureJacksonAdapter = new JacksonAdapter();
    serviceResponseFactory = new ServiceResponseBuilder.Factory();
  }

  protected Azure getAzureClientByContext(AzureClientContext context) {
    AzureConfig azureConfig = context.getAzureConfig();
    String subscriptionId = context.getSubscriptionId();
    return getAzureClient(azureConfig, subscriptionId);
  }

  protected Azure getAzureClientWithDefaultSubscription(AzureConfig azureConfig) {
    try {
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureConfig.getClientId(),
          azureConfig.getTenantId(), String.valueOf(azureConfig.getKey()),
          AzureUtils.getAzureEnvironment(azureConfig.getAzureEnvironmentType()));

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withDefaultSubscription();
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  protected Azure getAzureClient(AzureConfig azureConfig, String subscriptionId) {
    if (isBlank(subscriptionId)) {
      throw new IllegalArgumentException(SUBSCRIPTION_ID_NULL_VALIDATION_MSG);
    }

    try {
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(azureConfig.getClientId(),
          azureConfig.getTenantId(), String.valueOf(azureConfig.getKey()),
          AzureUtils.getAzureEnvironment(azureConfig.getAzureEnvironmentType()));

      return Azure.configure().withLogLevel(LogLevel.NONE).authenticate(credentials).withSubscription(subscriptionId);
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
      throw new InvalidRequestException("Failed to connect to Azure cluster. " + ExceptionUtils.getMessage(e), USER);
    }
  }

  protected Azure getAzureClientWithUserAssignedManagedIdentity(
      String clientId, AzureEnvironmentType azureEnvironmentType) {
    return getAzureClientWithManagedIdentity(true, clientId, azureEnvironmentType);
  }

  protected Azure getAzureClientWithSystemAssignedManagedIdentity(AzureEnvironmentType azureEnvironmentType) {
    return getAzureClientWithManagedIdentity(false, null, azureEnvironmentType);
  }

  protected Azure getAzureClientWithManagedIdentity(
      boolean isUserAssignedManagedIdentity, String clientId, AzureEnvironmentType azureEnvironmentType) {
    return getAzureClientWithManagedIdentity(isUserAssignedManagedIdentity, clientId, null, azureEnvironmentType);
  }

  protected Azure getAzureClientWithManagedIdentity(boolean isUserAssignedManagedIdentity, String clientId,
      String subscription, AzureEnvironmentType azureEnvironmentType) {
    Azure azure = null;

    try {
      AzureTokenCredentials azureTokenCredentials =
          getMSICredentials(isUserAssignedManagedIdentity, clientId, azureEnvironmentType);

      if (EmptyPredicate.isEmpty(subscription)) {
        azure =
            Azure.configure().withLogLevel(LogLevel.NONE).authenticate(azureTokenCredentials).withDefaultSubscription();
      } else {
        azure = Azure.configure()
                    .withLogLevel(LogLevel.NONE)
                    .authenticate(azureTokenCredentials)
                    .withSubscription(subscription);
      }
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }

    return azure;
  }

  private AzureTokenCredentials getMSICredentials(
      boolean isUserAssignedManagedIdentity, String clientId, AzureEnvironmentType azureEnvironmentType) {
    MSICredentials msiCredentials = new MSICredentials(AzureUtils.getAzureEnvironment(azureEnvironmentType));

    if (isUserAssignedManagedIdentity) {
      msiCredentials.withClientId(clientId);
    }
    return msiCredentials;
  }

  protected void handleAzureAuthenticationException(Exception e) {
    String message = "Authentication failed";
    Throwable e1 = e;
    while (e1.getCause() != null) {
      e1 = e1.getCause();
      if (e1 instanceof AuthenticationException || e1 instanceof InvalidKeyException) {
        message = "Invalid Azure credentials." + e1.getMessage();
      }
      if (e1 instanceof InterruptedException) {
        message = "Failed to connect to Azure cluster. " + ExceptionUtils.getMessage(e1);
      }
    }

    throw NestedExceptionUtils.hintWithExplanationException(
        "Check your Azure credentials", "Failed to connect to Azure", new AzureAuthenticationException(message));
  }

  protected AzureManagementRestClient getAzureManagementRestClient(AzureEnvironmentType azureEnvironmentType) {
    String url = AzureUtils.getAzureEnvironment(azureEnvironmentType).resourceManagerEndpoint();
    return getAzureRestClient(url, AzureManagementRestClient.class);
  }

  protected AzureBlueprintRestClient getAzureBlueprintRestClient(AzureEnvironmentType azureEnvironmentType) {
    String url = AzureUtils.getAzureEnvironment(azureEnvironmentType).resourceManagerEndpoint();
    return getAzureRestClient(url, AzureBlueprintRestClient.class);
  }

  protected AzureContainerRegistryRestClient getAzureContainerRegistryRestClient(final String repositoryHost) {
    String repositoryHostUrl = buildRepositoryHostUrl(repositoryHost);
    return getAzureRestClient(repositoryHostUrl, AzureContainerRegistryRestClient.class);
  }

  protected <T> T getAzureRestClient(String url, Class<T> clazz) {
    OkHttpClient okHttpClient = getOkHttpClientBuilder()
                                    .connectTimeout(AzureConstants.REST_CLIENT_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                                    .readTimeout(AzureConstants.REST_CLIENT_READ_TIMEOUT, TimeUnit.SECONDS)
                                    .proxy(Http.checkAndGetNonProxyIfApplicable(url))
                                    .retryOnConnectionFailure(true)
                                    .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .client(okHttpClient)
                            .baseUrl(url)
                            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(clazz);
  }

  protected String getAzureBearerAuthToken(AzureConfig azureConfig) {
    try {
      AzureEnvironment azureEnvironment = AzureUtils.getAzureEnvironment(azureConfig.getAzureEnvironmentType());
      ApplicationTokenCredentials credentials = new ApplicationTokenCredentials(
          azureConfig.getClientId(), azureConfig.getTenantId(), new String(azureConfig.getKey()), azureEnvironment);

      String token = credentials.getToken(azureEnvironment.managementEndpoint());
      return "Bearer " + token;
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  protected String getAzureBasicAuthHeader(final String username, final String password) {
    return "Basic " + encodeBase64String(format("%s:%s", username, password).getBytes(UTF_8));
  }

  protected String buildRepositoryHostUrl(String repositoryHost) {
    return format("https://%s%s", repositoryHost, repositoryHost.endsWith("/") ? "" : "/");
  }

  protected ApplicationTokenCredentials getAuthenticationTokenCredentials(AzureNGConfig azureNGConfig) {
    AzureNGManualCredentialsConfig azureConfig = (AzureNGManualCredentialsConfig) azureNGConfig;
    if (azureConfig.getKey() != null) {
      return new ApplicationTokenCredentials(azureConfig.getClientId(), azureConfig.getTenantId(),
          String.valueOf(azureConfig.getKey()), AzureUtils.getAzureEnvironment(azureConfig.getAzureEnvironmentType()));
    }
    if (azureConfig.getCert() != null) {
      return new ApplicationTokenCredentials(azureConfig.getClientId(), azureConfig.getTenantId(),
          azureConfig.getCert(), null, AzureUtils.getAzureEnvironment(azureConfig.getAzureEnvironmentType()));
    }
    throw new AzureAuthenticationException("Authentication details are not provided");
  }

  protected Azure getAzureClientNg(AzureNGConfig azureNGConfig, String subscriptionId) {
    Azure azure = null;
    try {
      if (azureNGConfig instanceof AzureNGManualCredentialsConfig) {
        AzureNGManualCredentialsConfig azureConfig = (AzureNGManualCredentialsConfig) azureNGConfig;
        if (isBlank(subscriptionId)) {
          azure = Azure.configure()
                      .withLogLevel(LogLevel.NONE)
                      .authenticate(getAuthenticationTokenCredentials(azureConfig))
                      .withDefaultSubscription();
        } else {
          azure = Azure.configure()
                      .withLogLevel(LogLevel.NONE)
                      .authenticate(getAuthenticationTokenCredentials(azureConfig))
                      .withSubscription(subscriptionId);
        }
      } else if (azureNGConfig instanceof AzureNGInheritDelegateCredentialsConfig) {
        AzureNGInheritDelegateCredentialsConfig azureConfig = (AzureNGInheritDelegateCredentialsConfig) azureNGConfig;
        azure = getAzureClientWithManagedIdentity(azureConfig.isUserAssignedManagedIdentity(),
            azureConfig.getClientId(), subscriptionId, azureConfig.getAzureEnvironmentType());
      } else {
        throw new AzureConfigException(
            format("AzureNGConfig not of expected type [%s]", azureNGConfig.getClass().getName()));
      }
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return azure;
  }

  protected String getAzureNGBearerAuthToken(AzureNGConfig azureNGConfig) {
    try {
      if (azureNGConfig instanceof AzureNGManualCredentialsConfig) {
        AzureNGManualCredentialsConfig azureConfig = (AzureNGManualCredentialsConfig) azureNGConfig;
        return "Bearer "
            + getAuthenticationTokenCredentials(azureConfig)
                  .getToken(AzureUtils.getAzureEnvironment(azureConfig.getAzureEnvironmentType()).managementEndpoint());
      } else if (azureNGConfig instanceof AzureNGInheritDelegateCredentialsConfig) {
        AzureNGInheritDelegateCredentialsConfig azureConfig = (AzureNGInheritDelegateCredentialsConfig) azureNGConfig;
        AzureTokenCredentials azureTokenCredentials = getMSICredentials(azureConfig.isUserAssignedManagedIdentity(),
            azureConfig.getClientId(), azureConfig.getAzureEnvironmentType());
        return "Bearer "
            + azureTokenCredentials.getToken(
                AzureUtils.getAzureEnvironment(azureConfig.getAzureEnvironmentType()).managementEndpoint());
      }
    } catch (Exception e) {
      handleAzureAuthenticationException(e);
    }
    return null;
  }

  protected String getRestAzureCredentials(AzureNGManualCredentialsConfig azureConfig) {
    if (azureConfig.getKey() != null) {
      return getAzureBasicAuthHeader(azureConfig.getClientId(), String.valueOf(azureConfig.getKey()));
    }
    throw new AzureAuthenticationException("Authentication details are not provided");
  }
}
