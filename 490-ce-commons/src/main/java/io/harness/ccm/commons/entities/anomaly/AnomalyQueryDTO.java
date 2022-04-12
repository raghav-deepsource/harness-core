/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.entities.anomaly;

import io.harness.ccm.commons.entities.CCMAggregation;
import io.harness.ccm.commons.entities.CCMFilter;
import io.harness.ccm.commons.entities.CCMGroupBy;
import io.harness.ccm.commons.entities.CCMSort;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(name = "AnomalyQueryDTO", description = "The query object for cost anomalies")
public class AnomalyQueryDTO {
  @Schema(description = "The filters for anomaly query") CCMFilter filter;
  @Schema(description = "The order by condition for anomaly query") List<CCMSort> orderBy;
  @Schema(description = "the group by clause for anomaly query") List<CCMGroupBy> groupBy;
  @Schema(description = "The aggregations for anomaly query") List<CCMAggregation> aggregations;
  @Schema(description = "The offset for anomaly query") Integer offset;
  @Schema(description = "The limit for anomaly query") Integer limit;
}
