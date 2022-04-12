/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.EcrConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SumoConfig;
import software.wings.beans.config.LogzConfig;
import software.wings.delegatetasks.cv.beans.CustomLogResponseMapper;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.service.impl.analysis.CustomLogDataCollectionInfo;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.logz.LogzDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;

import java.util.Set;

public class DelegateTasksMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(NewRelicMetricDataRecord.class);
    set.add(DelegateConnectionResult.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    w.put("service.impl.analysis.DataCollectionTaskResult", DataCollectionTaskResult.class);
    w.put("service.impl.analysis.CustomLogDataCollectionInfo", CustomLogDataCollectionInfo.class);
    w.put("delegatetasks.cv.beans.CustomLogResponseMapper", CustomLogResponseMapper.class);
    w.put("beans.AppDynamicsConfig", AppDynamicsConfig.class);
    w.put("beans.NewRelicConfig", NewRelicConfig.class);
    w.put("beans.DynaTraceConfig", DynaTraceConfig.class);
    w.put("beans.SumoConfig", SumoConfig.class);
    w.put("service.impl.sumo.SumoDataCollectionInfo", SumoDataCollectionInfo.class);
    w.put("beans.config.LogzConfig", LogzConfig.class);
    w.put("beans.ElkConfig", ElkConfig.class);
    w.put("service.impl.elk.ElkDataCollectionInfo", ElkDataCollectionInfo.class);
    w.put("service.impl.logz.LogzDataCollectionInfo", LogzDataCollectionInfo.class);
    w.put("beans.EcrConfig", EcrConfig.class);
  }
}
