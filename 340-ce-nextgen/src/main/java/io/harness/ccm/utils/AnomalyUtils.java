/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.utils;

import io.harness.ccm.commons.constants.AnomalyFieldConstants;
import io.harness.ccm.commons.constants.ViewFieldConstants;
import io.harness.ccm.commons.utils.TimeUtils;
import io.harness.timescaledb.tables.pojos.Anomalies;

public class AnomalyUtils {
  private static final String SEPARATOR = "/";

  public static String getRelativeTime(long anomalyTime, String template) {
    long currentTime = System.currentTimeMillis();
    long timeDiff = currentTime - anomalyTime;
    long days = timeDiff / TimeUtils.ONE_DAY_MILLIS;
    if (days != 0) {
      return days > 1 ? String.format(template, days, "days") : String.format(template, 1, "day");
    }
    long hours = timeDiff / TimeUtils.ONE_HOUR_MILLIS;
    if (hours != 0) {
      return hours > 1 ? String.format(template, days, "hours") : String.format(template, 1, "hour");
    }
    long minutes = timeDiff / TimeUtils.ONE_MINUTE_MILLIS;
    return minutes > 1 ? String.format(template, minutes, "minutes") : String.format(template, 1, "minute");
  }

  public static Double getAnomalyTrend(Double actualCost, Double expectedCost) {
    return expectedCost != 0 ? Math.round(((actualCost - expectedCost) / expectedCost) * 10000D) / 100D : 0;
  }

  public static String getResourceName(Anomalies anomaly) {
    StringBuilder builder = new StringBuilder();
    if (anomaly.getClustername() != null) {
      builder.append(anomaly.getClustername());
      if (anomaly.getNamespace() != null) {
        builder.append(SEPARATOR);
        builder.append(anomaly.getNamespace());
      }
      if (anomaly.getWorkloadname() != null) {
        builder.append(SEPARATOR);
        builder.append(anomaly.getWorkloadname());
      }
    } else if (anomaly.getGcpproject() != null) {
      builder.append(anomaly.getGcpproject());
      if (anomaly.getGcpproduct() != null) {
        builder.append(SEPARATOR);
        builder.append(anomaly.getGcpproduct());
      }
      if (anomaly.getGcpskudescription() != null) {
        builder.append(SEPARATOR);
        builder.append(anomaly.getGcpskudescription());
      }
    } else if (anomaly.getAwsaccount() != null) {
      builder.append(anomaly.getAwsaccount());
      if (anomaly.getAwsservice() != null) {
        builder.append(SEPARATOR);
        builder.append(anomaly.getAwsservice());
      }
      if (anomaly.getAwsusagetype() != null) {
        builder.append(SEPARATOR);
        builder.append(anomaly.getAwsusagetype());
      }
    }
    return builder.toString();
  }

  public static String getResourceInfo(Anomalies anomaly) {
    StringBuilder builder = new StringBuilder();
    if (anomaly.getClustername() != null) {
      builder.append(ViewFieldConstants.CLUSTER_NAME_FIELD_ID);
      if (anomaly.getNamespace() != null) {
        builder.append(SEPARATOR);
        builder.append(ViewFieldConstants.NAMESPACE_FIELD_ID);
      }
      if (anomaly.getWorkloadname() != null) {
        builder.append(SEPARATOR);
        builder.append(ViewFieldConstants.WORKLOAD_NAME_FIELD_ID);
      }
      return builder.toString();
    } else if (anomaly.getGcpproject() != null) {
      builder.append(ViewFieldConstants.GCP_PROJECT_FIELD_ID);
      if (anomaly.getGcpproduct() != null) {
        builder.append(SEPARATOR);
        builder.append(ViewFieldConstants.GCP_PRODUCT_FIELD_ID);
      }
      if (anomaly.getGcpskuid() != null) {
        builder.append(SEPARATOR);
        builder.append(ViewFieldConstants.GCP_SKU_DESCRIPTION_FIELD_ID);
      }
      return builder.toString();
    } else if (anomaly.getAwsaccount() != null) {
      builder.append(ViewFieldConstants.AWS_ACCOUNT_FIELD_ID);
      if (anomaly.getAwsservice() != null) {
        builder.append(SEPARATOR);
        builder.append(ViewFieldConstants.AWS_SERVICE_FIELD_ID);
      }
      if (anomaly.getAwsusagetype() != null) {
        builder.append(SEPARATOR);
        builder.append(ViewFieldConstants.AWS_USAGE_TYPE_ID);
      }
      return builder.toString();
    }

    return "";
  }

  public static String getGroupByField(Anomalies anomaly) {
    if (anomaly.getClustername() != null) {
      if (anomaly.getWorkloadname() != null) {
        return ViewFieldConstants.WORKLOAD_NAME_FIELD_ID;
      }
      if (anomaly.getNamespace() != null) {
        return ViewFieldConstants.NAMESPACE_FIELD_ID;
      }
      return ViewFieldConstants.CLUSTER_NAME_FIELD_ID;
    } else if (anomaly.getGcpproject() != null) {
      if (anomaly.getGcpskudescription() != null) {
        return ViewFieldConstants.GCP_SKU_DESCRIPTION_FIELD_ID;
      }
      if (anomaly.getGcpproduct() != null) {
        return ViewFieldConstants.GCP_PRODUCT_FIELD_ID;
      }
      return ViewFieldConstants.GCP_PROJECT_FIELD_ID;
    } else if (anomaly.getAwsaccount() != null) {
      if (anomaly.getAwsusagetype() != null) {
        return ViewFieldConstants.AWS_USAGE_TYPE_ID;
      }
      if (anomaly.getAwsservice() != null) {
        return ViewFieldConstants.AWS_SERVICE_FIELD_ID;
      }
      return ViewFieldConstants.AWS_ACCOUNT_FIELD_ID;
    }
    return null;
  }

  public static String getCloudProvider(Anomalies anomaly) {
    if (anomaly.getClustername() != null) {
      return AnomalyFieldConstants.CLUSTER.toUpperCase();
    } else if (anomaly.getAwsaccount() != null) {
      return AnomalyFieldConstants.AWS;
    } else if (anomaly.getGcpproject() != null) {
      return AnomalyFieldConstants.GCP;
    }
    return "";
  }
}
