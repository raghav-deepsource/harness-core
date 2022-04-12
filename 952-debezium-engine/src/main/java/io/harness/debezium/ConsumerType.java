/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.debezium;

// Consumer type governs what all consumer types you can have
// Right now we only have EVENTS_FRAMEWORK but can be diff like HTTP which will
// just post the event s to an http endpoint
public enum ConsumerType { EVENTS_FRAMEWORK }
