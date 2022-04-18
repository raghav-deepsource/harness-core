/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.filestore;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.core.entities.NGFile.NGFiles;

import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(HarnessTeam.CDP)
@UtilityClass
public class FileStoreRepositoryCriteriaCreator {
  public Criteria createCriteriaByScopeAndParentIdentifier(Scope scope, final String parentIdentifier) {
    Criteria scopeCriteria = createScopeCriteria(scope);
    return scopeCriteria.and(NGFiles.parentIdentifier).is(parentIdentifier);
  }

  public Criteria createScopeCriteria(Scope scope) {
    Criteria criteria = new Criteria();
    criteria.and(NGFiles.accountIdentifier).is(scope.getAccountIdentifier());
    criteria.and(NGFiles.orgIdentifier).is(scope.getOrgIdentifier());
    criteria.and(NGFiles.projectIdentifier).is(scope.getProjectIdentifier());
    return criteria;
  }

  public Sort createSortByLastModifiedAtDesc() {
    return Sort.by(Sort.Direction.DESC, NGFiles.lastModifiedAt);
  }
}
