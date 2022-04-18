package io.harness.cdng.visitor.helpers.serviceconfig;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.service.beans.DeploymentTemplateServiceSpec;
import io.harness.walktree.visitor.validation.ConfigValidator;
import io.harness.walktree.visitor.validation.ValidationVisitor;

@OwnedBy(HarnessTeam.CDP)
public class DeploymentTemplateServiceSpecVisitorHelper implements ConfigValidator {
  @Override
  public Object createDummyVisitableElement(Object originalElement) {
    return DeploymentTemplateServiceSpec.builder().build();
  }

  @Override
  public void validate(Object object, ValidationVisitor visitor) {
    // nothing to validate
  }
}
