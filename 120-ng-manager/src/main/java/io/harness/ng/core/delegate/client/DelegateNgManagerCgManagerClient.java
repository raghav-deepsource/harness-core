/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.delegate.client;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;

import io.harness.NGCommonEntityConstants;
import io.harness.delegate.beans.*;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.AuthRule;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import retrofit2.Call;
import retrofit2.http.*;

// this client is used to communicate between ng manager and cg manager for all delegate related stuff
public interface DelegateNgManagerCgManagerClient {
  String DELEGATE_TOKEN_NG_API = "delegate-token/ng";
  String DELEGATE_GROUP_TAGS_API = "delegate-group-tags-internal";
  String DELEGATE_MTLS_ENDPOINT_API = "delegate-mtls/ng/endpoint";
  String DELEGATE_MTLS_PREFIX_AVAILABLE_API = "delegate-mtls/ng/check-availability";

  //------------------------ Delegate Token -------------------------------------

  @POST(DELEGATE_TOKEN_NG_API)
  Call<RestResponse<DelegateTokenDetails>> createToken(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query("tokenName") @NotNull String tokenName);

  @PUT(DELEGATE_TOKEN_NG_API)
  Call<RestResponse<DelegateTokenDetails>> revokeToken(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query("tokenName") @NotNull String tokenName);

  @GET(DELEGATE_TOKEN_NG_API)
  Call<RestResponse<List<DelegateTokenDetails>>> getTokens(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query("status") DelegateTokenStatus status);

  @GET(DELEGATE_TOKEN_NG_API + "/delegate-groups")
  Call<RestResponse<DelegateGroupListing>> getDelegateGroupsUsingToken(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query("delegateTokenName") String delegateTokenName);

  @GET(DELEGATE_TOKEN_NG_API + "/delegate-token-value")
  Call<RestResponse<String>> getDelegateTokenValue(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query("delegateTokenName") String delegateTokenName);

  //------------------------ Delegate Group Tags, Deprecated Apis -----------------------------------

  @PUT(DELEGATE_GROUP_TAGS_API + "/tags")
  Call<RestResponse<DelegateGroup>> updateDelegateGroupTags_old(
      @Query(NGCommonEntityConstants.IDENTIFIER_KEY) @NotNull String groupIdentifier,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @Body @NotNull DelegateGroupTags tags);

  //------------------------ Delegate Group Tags, New Apis -----------------------------------

  @GET(DELEGATE_GROUP_TAGS_API)
  Call<RestResponse<Optional<DelegateGroupDTO>>> getDelegateGroupTags(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.GROUP_IDENTIFIER_KEY) @NotNull String groupIdentifier);

  @POST(DELEGATE_GROUP_TAGS_API)
  Call<RestResponse<Optional<DelegateGroupDTO>>> addDelegateGroupTags(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.GROUP_IDENTIFIER_KEY) @NotNull String groupIdentifier,
      @Body @NotNull DelegateGroupTags tags);

  @PUT(DELEGATE_GROUP_TAGS_API)
  Call<RestResponse<Optional<DelegateGroupDTO>>> updateDelegateGroupTags(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Query(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @Query(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @Query(NGCommonEntityConstants.GROUP_IDENTIFIER_KEY) @NotNull String groupIdentifier,
      @Body @NotNull DelegateGroupTags tags);

  //------------------------ Delegate Group Upsert -----------------------------------

  @PUT(DELEGATE_TOKEN_NG_API + "/upsert")
  Call<RestResponse<DelegateGroup>> upsert(@Query(NGCommonEntityConstants.NAME_KEY) @NotNull String delegateName,
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Body DelegateSetupDetails delegateSetupDetails);

  //------------------------ Delegate mTLS Endpoint Apis -----------------------------------

  @PUT(DELEGATE_MTLS_ENDPOINT_API)
  Call<RestResponse<DelegateMtlsEndpointDetails>> createEndpointForAccount(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Body @NotNull DelegateMtlsEndpointRequest endpointRequest);

  @POST(DELEGATE_MTLS_ENDPOINT_API)
  Call<RestResponse<DelegateMtlsEndpointDetails>> updateEndpointForAccount(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Body @NotNull DelegateMtlsEndpointRequest endpointRequest);

  @PATCH(DELEGATE_MTLS_ENDPOINT_API)
  Call<RestResponse<DelegateMtlsEndpointDetails>> patchEndpointForAccount(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier,
      @Body @NotNull DelegateMtlsEndpointRequest patchRequest);

  @DELETE(DELEGATE_MTLS_ENDPOINT_API)
  Call<RestResponse<Boolean>> deleteEndpointForAccount(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier);

  @GET(DELEGATE_MTLS_ENDPOINT_API)
  Call<RestResponse<DelegateMtlsEndpointDetails>> getEndpointForAccount(
      @Query(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountIdentifier);

  @GET(DELEGATE_MTLS_PREFIX_AVAILABLE_API)
  Call<RestResponse<Boolean>> isDomainPrefixAvailable(@Query("domainPrefix") @NotNull String domainPrefix);
}
