/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.ExecutionStatusResponseData;
import io.harness.context.ContextElementType;
import io.harness.exception.InvalidRequestException;
import io.harness.tasks.ResponseData;

import software.wings.api.ForkElement;
import software.wings.beans.ArtifactCollectLoopParams;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactInput;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.StateExecutionInstance.StateExecutionInstanceKeys;
import software.wings.sm.StateExecutionInstanceHelper;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ForkState.ForkStateExecutionData;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import org.mongodb.morphia.annotations.Transient;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(HarnessTeam.CDC)
public class ArtifactCollectLoopState extends State {
  @Getter List<ArtifactInput> artifactInputList;

  @Transient @Inject StateExecutionInstanceHelper instanceHelper;
  @Transient @Inject WorkflowExecutionServiceImpl executionService;
  @Transient @Inject WingsPersistence wingsPersistence;

  public ArtifactCollectLoopState(String name) {
    super(name, StateType.ARTIFACT_COLLECT_LOOP_STATE.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return executeInternal((ExecutionContextImpl) context);
  }

  private ExecutionResponse executeInternal(ExecutionContextImpl context) {
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    List<String> correlationIds = new ArrayList<>();

    ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
    ForkStateExecutionData forkStateExecutionData = new ForkStateExecutionData();
    List<String> forkStateNames = new ArrayList<>();
    forkStateExecutionData.setElements(new ArrayList<>());
    int i = 1;
    for (ArtifactInput artifactInput : artifactInputList) {
      String stateName = getName() + "_" + i;
      forkStateNames.add(stateName);
      ForkElement element =
          ForkElement.builder().stateName(stateName).parentId(stateExecutionInstance.getUuid()).build();
      StateExecutionInstance childStateExecutionInstance = instanceHelper.clone(stateExecutionInstance);
      childStateExecutionInstance.setStateParams(null);

      childStateExecutionInstance.setContextElement(element);
      childStateExecutionInstance.setDisplayName(stateName);
      childStateExecutionInstance.setStateName(stateName);
      childStateExecutionInstance.setParentLoopedState(true);
      childStateExecutionInstance.setLoopedStateParams(getLoopStateParams(artifactInput, stateName));
      childStateExecutionInstance.setStateType(StateType.ARTIFACT_COLLECTION.getName());
      childStateExecutionInstance.setNotifyId(element.getUuid());
      executionResponseBuilder.stateExecutionInstance(childStateExecutionInstance);
      correlationIds.add(element.getUuid());
      forkStateExecutionData.getElements().add(childStateExecutionInstance.getContextElement().getName());
      i++;
    }
    forkStateExecutionData.setForkStateNames(forkStateNames);

    return executionResponseBuilder.stateExecutionData(forkStateExecutionData)
        .async(true)
        .correlationIds(correlationIds)
        .build();
  }

  private ArtifactCollectLoopParams getLoopStateParams(ArtifactInput artifactInput, String name) {
    return ArtifactCollectLoopParams.builder()
        .artifactStreamId(artifactInput.getArtifactStreamId())
        .buildNo(artifactInput.getBuildNo())
        .stepName(name)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    ExecutionStatus executionStatusOfChildren = ExecutionStatus.SUCCESS;
    for (ResponseData status : response.values()) {
      ExecutionStatus executionStatus = ((ExecutionStatusResponseData) status).getExecutionStatus();
      if (executionStatus != ExecutionStatus.SUCCESS) {
        executionStatusOfChildren = executionStatus;
      }
    }

    if (ExecutionStatus.SUCCESS.equals(executionStatusOfChildren)) {
      updateArtifactsInContext((ExecutionContextImpl) context);
    }
    return ExecutionResponse.builder().executionStatus(executionStatusOfChildren).build();
  }

  private void updateArtifactsInContext(ExecutionContextImpl context) {
    String appId = context.getAppId();
    String workflowExecutionId = context.getWorkflowExecutionId();
    List<Artifact> artifacts = executionService.getArtifactsCollected(appId, workflowExecutionId);
    if (isEmpty(artifacts)) {
      return;
    }
    StateExecutionInstance stateExecutionInstance = context.getStateExecutionInstance();
    addArtifactsToWorkflowExecution(appId, workflowExecutionId, artifacts);
    addArtifactsToStateExecutionInstance(appId, stateExecutionInstance, artifacts);
  }

  private void addArtifactsToWorkflowExecution(String appId, String workflowExecutionId, List<Artifact> artifacts) {
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .filter(WorkflowExecutionKeys.appId, appId)
                                         .filter(WorkflowExecutionKeys.uuid, workflowExecutionId);

    UpdateOperations<WorkflowExecution> updateOps =
        wingsPersistence.createUpdateOperations(WorkflowExecution.class)
            .addToSet(WorkflowExecutionKeys.artifacts, artifacts)
            .addToSet(WorkflowExecutionKeys.executionArgs_artifacts, artifacts);

    wingsPersistence.update(query, updateOps);
  }

  private void addArtifactsToStateExecutionInstance(
      String appId, StateExecutionInstance stateExecutionInstance, List<Artifact> artifacts) {
    Query<StateExecutionInstance> query = wingsPersistence.createQuery(StateExecutionInstance.class)
                                              .filter(StateExecutionInstanceKeys.appId, appId)
                                              .filter(ID_KEY, stateExecutionInstance.getUuid());

    List<ContextElement> contextElements = stateExecutionInstance.getContextElements();
    WorkflowStandardParams workflowStandardParams =
        (WorkflowStandardParams) contextElements.stream()
            .filter(contextElement -> contextElement.getElementType() == ContextElementType.STANDARD)
            .findFirst()
            .orElse(null);

    if (workflowStandardParams == null) {
      throw new InvalidRequestException("Workflow Standard Params can not be null");
    }

    List<String> artifactIds = artifacts.stream().map(Artifact::getUuid).collect(Collectors.toList());

    if (isNotEmpty(workflowStandardParams.getArtifactIds())) {
      List<String> stdParamsArtifactIds = workflowStandardParams.getArtifactIds();
      stdParamsArtifactIds.addAll(artifactIds);
      workflowStandardParams.setArtifactIds(stdParamsArtifactIds);
    } else {
      workflowStandardParams.setArtifactIds(artifactIds);
    }

    UpdateOperations<StateExecutionInstance> updateOps =
        wingsPersistence.createUpdateOperations(StateExecutionInstance.class).set("contextElements", contextElements);

    wingsPersistence.update(query, updateOps);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // nothing to handle here.
  }
}
