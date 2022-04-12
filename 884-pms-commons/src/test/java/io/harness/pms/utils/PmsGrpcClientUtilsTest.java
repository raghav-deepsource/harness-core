/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.utils;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.Getter;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsGrpcClientUtilsTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldRetryAndProcessExceptionForUnavailable() {
    RuntimeException throwable = new StatusRuntimeException(Status.UNAVAILABLE);
    ThrowingCall throwingCall = new ThrowingCall(throwable);
    assertThatThrownBy(() -> PmsGrpcClientUtils.retryAndProcessException(throwingCall::call, 0))
        .isInstanceOf(WingsException.class);
    assertThat(throwingCall.getCount()).isEqualTo(3);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldRetryAndProcessExceptionForUnknown() {
    RuntimeException throwable = new StatusRuntimeException(Status.UNKNOWN);
    ThrowingCall throwingCall2 = new ThrowingCall(throwable);
    assertThatThrownBy(() -> PmsGrpcClientUtils.retryAndProcessException(throwingCall2::call, 0))
        .isInstanceOf(WingsException.class);
    assertThat(throwingCall2.getCount()).isEqualTo(3);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldRetryAndProcessExceptionForDeadlineExceeded() {
    RuntimeException throwable = new StatusRuntimeException(Status.DEADLINE_EXCEEDED);
    ThrowingCall throwingCall = new ThrowingCall(throwable);
    assertThatThrownBy(() -> PmsGrpcClientUtils.retryAndProcessException(throwingCall::call, 0))
        .isInstanceOf(WingsException.class);
    assertThat(throwingCall.getCount()).isEqualTo(3);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldRetryAndProcessExceptionForResourceExhausted() {
    RuntimeException throwable = new StatusRuntimeException(Status.RESOURCE_EXHAUSTED);
    ThrowingCall throwingCall = new ThrowingCall(throwable);
    assertThatThrownBy(() -> PmsGrpcClientUtils.retryAndProcessException(throwingCall::call, 0))
        .isInstanceOf(WingsException.class);
    assertThat(throwingCall.getCount()).isEqualTo(3);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldRetryAndProcessExceptionForInternal() {
    RuntimeException throwable = new StatusRuntimeException(Status.INTERNAL);
    ThrowingCall throwingCall = new ThrowingCall(throwable);
    assertThatThrownBy(() -> PmsGrpcClientUtils.retryAndProcessException(throwingCall::call, 0))
        .isInstanceOf(WingsException.class);
    assertThat(throwingCall.getCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldRetryAndProcessExceptionForOther() {
    RuntimeException throwable = new RuntimeException("error");
    ThrowingCall throwingCall = new ThrowingCall(throwable);
    assertThatThrownBy(() -> PmsGrpcClientUtils.retryAndProcessException(throwingCall::call, 0))
        .isInstanceOf(WingsException.class);
    assertThat(throwingCall.getCount()).isEqualTo(1);
  }

  private static class ThrowingCall {
    private final RuntimeException exception;
    @Getter private int count = 0;

    ThrowingCall(RuntimeException exception) {
      this.exception = exception;
    }

    public int call(int ignored) {
      this.count++;
      if (exception != null) {
        throw exception;
      }
      return this.count;
    }
  }
}
