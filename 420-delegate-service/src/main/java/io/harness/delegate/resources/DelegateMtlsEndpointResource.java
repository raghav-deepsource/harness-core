/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.resources;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;
import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMtlsEndpointDetails;
import io.harness.delegate.beans.DelegateMtlsEndpointRequest;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.DelegateMtlsEndpointService;

import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import com.sun.org.apache.xpath.internal.operations.Bool;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import lombok.extern.slf4j.Slf4j;

@Api("delegate-mtls")
@Path("/")
@Produces("application/json")
@Consumes("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateMtlsEndpointResource {
  private final DelegateMtlsEndpointService delegateMtlsEndpointService;
  private final AccessControlClient accessControlClient;

  @Inject
  public DelegateMtlsEndpointResource(
      DelegateMtlsEndpointService delegateMtlsEndpointService, AccessControlClient accessControlClient) {
    this.delegateMtlsEndpointService = delegateMtlsEndpointService;
    this.accessControlClient = accessControlClient;
  }

  @PUT
  @Path("accounts/{accountId}/delegate-mtls/endpoint")
  @Timed
  @ExceptionMetered
  //  @AuthRule(skipAuth = true)
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<DelegateMtlsEndpointDetails> createEndpointForAccount(
      @Parameter(required = true, description = "The account id.") @PathParam("accountId") @NotNull String accountId,
      @RequestBody(required = true, description = "The details of the delegate mTLS endpoint to create.")
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      return new RestResponse<>(this.delegateMtlsEndpointService.createEndpointForAccount(accountId, endpointRequest));
    }
  }

  @POST
  @Path("accounts/{accountId}/delegate-mtls/endpoint")
  @Timed
  @ExceptionMetered
  //  @AuthRule(skipAuth = true)
  @AuthRule(permissionType = MANAGE_DELEGATES)
  public RestResponse<DelegateMtlsEndpointDetails> updateEndpointForAccount(
      @Parameter(required = true, description = "The account id.") @PathParam("accountId") @NotNull String accountId,
      @RequestBody(required = true, description = "The details of the delegate mTLS endpoint to update.")
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    return new RestResponse<>(this.delegateMtlsEndpointService.updateEndpointForAccount(accountId, endpointRequest));
  }

  @DELETE
  @Path("accounts/{accountId}/delegate-mtls/endpoint")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> deleteEndpointForAccount(
      @Parameter(required = true, description = "The account id.") @PathParam("accountId") @NotNull String accountId) {
    return new RestResponse<>(this.delegateMtlsEndpointService.deleteEndpointForAccount(accountId));
  }

  @GET
  @Path("accounts/{accountId}/delegate-mtls/endpoint")
  @Timed
  @ExceptionMetered
  public RestResponse<DelegateMtlsEndpointDetails> getEndpointForAccount(
      @Parameter(required = false, description = "The account id.") @PathParam("accountId") @NotNull String accountId) {
    return new RestResponse<>(this.delegateMtlsEndpointService.getEndpointForAccount(accountId));
  }

  @GET
  @Path("delegate-mtls/check-availability")
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> isEndpointFqdnAvailable(
      @Parameter(required = false, description = "The FQDN to verify.") @QueryParam("fqdn") @NotNull String fqdn) {
    return new RestResponse<>(this.delegateMtlsEndpointService.isEndpointFqdnAvailable(fqdn));
  }
}
