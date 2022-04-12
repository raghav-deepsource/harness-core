/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.models;

import io.harness.ng.core.dto.secrets.BaseWinRmSpecDTO;
import io.harness.ng.core.dto.secrets.KerberosWinRmConfigDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Kerberos")
public class KerberosWinRmConfig extends KerberosBaseConfig implements BaseWinRmSpec {
  private String domain;
  private String username;
  private boolean useSSL;
  private boolean skipCertChecks;
  private boolean useNoProfile;

  @Override
  public BaseWinRmSpecDTO toDTO() {
    return KerberosWinRmConfigDTO.builder()
        .domain(domain)
        .username(username)
        .useSSL(useSSL)
        .skipCertChecks(skipCertChecks)
        .useNoProfile(useNoProfile)
        .principal(getPrincipal())
        .realm(getRealm())
        .tgtGenerationMethod(getTgtGenerationMethod())
        .spec(Optional.ofNullable(getSpec()).map(TGTGenerationSpec::toDTO).orElse(null))
        .build();
  }
}
