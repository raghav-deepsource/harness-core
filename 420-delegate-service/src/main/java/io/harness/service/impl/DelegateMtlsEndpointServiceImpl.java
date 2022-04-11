/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.*;
import io.harness.delegate.beans.DelegateMtlsEndpoint.DelegateMtlsEndpointKeys;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateMtlsEndpointService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DuplicateKeyException;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateMtlsEndpointServiceImpl implements DelegateMtlsEndpointService {
  private final HPersistence persistence;

  @Inject
  public DelegateMtlsEndpointServiceImpl(HPersistence persistence) {
    this.persistence = persistence;
  }

  @Override
  public DelegateMtlsEndpointDetails createEndpointForAccount(
      String accountId, DelegateMtlsEndpointRequest endpointRequest) {
    DelegateMtlsEndpoint endpoint = DelegateMtlsEndpoint.builder()
                                        .uuid(generateUuid())
                                        .accountId(accountId)
                                        .fqdn(endpointRequest.getFqdn())
                                        .mode(endpointRequest.getMode())
                                        .caCertificates(endpointRequest.getCaCertificates())
                                        .build();

    try {
      persistence.save(endpoint);
    } catch (DuplicateKeyException e) {
      // For the exception message we assume that uuid / accountId isn't the cause of the duplicate, but it's the fqdn.
      throw new InvalidRequestException(
          format("Delegate mTLS endpoint with given fqdn '%s' already exists.", endpointRequest.getFqdn()));
    }

    return this.buildEndpointDetails(endpoint);
  }

  @Override
  public DelegateMtlsEndpointDetails updateEndpointForAccount(
      String accountId, DelegateMtlsEndpointRequest endpointRequest) {
    Query<DelegateMtlsEndpoint> query =
        persistence.createQuery(DelegateMtlsEndpoint.class).filter(DelegateMtlsEndpointKeys.accountId, accountId);

    UpdateOperations<DelegateMtlsEndpoint> updateOperations =
        persistence.createUpdateOperations(DelegateMtlsEndpoint.class)
            .set(DelegateMtlsEndpointKeys.fqdn, endpointRequest.getFqdn())
            .set(DelegateMtlsEndpointKeys.caCertificates, endpointRequest.getCaCertificates())
            .set(DelegateMtlsEndpointKeys.mode, endpointRequest.getMode());

    DelegateMtlsEndpoint updatedEndpoint =
        persistence.findAndModify(query, updateOperations, new FindAndModifyOptions());

    return this.buildEndpointDetails(updatedEndpoint);
  }

  @Override
  public DelegateMtlsEndpointDetails getEndpointForAccount(String accountId) {
    DelegateMtlsEndpoint endpoint = persistence.createQuery(DelegateMtlsEndpoint.class)
                                        .field(DelegateMtlsEndpointKeys.accountId)
                                        .equal(accountId)
                                        .get();

    if (endpoint == null) {
      return null;
    }

    return endpoint == null ? null : this.buildEndpointDetails(endpoint);
  }

  @Override
  public boolean isEndpointFqdnAvailable(String fqdn) {
    DelegateMtlsEndpoint endpoint =
        persistence.createQuery(DelegateMtlsEndpoint.class).field(DelegateMtlsEndpointKeys.fqdn).equal(fqdn).get();

    return endpoint == null;
  }

  @Override
  public boolean deleteEndpointForAccount(String accountId) {
    Query<DelegateMtlsEndpoint> query =
        persistence.createQuery(DelegateMtlsEndpoint.class).filter(DelegateMtlsEndpointKeys.accountId, accountId);

    return persistence.delete(query);
  }

  private DelegateMtlsEndpointDetails buildEndpointDetails(DelegateMtlsEndpoint endpoint) {
    return DelegateMtlsEndpointDetails.builder()
        .uuid(endpoint.getUuid())
        .accountId(endpoint.getAccountId())
        .fqdn(endpoint.getFqdn())
        .caCertificates(endpoint.getCaCertificates())
        .mode(endpoint.getMode())
        .build();
  }
}
