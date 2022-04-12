/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.gitsyncerror.dtos;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.GitSyncApiConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@Schema(name = "GitToHarnessErrorDetails", description = "This contains additional details of Git To Harness Errors")
@OwnedBy(PL)
public class GitToHarnessErrorDetailsDTO implements GitSyncErrorDetailsDTO {
  @Schema(description = "Commit Id") String gitCommitId;
  @Schema(description = "Git File Content") String yamlContent;
  @Schema(description = GitSyncApiConstants.COMMIT_MESSAGE_PARAM_MESSAGE) String commitMessage;
  @Schema(description = "Commit Id that resolved the Git Sync Error") String resolvedByCommitId;
  @Schema(description = GitSyncApiConstants.ENTITY_GIT_URL_DESCRIPTION) String entityUrl;
}
