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
import io.harness.exception.EntityNotFoundException;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.service.intfc.DelegateMtlsEndpointService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.DuplicateKeyException;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateMtlsEndpointServiceImpl implements DelegateMtlsEndpointService {
  private static final String ERROR_ACCOUNT_NOT_FOUND_FORMAT = "Delegate mTLS endpoint for account '%s' was not found.";

  private static UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_ALL_SCHEMES);

  private final HPersistence persistence;
  private final String delegateMtlsSubdomain;

  @Inject
  public DelegateMtlsEndpointServiceImpl(
      HPersistence persistence, @Named("delegateMtlsSubdomain") String delegateMtlsSubdomain) {
    this.persistence = persistence;
    this.delegateMtlsSubdomain = delegateMtlsSubdomain;
  }

  @Override
  public DelegateMtlsEndpointDetails createEndpointForAccount(
      String accountId, DelegateMtlsEndpointRequest endpointRequest) {
    // validate request before further processing
    this.validateEndpointRequest(endpointRequest);

    String fqdn = this.getFqdn(endpointRequest.getDomainPrefix());
    DelegateMtlsEndpoint endpoint = DelegateMtlsEndpoint.builder()
                                        .uuid(generateUuid())
                                        .accountId(accountId)
                                        .fqdn(fqdn)
                                        .mode(endpointRequest.getMode())
                                        .caCertificates(endpointRequest.getCaCertificates())
                                        .build();

    try {
      persistence.save(endpoint);
    } catch (DuplicateKeyException e) {
      // For the exception message we assume that uuid / accountId isn't the cause of the duplicate, but it's the fqdn.
      throw new InvalidRequestException(format(
          "Delegate mTLS endpoint with given domain prefix '%s' already exists.", endpointRequest.getDomainPrefix()));
    }

    return this.buildEndpointDetails(endpoint);
  }

  @Override
  public DelegateMtlsEndpointDetails updateEndpointForAccount(
      String accountId, DelegateMtlsEndpointRequest endpointRequest) {
    // validate request before further processing
    this.validateEndpointRequest(endpointRequest);

    String fqdn = this.getFqdn(endpointRequest.getDomainPrefix());
    Query<DelegateMtlsEndpoint> query =
        persistence.createQuery(DelegateMtlsEndpoint.class).filter(DelegateMtlsEndpointKeys.accountId, accountId);

    UpdateOperations<DelegateMtlsEndpoint> updateOperations =
        persistence.createUpdateOperations(DelegateMtlsEndpoint.class)
            .set(DelegateMtlsEndpointKeys.fqdn, fqdn)
            .set(DelegateMtlsEndpointKeys.caCertificates, endpointRequest.getCaCertificates())
            .set(DelegateMtlsEndpointKeys.mode, endpointRequest.getMode());

    // only update (no insert) and return the resulting endpoint.
    FindAndModifyOptions options = new FindAndModifyOptions();
    options.returnNew(true);
    options.upsert(false);

    DelegateMtlsEndpoint updatedEndpoint = persistence.findAndModify(query, updateOperations, options);

    if (updatedEndpoint == null) {
      throw new EntityNotFoundException(String.format(ERROR_ACCOUNT_NOT_FOUND_FORMAT, accountId));
    }

    return this.buildEndpointDetails(updatedEndpoint);
  }

  @Override
  public DelegateMtlsEndpointDetails patchEndpointForAccount(
      String accountId, DelegateMtlsEndpointRequest patchRequest) {
    Query<DelegateMtlsEndpoint> query =
        persistence.createQuery(DelegateMtlsEndpoint.class).filter(DelegateMtlsEndpointKeys.accountId, accountId);

    // Build update operation based on the patch request
    boolean emptyPatch = true;
    UpdateOperations<DelegateMtlsEndpoint> updateOperations =
        persistence.createUpdateOperations(DelegateMtlsEndpoint.class);

    // patch domain prefix
    if (StringUtils.isNotEmpty(patchRequest.getDomainPrefix())) {
      this.validateDomainPrefix(patchRequest.getDomainPrefix());

      String fqdn = this.getFqdn(patchRequest.getDomainPrefix());
      updateOperations = updateOperations.set(DelegateMtlsEndpointKeys.fqdn, fqdn);
      emptyPatch = false;
    }

    // patch CA certificates
    if (StringUtils.isNotEmpty(patchRequest.getCaCertificates())) {
      this.validateCaCertificates(patchRequest.getCaCertificates());

      updateOperations =
          updateOperations.set(DelegateMtlsEndpointKeys.caCertificates, patchRequest.getCaCertificates());
      emptyPatch = false;
    }

    // patch mode
    if (patchRequest.getMode() != null) {
      updateOperations = updateOperations.set(DelegateMtlsEndpointKeys.mode, patchRequest.getMode());
      emptyPatch = false;
    }

    // throw in case the patch request was empty
    if (emptyPatch) {
      throw new InvalidRequestException("The patch request is empty.");
    }

    // only update (no insert) and return the full resulting endpoint.
    FindAndModifyOptions options = new FindAndModifyOptions();
    options.returnNew(true);
    options.upsert(false);

    DelegateMtlsEndpoint updatedEndpoint = persistence.findAndModify(query, updateOperations, options);

    if (updatedEndpoint == null) {
      throw new EntityNotFoundException(String.format(ERROR_ACCOUNT_NOT_FOUND_FORMAT, accountId));
    }

    return this.buildEndpointDetails(updatedEndpoint);
  }

  @Override
  public DelegateMtlsEndpointDetails getEndpointForAccount(String accountId) {
    DelegateMtlsEndpoint endpoint = persistence.createQuery(DelegateMtlsEndpoint.class)
                                        .field(DelegateMtlsEndpointKeys.accountId)
                                        .equal(accountId)
                                        .get();

    if (endpoint == null) {
      throw new EntityNotFoundException(String.format(ERROR_ACCOUNT_NOT_FOUND_FORMAT, accountId));
    }

    return this.buildEndpointDetails(endpoint);
  }

  @Override
  public boolean deleteEndpointForAccount(String accountId) {
    Query<DelegateMtlsEndpoint> query =
        persistence.createQuery(DelegateMtlsEndpoint.class).filter(DelegateMtlsEndpointKeys.accountId, accountId);

    return persistence.delete(query);
  }

  @Override
  public boolean isDomainPrefixAvailable(String domainPrefix) {
    String fqdn = this.getFqdn(domainPrefix);
    DelegateMtlsEndpoint endpoint =
        persistence.createQuery(DelegateMtlsEndpoint.class).field(DelegateMtlsEndpointKeys.fqdn).equal(fqdn).get();

    return endpoint == null;
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

  /**
   * Validates a complete endpoint request to ensure its correctness.
   *
   * @param endpointRequest The request to validate.
   * @throws InvalidRequestException If the request is invalid.
   */
  private void validateEndpointRequest(DelegateMtlsEndpointRequest endpointRequest) {
    // validate certificates
    this.validateCaCertificates(endpointRequest.getCaCertificates());

    // validate domain prefix
    this.validateDomainPrefix(endpointRequest.getDomainPrefix());

    // validate mode
    if (endpointRequest.getMode() == null) {
      throw new InvalidRequestException("No delegate mTLS mode was provided.");
    }
  }

  /**
   * Validates a list of CA certificates provided in PEM format.
   *
   * @param caCertificates The certificates to verify.
   * @throws InvalidRequestException If any of the certificates is invalid.
   */
  private void validateCaCertificates(String caCertificates) {
    // TODO: Add more CERT validations.
    if (StringUtils.isBlank(caCertificates)) {
      throw new InvalidRequestException("No CA certificate was provided for the delegate mTLS endpoint.");
    }
  }

  /**
   * Validates a provided domain prefix (doesn't verify if it's already taken).
   *
   * @param domainPrefix The domain prefix to verify.
   * @throws InvalidRequestException If the domain prefix is invalid.
   */
  private void validateDomainPrefix(String domainPrefix) {
    // domain prefix is mandatory.
    if (StringUtils.isBlank(domainPrefix)) {
      throw new InvalidRequestException("No domain prefix was provided for the delegate mTLS endpoint.");
    }

    // no multi-subdomain prefix is allowed - otherwise our wildcard server certificate won't work.
    if (domainPrefix.contains(".")) {
      throw new InvalidRequestException(
          String.format("Provided domain prefix '%s' has more than one level.", domainPrefix));
    }

    // ensure the resulting fqdn is valid.
    String fqdn = this.getFqdn(domainPrefix);
    if (!urlValidator.isValid("http://" + fqdn)) {
      throw new InvalidRequestException(
          String.format("Provided domain prefix '%s' is invalid (full fqdn: '%s').", domainPrefix, fqdn));
    }
  }

  /**
   * Creates the fully qualified domain name for a delegate mTLS endpoint with the given domain prefix.
   *
   * @param domainPrefix The domain prefix.
   * @return The FQDN.
   */
  private String getFqdn(String domainPrefix) {
    return String.format("%s.%s", domainPrefix, this.delegateMtlsSubdomain);
  }
}
