/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.remote;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.MEET;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.common.dtos.GitSyncConfigDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

@OwnedBy(PL)
public class YamlGitConfigResourceTest extends GitSyncTestBase {
  @InjectMocks YamlGitConfigResource yamlGitConfigResource;
  GitSyncConfigDTO gitSyncConfigDTO;
  String name = " name ";
  String branch = " branch ";
  String repo = " repo ";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MEET)
  @Category(UnitTests.class)
  public void testFormatRequest() {
    gitSyncConfigDTO = GitSyncConfigDTO.builder().name(name).branch(branch).repo(repo).build();
    yamlGitConfigResource.formatRequest(gitSyncConfigDTO);
    assertThat(gitSyncConfigDTO.getName()).isEqualTo("name");
    assertThat(gitSyncConfigDTO.getBranch()).isEqualTo("branch");
    assertThat(gitSyncConfigDTO.getRepo()).isEqualTo("repo");
  }
}
