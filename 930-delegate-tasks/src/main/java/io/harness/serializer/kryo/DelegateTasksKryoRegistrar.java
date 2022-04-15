/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.kryo;

import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskParameters;
import io.harness.delegate.task.executioncapability.BatchCapabilityCheckTaskResponse;
import io.harness.delegate.task.winrm.AuthenticationScheme;
import io.harness.serializer.KryoRegistrar;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.config.LogzConfig;
import software.wings.beans.trigger.WebHookTriggerResponseData;
import software.wings.beans.trigger.WebhookTriggerParameters;
import software.wings.delegatetasks.DelegateStateType;
import software.wings.delegatetasks.buildsource.BuildCollectParameters;
import software.wings.delegatetasks.cv.DataCollectionException;
import software.wings.delegatetasks.cv.beans.CustomLogResponseMapper;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.LogElement;
import software.wings.service.impl.analysis.SetupTestNodeData;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsSetupTestNodeData;
import software.wings.service.impl.dynatrace.DynaTraceApplication;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.impl.dynatrace.DynaTraceMetricDataResponse;
import software.wings.service.impl.dynatrace.DynaTraceSetupTestNodeData;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.elk.ElkLogFetchRequest;
import software.wings.service.impl.elk.ElkQueryType;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicSetupTestNodeData;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.intfc.analysis.ClusterLevel;
import software.wings.utils.ArtifactType;

import com.esotericsoftware.kryo.Kryo;

public class DelegateTasksKryoRegistrar implements KryoRegistrar {
  @Override
  public void register(Kryo kryo) {
    kryo.register(EcrConfig.class, 5011);
    kryo.register(ElkConfig.class, 5017);
    kryo.register(ExecutionLogCallback.class, 5044);
    kryo.register(ElkDataCollectionInfo.class, 5169);
    kryo.register(LogzDataCollectionInfo.class, 5170);
    kryo.register(NewRelicDataCollectionInfo.class, 5171);
    kryo.register(SumoDataCollectionInfo.class, 5173);
    kryo.register(NewRelicConfig.class, 5175);
    kryo.register(LogzConfig.class, 5176);
    kryo.register(SumoConfig.class, 5178);
    kryo.register(DynaTraceTimeSeries.class, 5239);
    kryo.register(DynaTraceConfig.class, 5237);
    kryo.register(DynaTraceDataCollectionInfo.class, 5238);
    kryo.register(AnalysisComparisonStrategy.class, 5240);
    kryo.register(ElkQueryType.class, 5275);
    kryo.register(ElkLogFetchRequest.class, 5376);
    kryo.register(DynaTraceMetricDataResponse.class, 5513);
    kryo.register(DynaTraceMetricDataResponse.DynaTraceMetricDataResult.class, 5514);
    kryo.register(DynaTraceSetupTestNodeData.class, 5512);
    kryo.register(NewRelicSetupTestNodeData.class, 5529);
    kryo.register(DataCollectionException.class, 7298);
    kryo.register(BatchCapabilityCheckTaskParameters.class, 8200);
    kryo.register(BatchCapabilityCheckTaskResponse.class, 8201);
    kryo.register(WebhookTriggerParameters.class, 8550);
    kryo.register(WebHookTriggerResponseData.class, 8552);
    kryo.register(AuthenticationScheme.class, 8600);
    kryo.register(AppDynamicsConfig.class, 5074);
    kryo.register(CustomLogDataCollectionInfo.class, 5492);
    kryo.register(DataCollectionTaskResult.DataCollectionTaskStatus.class, 5185);
    kryo.register(DataCollectionTaskResult.class, 5184);
    kryo.register(LogElement.class, 5486);
    kryo.register(SetupTestNodeData.class, 5530);
    kryo.register(SetupTestNodeData.Instance.class, 7470);
    kryo.register(AppdynamicsSetupTestNodeData.class, 5531);
    kryo.register(AppdynamicsDataCollectionInfo.class, 5168);
    kryo.register(TimeSeriesMlAnalysisType.class, 5347);
    kryo.register(CustomLogResponseMapper.class, 5493);
    kryo.register(NewRelicMetricDataRecord.class, 7347);
    kryo.register(ClusterLevel.class, 7348);
    kryo.register(DynaTraceApplication.class, 8074);
    kryo.register(DelegateStateType.class, 8601);
    kryo.register(ArtifactType.class, 5117);
    kryo.register(BuildCollectParameters.class, 8602);
  }
}
