/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.delegate.resources;

import static io.harness.account.accesscontrol.AccountAccessControlPermissions.EDIT_ACCOUNT_PERMISSION;
import static io.harness.account.accesscontrol.AccountAccessControlPermissions.VIEW_ACCOUNT_PERMISSION;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.account.accesscontrol.ResourceTypes;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMtlsEndpointDetails;
import io.harness.delegate.beans.DelegateMtlsEndpointRequest;
import io.harness.delegate.utils.DelegateMtlsConstants;
import io.harness.eraro.ErrorCode;
import io.harness.exception.AccessDeniedException;
import io.harness.exception.WingsException;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.delegate.client.DelegateNgManagerCgManagerClient;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.remote.client.RestClientUtils;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.dropwizard.jersey.PATCH;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

/**
 * Exposes the delegate mTLS endpoint management REST Api for NG.
 *
 * Note:
 *    NG manager doesn't execute the commands itself, but instead calls the CG manager's internal NG api endpoint.
 *    (Similar to other delegate related REST APIs)
 *
 *    As of now limited access for harness support only.
 */
@Api(DelegateMtlsConstants.API_PATH)
@Path(DelegateMtlsConstants.API_PATH)
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@Tag(name = "Delegate mTLS Endpoint Management",
    description = "Contains APIs related to Delegate mTLS Endpoint management.")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = FailureDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = FailureDTO.class))
    })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content =
    {
      @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDTO.class))
      , @Content(mediaType = "application/yaml", schema = @Schema(implementation = ErrorDTO.class))
    })
public class DelegateMtlsEndpointNgResource {
  private final DelegateNgManagerCgManagerClient delegateMtlsEndpointInternalClient;
  private final NgUserService ngUserService;

  @Inject
  public DelegateMtlsEndpointNgResource(
      DelegateNgManagerCgManagerClient delegateMtlsEndpointInternalClient, NgUserService ngUserService) {
    this.delegateMtlsEndpointInternalClient = delegateMtlsEndpointInternalClient;
    this.ngUserService = ngUserService;
  }

