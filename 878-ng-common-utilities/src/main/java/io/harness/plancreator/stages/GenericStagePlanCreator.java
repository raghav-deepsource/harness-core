/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.stages;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.yaml.YAMLFieldNameConstants.STAGES;

import io.harness.advisers.nextstep.NextStepAdviserParameters;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.plancreator.steps.StepParameterCommonUtils;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.plancreator.steps.common.StageElementParameters.StageElementParametersBuilder;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.execution.utils.SkipInfoUtils;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.creators.ChildrenPlanCreator;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.pms.yaml.YamlField;
import io.harness.serializer.KryoSerializer;
import io.harness.when.utils.RunInfoUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.SneakyThrows;

@OwnedBy(PIPELINE)
@TargetModule(HarnessModule._882_PMS_SDK_CORE)
public abstract class GenericStagePlanCreator extends ChildrenPlanCreator<StageElementConfig> {
  @Inject private KryoSerializer kryoSerializer;

  public abstract Set<String> getSupportedStageTypes();

  public abstract StepType getStepType(StageElementConfig stageElementConfig);

  public abstract SpecParameters getSpecParameters(
      String childNodeId, PlanCreationContext ctx, StageElementConfig stageElementConfig);

  @Override
  public Class<StageElementConfig> getFieldClass() {
    return StageElementConfig.class;
  }

  @Override
  public Map<String, Set<String>> getSupportedTypes() {
    Set<String> stageTypes = getSupportedStageTypes();
    if (EmptyPredicate.isEmpty(stageTypes)) {
      return Collections.emptyMap();
    }
    return Collections.singletonMap(YAMLFieldNameConstants.STAGE, stageTypes);
  }

  @SneakyThrows
  @Override
  public PlanNode createPlanForParentNode(
      PlanCreationContext ctx, StageElementConfig stageElementConfig, List<String> childrenNodeIds) {
    StageElementParametersBuilder stageParameters = StepParameterCommonUtils.getStageParameters(stageElementConfig);
    YamlField specField =
        Preconditions.checkNotNull(ctx.getCurrentField().getNode().getField(YAMLFieldNameConstants.SPEC));
    stageParameters.specConfig(getSpecParameters(specField.getNode().getUuid(), ctx, stageElementConfig));
    return PlanNode.builder()
        .uuid(stageElementConfig.getUuid())
        .name(stageElementConfig.getName())
        .identifier(stageElementConfig.getIdentifier())
        .group(StepOutcomeGroup.STAGE.name())
        .stepParameters(stageParameters.build())
        .stepType(getStepType(stageElementConfig))
        .skipCondition(SkipInfoUtils.getSkipCondition(stageElementConfig.getSkipCondition()))
        .whenCondition(RunInfoUtils.getRunCondition(stageElementConfig.getWhen()))
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.CHILD).build())
                .build())
        .adviserObtainments(getAdviserObtainmentFromMetaData(ctx.getCurrentField()))
        .build();
  }

  /**
   * Adds the nextStepAdviser to the given node if it is not the end stage
   */
  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(YamlField stageField) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();
    if (stageField != null && stageField.getNode() != null) {
      // if parent is parallel, then we need not add nextStepAdvise as all the executions will happen in parallel
      if (stageField.checkIfParentIsParallel(STAGES)) {
        return adviserObtainments;
      }
      YamlField siblingField = stageField.getNode().nextSiblingFromParentArray(
          stageField.getName(), Arrays.asList(YAMLFieldNameConstants.STAGE, YAMLFieldNameConstants.PARALLEL));
      if (siblingField != null && siblingField.getNode().getUuid() != null) {
        adviserObtainments.add(
            AdviserObtainment.newBuilder()
                .setType(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.NEXT_STEP.name()).build())
                .setParameters(ByteString.copyFrom(kryoSerializer.asBytes(
                    NextStepAdviserParameters.builder().nextNodeId(siblingField.getNode().getUuid()).build())))
                .build());
      }
    }
    return adviserObtainments;
  }
}
