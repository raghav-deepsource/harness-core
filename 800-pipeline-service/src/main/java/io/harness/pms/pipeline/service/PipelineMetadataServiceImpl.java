/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.pipeline.service;

import io.harness.exception.InvalidRequestException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.pms.pipeline.PipelineMetadataV2;
import io.harness.repositories.pipeline.PipelineMetadataV2Repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class PipelineMetadataServiceImpl implements PipelineMetadataService {
  @Inject private PipelineMetadataV2Repository pipelineMetadataV2Repository;
  @Inject private PersistentLocker persistentLocker;

  @Override
  public int incrementExecutionCounter(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier) {
    PipelineMetadataV2 pipelineMetadataV2 =
        pipelineMetadataV2Repository.incCounter(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    if (pipelineMetadataV2 != null) {
      return pipelineMetadataV2.getRunSequence();
    }
    String lockName =
        String.format("pipelineMetadata/%s/%s/%s/%s", accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
    try (AcquiredLock<?> lock =
             persistentLocker.waitToAcquireLock(lockName, Duration.ofSeconds(1), Duration.ofSeconds(2))) {
      if (lock == null) {
        log.error("Count not acquire lock");
        throw new InvalidRequestException("Unable to update build sequence, please retry the execution");
      }
      Optional<PipelineMetadataV2> pipelineMetadataV2Updated = pipelineMetadataV2Repository.cloneFromPipelineMetadata(
          accountId, orgIdentifier, projectIdentifier, pipelineIdentifier);
      return pipelineMetadataV2Updated.map(PipelineMetadataV2::getRunSequence).orElse(-1);
    }
  }

  @Override
  public PipelineMetadataV2 save(PipelineMetadataV2 metadata) {
    return pipelineMetadataV2Repository.save(metadata);
  }

  @Override
  public Optional<PipelineMetadataV2> getMetadata(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    return pipelineMetadataV2Repository.getPipelineMetadata(accountId, orgIdentifier, projectIdentifier, identifier);
  }
}
