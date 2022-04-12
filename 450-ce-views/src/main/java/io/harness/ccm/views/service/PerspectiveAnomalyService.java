package io.harness.ccm.views.service;

import io.harness.ccm.commons.entities.anomaly.AnomalyData;

import java.time.Instant;
import java.util.List;
import lombok.NonNull;

public interface PerspectiveAnomalyService {
  List<AnomalyData> listPerspectiveAnomaliesForDate(
      @NonNull String accountIdentifier, @NonNull String perspectiveId, Instant date);
}
