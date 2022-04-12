/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.migration.NGMigration;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.utils.RetryUtils;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.util.CloseableIterator;

@Slf4j
@OwnedBy(PL)
public class UserGroupSanitizationMigration implements NGMigration {
  public static final int BATCH_SIZE = 500;
  private final RetryPolicy<Object> retryPolicy = RetryUtils.getRetryPolicy(
      "[Retrying]: Failed migrating User Group; attempt: {}", "[Failed]: Failed migrating User Group; attempt: {}",
      ImmutableList.of(OptimisticLockingFailureException.class, DuplicateKeyException.class), Duration.ofSeconds(1), 3,
      log);
  private final UserGroupService userGroupService;
  private final MongoTemplate mongoTemplate;

  @Inject
  public UserGroupSanitizationMigration(UserGroupService userGroupService, MongoTemplate mongoTemplate) {
    this.userGroupService = userGroupService;
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void migrate() {
    log.info("UserGroupSanitizationMigration starts ...");

    CloseableIterator<UserGroup> iterator = runQueryWithBatch(new Criteria(), BATCH_SIZE);
    while (iterator.hasNext()) {
      UserGroup userGroup = iterator.next();
      handleWithRetries(userGroup);
    }

    log.info("UserGroupSanitizationMigration completed.");
  }

  private void sanitizeUserGroup(UserGroup userGroup) {
    userGroupService.sanitize(
        Scope.of(userGroup.getAccountIdentifier(), userGroup.getOrgIdentifier(), userGroup.getProjectIdentifier()),
        userGroup.getIdentifier());
  }

  private void handleWithRetries(UserGroup userGroup) {
    if (Boolean.TRUE.equals(userGroup.getExternallyManaged()) || Boolean.TRUE.equals(userGroup.getIsSsoLinked())) {
      log.info(String.format(
          "[UserGroupSanitizationMigration] Skipping migration for user group with account %s, org %s, project %s and identifier %s",
          userGroup.getAccountIdentifier(), userGroup.getOrgIdentifier(), userGroup.getProjectIdentifier(),
          userGroup.getIdentifier()));
      return;
    }
    try {
      Failsafe.with(retryPolicy).run(() -> sanitizeUserGroup(userGroup));
    } catch (Exception exception) {
      log.error(
          String.format(
              "[UserGroupSanitizationMigration] Unexpected error occurred during migration of user group with account %s, org %s, project %s and identifier %s",
              userGroup.getAccountIdentifier(), userGroup.getOrgIdentifier(), userGroup.getProjectIdentifier(),
              userGroup.getIdentifier()),
          exception);
    }
  }

  private CloseableIterator<UserGroup> runQueryWithBatch(Criteria criteria, int batchSize) {
    Query query = new Query(criteria);
    query.cursorBatchSize(batchSize);
    return mongoTemplate.stream(query, UserGroup.class);
  }
}
