package io.harness.plancreator.steps.common;

import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import java.util.List;

public interface WithDelegateSelector {
  ParameterField<List<TaskSelectorYaml>> getDelegateSelectors();

  void setDelegateSelectors(ParameterField<List<TaskSelectorYaml>> delegateSelectors);
}
