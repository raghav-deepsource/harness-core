/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.environment.mappers;

import static io.harness.rule.OwnerRule.ARCHIT;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentRequestDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.environment.yaml.NGEnvironmentConfig;
import io.harness.ng.core.environment.yaml.NGEnvironmentInfoConfig;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EnvironmentMapperTest extends CategoryTest {
  EnvironmentRequestDTO environmentRequestDTO;
  EnvironmentResponseDTO environmentResponseDTO;
  Environment requestEnvironment;
  Environment responseEnvironment;
  List<NGTag> tags;
  NGEnvironmentConfig ngEnvironmentConfig;
  @Before
  public void setUp() {
    tags = Arrays.asList(NGTag.builder().key("k1").value("v1").build(), NGTag.builder().key("k2").value("v2").build());
    ngEnvironmentConfig = NGEnvironmentConfig.builder()
                              .ngEnvironmentInfoConfig(NGEnvironmentInfoConfig.builder()
                                                           .identifier("ENV")
                                                           .orgIdentifier("ORG_ID")
                                                           .projectIdentifier("PROJECT_ID")
                                                           .tags(ImmutableMap.of("k1", "v1", "k2", "v2"))
                                                           .type(EnvironmentType.PreProduction)
                                                           .build())
                              .build();
    environmentRequestDTO = EnvironmentRequestDTO.builder()
                                .identifier("ENV")
                                .orgIdentifier("ORG_ID")
                                .projectIdentifier("PROJECT_ID")
                                .color("BLACK")
                                .type(EnvironmentType.PreProduction)
                                .tags(ImmutableMap.of("k1", "v1", "k2", "v2"))
                                .build();

    environmentResponseDTO = EnvironmentResponseDTO.builder()
                                 .accountId("ACCOUNT_ID")
                                 .identifier("ENV")
                                 .orgIdentifier("ORG_ID")
                                 .projectIdentifier("PROJECT_ID")
                                 .color("BLACK")
                                 .type(EnvironmentType.PreProduction)
                                 .deleted(false)
                                 .tags(ImmutableMap.of("k1", "v1", "k2", "v2"))
                                 .build();

    requestEnvironment = Environment.builder()
                             .accountId("ACCOUNT_ID")
                             .identifier("ENV")
                             .orgIdentifier("ORG_ID")
                             .color("BLACK")
                             .projectIdentifier("PROJECT_ID")
                             .type(EnvironmentType.PreProduction)
                             .deleted(false)
                             .tags(tags)
                             .yaml(NGEnvironmentEntityMapper.toYaml(ngEnvironmentConfig))
                             .build();

    responseEnvironment = Environment.builder()
                              .accountId("ACCOUNT_ID")
                              .identifier("ENV")
                              .orgIdentifier("ORG_ID")
                              .projectIdentifier("PROJECT_ID")
                              .color("BLACK")
                              .type(EnvironmentType.PreProduction)
                              .id("UUID")
                              .deleted(false)
                              .tags(tags)
                              .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testToEnvironment() {
    Environment environment = EnvironmentMapper.toEnvironmentEntity("ACCOUNT_ID", environmentRequestDTO);
    assertThat(environment).isNotNull();
    assertThat(environment).isEqualTo(requestEnvironment);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testWriteDTO() {
    EnvironmentResponseDTO environmentDTO = EnvironmentMapper.writeDTO(responseEnvironment);
    assertThat(environmentDTO).isNotNull();
    assertThat(environmentDTO).isEqualTo(environmentResponseDTO);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetEnvironmentResponseList() {
    Environment env1 = requestEnvironment.withVersion(10L);
    Environment env2 = requestEnvironment.withVersion(20L);

    List<EnvironmentResponse> environmentResponseList = EnvironmentMapper.toResponseWrapper(Arrays.asList(env1, env2));
    assertThat(environmentResponseList.size()).isEqualTo(2);
    assertThat(environmentResponseList.get(0).getEnvironment().getVersion()).isEqualTo(10l);
    assertThat(environmentResponseList.get(1).getEnvironment().getVersion()).isEqualTo(20L);
  }
}