  @PUT
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @NGAccessControlCheck(resourceType = ResourceTypes.ACCOUNT, permission = EDIT_ACCOUNT_PERMISSION)
  @ApiOperation(
      nickname = "createDelegateMtlsEndpointForAccount", value = "Creates the delegate mTLS endpoint for an account.")
  @Operation(operationId = "createDelegateMtlsEndpointForAccount",
      summary = "Creates the delegate mTLS endpoint for an account.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "The details of the newly created mTLS endpoint.")
      })
  public RestResponse<DelegateMtlsEndpointDetails>
  createEndpointForAccount(
      @Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @RequestBody(required = true, description = "The details of the delegate mTLS endpoint to create.")
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(RestClientUtils.getResponse(
          this.delegateMtlsEndpointInternalClient.createEndpointForAccount(accountIdentifier, endpointRequest)));
    }
  }

  @POST
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @NGAccessControlCheck(resourceType = ResourceTypes.ACCOUNT, permission = EDIT_ACCOUNT_PERMISSION)
  @ApiOperation(nickname = "updateDelegateMtlsEndpointForAccount",
      value = "Updates the existing delegate mTLS endpoint for an account.")
  @Operation(operationId = "updateDelegateMtlsEndpointForAccount",
      summary = "Updates the existing delegate mTLS endpoint for an account.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "The details of the updated mTLS endpoint.")
      })
  public RestResponse<DelegateMtlsEndpointDetails>
  updateEndpointForAccount(
      @Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @RequestBody(required = true, description = "The updated details of the delegate mTLS endpoint.")
      @NotNull DelegateMtlsEndpointRequest endpointRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(RestClientUtils.getResponse(
          this.delegateMtlsEndpointInternalClient.updateEndpointForAccount(accountIdentifier, endpointRequest)));
    }
  }

  @PATCH
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @NGAccessControlCheck(resourceType = ResourceTypes.ACCOUNT, permission = EDIT_ACCOUNT_PERMISSION)
  @ApiOperation(nickname = "patchDelegateMtlsEndpointForAccount",
      value = "Updates selected properties of the existing delegate mTLS endpoint for an account.")
  @Operation(operationId = "patchDelegateMtlsEndpointForAccount",
      summary = "Updates selected properties of the existing delegate mTLS endpoint for an account.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "The details of the updated mTLS endpoint.")
      })
  public RestResponse<DelegateMtlsEndpointDetails>
  patchEndpointForAccount(
      @Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier,
      @RequestBody(required = true, description = "A subset of the details to update for the delegate mTLS endpoint.")
      @NotNull DelegateMtlsEndpointRequest patchRequest) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(RestClientUtils.getResponse(
          this.delegateMtlsEndpointInternalClient.patchEndpointForAccount(accountIdentifier, patchRequest)));
    }
  }

  @DELETE
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @NGAccessControlCheck(resourceType = ResourceTypes.ACCOUNT, permission = EDIT_ACCOUNT_PERMISSION)
  @ApiOperation(
      nickname = "deleteDelegateMtlsEndpointForAccount", value = "Removes the delegate mTLS endpoint for an account.")
  @Operation(operationId = "deleteDelegateMtlsEndpointForAccount",
      summary = "Removes the delegate mTLS endpoint for an account.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "True if and only if the endpoint existed and got removed.")
      })
  public RestResponse<Boolean>
  deleteEndpointForAccount(@Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE)
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(RestClientUtils.getResponse(
          this.delegateMtlsEndpointInternalClient.deleteEndpointForAccount(accountIdentifier)));
    }
  }

  @GET
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT)
  @Timed
  @ExceptionMetered
  @NGAccessControlCheck(resourceType = ResourceTypes.ACCOUNT, permission = VIEW_ACCOUNT_PERMISSION)
  @ApiOperation(
      nickname = "getDelegateMtlsEndpointForAccount", value = "Gets the delegate mTLS endpoint for an account.")
  @Operation(operationId = "getDelegateMtlsEndpointForAccount",
      summary = "Gets the delegate mTLS endpoint for an account.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "The delegate mTLS endpoint of the account, or null if it doesn't exist.")
      })
  public RestResponse<DelegateMtlsEndpointDetails>
  getEndpointForAccount(@Parameter(required = true, description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE)
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull String accountIdentifier) {
    try (AutoLogContext ignore1 = new AccountLogContext(accountIdentifier, OVERRIDE_ERROR)) {
      this.ensureOperationIsExecutedByHarnessSupport();
      return new RestResponse<>(RestClientUtils.getResponse(
          this.delegateMtlsEndpointInternalClient.getEndpointForAccount(accountIdentifier)));
    }
  }

  @GET
  @Path(DelegateMtlsConstants.API_PATH_ENDPOINT_CHECK_AVAILABILITY)
  @Timed
  @ExceptionMetered
  @ApiOperation(nickname = "checkDelegateMtlsEndpointDomainPrefixAvailability",
      value = "Checks whether a given delegate mTLS endpoint domain prefix is available.")
  @Operation(operationId = "checkDelegateMtlsEndpointDomainPrefixAvailability",
      summary = "Checks whether a given delegate mTLS endpoint domain prefix is available.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "True if and only if the domain prefix is currently not in use by any existing delegate mTLS endpoint.")
      })
  public RestResponse<Boolean>
  isDomainPrefixAvailable(
      @Parameter(required = true, description = DelegateMtlsConstants.API_PARAM_DESCRIPTION_DOMAIN_PREFIX) @QueryParam(
          DelegateMtlsConstants.API_PARAM_NAME_DOMAIN_PREFIX) @NotNull String domainPrefix) {
    this.ensureOperationIsExecutedByHarnessSupport();
    return new RestResponse<>(
        RestClientUtils.getResponse(this.delegateMtlsEndpointInternalClient.isDomainPrefixAvailable(domainPrefix)));
  }

  /**
   * Throws if the user executing the command isn't a Harness Support Group member.
   * This is required initially to ensure only harness support can add / remove endpoints in prod.
   *
   * @throws AccessDeniedException If the user isn't a member of the Harness Support Group.
   */
  private void ensureOperationIsExecutedByHarnessSupport() {
    if (!this.ngUserService.verifyHarnessSupportGroupUser()) {
      throw new AccessDeniedException("Only Harness Support Group Users can access this endpoint.",
          ErrorCode.NG_ACCESS_DENIED, WingsException.USER);
    }
  }
}
