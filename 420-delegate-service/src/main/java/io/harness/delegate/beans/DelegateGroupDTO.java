/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.beans;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.gitsync.beans.YamlDTO;

import java.util.Collections;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@OwnedBy(DEL)
public class DelegateGroupDTO implements YamlDTO {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;

  private String name;
  private String identifier;

  Set<String> tags;

  public static DelegateGroupDTO convertToDTO(DelegateGroup delegateGroup) {
    String orgIdentifier = delegateGroup.getOwner() != null
        ? DelegateEntityOwnerHelper.extractOrgIdFromOwnerIdentifier(delegateGroup.getOwner().getIdentifier())
        : null;
    String projectIdentifier = delegateGroup.getOwner() != null
        ? DelegateEntityOwnerHelper.extractProjectIdFromOwnerIdentifier(delegateGroup.getOwner().getIdentifier())
        : null;
    return DelegateGroupDTO.builder()
        .accountIdentifier(delegateGroup.getAccountId())
        .orgIdentifier(orgIdentifier)
        .projectIdentifier(projectIdentifier)
        .name(delegateGroup.getName())
        .identifier(delegateGroup.getIdentifier())
        .tags(isNotEmpty(delegateGroup.getTags()) ? delegateGroup.getTags() : Collections.emptySet())
        .build();
  }
}
