/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.envGroup;

import static io.harness.exception.WingsException.USER_SRE;

import static java.lang.String.format;

import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.events.EnvironmentGroupCreateEvent;
import io.harness.cdng.events.EnvironmentGroupDeleteEvent;
import io.harness.cdng.events.EnvironmentGroupUpdateEvent;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.repository.support.PageableExecutionUtils;

/*
   TODO: need to do changes for environment group feature based on if this is available to or will be behind enforcement
   plan
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Slf4j
public class EnvironmentGroupRepositoryCustomImpl implements EnvironmentGroupRepositoryCustom {
  private final GitSyncSdkService gitSyncSdkService;
  private final GitAwarePersistence gitAwarePersistence;
  private final OutboxService outboxService;
  private final EnvironmentService environmentService;

  @Override
  public Optional<EnvironmentGroupEntity> findByAccountIdAndOrgIdentifierAndProjectIdentifierAndIdentifierAndDeletedNot(
      String accountId, String orgIdentifier, String projectIdentifier, String envGroupId, boolean notDeleted) {
    return gitAwarePersistence.findOne(Criteria.where(EnvironmentGroupEntity.EnvironmentGroupKeys.deleted)
                                           .is(!notDeleted)
                                           .and(EnvironmentGroupEntity.EnvironmentGroupKeys.projectIdentifier)
                                           .is(projectIdentifier)
                                           .and(EnvironmentGroupEntity.EnvironmentGroupKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(EnvironmentGroupEntity.EnvironmentGroupKeys.accountId)
                                           .is(accountId)
                                           .and(EnvironmentGroupEntity.EnvironmentGroupKeys.identifier)
                                           .is(envGroupId),
        projectIdentifier, orgIdentifier, accountId, EnvironmentGroupEntity.class);
  }

  @Override
  public EnvironmentGroupEntity create(EnvironmentGroupEntity environmentGroupEntity) {
    // validate EnvIdentifiers list
    if (!environmentGroupEntity.getEnvIdentifiers().isEmpty()) {
      validateNotExistentEnvIdentifiers(environmentGroupEntity);
    }
    EnvironmentGroupEntity savedEntity;
    Supplier<OutboxEvent> functor = null;
    // If git sync is disabled, then outbox the event
    boolean gitSyncEnabled = gitSyncSdkService.isGitSyncEnabled(environmentGroupEntity.getAccountIdentifier(),
        environmentGroupEntity.getOrgIdentifier(), environmentGroupEntity.getProjectIdentifier());
    if (!gitSyncEnabled) {
      functor = ()
          -> outboxService.save(EnvironmentGroupCreateEvent.builder()
                                    .accountIdentifier(environmentGroupEntity.getAccountIdentifier())
                                    .orgIdentifier(environmentGroupEntity.getOrgIdentifier())
                                    .projectIdentifier(environmentGroupEntity.getProjectIdentifier())
                                    .environmentGroupEntity(environmentGroupEntity)
                                    .build());
    }
    try {
      savedEntity = gitAwarePersistence.save(environmentGroupEntity, environmentGroupEntity.getYaml(), ChangeType.ADD,
          EnvironmentGroupEntity.class, functor);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          format("Identifier %s already exists in organization id %s and project id %s",
              environmentGroupEntity.getIdentifier(), environmentGroupEntity.getProjectIdentifier(),
              environmentGroupEntity.getOrgIdentifier()),
          USER_SRE, ex);
    } catch (Exception e) {
      throw new InvalidRequestException(
          String.format("Error occurred while saving environment group - %s", e.getMessage()));
    }

    return savedEntity;
  }

  @Override
  public Page<EnvironmentGroupEntity> list(
      Criteria criteria, Pageable pageRequest, String projectIdentifier, String orgIdentifier, String accountId) {
    List<EnvironmentGroupEntity> environmentGroupEntities = gitAwarePersistence.find(
        criteria, pageRequest, projectIdentifier, orgIdentifier, accountId, EnvironmentGroupEntity.class);
    return PageableExecutionUtils.getPage(environmentGroupEntities, pageRequest,
        ()
            -> gitAwarePersistence.count(
                criteria, projectIdentifier, orgIdentifier, accountId, EnvironmentGroupEntity.class));
  }

  @Override
  public EnvironmentGroupEntity deleteEnvGroup(EnvironmentGroupEntity entityToDelete) {
    Supplier<OutboxEvent> functor = null;
    // If git sync is disabled, then outbox the event
    boolean gitSyncEnabled = gitSyncSdkService.isGitSyncEnabled(entityToDelete.getAccountIdentifier(),
        entityToDelete.getOrgIdentifier(), entityToDelete.getProjectIdentifier());

    // if git is disabled, then only outbox the event
    if (!gitSyncEnabled) {
      functor = ()
          -> outboxService.save(EnvironmentGroupDeleteEvent.builder()
                                    .accountIdentifier(entityToDelete.getAccountIdentifier())
                                    .orgIdentifier(entityToDelete.getOrgIdentifier())
                                    .projectIdentifier(entityToDelete.getProjectIdentifier())
                                    .environmentGroupEntity(entityToDelete)
                                    .build());
    }

    return gitAwarePersistence.save(
        entityToDelete, entityToDelete.getYaml(), ChangeType.DELETE, EnvironmentGroupEntity.class, functor);
  }

  @Override
  public EnvironmentGroupEntity update(EnvironmentGroupEntity updatedEntity, EnvironmentGroupEntity originalEntity) {
    // validate EnvIdentifiers list
    if (!updatedEntity.getEnvIdentifiers().isEmpty()) {
      validateNotExistentEnvIdentifiers(updatedEntity);
    }

    Supplier<OutboxEvent> functor = null;
    // If git sync is disabled, then outbox the event
    boolean gitSyncEnabled = gitSyncSdkService.isGitSyncEnabled(
        updatedEntity.getAccountIdentifier(), updatedEntity.getOrgIdentifier(), updatedEntity.getProjectIdentifier());

    // if git is disabled, then only outbox the event
    if (!gitSyncEnabled) {
      functor = ()
          -> outboxService.save(EnvironmentGroupUpdateEvent.builder()
                                    .accountIdentifier(updatedEntity.getAccountIdentifier())
                                    .orgIdentifier(updatedEntity.getOrgIdentifier())
                                    .projectIdentifier(updatedEntity.getProjectIdentifier())
                                    .newEnvironmentGroupEntity(updatedEntity)
                                    .oldEnvironmentGroupEntity(originalEntity)
                                    .build());
    }

    try {
      return gitAwarePersistence.save(
          updatedEntity, updatedEntity.getYaml(), ChangeType.MODIFY, EnvironmentGroupEntity.class, functor);
    } catch (Exception e) {
      throw new InvalidRequestException(String.format(
          "Error occurred while updating Environment Group %s - %s", updatedEntity.getIdentifier(), e.getMessage()));
    }
  }

  @VisibleForTesting
  void validateNotExistentEnvIdentifiers(EnvironmentGroupEntity environmentGroupEntity) {
    List<String> exitsEnvIdentifiers = environmentService.fetchesNonDeletedEnvIdentifiersFromList(
        environmentGroupEntity.getAccountId(), environmentGroupEntity.getOrgIdentifier(),
        environmentGroupEntity.getProjectIdentifier(), environmentGroupEntity.getEnvIdentifiers());
    List<String> nonExistentEnvIds = environmentGroupEntity.getEnvIdentifiers()
                                         .stream()
                                         .filter(id -> !exitsEnvIdentifiers.contains(id))
                                         .collect(Collectors.toList());
    if (!nonExistentEnvIds.isEmpty()) {
      throw new InvalidRequestException(String.format(
          "These environment identifiers are either deleted or not present for this project %s", nonExistentEnvIds));
    }
  }
}
