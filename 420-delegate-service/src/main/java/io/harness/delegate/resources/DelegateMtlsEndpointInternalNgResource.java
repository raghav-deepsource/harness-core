/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMtlsEndpointDetails;
import io.harness.delegate.beans.DelegateMtlsEndpointRequest;
import io.harness.delegate.utils.DelegateMtlsConstants;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.InternalApi;
import io.harness.service.intfc.DelegateMtlsEndpointService;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Hidden;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Exposes the internal delegate mTLS endpoint management REST Api on CG which is called by NG.
 */
@Api(DelegateMtlsConstants.API_PATH_NG_INTERNAL)
@Path(DelegateMtlsConstants.API_PATH_NG_INTERNAL)
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@Hidden
public class DelegateMtlsEndpointInternalNgResource {
  private final DelegateMtlsEndpointService delegateMtlsEndpointService;

  @Inject
  public DelegateMtlsEndpointInternalNgResource(DelegateMtlsEndpointService delegateMtlsEndpointService) {
    this.delegateMtlsEndpointService = delegateMtlsEndpointService;
  }

  @PUT
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<DelegateMtlsEndpointDetails> createEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          this.delegateMtlsEndpointService.createEndpointForAccount(accountIdentifier, endpointRequest));
    }
  }

  @POST
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<DelegateMtlsEndpointDetails> updateEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          this.delegateMtlsEndpointService.updateEndpointForAccount(accountIdentifier, endpointRequest));
    }
  }

  @PATCH
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<DelegateMtlsEndpointDetails> patchEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @NotNull DelegateMtlsEndpointRequest patchRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(
          this.delegateMtlsEndpointService.patchEndpointForAccount(accountIdentifier, patchRequest));
    }
  }

  @DELETE
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<Boolean> deleteEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(this.delegateMtlsEndpointService.deleteEndpointForAccount(accountIdentifier));
    }
  }

  @GET
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<DelegateMtlsEndpointDetails> getEndpointForAccount(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      return new RestResponse<>(this.delegateMtlsEndpointService.getEndpointForAccount(accountIdentifier));
    }
  }

  @GET
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT_CHECK_AVAILABILITY)
  @Timed
  @ExceptionMetered
  @InternalApi
  @Hidden
  public RestResponse<Boolean> isDomainPrefixAvailable(
      @QueryParam(DelegateMtlsConstants.API_PARAM_NAME_DOMAIN_PREFIX) @NotNull String domainPrefix) {
    return new RestResponse<>(this.delegateMtlsEndpointService.isDomainPrefixAvailable(domainPrefix));
  }
}
