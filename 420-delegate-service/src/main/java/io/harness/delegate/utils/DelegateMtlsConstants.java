/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DEL)
public class DelegateMtlsConstants {
  // mTLS delegate related REST api entrypoints.
  public static final String API_PATH = "/delegate-mtls";
  public static final String API_PATH_NG_INTERNAL = API_PATH + "/ng";

  public static final String API_PATH_ENDPOINT = "endpoint";
  public static final String API_PATH_ENDPOINT_CHECK_AVAILABILITY = "check-availability";

  // Common parameter names and descriptions.
  public static final String API_PARAM_NAME_DOMAIN_PREFIX = "domainPrefix";
  public static final String API_PARAM_DESCRIPTION_DOMAIN_PREFIX = "The domain prefix to check.";
}
