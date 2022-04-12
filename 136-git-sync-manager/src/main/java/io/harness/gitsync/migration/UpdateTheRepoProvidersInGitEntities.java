/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.core.query.Update.update;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.gitsync.common.beans.GitFileLocation.GitFileLocationKeys;
import io.harness.gitsync.common.dtos.RepoProviders;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import com.mongodb.client.result.UpdateResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(PL)
public class UpdateTheRepoProvidersInGitEntities implements NGMigration {
  private final MongoTemplate mongoTemplate;

  @Override
  public void migrate() {
    log.info("Started the migration to add the repo providers in git sync entities");
    try {
      updateTheRecordsWithTheTypeBitbucket();
      updateTheRecordWithTheTypeGithub();
      log.info("UpdateTheRepoProvidersInGitSyncEntities Migration completed");
    } catch (Exception ex) {
      log.error("Error occurred while adding the repo provider type in git entities migration", ex);
    }
  }

  private void updateTheRecordsWithTheTypeBitbucket() {
    log.info("Updating the git entity records with the type Bitbucket");
    Update updatingRecordWithBitbucketType = update(GitFileLocationKeys.repoProvider, RepoProviders.BITBUCKET);
    Criteria criteriaForErrorsInBitbucket = Criteria.where(GitFileLocationKeys.repo).regex("/.*bitbucket.*/");
    UpdateResult updateResult = mongoTemplate.updateMulti(
        query(criteriaForErrorsInBitbucket), updatingRecordWithBitbucketType, GitFileLocation.class);
    log.info("Total {} records updated with value bitbucket", updateResult.getModifiedCount());
  }

  private void updateTheRecordWithTheTypeGithub() {
    log.info("Updating the git entity records with the type Github");
    Update updatingRecordWithGithubType = update(GitFileLocationKeys.repoProvider, RepoProviders.GITHUB);
    Criteria criteriaForErrorsInGithub = Criteria.where(GitFileLocationKeys.repo).not().regex("/.*bitbucket.*/");
    UpdateResult updateResultForGithub = mongoTemplate.updateMulti(
        query(criteriaForErrorsInGithub), updatingRecordWithGithubType, GitFileLocation.class);
    log.info("Total {} records updated with value github", updateResultForGithub.getModifiedCount());
  }
}
