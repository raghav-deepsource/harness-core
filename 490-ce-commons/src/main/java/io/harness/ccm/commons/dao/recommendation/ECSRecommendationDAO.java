/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.commons.dao.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.utils.TimeUtils.offsetDateTimeNow;
import static io.harness.ccm.commons.utils.TimeUtils.toOffsetDateTime;
import static io.harness.timescaledb.Tables.CE_RECOMMENDATIONS;

import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.newCpuHistogramV2;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.RecommenderUtils.newMemoryHistogramV2;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.retry.RetryOnException;
import io.harness.ccm.commons.beans.recommendation.ResourceType;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSPartialRecommendationHistogram;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSPartialRecommendationHistogram.ECSPartialRecommendationHistogramKeys;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSServiceRecommendation;
import io.harness.ccm.commons.entities.ecs.recommendation.ECSServiceRecommendation.ECSServiceRecommendationKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

@Slf4j
@Singleton
@OwnedBy(CE)
public class ECSRecommendationDAO {
  private static final int RETRY_COUNT = 3;
  private static final int SLEEP_DURATION = 100;

  @Inject private HPersistence hPersistence;
  @Inject private DSLContext dslContext;

  @NonNull
  public String savePartialRecommendation(ECSPartialRecommendationHistogram partialRecommendationHistogram) {
    return hPersistence.save(partialRecommendationHistogram);
  }

  @NonNull
  public Optional<ECSServiceRecommendation> fetchECSRecommendationById(
      @NonNull String accountIdentifier, @NonNull String id) {
    return Optional.ofNullable(hPersistence.createQuery(ECSServiceRecommendation.class)
                                   .filter(ECSServiceRecommendationKeys.accountId, accountIdentifier)
                                   .filter(ECSServiceRecommendationKeys.uuid, id)
                                   .get());
  }

  public List<ECSPartialRecommendationHistogram> fetchPartialRecommendationHistograms(@NonNull String accountId,
      @NonNull String clusterId, @NonNull String serviceArn, @NonNull Instant startTime, @NonNull Instant endTime) {
    return hPersistence.createQuery(ECSPartialRecommendationHistogram.class)
        .filter(ECSPartialRecommendationHistogramKeys.accountId, accountId)
        .filter(ECSPartialRecommendationHistogramKeys.clusterId, clusterId)
        .filter(ECSPartialRecommendationHistogramKeys.serviceArn, serviceArn)
        .field(ECSPartialRecommendationHistogramKeys.date)
        .greaterThanOrEq(startTime)
        .field(ECSPartialRecommendationHistogramKeys.date)
        .lessThanOrEq(endTime)
        .asList();
  }

  public ECSServiceRecommendation fetchServiceRecommendation(
      @NonNull String accountId, @NonNull String clusterId, @NonNull String serviceArn) {
    return Optional
        .ofNullable(hPersistence.createQuery(ECSServiceRecommendation.class)
                        .filter(ECSServiceRecommendationKeys.accountId, accountId)
                        .filter(ECSServiceRecommendationKeys.clusterId, clusterId)
                        .filter(ECSServiceRecommendationKeys.serviceArn, serviceArn)
                        .get())
        .orElseGet(()
                       -> ECSServiceRecommendation.builder()
                              .accountId(accountId)
                              .clusterId(clusterId)
                              .serviceArn(serviceArn)
                              .serviceName(serviceArn.substring(serviceArn.lastIndexOf('/') + 1))
                              .cpuHistogram(newCpuHistogramV2().saveToCheckpoint())
                              .memoryHistogram(newMemoryHistogramV2().saveToCheckpoint())
                              .build());
  }

  @NonNull
  public String saveRecommendation(ECSServiceRecommendation ecsServiceRecommendation) {
    return hPersistence.save(ecsServiceRecommendation);
  }

  @RetryOnException(retryCount = RETRY_COUNT, sleepDurationInMilliseconds = SLEEP_DURATION)
  public void upsertCeRecommendation(@NonNull String uuid, @NonNull String accountId, @NonNull String clusterName,
      @NonNull String serviceName, @Nullable Double monthlyCost, @Nullable Double monthlySaving,
      boolean shouldShowRecommendation, @NonNull Instant lastReceivedUntilAt) {
    dslContext.insertInto(CE_RECOMMENDATIONS)
        .set(CE_RECOMMENDATIONS.ACCOUNTID, accountId)
        .set(CE_RECOMMENDATIONS.ID, uuid)
        .set(CE_RECOMMENDATIONS.CLUSTERNAME, clusterName)
        .set(CE_RECOMMENDATIONS.NAME, serviceName)
        .set(CE_RECOMMENDATIONS.RESOURCETYPE, ResourceType.ECS_SERVICE.name())
        .set(CE_RECOMMENDATIONS.MONTHLYCOST, monthlyCost)
        .set(CE_RECOMMENDATIONS.MONTHLYSAVING, monthlySaving)
        .set(CE_RECOMMENDATIONS.ISVALID, shouldShowRecommendation)
        .set(CE_RECOMMENDATIONS.LASTPROCESSEDAT, toOffsetDateTime(lastReceivedUntilAt))
        .set(CE_RECOMMENDATIONS.UPDATEDAT, offsetDateTimeNow())
        .onConflictOnConstraint(CE_RECOMMENDATIONS.getPrimaryKey())
        .doUpdate()
        .set(CE_RECOMMENDATIONS.MONTHLYCOST, monthlyCost)
        .set(CE_RECOMMENDATIONS.MONTHLYSAVING, monthlySaving)
        .set(CE_RECOMMENDATIONS.ISVALID, shouldShowRecommendation)
        .set(CE_RECOMMENDATIONS.CLUSTERNAME, clusterName) // for updating older rows having clusterId instead
        .set(CE_RECOMMENDATIONS.LASTPROCESSEDAT, toOffsetDateTime(lastReceivedUntilAt))
        .set(CE_RECOMMENDATIONS.UPDATEDAT, offsetDateTimeNow())
        .execute();
  }
}
