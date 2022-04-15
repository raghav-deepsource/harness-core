/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.resources;

import static io.harness.exception.WingsException.USER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMtlsEndpointDetails;
import io.harness.delegate.beans.DelegateMtlsEndpointRequest;
import io.harness.exception.UnauthorizedException;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.DelegateMtlsEndpointService;

import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.HarnessUserGroupService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import lombok.extern.slf4j.Slf4j;

@Api(value = "delegate-mtls")
@Path("/delegate-mtls")
@Produces("application/json")
@Consumes("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateMtlsEndpointResource {
  // names / descriptions of common api path prefix / parameters.
  private static final String ENDPOINT_API = "endpoint";
  private static final String ACCOUNT_ID_PARAM = "accountId";
  private static final String ACCOUNT_ID_DESCRIPTION = "The account id.";

  private final HarnessUserGroupService harnessUserGroupService;
  private final DelegateMtlsEndpointService delegateMtlsEndpointService;

  @Inject
  public DelegateMtlsEndpointResource(
      DelegateMtlsEndpointService delegateMtlsEndpointService, HarnessUserGroupService harnessUserGroupService) {
    this.delegateMtlsEndpointService = delegateMtlsEndpointService;
    this.harnessUserGroupService = harnessUserGroupService;
  }

  @PUT
  @Path(ENDPOINT_API)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<DelegateMtlsEndpointDetails> createEndpointForAccount(
      @ApiParam(required = true, value = ACCOUNT_ID_DESCRIPTION) @QueryParam(
          ACCOUNT_ID_PARAM) @NotNull String accountId,
      @ApiParam(required = true, value = "The details of the delegate mTLS endpoint to create.")
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(this.delegateMtlsEndpointService.createEndpointForAccount(accountId, endpointRequest));
    }
  }

  @POST
  @Path(ENDPOINT_API)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<DelegateMtlsEndpointDetails> updateEndpointForAccount(
      @ApiParam(required = true, value = ACCOUNT_ID_DESCRIPTION) @QueryParam(
          ACCOUNT_ID_PARAM) @NotNull String accountId,
      @ApiParam(required = true, value = "The updated details of the delegate mTLS endpoint.")
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(this.delegateMtlsEndpointService.updateEndpointForAccount(accountId, endpointRequest));
    }
  }

  @PATCH
  @Path(ENDPOINT_API)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<DelegateMtlsEndpointDetails> patchEndpointForAccount(
      @ApiParam(required = true, value = ACCOUNT_ID_DESCRIPTION) @QueryParam(
          ACCOUNT_ID_PARAM) @NotNull String accountId,
      @ApiParam(required = true, value = "A subset of the details to update for the delegate mTLS endpoint.")
      @NotNull DelegateMtlsEndpointRequest patchRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(this.delegateMtlsEndpointService.patchEndpointForAccount(accountId, patchRequest));
    }
  }

  @DELETE
  @Path(ENDPOINT_API)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<Boolean> deleteEndpointForAccount(@ApiParam(
      required = true, value = ACCOUNT_ID_DESCRIPTION) @QueryParam(ACCOUNT_ID_PARAM) @NotNull String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(this.delegateMtlsEndpointService.deleteEndpointForAccount(accountId));
    }
  }

  @GET
  @Path(ENDPOINT_API)
  @Timed
  @ExceptionMetered
  @AuthRule(permissionType = ACCOUNT_MANAGEMENT)
  public RestResponse<DelegateMtlsEndpointDetails> getEndpointForAccount(@ApiParam(
      required = true, value = ACCOUNT_ID_DESCRIPTION) @QueryParam(ACCOUNT_ID_PARAM) @NotNull String accountId) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(this.delegateMtlsEndpointService.getEndpointForAccount(accountId));
    }
  }

  @GET
  @Path("check-availability")
  @Timed
  @ExceptionMetered
  @AuthRule(skipAuth = true)
  public RestResponse<Boolean> isDomainPrefixAvailable(@ApiParam(required = true,
      value = "The domain prefix to check.") @QueryParam("domainPrefix") @NotNull String domainPrefix) {
    return new RestResponse<>(this.delegateMtlsEndpointService.isDomainPrefixAvailable(domainPrefix));
  }

  /**
   * Throws if the user executing the command isn't a harness support user.
   * This is required initially to ensure only harness support can add / remove endpoints in prod.
   */
  private void ensureOperationIsExecutedByHarnessSupport() {
    String userId = UserThreadLocal.get().getUuid();
    if (!this.harnessUserGroupService.isHarnessSupportUser(userId)) {
      throw new UnauthorizedException("Only Harness Support Group Users can access this endpoint.", USER);
    }
  }
}
