/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.helpers;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.BHAVYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.manage.GlobalContextManager;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PL)
public class GitContextHelperTest extends CategoryTest {
  private static final String Branch = "branch";
  private static final String BaseBranch = "baseBranch";

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testGetBranchForRefEntityValidations() {
    final GitEntityInfo gitEntityInfo =
        GitEntityInfo.builder().yamlGitConfigId("ygs").branch(Branch).isNewBranch(true).baseBranch(BaseBranch).build();
    try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
      GlobalContextManager.upsertGlobalContextRecord(
          GitSyncBranchContext.builder().gitBranchInfo(gitEntityInfo).build());
      String branch = GitContextHelper.getBranchForRefEntityValidations();
      assertThat(branch).isEqualTo(BaseBranch);
    }
  }
}
