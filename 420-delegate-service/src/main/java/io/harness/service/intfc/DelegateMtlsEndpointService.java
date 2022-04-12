/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateMtlsEndpointDetails;
import io.harness.delegate.beans.DelegateMtlsEndpointRequest;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateMtlsEndpointService {
  DelegateMtlsEndpointDetails createEndpointForAccount(String accountId, DelegateMtlsEndpointRequest endpointRequest);
  DelegateMtlsEndpointDetails updateEndpointForAccount(String accountId, DelegateMtlsEndpointRequest endpointRequest);
  DelegateMtlsEndpointDetails patchEndpointForAccount(String accountId, DelegateMtlsEndpointRequest endpointRequest);
  DelegateMtlsEndpointDetails getEndpointForAccount(String accountId);
  boolean deleteEndpointForAccount(String accountId);
  boolean isDomainPrefixAvailable(String fqdn);
}
