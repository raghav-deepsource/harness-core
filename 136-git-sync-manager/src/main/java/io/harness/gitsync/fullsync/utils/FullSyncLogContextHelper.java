/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.fullsync.utils;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.NullSafeImmutableMap;

import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class FullSyncLogContextHelper {
  private static String MESSAGE_ID = "messageId";
  private static String EVENT_TYPE = "eventType";

  public static Map<String, String> getContext(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String messageId) {
    NullSafeImmutableMap.NullSafeBuilder<String, String> nullSafeBuilder = NullSafeImmutableMap.builder();
    nullSafeBuilder.putIfNotNull(MESSAGE_ID, messageId);
    nullSafeBuilder.putIfNotNull(ACCOUNT_KEY, accountIdentifier);
    nullSafeBuilder.putIfNotNull(ORG_KEY, orgIdentifier);
    nullSafeBuilder.putIfNotNull(PROJECT_KEY, projectIdentifier);
    nullSafeBuilder.putIfNotNull(EVENT_TYPE, "Full-Sync");
    return nullSafeBuilder.build();
  }
}
