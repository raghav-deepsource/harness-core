package io.harness.delegate.events;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.ResourceTypeConstants;
import io.harness.delegate.beans.DelegateSetupDetails;
import io.harness.ng.core.Resource;
import io.harness.ng.core.ResourceConstants;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@OwnedBy(HarnessTeam.DEL)
@Getter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateRegisterEvent extends AbstractDelegateConfigurationEvent {
  private String hostName;
  private DelegateSetupDetails delegateSetupDetails;
  public static final String DELEGATE_REGISTER_EVENT = "DelegateRegisterEvent";

  @Override
  public Resource getResource() {
    Map<String, String> labels = new HashMap<>();
    labels.put(ResourceConstants.LABEL_KEY_RESOURCE_NAME, delegateSetupDetails.getName());
    return Resource.builder()
        .identifier(delegateSetupDetails.getName())
        .labels(labels)
        .type(ResourceTypeConstants.DELEGATE)
        .build();
  }

  @Override
  public String getEventType() {
    return DELEGATE_REGISTER_EVENT;
  }
}
