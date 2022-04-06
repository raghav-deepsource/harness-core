/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto.secrets;

import io.harness.ng.core.models.SecretSpec;
import io.harness.ng.core.models.WinRmCredentialsSpec;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("WinRmCredentials")
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(name = "WinRmCredentialsSpec", description = "This is the WinRm authentication details defined in Harness.")
public class WinRmCredentialsSpecDTO extends SecretSpecDTO {
  @Schema(description = "WinRm port") int port;
  @NotNull WinRmAuthDTO auth;

  @Override
  @JsonIgnore
  public Optional<String> getErrorMessageForInvalidYaml() {
    return Optional.empty();
  }

  @Override
  public SecretSpec toEntity() {
    return WinRmCredentialsSpec.builder().port(getPort()).auth(this.auth.toEntity()).build();
  }

  @Builder
  public WinRmCredentialsSpecDTO(int port, WinRmAuthDTO auth) {
    this.port = port;
    this.auth = auth;
  }
}
