/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.ORG_PARAM_MESSAGE;
import static io.harness.NGCommonEntityConstants.PROJECT_PARAM_MESSAGE;
import static io.harness.connector.accesscontrol.ConnectorsAccessControlPermissions.VIEW_CONNECTOR_PERMISSION;
import static io.harness.utils.PageUtils.getNGPageResponse;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.accesscontrol.NGAccessControlCheck;
import io.harness.accesscontrol.ResourceIdentifier;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.accesscontrol.ResourceTypes;
import io.harness.connector.services.NGHostService;
import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.beans.connector.pdcconnector.HostDTO;
import io.harness.delegate.beans.connector.pdcconnector.HostFilterDTO;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.OrgIdentifier;
import io.harness.ng.core.ProjectIdentifier;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.data.domain.Page;
import retrofit2.http.Body;

@Api("/hosts")
@Path("/hosts")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html", "text/plain"})
@Tag(name = "Hosts", description = "This contains APIs related to Hosts as defined in Harness")
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
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@NextGenManagerAuth
@OwnedBy(HarnessTeam.CDP)
public class NGHostResource {
  private final NGHostService hostService;

  @Inject
  public NGHostResource(NGHostService hostService) {
    this.hostService = hostService;
  }

  @POST
  @Path("filter")
  @ApiOperation(value = "Get hosts filtered by connector", nickname = "filterHostsByConnector")
  @Operation(operationId = "filterHostsByConnector",
      summary = "Gets the list of hosts filtered by accountIdentifier and connectorIdentifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Returns the list of hosts filtered by accountIdentifier and connectorIdentifier")
      })
  @NGAccessControlCheck(resourceType = ResourceTypes.CONNECTOR, permission = VIEW_CONNECTOR_PERMISSION)
  public ResponseDTO<PageResponse<HostDTO>>
  filterHostsByConnector(
      @RequestBody(description = "Details of Page including: size, index, sort") @BeanParam PageRequest pageRequest,
      @Parameter(description = ACCOUNT_PARAM_MESSAGE, required = true) @NotBlank @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier String accountIdentifier,
      @Parameter(description = ORG_PARAM_MESSAGE) @OrgIdentifier @QueryParam(
          NGCommonEntityConstants.ORG_KEY) @io.harness.accesscontrol.OrgIdentifier String orgIdentifier,
      @Parameter(description = PROJECT_PARAM_MESSAGE) @ProjectIdentifier @QueryParam(
          NGCommonEntityConstants.PROJECT_KEY) @io.harness.accesscontrol.ProjectIdentifier String projectIdentifier,
      @Parameter(description = "Connector Identifier") @EntityIdentifier @QueryParam(
          NGCommonEntityConstants.IDENTIFIER_KEY) @ResourceIdentifier String connectorIdentifier,
      @RequestBody(description = "Details of the filters applied") @Body HostFilterDTO filter) {
    Page<HostDTO> connectorResponseDTO = hostService.filterHostsByConnector(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, filter, pageRequest);
    return ResponseDTO.newResponse(getNGPageResponse(connectorResponseDTO));
  }
}
