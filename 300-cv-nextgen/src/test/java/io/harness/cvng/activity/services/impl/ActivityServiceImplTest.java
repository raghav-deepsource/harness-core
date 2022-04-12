/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.activity.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.FAILED_TO_ACQUIRE_PERSISTENT_LOCK;
import static io.harness.exception.WingsException.SRE;
import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.KANHAIYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.beans.ActivityVerificationSummary;
import io.harness.cvng.activity.beans.DeploymentActivityResultDTO;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.ActivityVerificationStatus;
import io.harness.cvng.beans.job.Sensitivity;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.cdng.services.api.CVNGStepTaskService;
import io.harness.cvng.core.beans.monitoredService.healthSouceSpec.HealthSourceDTO;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.verificationjob.entities.CanaryVerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance;
import io.harness.cvng.verificationjob.services.api.VerificationJobInstanceService;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.exception.PersistentLockException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ActivityServiceImplTest extends CvNextGenTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private ActivityService activityService;
  @Inject private VerificationJobService realVerificationJobService;
  @Inject private VerificationJobInstanceService realVerificationJobInstanceService;
  @Inject private CVNGStepTaskService cvngStepTaskService;
  @Mock private VerificationJobService verificationJobService;
  @Mock private VerificationJobInstanceService verificationJobInstanceService;
  @Mock private PersistentLocker mockedPersistentLocker;

  private String projectIdentifier;
  private String orgIdentifier;
  private String accountId;
  private Instant instant;
  private String serviceIdentifier;
  private String envIdentifier;
  private String deploymentTag;
  private BuilderFactory builderFactory;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    builderFactory = BuilderFactory.getDefault();
    instant = Instant.parse("2020-07-27T10:44:06.390Z");
    projectIdentifier = builderFactory.getContext().getProjectIdentifier();
    orgIdentifier = builderFactory.getContext().getOrgIdentifier();
    accountId = builderFactory.getContext().getAccountId();
    serviceIdentifier = builderFactory.getContext().getServiceIdentifier();
    envIdentifier = builderFactory.getContext().getEnvIdentifier();
    deploymentTag = "build#1";

    FieldUtils.writeField(activityService, "verificationJobInstanceService", verificationJobInstanceService, true);
    when(verificationJobInstanceService.getCVConfigsForVerificationJob(any()))
        .thenReturn(Lists.newArrayList(new AppDynamicsCVConfig()));
    realVerificationJobService.createDefaultVerificationJobs(accountId, orgIdentifier, projectIdentifier);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpsert_createEntityIsEqual() {
    useMockedPersistentLocker();
    Activity activity = builderFactory.getDeploymentActivityBuilder().build();
    String activityUuid = activityService.upsert(activity);
    Activity activityFromDb = activityService.get(activityUuid);
    assertThat(activityFromDb).isEqualTo(activity);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpsert_updateEntity() {
    useMockedPersistentLocker();
    Activity existingActivity = builderFactory.getDeploymentActivityBuilder().build();
    Activity updatingActivity = builderFactory.getDeploymentActivityBuilder().build();
    List<String> verificationJobInstanceIds = new ArrayList<>();
    verificationJobInstanceIds.addAll(existingActivity.getVerificationJobInstanceIds());
    verificationJobInstanceIds.addAll(updatingActivity.getVerificationJobInstanceIds());
    ActivityVerificationSummary summary = createActivitySummary(Instant.now());
    summary.setTotal(2);
    summary.setPassed(2);
    summary.setProgress(0);

    List<VerificationJobInstance> jobInstances = Arrays.asList(builderFactory.verificationJobInstanceBuilder()
                                                                   .deploymentStartTime(Instant.now())
                                                                   .startTime(Instant.now())
                                                                   .uuid(verificationJobInstanceIds.get(0))
                                                                   .build(),
        builderFactory.verificationJobInstanceBuilder()
            .deploymentStartTime(Instant.now())
            .startTime(Instant.now())
            .uuid(verificationJobInstanceIds.get(1))
            .build());
    when(verificationJobInstanceService.get(existingActivity.getVerificationJobInstanceIds())).thenReturn(jobInstances);
    when(verificationJobInstanceService.getActivityVerificationSummary(eq(jobInstances))).thenReturn(summary);

    activityService.upsert(existingActivity);
    String uuid = activityService.upsert(updatingActivity);

    Activity activityFromDb = activityService.get(uuid);
    assertThat(activityFromDb.getVerificationJobInstanceIds()).isEqualTo(verificationJobInstanceIds);
    assertThat(activityFromDb.getVerificationSummary()).isEqualTo(summary);
    assertThat(activityFromDb.getActivityName()).isEqualTo(updatingActivity.getActivityName());
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpsert_whenEntityLocked() {
    useMockedPersistentLocker();
    when(mockedPersistentLocker.waitToAcquireLock(any(), any(), any(), any()))
        .thenThrow(new PersistentLockException("Lock not acquired", FAILED_TO_ACQUIRE_PERSISTENT_LOCK, SRE));
    Activity activity = builderFactory.getDeploymentActivityBuilder().build();
    assertThatThrownBy(() -> activityService.upsert(activity)).isInstanceOf(PersistentLockException.class);
    assertThat(activity.getUuid()).isNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testUpsert_lockClose() {
    useMockedPersistentLocker();
    AcquiredLock acquiredLock = mock(AcquiredLock.class);
    when(mockedPersistentLocker.waitToAcquireLock(any(), any(), any(), any())).thenReturn(acquiredLock);
    activityService.upsert(builderFactory.getDeploymentActivityBuilder().build());
    verify(acquiredLock).close();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetActivityStatus() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    String activityId = activityService.createActivity(builderFactory.getDeploymentActivityBuilder()
                                                           .verificationJobs(Collections.singletonList(verificationJob))
                                                           .build());
    DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary deploymentVerificationJobInstanceSummary =
        DeploymentActivityResultDTO.DeploymentVerificationJobInstanceSummary.builder()
            .durationMs(verificationJob.getDuration().toMillis())
            .status(ActivityVerificationStatus.NOT_STARTED)
            .build();
    when(verificationJobInstanceService.getDeploymentVerificationJobInstanceSummary(anyList()))
        .thenReturn(deploymentVerificationJobInstanceSummary);
    assertThat(deploymentVerificationJobInstanceSummary.getActivityId()).isNull();
    ActivityStatusDTO activityStatusDTO = activityService.getActivityStatus(accountId, activityId);
    assertThat(activityStatusDTO.getActivityId()).isEqualTo(activityId);
    assertThat(activityStatusDTO.getDurationMs()).isEqualTo(verificationJob.getDuration().toMillis());
    assertThat(activityStatusDTO.getStatus()).isEqualTo(ActivityVerificationStatus.NOT_STARTED);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testAbort_inNotStarted() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();
    String activityId = activityService.createActivity(builderFactory.getDeploymentActivityBuilder()
                                                           .verificationJobs(Collections.singletonList(verificationJob))
                                                           .build());
    Activity activity = activityService.get(activityId);
    List<String> verificationJobs = Lists.newArrayList("JOB_INSTANCE_ID");
    activity.setVerificationJobInstanceIds(verificationJobs);
    hPersistence.save(activity);

    activityService.abort(activityId);

    Activity updatedActivity = activityService.get(activityId);
    verify(verificationJobInstanceService).abort(Lists.newArrayList(updatedActivity.getVerificationJobInstanceIds()));
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testAbort_inError() {
    VerificationJob verificationJob = createVerificationJob();
    when(verificationJobService.getVerificationJob(
             accountId, orgIdentifier, projectIdentifier, verificationJob.getIdentifier()))
        .thenReturn(verificationJob);
    instant = Instant.now();
    String activityId = activityService.createActivity(builderFactory.getDeploymentActivityBuilder()
                                                           .verificationJobs(Collections.singletonList(verificationJob))
                                                           .build());
    Activity activity = activityService.get(activityId);
    activity.setAnalysisStatus(ActivityVerificationStatus.ERROR);
    hPersistence.save(activity);

    activityService.abort(activityId);

    Activity updatedActivity = activityService.get(activityId);
    // assert that errored activity is not aborted
    assertThat(updatedActivity.getAnalysisStatus()).isEqualTo(ActivityVerificationStatus.ERROR);
    verify(verificationJobInstanceService, never()).abort(any());
  }

  @Test
  @Owner(developers = KANHAIYA)
  @Category(UnitTests.class)
  public void testHealthSources() throws IllegalAccessException {
    String verificationJobInstanceId = generateUuid();
    String cvConfigIdentifier = "nameSpaced/identifier";
    String activityId = "activityId";
    CVConfig cvConfig = builderFactory.appDynamicsCVConfigBuilder().identifier(cvConfigIdentifier).build();
    CVNGStepTask cvngStepTask = builderFactory.cvngStepTaskBuilder()
                                    .accountId(accountId)
                                    .skip(true)
                                    .callbackId(activityId)
                                    .verificationJobInstanceId(verificationJobInstanceId)
                                    .status(CVNGStepTask.Status.IN_PROGRESS)
                                    .build();
    cvngStepTaskService.create(cvngStepTask);
    VerificationJobInstance verificationJobInstance = builderFactory.verificationJobInstanceBuilder()
                                                          .uuid(verificationJobInstanceId)
                                                          .cvConfigMap(new HashMap<String, CVConfig>() {
                                                            { put(cvConfigIdentifier, cvConfig); }
                                                          })
                                                          .build();
    realVerificationJobInstanceService.create(verificationJobInstance);
    FieldUtils.writeField(activityService, "verificationJobInstanceService", realVerificationJobInstanceService, true);
    Set<HealthSourceDTO> healthSourceDTOSet = activityService.healthSources(accountId, activityId);
    assertThat(healthSourceDTOSet.size()).isEqualTo(1);
    assertThat(healthSourceDTOSet.iterator().next().getIdentifier()).isEqualTo(cvConfigIdentifier);
  }

  private VerificationJob createVerificationJob() {
    CanaryVerificationJob testVerificationJob = new CanaryVerificationJob();
    testVerificationJob.setUuid(generateUuid());
    testVerificationJob.setAccountId(accountId);
    testVerificationJob.setIdentifier("identifier");
    testVerificationJob.setJobName(generateUuid());
    testVerificationJob.setMonitoringSources(Collections.singletonList("monitoringIdentifier"));
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    testVerificationJob.setServiceIdentifier(serviceIdentifier, false);
    testVerificationJob.setEnvIdentifier(envIdentifier, false);
    testVerificationJob.setDuration(Duration.ofMinutes(5));
    testVerificationJob.setProjectIdentifier(projectIdentifier);
    testVerificationJob.setOrgIdentifier(orgIdentifier);
    return testVerificationJob;
  }

  private ActivityVerificationSummary createActivitySummary(Instant startTime) {
    return ActivityVerificationSummary.builder()
        .total(1)
        .startTime(startTime.toEpochMilli())
        .risk(Risk.OBSERVE)
        .progress(1)
        .notStarted(0)
        .durationMs(Duration.ofMinutes(15).toMillis())
        .remainingTimeMs(1200000)
        .progressPercentage(25)
        .build();
  }

  @SneakyThrows
  private void useMockedPersistentLocker() {
    FieldUtils.writeField(activityService, "persistentLocker", mockedPersistentLocker, true);
  }
}
