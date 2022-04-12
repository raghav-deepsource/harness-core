/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.envGroup.mappers;

import static io.harness.NGConstants.HARNESS_BLUE;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;
import static io.harness.ng.core.utils.NGUtils.validate;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.envGroup.beans.EnvironmentGroupConfig;
import io.harness.cdng.envGroup.beans.EnvironmentGroupEntity;
import io.harness.cdng.envGroup.beans.EnvironmentGroupWrapperConfig;
import io.harness.data.structure.CollectionUtils;
import io.harness.encryption.ScopeHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.ng.core.EntityDetail;
import io.harness.ng.core.envGroup.dto.EnvironmentGroupDeleteResponse;
import io.harness.ng.core.envGroup.dto.EnvironmentGroupResponse;
import io.harness.ng.core.envGroup.dto.EnvironmentGroupResponseDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.pms.yaml.YamlUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

@OwnedBy(PIPELINE)
@UtilityClass
public class EnvironmentGroupMapper {
  public EnvironmentGroupResponseDTO writeDTO(
      EnvironmentGroupEntity envGroup, List<EnvironmentResponse> envResponseList) {
    return EnvironmentGroupResponseDTO.builder()
        .accountId(envGroup.getAccountId())
        .orgIdentifier(envGroup.getOrgIdentifier())
        .projectIdentifier(envGroup.getProjectIdentifier())
        .identifier(envGroup.getIdentifier())
        .name(envGroup.getName())
        .color(Optional.ofNullable(envGroup.getColor()).orElse(HARNESS_BLUE))
        .description(envGroup.getDescription())
        .deleted(envGroup.getDeleted())
        .tags(convertToMap(envGroup.getTags()))
        .version(envGroup.getVersion())
        .envIdentifiers(envGroup.getEnvIdentifiers())
        .gitDetails(EntityGitDetailsMapper.mapEntityGitDetails(envGroup))
        .envResponse(CollectionUtils.emptyIfNull(envResponseList))
        .build();
  }

  public EnvironmentGroupResponse toResponseWrapper(
      EnvironmentGroupEntity envGroup, List<EnvironmentResponse> envResponseList) {
    return EnvironmentGroupResponse.builder()
        .environment(writeDTO(envGroup, envResponseList))
        .createdAt(envGroup.getCreatedAt())
        .lastModifiedAt(envGroup.getLastModifiedAt())
        .build();
  }

  public EnvironmentGroupDeleteResponse toDeleteResponseWrapper(EnvironmentGroupEntity envGroup) {
    return EnvironmentGroupDeleteResponse.builder()
        .accountId(envGroup.getAccountId())
        .orgIdentifier(envGroup.getOrgIdentifier())
        .projectIdentifier(envGroup.getProjectIdentifier())
        .identifier(envGroup.getIdentifier())
        .deleted(envGroup.getDeleted())
        .build();
  }

  public EnvironmentGroupEntity toEnvironmentEntity(String accId, String orgId, String projectId, String yaml) {
    EnvironmentGroupWrapperConfig environmentGroupWrapperConfig;
    try {
      environmentGroupWrapperConfig = YamlUtils.read(yaml, EnvironmentGroupWrapperConfig.class);
    } catch (IOException e) {
      throw new InvalidRequestException(String.format(" Environment Group could not be created - %s", e.getMessage()));
    }
    // Validates nonEmpty checks for environmentGroupWrapperConfig variables
    EnvironmentGroupConfig environmentGroupConfig = environmentGroupWrapperConfig.getEnvironmentGroupConfig();
    validate(environmentGroupConfig);

    validateOrgAndProjIdForEnvironmentGroup(accId, orgId, projectId, environmentGroupConfig);
    return EnvironmentGroupEntity.builder()
        .accountId(accId)
        .projectIdentifier(projectId)
        .orgIdentifier(orgId)
        .identifier(environmentGroupConfig.getIdentifier().trim())
        .name(environmentGroupConfig.getName().trim())
        .color(environmentGroupConfig.getColor())
        .description(environmentGroupConfig.getDescription())
        .tags(convertToList(environmentGroupConfig.getTags()))
        .envIdentifiers(CollectionUtils.emptyIfNull(environmentGroupConfig.getEnvIdentifiers()))
        .yaml(yaml)
        .build();
  }

  private static void validateOrgAndProjIdForEnvironmentGroup(
      String accId, String orgId, String projectId, EnvironmentGroupConfig environmentGroupConfig) {
    // validate Org Id
    if (!environmentGroupConfig.getOrgIdentifier().equals(orgId)) {
      throw new InvalidRequestException("Organization Identifier passed in query param is not same as passed in yaml");
    }

    // validate Pro Id
    if (!environmentGroupConfig.getProjectIdentifier().equals(projectId)) {
      throw new InvalidRequestException("Project Identifier passed in query param is not same as passed in yaml");
    }
  }

  public EntityDetail getEntityDetail(EnvironmentGroupEntity entity) {
    return EntityDetail.builder()
        .name(entity.getName())
        .type(EntityType.ENVIRONMENT_GROUP)
        .entityRef(IdentifierRef.builder()
                       .accountIdentifier(entity.getAccountIdentifier())
                       .orgIdentifier(entity.getOrgIdentifier())
                       .projectIdentifier(entity.getProjectIdentifier())
                       .scope(ScopeHelper.getScope(
                           entity.getAccountIdentifier(), entity.getOrgIdentifier(), entity.getProjectIdentifier()))
                       .identifier(entity.getIdentifier())
                       .build())
        .build();
  }
}
