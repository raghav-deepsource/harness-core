/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.migration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.data.structure.CollectionUtils;
import io.harness.gitsync.common.service.GitBranchSyncService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.core.beans.GitCommit;
import io.harness.gitsync.core.beans.GitCommit.GitCommitKeys;
import io.harness.migration.NGMigration;

import com.google.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class HandleNullCommitIdInDB implements NGMigration {
  private final MongoTemplate mongoTemplate;
  private final YamlGitConfigService yamlGitConfigService;
  private final GitBranchSyncService gitBranchSyncService;

  @Override
  public void migrate() {
    log.info("Started processing the migration to handle the null commitId");
    try {
      Criteria criteria = Criteria.where(GitCommitKeys.commitId).is(null);
      List<GitCommit> gitCommitList =
          mongoTemplate.find(new Query(criteria).with(PageRequest.of(0, 50)), GitCommit.class);
      log.info("The size of the git commits to be processed is {}", CollectionUtils.emptyIfNull(gitCommitList));
      deleteTheCommit(gitCommitList);
      log.info("Completed processing the migration to handle the null commitId");
    } catch (Exception exception) {
      log.error("Error occurred while handling the null commitId", exception);
    }
  }

  private void deleteTheCommit(List<GitCommit> gitCommitList) {
    if (isEmpty(gitCommitList)) {
      log.info("No commit Ids to be deleted");
    }
    for (GitCommit gitCommit : gitCommitList) {
      String repoUrl = gitCommit.getRepoURL();
      String branch = gitCommit.getBranchName();
      try {
        deleteTheGitCommitRecord(gitCommit);
      } catch (Exception ex) {
        log.error("Exception while processing the repo {} and branch {}", repoUrl, branch, ex);
      }
    }
  }

  private void deleteTheGitCommitRecord(GitCommit gitCommit) {
    log.info("Deleting the gitCommit with the uuid {} and the record {}", gitCommit.getUuid(), gitCommit);
    Criteria criteria = Criteria.where(GitCommitKeys.uuid).is(gitCommit.getUuid());
    mongoTemplate.remove(query(criteria), GitCommit.class);
    log.info("Removed {} record for the commitId {}", gitCommit, gitCommit.getUuid());
  }
}
