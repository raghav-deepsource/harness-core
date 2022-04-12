/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.eraro.ErrorCode.ENTITY_NOT_FOUND;

import io.harness.eraro.Level;

public class EntityNotFoundException extends WingsException {
  private static final String MESSAGE_KEY = "message";

  public EntityNotFoundException(String message) {
    super(message, null, ENTITY_NOT_FOUND, Level.ERROR, null, null);
    param(MESSAGE_KEY, message);
  }

  public EntityNotFoundException(String message, Throwable cause) {
    super(message, cause, ENTITY_NOT_FOUND, Level.ERROR, null, null);
  }
}
