/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.migration;

import static io.harness.gitsync.common.dtos.RepoProviders.BITBUCKET;
import static io.harness.gitsync.common.dtos.RepoProviders.GITHUB;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.beans.GitFileLocation;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.springframework.data.mongodb.core.MongoTemplate;

public class UpdateTheRepoProvidersInGitEntitiesTest extends GitSyncTestBase {
  @Inject MongoTemplate mongoTemplate;
  @InjectMocks @Inject UpdateTheRepoProvidersInGitEntities updateTheRepoProvidersInGitEntitiesMigration;

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void migrateTest() {
    GitFileLocation bitbucketEntity1 = GitFileLocation.builder()
                                           .gitSyncConfigId("gitSyncConfigId")
                                           .branch("branch")
                                           .repo("https://bitbucket.org/deepakpatankar/test/")
                                           .build();

    GitFileLocation bitbucketEntity2 = GitFileLocation.builder()
                                           .gitSyncConfigId("gitSyncConfigId")
                                           .branch("branch")
                                           .repo("https://bitbucket.org/deepakpatankar/test2/")
                                           .build();

    GitFileLocation githubEntity1 = GitFileLocation.builder()
                                        .gitSyncConfigId("gitSyncConfigId")
                                        .branch("branch")
                                        .repo("https://github.com/harness/harness-core")
                                        .build();

    GitFileLocation githubEntity2 = GitFileLocation.builder()
                                        .gitSyncConfigId("gitSyncConfigId")
                                        .branch("branch")
                                        .repo("https://github.com/harness/harness-test")
                                        .build();

    GitFileLocation githubEntity3 = GitFileLocation.builder()
                                        .gitSyncConfigId("gitSyncConfigId")
                                        .branch("branch")
                                        .repo("https://123.124.50.250:8000/deepakpatankar/test2/")
                                        .build();

    mongoTemplate.save(bitbucketEntity1);
    mongoTemplate.save(bitbucketEntity2);
    mongoTemplate.save(githubEntity1);
    mongoTemplate.save(githubEntity2);
    mongoTemplate.save(githubEntity3);

    updateTheRepoProvidersInGitEntitiesMigration.migrate();

    List<GitFileLocation> allGitEntities = mongoTemplate.findAll(GitFileLocation.class);
    assertThat(allGitEntities.size()).isEqualTo(5);

    List<GitFileLocation> entitiesInBitbucketFiles =
        allGitEntities.stream().filter(entity -> entity.getRepoProvider() == BITBUCKET).collect(Collectors.toList());
    assertThat(entitiesInBitbucketFiles.size()).isEqualTo(2);

    List<GitFileLocation> entitiesInGithubFiles =
        allGitEntities.stream().filter(entity -> entity.getRepoProvider() == GITHUB).collect(Collectors.toList());
    assertThat(entitiesInGithubFiles.size()).isEqualTo(3);
  }
}
