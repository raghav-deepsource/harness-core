/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.schema;

import static io.harness.yaml.schema.beans.SchemaConstants.ALL_OF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.ENUM_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.ONE_OF_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.PROPERTIES_NODE;
import static io.harness.yaml.schema.beans.SchemaConstants.TYPE_NODE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
// Todo: to be deleted after finishing the steps migration to new schema
public class YamlSchemaTransientHelper {
  public void deleteSpecNodeInStageElementConfig(JsonNode stageElementConfig) {
    JsonNodeUtils.deletePropertiesInJsonNode((ObjectNode) stageElementConfig.get(PROPERTIES_NODE), "spec");
  }

  public void removeV2StagesFromStageElementConfig(JsonNode stageElementConfigNode) {
    for (JsonNode jsonNode : stageElementConfigNode.get(ONE_OF_NODE)) {
      removeV2StepFromAllOfNode((ArrayNode) jsonNode.get(ALL_OF_NODE));
      try {
        ArrayNode enumNode = (ArrayNode) jsonNode.get(PROPERTIES_NODE).get(TYPE_NODE).get(ENUM_NODE);
        removeV2EnumsFromTypeNode(enumNode);
      } catch (Exception e) {
        // Type ENUM node not present.(Would be template node)
      }
    }
  }

  public void removeV2StepEnumsFromStepElementConfig(JsonNode stepElementConfigNode) {
    if (stepElementConfigNode == null) {
      return;
    }
    removeV2StepFromAllOfNode((ArrayNode) stepElementConfigNode.get(ALL_OF_NODE));
    ArrayNode enumNode = (ArrayNode) stepElementConfigNode.get(PROPERTIES_NODE).get(TYPE_NODE).get(ENUM_NODE);
    removeV2EnumsFromTypeNode(enumNode);
  }
  public void removeV2EnumsFromTypeNode(ArrayNode enumNode) {
    if (enumNode == null) {
      return;
    }
    enumNode.removeAll();
  }

  public void removeV2StepFromAllOfNode(ArrayNode allOfNode) {
    if (allOfNode == null) {
      return;
    }
    allOfNode.removeAll();
  }
}
