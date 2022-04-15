/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.datahandler.services;

import io.harness.delegate.beans.AccountVersionOverride;
import io.harness.delegate.beans.AccountVersionOverride.AccountVersionOverrideKeys;
import io.harness.persistence.HPersistence;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@RequiredArgsConstructor(onConstructor_ = @Inject)
public class AdminDelegateVersionService {
  private final HPersistence persistence;

  public void setDelegateImageTag(final String delegateTag, final String accountId, final int validFor) {
    setVersionOverride(accountId, AccountVersionOverrideKeys.delegateImageTag, delegateTag, validFor);
  }

  public void setUpgraderImageTag(final String upgraderTag, final String accountId, final int validFor) {
    setVersionOverride(accountId, AccountVersionOverrideKeys.upgraderImageTag, upgraderTag, validFor);
  }

  public void setDelegateVersion(final String delegateVersion, final String accountId, final int validFor) {
    setVersionOverride(
        accountId, AccountVersionOverrideKeys.delegateJarVersions, Lists.newArrayList(delegateVersion), validFor);
  }

  public void setWatcherVersion(final String watcherVersion, final String accountId, final int validFor) {
    setVersionOverride(
        accountId, AccountVersionOverrideKeys.watcherJarVersions, Lists.newArrayList(watcherVersion), validFor);
  }

  private void setVersionOverride(
      final String accountId, final String overrideKey, final Object overrideValue, final int validFor) {
    final DateTime validity = DateTime.now().plusDays(validFor);
    final Query<AccountVersionOverride> filter =
        persistence.createQuery(AccountVersionOverride.class).filter(AccountVersionOverrideKeys.accountId, accountId);
    final UpdateOperations<AccountVersionOverride> updateOperation =
        persistence.createUpdateOperations(AccountVersionOverride.class)
            .set(overrideKey, overrideValue)
            .set(AccountVersionOverrideKeys.validUntil, validity.toDate());

    persistence.upsert(filter, updateOperation);
  }
}
