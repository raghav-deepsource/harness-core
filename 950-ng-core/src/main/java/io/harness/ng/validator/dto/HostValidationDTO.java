/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.validator.dto;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "This has validation details for the host")
@OwnedBy(CDP)
public class HostValidationDTO {
  public enum HostValidationStatus {
    SUCCESS,
    FAILED;

    public static HostValidationStatus fromBoolean(boolean status) {
      return status ? SUCCESS : FAILED;
    }
  }
  @Schema(description = "Hostname") private String host;
  @Schema(description = "This has the validation status for a host.") private HostValidationStatus status;
  @Schema(description = "Host error details") private ErrorDetail error;
}
