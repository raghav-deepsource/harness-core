package io.harness.cdng.service.beans;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSetWrapper;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.ManifestOverrideSetWrapper;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.variables.beans.NGVariableOverrideSetWrapper;
import io.harness.cdng.visitor.helpers.serviceconfig.DeploymentTemplateServiceSpecVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName(ServiceSpecType.DEPLOYMENT_TEMPLATE)
@SimpleVisitorHelper(helperClass = DeploymentTemplateServiceSpecVisitorHelper.class)
@TypeAlias("DeploymentTemplateServiceSpec")
@RecasterAlias("io.harness.cdng.service.beans.DeploymentTemplateServiceSpec")
@OwnedBy(HarnessTeam.CDP)
public class DeploymentTemplateServiceSpec implements ServiceSpec, Visitable {
  List<NGVariable> variables;
  ArtifactListConfig artifacts;
  List<ManifestConfigWrapper> manifests;

  List<NGVariableOverrideSetWrapper> variableOverrideSets;
  List<ArtifactOverrideSetWrapper> artifactOverrideSets;
  List<ManifestOverrideSetWrapper> manifestOverrideSets;

  String metadata;

  @Override
  public String getType() {
    return ServiceDefinitionType.DEPLOYMENT_TEMPLATE.getYamlName();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    if (EmptyPredicate.isNotEmpty(variables)) {
      variables.forEach(ngVariable -> children.add("variables", ngVariable));
    }

    children.add("artifacts", artifacts);
    if (EmptyPredicate.isNotEmpty(artifactOverrideSets)) {
      artifactOverrideSets.forEach(artifactOverrideSet -> children.add("artifactOverrideSets", artifactOverrideSet));
    }
    return children;
  }
}
