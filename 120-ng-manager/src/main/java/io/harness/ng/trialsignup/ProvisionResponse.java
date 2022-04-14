package io.harness.ng.trialsignup;

public class ProvisionResponse {
  enum Status {
    SUCCESS,
    DELEGATE_PROVISION_FAILURE,
    K8S_CONNECTOR_PROVISION_FAILURE,
    DOCKER_CONNECTOR_PROVISION_FAILURE,
  }
}
