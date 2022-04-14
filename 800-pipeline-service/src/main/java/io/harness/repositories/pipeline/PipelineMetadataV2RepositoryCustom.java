/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.pipeline.PipelineMetadataV2;

import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineMetadataV2RepositoryCustom {
  PipelineMetadataV2 incCounter(String accountId, String orgId, String projectIdentifier, String pipelineId);

  Optional<PipelineMetadataV2> getPipelineMetadata(
      String accountId, String orgId, String projectIdentifier, String identifier);

  Optional<PipelineMetadataV2> cloneFromPipelineMetadata(
      String accountId, String orgId, String projectIdentifier, String identifier);
}
