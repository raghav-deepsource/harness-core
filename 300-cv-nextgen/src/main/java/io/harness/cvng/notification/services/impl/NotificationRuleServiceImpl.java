/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.notification.services.impl;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.notification.beans.NotificationRuleDTO;
import io.harness.cvng.notification.beans.NotificationRuleResponse;
import io.harness.cvng.notification.entities.NotificationRule;
import io.harness.cvng.notification.entities.NotificationRule.NotificationRuleKeys;
import io.harness.cvng.notification.services.api.NotificationRuleService;
import io.harness.cvng.notification.transformer.NotificationRuleEntityAndDTOTransformer;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

public class NotificationRuleServiceImpl implements NotificationRuleService {
  @Inject private HPersistence hPersistence;
  @Inject private NotificationRuleEntityAndDTOTransformer notificationRuleEntityAndDTOTransformer;

  @Override
  public NotificationRuleResponse create(ProjectParams projectParams, NotificationRuleDTO notificationRuleDTO) {
    validateCreate(projectParams, notificationRuleDTO);
    NotificationRule notificationRule =
        notificationRuleEntityAndDTOTransformer.getEntity(projectParams, notificationRuleDTO);
    hPersistence.save(notificationRule);
    return getNotificationRuleResponse(projectParams, notificationRuleDTO.getIdentifier());
  }

  @Override
  public NotificationRule getEntity(ProjectParams projectParams, String identifier) {
    return hPersistence.createQuery(NotificationRule.class)
        .filter(NotificationRuleKeys.accountId, projectParams.getAccountIdentifier())
        .filter(NotificationRuleKeys.orgIdentifier, projectParams.getOrgIdentifier())
        .filter(NotificationRuleKeys.projectIdentifier, projectParams.getProjectIdentifier())
        .filter(NotificationRuleKeys.identifier, identifier)
        .get();
  }

  @Override
  public NotificationRuleResponse update(
      ProjectParams projectParams, String identifier, NotificationRuleDTO notificationRuleDTO) {
    Preconditions.checkArgument(identifier.equals(notificationRuleDTO.getIdentifier()),
        String.format(
            "Identifier %s does not match with path identifier %s", notificationRuleDTO.getIdentifier(), identifier));
    NotificationRule notificationRule = getEntity(projectParams, notificationRuleDTO.getIdentifier());
    if (notificationRule == null) {
      throw new InvalidRequestException(String.format(
          "NotificationRule  with identifier %s, accountId %s, orgIdentifier %s and projectIdentifier %s  is not present",
          notificationRuleDTO.getIdentifier(), projectParams.getAccountIdentifier(), projectParams.getOrgIdentifier(),
          projectParams.getProjectIdentifier()));
    }
    UpdateOperations<NotificationRule> updateOperations = hPersistence.createUpdateOperations(NotificationRule.class);
    updateOperations.set(NotificationRuleKeys.identifier, notificationRuleDTO.getIdentifier());
    updateOperations.set(NotificationRuleKeys.name, notificationRuleDTO.getName());
    updateOperations.set(NotificationRuleKeys.enabled, notificationRuleDTO.isEnabled());
    updateOperations.set(NotificationRuleKeys.type, notificationRuleDTO.getType());
    // TODO: figure out a way to upsert NotificationRuleSpec
    hPersistence.update(notificationRule, updateOperations);
    return getNotificationRuleResponse(projectParams, notificationRuleDTO.getIdentifier());
  }

  @Override
  public Boolean delete(ProjectParams projectParams, String identifier) {
    NotificationRule notificationRule = getEntity(projectParams, identifier);
    if (notificationRule == null) {
      throw new InvalidRequestException(String.format(
          "NotificationRule  with identifier %s, accountId %s, orgIdentifier %s and projectIdentifier %s  is not present",
          identifier, projectParams.getAccountIdentifier(), projectParams.getOrgIdentifier(),
          projectParams.getProjectIdentifier()));
    }
    // TODO: all the references of this notificationRule should also be deleted e.g. inside SLO and MonitoredService
    return hPersistence.delete(notificationRule);
  }

  @Override
  public PageResponse<NotificationRuleResponse> get(ProjectParams projectParams, Integer pageNumber, Integer pageSize) {
    Query<NotificationRule> notificationRuleQuery =
        hPersistence.createQuery(NotificationRule.class)
            .disableValidation()
            .filter(NotificationRuleKeys.accountId, projectParams.getAccountIdentifier())
            .filter(NotificationRuleKeys.orgIdentifier, projectParams.getOrgIdentifier())
            .filter(NotificationRuleKeys.projectIdentifier, projectParams.getProjectIdentifier())
            .order(Sort.descending(NotificationRuleKeys.lastUpdatedAt));
    List<NotificationRule> notificationRuleList = notificationRuleQuery.asList();

    PageResponse<NotificationRule> notificationRuleEntitiesPageResponse =
        PageUtils.offsetAndLimit(notificationRuleList, pageNumber, pageSize);
    List<NotificationRuleResponse> notificationRulePageResponse = notificationRuleEntitiesPageResponse.getContent()
                                                                      .stream()
                                                                      .map(this::notificationRuleEntityToResponse)
                                                                      .collect(Collectors.toList());
    return PageResponse.<NotificationRuleResponse>builder()
        .pageSize(pageSize)
        .pageIndex(pageNumber)
        .totalPages(notificationRuleEntitiesPageResponse.getTotalPages())
        .totalItems(notificationRuleEntitiesPageResponse.getTotalItems())
        .pageItemCount(notificationRuleEntitiesPageResponse.getPageItemCount())
        .content(notificationRulePageResponse)
        .build();
  }

  private void validateCreate(ProjectParams projectParams, NotificationRuleDTO notificationRuleDTO) {
    NotificationRule notificationRule = getEntity(projectParams, notificationRuleDTO.getIdentifier());
    if (notificationRule != null) {
      throw new DuplicateFieldException(String.format(
          "notificationRule with identifier %s and orgIdentifier %s and projectIdentifier %s is already present",
          notificationRuleDTO.getIdentifier(), projectParams.getOrgIdentifier(), projectParams.getProjectIdentifier()));
    }
  }

  private NotificationRuleResponse getNotificationRuleResponse(ProjectParams projectParams, String identifier) {
    NotificationRule notificationRule = getEntity(projectParams, identifier);
    return notificationRuleEntityToResponse(notificationRule);
  }

  private NotificationRuleResponse notificationRuleEntityToResponse(NotificationRule notificationRule) {
    NotificationRuleDTO notificationRuleDTO =
        NotificationRuleDTO.builder()
            .orgIdentifier(notificationRule.getOrgIdentifier())
            .projectIdentifier(notificationRule.getProjectIdentifier())
            .identifier(notificationRule.getIdentifier())
            .name(notificationRule.getName())
            .type(notificationRule.getType())
            .spec(notificationRuleEntityAndDTOTransformer.getDto(notificationRule).getSpec())
            .build();
    return NotificationRuleResponse.builder()
        .notificationRule(notificationRuleDTO)
        .createdAt(notificationRule.getCreatedAt())
        .lastModifiedAt(notificationRule.getLastUpdatedAt())
        .build();
  }
}
