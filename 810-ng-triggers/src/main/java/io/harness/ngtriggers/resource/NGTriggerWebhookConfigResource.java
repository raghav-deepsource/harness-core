/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ngtriggers.resource;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.constants.Constants.UNRECOGNIZED_WEBHOOK;
import static io.harness.ngtriggers.Constants.TRIGGER_KEY;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HeaderConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ngtriggers.beans.dto.WebhookEventProcessingDetails;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.action.BitbucketPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubIssueCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.action.GitlabPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabTriggerEvent;
import io.harness.ngtriggers.helpers.WebhookConfigHelper;
import io.harness.ngtriggers.mapper.NGTriggerElementMapper;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.security.annotations.PublicApi;
import io.harness.utils.YamlPipelineUtils;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Api("webhook")
@Path("webhook")
@Produces({"application/json", "application/yaml", "text/plain"})
@Consumes({"application/json", "application/yaml", "text/plain"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Tag(name = "Webhook Triggers", description = "This contains APIs related to Webhook Triggers.")
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
@Slf4j
@OwnedBy(PIPELINE)
public class NGTriggerWebhookConfigResource {
  private final NGTriggerService ngTriggerService;
  private final NGTriggerElementMapper ngTriggerElementMapper;

  @GET
  @Operation(hidden = true, operationId = "getSourceRepos",
      summary = "Gets source repo types with all supported events.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns source repo types with all supported events.")
      })
  @Path("/sourceRepos")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getSourceRepoToEvent")
  public ResponseDTO<Map<WebhookSourceRepo, List<WebhookEvent>>>
  getSourceRepoToEvent() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getSourceRepoToEvent());
  }

  @GET
  @Operation(hidden = true, operationId = "getGitTriggerEventDetails",
      summary = "Gets trigger git actions for each supported event.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns trigger git actions for each supported event.")
      })
  @Path("/gitTriggerEventDetails")
  @ApiOperation(value = "Get trigger git actions with Events", nickname = "getGitTriggerEventDetails")
  public ResponseDTO<Map<String, Map<String, List<String>>>>
  getGitTriggerEventDetails() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGitTriggerEventDetails());
  }

  @GET
  @Operation(hidden = true, operationId = "getWebhookTriggerTypes", summary = "Gets all supported scm webhook type.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all supported scm webhook type.")
      })
  @Path("/webhookTriggerTypes")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getWebhookTriggerTypes")
  public ResponseDTO<List<WebhookTriggerType>>
  getWebhookTriggerTypes() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getWebhookTriggerType());
  }

  @GET
  @Operation(hidden = true, operationId = "getGithubTriggerEvents",
      summary = "Gets all supported Github trigger events.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all supported Github trigger events.")
      })
  @Path("/githubTriggerEvents")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGithubTriggerEvents")
  public ResponseDTO<List<GithubTriggerEvent>>
  getGithubTriggerEvents() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGithubTriggerEvents());
  }

  @GET
  @Operation(hidden = true, operationId = "getGithubPRActions", summary = "Gets all supported Github PR event actions",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all supported Github PR event actions.")
      })
  @Path("/githubPRActions")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGithubPRActions")
  public ResponseDTO<List<GithubPRAction>>
  getGithubPRActions() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGithubPRAction());
  }

  @GET
  @Operation(hidden = true, operationId = "getGithubIssueCommentActions",
      summary = "Gets all supported Github Issue comment event actions",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all supported Github Issue comment event actions.")
      })
  @Path("/githubIssueCommentActions")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGithubIssueCommentActions")
  public ResponseDTO<List<GithubIssueCommentAction>>
  getGithubIssueCommentActions() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGithubIssueCommentAction());
  }

  @GET
  @Operation(hidden = true, operationId = "getGitlabTriggerEvents",
      summary = "Gets all supported Gitlab trigger events.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all supported Gitlab trigger events.")
      })
  @Path("/gitlabTriggerEvents")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGitlabTriggerEvents")
  public ResponseDTO<List<GitlabTriggerEvent>>
  getGitlabTriggerEvents() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGitlabTriggerEvents());
  }

  @GET
  @Operation(hidden = true, operationId = "getGitlabPRActions", summary = "Gets all supported GitLab PR event actions.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all supported GitLab PR event actions.")
      })
  @Path("/gitlabPRActions")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getGitlabPRActions")
  public ResponseDTO<List<GitlabPRAction>>
  getGitlabTriggerActions() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getGitlabPRAction());
  }

  @GET
  @Operation(hidden = true, operationId = "getBitbucketTriggerEvents",
      summary = "Gets all supported Bitbucket trigger events.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all supported Bitbucket trigger events.")
      })
  @Path("/bitbucketTriggerEvents")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getBitbucketTriggerEvents")
  public ResponseDTO<List<BitbucketTriggerEvent>>
  getBitbucketTriggerEvents() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getBitbucketTriggerEvents());
  }

  @GET
  @Operation(hidden = true, operationId = "getBitbucketPRActions",
      summary = "Gets all supported Bitbucket PR event actions.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns all supported Bitbucket PR event actions.")
      })
  @Path("/bitbucketPRActions")
  @ApiOperation(value = "Get Source Repo types with Events", nickname = "getBitbucketPRActions")
  public ResponseDTO<List<BitbucketPRAction>>
  getBitbucketPRActions() {
    return ResponseDTO.newResponse(WebhookConfigHelper.getBitbucketPRAction());
  }

  @GET
  @Operation(hidden = true, operationId = "getActionsList",
      summary = "Get all supported actions for event type and source.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "Returns all the supported actions for the specified event type and source.")
      })
  @Path("/actions")
  @ApiOperation(value = "Get Actions for event type and source", nickname = "getActionsList")
  public ResponseDTO<List<WebhookAction>>
  getActionsList(
      @NotNull @QueryParam("sourceRepo") WebhookSourceRepo sourceRepo, @NotNull @QueryParam("event") String event) {
    WebhookEvent webhookEvent;
    try {
      webhookEvent = YamlPipelineUtils.read(event, WebhookEvent.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Event: " + event + " is not valid");
    }
    return ResponseDTO.newResponse(WebhookConfigHelper.getActionsList(sourceRepo, webhookEvent));
  }

  @POST
  @Operation(operationId = "processWebhookEvent", summary = "Handles event payload for webhook triggers.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns UUID of newly created webhook processing event.")
      })
  @Path("/trigger")
  @ApiOperation(value = "accept webhook event", nickname = "webhookEndpoint")
  @PublicApi
  public ResponseDTO<String>
  processWebhookEvent(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier, @NotNull String eventPayload,
      @Context HttpHeaders httpHeaders) {
    List<HeaderConfig> headerConfigs = new ArrayList<>();
    httpHeaders.getRequestHeaders().forEach(
        (k, v) -> headerConfigs.add(HeaderConfig.builder().key(k).values(v).build()));

    TriggerWebhookEvent eventEntity =
        ngTriggerElementMapper
            .toNGTriggerWebhookEvent(accountIdentifier, orgIdentifier, projectIdentifier, eventPayload, headerConfigs)
            .build();
    if (eventEntity != null) {
      TriggerWebhookEvent newEvent = ngTriggerService.addEventToQueue(eventEntity);
      return ResponseDTO.newResponse(newEvent.getUuid());
    } else {
      return ResponseDTO.newResponse(UNRECOGNIZED_WEBHOOK);
    }
  }

  @POST
  @Operation(operationId = "processCustomWebhookEvent", summary = "Handles event payload for custom webhook triggers.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "default", description = "Returns UUID of newly created custom webhook processing event.")
      })
  @Path("/custom")
  @ApiOperation(value = "accept custom webhook event", nickname = "customWebhookEndpoint")
  @PublicApi
  public ResponseDTO<String>
  processWebhookEvent(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @NotNull @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.PIPELINE_KEY) String pipelineIdentifier,
      @QueryParam(TRIGGER_KEY) String triggerIdentifier, @NotNull String eventPayload,
      @Context HttpHeaders httpHeaders) {
    List<HeaderConfig> headerConfigs = new ArrayList<>();
    httpHeaders.getRequestHeaders().forEach(
        (k, v) -> headerConfigs.add(HeaderConfig.builder().key(k).values(v).build()));

    TriggerWebhookEvent eventEntity =
        ngTriggerElementMapper
            .toNGTriggerWebhookEventForCustom(accountIdentifier, orgIdentifier, projectIdentifier, pipelineIdentifier,
                triggerIdentifier, eventPayload, headerConfigs)
            .build();
    if (eventEntity != null) {
      TriggerWebhookEvent newEvent = ngTriggerService.addEventToQueue(eventEntity);
      return ResponseDTO.newResponse(newEvent.getUuid());
    } else {
      return ResponseDTO.newResponse(UNRECOGNIZED_WEBHOOK);
    }
  }

  @GET
  @Operation(operationId = "fetchWebhookDetails", summary = "Gets webhook event processing details for input eventId.",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "default", description = "Returns webhook event processing details for input eventId.")
      })
  @Path("/triggerProcessingDetails")
  @ApiOperation(value = "fetch webhook event details", nickname = "triggerProcessingDetails")
  @PublicApi
  public ResponseDTO<WebhookEventProcessingDetails>
  fetchWebhookDetails(@NotNull @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @NotNull @QueryParam("eventId") String eventId) {
    return ResponseDTO.newResponse(ngTriggerService.fetchTriggerEventHistory(accountIdentifier, eventId));
  }
}
