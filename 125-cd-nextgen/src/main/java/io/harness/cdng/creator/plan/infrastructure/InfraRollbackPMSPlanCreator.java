/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.infrastructure;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.beans.RollbackNode;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters;
import io.harness.cdng.pipeline.beans.RollbackOptionalChildChainStepParameters.RollbackOptionalChildChainStepParametersBuilder;
import io.harness.cdng.pipeline.steps.RollbackOptionalChildChainStep;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.plancreator.NGCommonUtilPlanCreationConstants;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;
import io.harness.pms.yaml.DependenciesUtils;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDC)
public class InfraRollbackPMSPlanCreator {
  public static final String INFRA_ROLLBACK_NODE_ID_SUFFIX = "_infraRollback";

  public static PlanCreationResponse createInfraRollbackPlan(PlanCreationContext ctx, YamlField infraField) {
    if (!isDynamicallyProvisioned(infraField)) {
      return PlanCreationResponse.builder().build();
    }

    Map<String, YamlField> dependencies = new HashMap();

    YamlField provisionerField = infraField.getNode()
                                     .getField(YamlTypes.INFRASTRUCTURE_DEF)
                                     .getNode()
                                     .getField(YAMLFieldNameConstants.PROVISIONER);

    RollbackOptionalChildChainStepParametersBuilder stepParametersBuilder =
        RollbackOptionalChildChainStepParameters.builder();

    YamlField rollbackStepsField = provisionerField.getNode().getField(YAMLFieldNameConstants.ROLLBACK_STEPS);

    if (rollbackStepsField != null && rollbackStepsField.getNode() != null
        && rollbackStepsField.getNode().asArray().size() > 0) {
      // Adding dependencies
      dependencies.put(
          rollbackStepsField.getNode().getUuid() + NGCommonUtilPlanCreationConstants.ROLLBACK_STEPS_NODE_ID_SUFFIX,
          rollbackStepsField);
      stepParametersBuilder.childNode(RollbackNode.builder()
                                          .nodeId(rollbackStepsField.getNode().getUuid()
                                              + NGCommonUtilPlanCreationConstants.ROLLBACK_STEPS_NODE_ID_SUFFIX)
                                          .shouldAlwaysRun(true)
                                          .build());
    }

    if (isEmpty(stepParametersBuilder.build().getChildNodes())) {
      return PlanCreationResponse.builder().build();
    }

    PlanNode infraRollbackNode =
        PlanNode.builder()
            .uuid(infraField.getNode().getUuid() + INFRA_ROLLBACK_NODE_ID_SUFFIX)
            .name(NGCommonUtilPlanCreationConstants.INFRA_ROLLBACK_NODE_NAME)
            .identifier(NGCommonUtilPlanCreationConstants.INFRA_ROLLBACK_NODE_IDENTIFIER)
            .stepType(RollbackOptionalChildChainStep.STEP_TYPE)
            .stepParameters(stepParametersBuilder.build())
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD_CHAIN).build())
                    .build())
            .skipExpressionChain(false)
            .build();

    return PlanCreationResponse.builder()
        .node(infraRollbackNode.getUuid(), infraRollbackNode)
        .dependencies(DependenciesUtils.toDependenciesProto(dependencies))
        .build();
  }

  private static boolean isDynamicallyProvisioned(YamlField infraField) {
    if (infraField == null) {
      return false;
    }
    YamlField infraDefField = infraField.getNode().getField(YamlTypes.INFRASTRUCTURE_DEF);
    if (infraDefField == null) {
      return false;
    }
    YamlField provisionerField = infraDefField.getNode().getField(YAMLFieldNameConstants.PROVISIONER);
    return provisionerField != null;
  }
}
