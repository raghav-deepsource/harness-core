package io.harness.exception.runtime;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@OwnedBy(CE)
@EqualsAndHashCode(callSuper = true)
public class CCMConnectorRuntimeException extends RuntimeException {
  private String message;
  private String hint;
  private String explanation;
  private int code;

  public CCMConnectorRuntimeException(String message, String hint, String explanation, int code) {
    this.message = message;
    this.hint = hint;
    this.explanation = explanation;
    this.code = code;
  }
}
