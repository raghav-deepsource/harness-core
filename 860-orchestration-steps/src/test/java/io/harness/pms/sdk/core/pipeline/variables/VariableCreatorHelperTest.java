/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.sdk.core.pipeline.variables;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.plancreator.pipeline.PipelineInfoConfig;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.pms.contracts.plan.YamlExtraProperties;
import io.harness.pms.contracts.plan.YamlProperties;
import io.harness.pms.sdk.core.variables.VariableCreatorHelper;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlUtils;
import io.harness.rule.Owner;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class VariableCreatorHelperTest extends CategoryTest {
  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void createVariablesForChildrenNodesV2WithNullMapAndList() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipelineCreatorBase.yaml");
    String pipelineJson = Resources.toString(testFile, Charsets.UTF_8);
    YamlField fullYamlField = YamlUtils.readTree(pipelineJson);

    // Pipeline Node
    YamlField pipelineField = fullYamlField.getNode().getField("pipeline");

    PipelineInfoConfig pipelineInfoConfig =
        YamlUtils.read(pipelineField.getNode().toString(), PipelineInfoConfig.class);
    Map<String, YamlExtraProperties> yamlExtraPropertiesMap = new HashMap<>();
    Map<String, YamlProperties> yamlPropertiesMap = new HashMap<>();

    VariableCreatorHelper.collectVariableExpressions(
        pipelineInfoConfig, yamlPropertiesMap, yamlExtraPropertiesMap, "pipeline", "pipeline");

    // check for extra properties expressions
    assertThat(yamlExtraPropertiesMap.containsKey(pipelineInfoConfig.getUuid())).isTrue();
    List<String> fqnExtraPropertiesList = yamlExtraPropertiesMap.get(pipelineInfoConfig.getUuid())
                                              .getPropertiesList()
                                              .stream()
                                              .map(YamlProperties::getFqn)
                                              .collect(Collectors.toList());
    assertThat(fqnExtraPropertiesList)
        .containsAll(
            Arrays.asList("pipeline.variables", "pipeline.identifier", "pipeline.tags", "pipeline.properties"));

    // check for name and description expressions
    assertThat(yamlPropertiesMap.containsKey(pipelineInfoConfig.getName())).isTrue();
    assertThat(yamlPropertiesMap.get(pipelineInfoConfig.getName()).getFqn()).isEqualTo("pipeline.name");

    assertThat(yamlPropertiesMap.containsKey(pipelineInfoConfig.getDescription().getResponseField())).isTrue();
    assertThat(yamlPropertiesMap.get(pipelineInfoConfig.getDescription().getResponseField()).getFqn())
        .isEqualTo("pipeline.description");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void createVariablesForChildrenNodesV2WithNullMapAndListForStage() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipelineCreatorBase.yaml");
    String pipelineJson = Resources.toString(testFile, Charsets.UTF_8);
    YamlField fullYamlField = YamlUtils.readTree(pipelineJson);

    // Pipeline Node
    YamlField pipelineField = fullYamlField.getNode().getField("pipeline");

    PipelineInfoConfig pipelineInfoConfig =
        YamlUtils.read(pipelineField.getNode().toString(), PipelineInfoConfig.class);
    Map<String, YamlExtraProperties> yamlExtraPropertiesMap = new HashMap<>();
    Map<String, YamlProperties> yamlPropertiesMap = new HashMap<>();

    StageElementConfig stageElementConfig =
        YamlUtils.read(pipelineInfoConfig.getStages().get(0).getStage().toString(), StageElementConfig.class);

    VariableCreatorHelper.collectVariableExpressions(
        stageElementConfig, yamlPropertiesMap, yamlExtraPropertiesMap, "pipeline.stages.stage1", "stage");

    // check for extra properties expressions
    assertThat(yamlExtraPropertiesMap.containsKey(stageElementConfig.getUuid())).isTrue();
    List<String> fqnExtraPropertiesList = yamlExtraPropertiesMap.get(stageElementConfig.getUuid())
                                              .getPropertiesList()
                                              .stream()
                                              .map(YamlProperties::getFqn)
                                              .collect(Collectors.toList());
    assertThat(fqnExtraPropertiesList)
        .containsAll(Arrays.asList("pipeline.stages.stage1.variables", "pipeline.stages.stage1.identifier",
            "pipeline.stages.stage1.tags", "pipeline.stages.stage1.type"));

    // enum when condition expression
    fqnExtraPropertiesList = yamlExtraPropertiesMap.get(stageElementConfig.getWhen().getUuid())
                                 .getPropertiesList()
                                 .stream()
                                 .map(YamlProperties::getFqn)
                                 .collect(Collectors.toList());
    assertThat(fqnExtraPropertiesList)
        .containsAll(Collections.singletonList("pipeline.stages.stage1.when.pipelineStatus"));

    // check for name and description and when condition expressions
    assertThat(yamlPropertiesMap.containsKey(stageElementConfig.getName())).isTrue();
    assertThat(yamlPropertiesMap.get(stageElementConfig.getName()).getFqn()).isEqualTo("pipeline.stages.stage1.name");

    assertThat(yamlPropertiesMap.containsKey(stageElementConfig.getDescription().getResponseField())).isTrue();
    assertThat(yamlPropertiesMap.get(stageElementConfig.getDescription().getResponseField()).getFqn())
        .isEqualTo("pipeline.stages.stage1.description");

    assertThat(yamlPropertiesMap.containsKey(stageElementConfig.getWhen().getCondition().getResponseField())).isTrue();
    assertThat(yamlPropertiesMap.get(stageElementConfig.getWhen().getCondition().getResponseField()).getFqn())
        .isEqualTo("pipeline.stages.stage1.when.condition");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void createVariablesForChildrenNodesV2WithMapAndListValues() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    final URL testFile = classLoader.getResource("pipelineWithTagsAndVariables.yaml");
    String pipelineJson = Resources.toString(testFile, Charsets.UTF_8);
    YamlField fullYamlField = YamlUtils.readTree(pipelineJson);

    // Pipeline Node
    YamlField pipelineField = fullYamlField.getNode().getField("pipeline");

    PipelineInfoConfig pipelineInfoConfig =
        YamlUtils.read(pipelineField.getNode().toString(), PipelineInfoConfig.class);
    Map<String, YamlExtraProperties> yamlExtraPropertiesMap = new HashMap<>();
    Map<String, YamlProperties> yamlPropertiesMap = new HashMap<>();

    VariableCreatorHelper.collectVariableExpressions(
        pipelineInfoConfig, yamlPropertiesMap, yamlExtraPropertiesMap, "pipeline", "pipeline");

    // check for extra properties expressions
    assertThat(yamlExtraPropertiesMap.containsKey(pipelineInfoConfig.getUuid())).isTrue();
    List<String> fqnExtraPropertiesList = yamlExtraPropertiesMap.get(pipelineInfoConfig.getUuid())
                                              .getPropertiesList()
                                              .stream()
                                              .map(YamlProperties::getFqn)
                                              .collect(Collectors.toList());
    assertThat(fqnExtraPropertiesList).containsOnly("pipeline.identifier");
    // null resources field in extraProperties
    assertThat(yamlExtraPropertiesMap.containsKey(pipelineInfoConfig.getProperties().getCi().getCodebase().getUuid()))
        .isTrue();

    // check for name expression
    assertThat(yamlPropertiesMap.containsKey(pipelineInfoConfig.getName())).isTrue();
    assertThat(yamlPropertiesMap.get(pipelineInfoConfig.getName()).getFqn()).isEqualTo("pipeline.name");

    // check for description expression
    assertThat(yamlPropertiesMap.containsKey(pipelineInfoConfig.getDescription().getResponseField())).isTrue();
    assertThat(yamlPropertiesMap.get(pipelineInfoConfig.getDescription().getResponseField()).getFqn())
        .isEqualTo("pipeline.description");

    // variables field (list type)
    assertThat(
        yamlPropertiesMap.containsKey(pipelineInfoConfig.getVariables().get(0).getCurrentValue().getResponseField()))
        .isTrue();
    assertThat(
        yamlPropertiesMap.get(pipelineInfoConfig.getVariables().get(0).getCurrentValue().getResponseField()).getFqn())
        .isEqualTo("pipeline.variables.test1");
    assertThat(
        yamlPropertiesMap.containsKey(pipelineInfoConfig.getVariables().get(0).getCurrentValue().getResponseField()))
        .isTrue();
    assertThat(
        yamlPropertiesMap.get(pipelineInfoConfig.getVariables().get(1).getCurrentValue().getResponseField()).getFqn())
        .isEqualTo("pipeline.variables.test2");

    // Map object type expressions
    assertThat(yamlPropertiesMap.containsKey(pipelineInfoConfig.getTags().get("t1"))).isTrue();
    assertThat(yamlPropertiesMap.get(pipelineInfoConfig.getTags().get("t1")).getFqn()).isEqualTo("pipeline.tags.t1");
  }
}
