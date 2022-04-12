/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans;

/**
 * Specifies the supported modes for delegate mTLS endpoints.
 */
public enum DelegateMtlsMode {
  /**
   * Allows both mTLS and non-mTLS delegates to connect to the manager.
   * This allows customers to seamlessly migrate their delegate fleet to mTLS.
   */
  LOOSE,

  /**
   * Allows only mTLS delegates to connect to the manager - non-mTLS delegates are rejected.
   */
  STRICT
}
