/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resourcegroup.framework.v2.service;

import static io.harness.rule.OwnerRule.REETIKA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.resourcegroup.ResourceGroupTestBase;
import io.harness.resourcegroup.framework.v2.service.impl.ResourceGroupValidatorImpl;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupDTO;
import io.harness.resourcegroup.v2.remote.dto.ResourceGroupRequest;
import io.harness.rule.Owner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.serializer.HObjectMapper;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ResourceGroupValidatorTest extends ResourceGroupTestBase {
  @Inject ResourceGroupValidatorImpl resourceGroupValidator;
  private ObjectMapper objectMapper;

  @Before
  public void setup() {
    objectMapper = new ObjectMapper();
    HObjectMapper.configureObjectMapperForNG(objectMapper);
  }

  public static String readFileAsString(String file) {
    try {
      return new String(Files.readAllBytes(Paths.get(file)));
    } catch (Exception ex) {
      Assert.fail("Failed reading the json from " + file + " with error " + ex.getMessage());
      return "";
    }
  }

  @Test
  @Owner(developers = REETIKA)
  @Category(UnitTests.class)
  public void validateResourceGroup() {
    String accountLevelResourceGroup = readFileAsString(
        "820-platform-service/libs/resource-group-service/src/test/resources/resourcegroups/v2/accountResourceGroupV2.json");
    ResourceGroupDTO resourceGroupDTO = null;
    try {
      resourceGroupDTO = objectMapper.readValue(accountLevelResourceGroup, ResourceGroupDTO.class);
    } catch (Exception ex) {
      Assert.fail("Encountered exception while deserializing resource group " + ex.getMessage());
    }
    try {
      resourceGroupValidator.validateResourceGroup(
          ResourceGroupRequest.builder().resourceGroup(resourceGroupDTO).build());
      fail("Expected failure as the resource filter is invalid");
    } catch (InvalidRequestException ex) {
      assertThat(ex.getParams().get("message"))
          .isEqualTo("Cannot provide specific resources when you include all resources");
    }
  }
}
