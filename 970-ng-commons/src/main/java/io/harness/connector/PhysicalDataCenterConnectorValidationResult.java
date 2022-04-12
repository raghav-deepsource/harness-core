/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.connector;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.ErrorDetail;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Schema(description = "Validation details for the PDC Connector")
@Value
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CDP)
public class PhysicalDataCenterConnectorValidationResult extends ConnectorValidationResult {
  @Schema(description = "Validation passed host list") List<String> validationPassedHosts;
  @Schema(description = "Validation failed host list") List<String> validationFailedHosts;

  @Builder
  PhysicalDataCenterConnectorValidationResult(ConnectivityStatus status, List<ErrorDetail> errors, String errorSummary,
      long testedAt, String delegateId, List<String> validationPassedHosts, List<String> validationFailedHosts) {
    super(status, errors, errorSummary, testedAt, delegateId);
    this.validationPassedHosts = validationPassedHosts;
    this.validationFailedHosts = validationFailedHosts;
  }

  public static class PhysicalDataCenterConnectorValidationResultBuilder extends ConnectorValidationResultBuilder {}
}
