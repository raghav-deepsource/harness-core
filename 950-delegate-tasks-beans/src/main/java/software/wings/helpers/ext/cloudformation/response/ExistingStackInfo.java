/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.helpers.ext.cloudformation.response;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExistingStackInfo {
  private boolean stackExisted;
  private String oldStackBody;
  private Map<String, String> oldStackParameters;
}
