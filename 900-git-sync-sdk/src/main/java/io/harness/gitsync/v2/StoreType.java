/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.v2;

public enum StoreType {
  // Will be used when entity is not persisted on git but rather live in DATABASE
  INLINE,

  // Will be used when entity is in git repo
  REMOTE
}
