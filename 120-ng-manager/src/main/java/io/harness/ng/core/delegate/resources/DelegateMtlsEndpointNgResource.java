/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.core.delegate.resources;

import static io.harness.delegate.utils.RbacConstants.DELEGATE_EDIT_PERMISSION;
import static io.harness.delegate.utils.RbacConstants.DELEGATE_RESOURCE_TYPE;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static software.wings.security.PermissionAttribute.ResourceType.DELEGATE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMtlsEndpointDetails;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.rest.RestResponse;
import io.harness.service.intfc.DelegateMtlsEndpointService;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.constraints.NotNull;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api("/delegate-mtls-ng")
@Path("/delegate-mtls-ng")
@Produces("application/json")
@Scope(DELEGATE)
@Slf4j
@OwnedBy(HarnessTeam.DEL)
@Tag(name = "Delegate mTLS Endpoint Resource",
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
  private final DelegateMtlsEndpointService delegateMtlsEndpointService;
  private final AccessControlClient accessControlClient;

  @Inject
  public DelegateMtlsEndpointNgResource(
      DelegateMtlsEndpointService delegateMtlsEndpointService, AccessControlClient accessControlClient) {
    this.delegateMtlsEndpointService = delegateMtlsEndpointService;
    this.accessControlClient = accessControlClient;
  }
  //
  //  @POST
  //  @Timed
  //  @ExceptionMetered
  //  public RestResponse<DelegateMtlsEndpointDetails> createEndpointForAccount(
  //          @Parameter(
  //                  required = true,
  //                  description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE
  //          ) @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @NotNull String accountId,
  //          @RequestBody(
  //                  required = true,
  //                  description = "The details of the delegate mTLS endpoint to create."
  //          ) @NotNull DelegateMtlsEndpointDetails endpointDetails) {
  //
  //    accessControlClient.checkForAccessOrThrow(ResourceScope.of(accountId, null, null),
  //            Resource.of(DELEGATE_RESOURCE_TYPE, null), DELEGATE_EDIT_PERMISSION);
  //
  //    try (AutoLogContext ignore1 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
  //      return new RestResponse<>(this.delegateMtlsEndpointService.createEndpointForAccount(accountId,
  //      endpointDetails));
  //    }
  //  }
}
