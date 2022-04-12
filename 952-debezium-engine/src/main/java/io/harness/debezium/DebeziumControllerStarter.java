/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 * This class manages the lifecycle of Debezium Controller threads.
 */

package io.harness.debezium;

import io.harness.lock.PersistentLocker;
import io.harness.redis.RedisConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DebeziumControllerStarter {
  @Inject @Named("DebeziumExecutorService") private ExecutorService debeziumExecutorService;
  @Inject private ChangeConsumerFactory consumerFactory;

  @SuppressWarnings({"rawtypes", "unchecked"})
  public void startDebeziumController(DebeziumConfig debeziumConfig, ChangeConsumerConfig changeConsumerConfig,
      PersistentLocker locker, RedisConfig redisLockConfig) {
    String monitoredDb = debeziumConfig.getDatabaseIncludeList();
    try {
      MongoDatabaseChangeConsumer changeConsumer = consumerFactory.get(monitoredDb, changeConsumerConfig);
      DebeziumController debeziumController =
          new DebeziumController(DebeziumConfiguration.getDebeziumProperties(debeziumConfig, redisLockConfig),
              changeConsumer, locker, debeziumExecutorService);
      debeziumExecutorService.submit(debeziumController);
    } catch (Exception e) {
      log.error("Cannot Start Debezium Controller for Database {}", monitoredDb, e);
    }
  }
}
