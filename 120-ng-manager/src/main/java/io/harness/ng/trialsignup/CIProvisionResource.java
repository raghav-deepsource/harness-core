package io.harness.ng.trialsignup;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@OwnedBy(CI)
@Api("trial-signup")
@Path("/trial-signup")
@Produces({"application/json"})
@Consumes({"application/json"})
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class CIProvisionResource {
  @Inject ProvisionService provisionService;

  @PUT
  @Path("provision")
  @ApiOperation(value = "Provision resources for signup", nickname = "provisionResourcesForCI")
  public ResponseDTO<ProvisionResponse.Status> provisionCIResources(
      @NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId) {
    provisionService.provisionCIResources(accountId);
    return null;
  }
}
