/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ng.validator.service.api;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.validator.dto.HostValidationDTO;

import java.util.List;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@OwnedBy(CDP)
public interface NGHostValidationService {
  /**
   * Validate SSH hosts credentials and connectivity.
   *
   * @param hosts the hosts (the host is host name and port number, or only host name)
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the account identifier
   * @param projectIdentifier the project identifier
   * @param secretIdentifierWithScope the secret identifier with scope
   * @return the list of host validation results
   */
  List<HostValidationDTO> validateSSHHosts(@NotNull List<String> hosts, String accountIdentifier,
      @Nullable String orgIdentifier, @Nullable String projectIdentifier, @NotNull String secretIdentifierWithScope);

  /**
   * Validate SSH host credentials and connectivity.
   *
   * @param host the host is host name and port number, or only host name
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the account identifier
   * @param projectIdentifier the project identifier
   * @param secretIdentifierWithScope the secret identifier with scope
   * @return host validation result
   */
  HostValidationDTO validateSSHHost(@NotNull String host, String accountIdentifier, @Nullable String orgIdentifier,
      @Nullable String projectIdentifier, @NotNull String secretIdentifierWithScope);
}
