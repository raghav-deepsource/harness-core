package io.harness.pms.pipeline;

import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.gitsync.v2.StoreType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PipelineEntityTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetData() {
    String yaml0 = "pipeline:\n\tidentifier: name\n";
    PipelineEntity pipelineEntity0 = PipelineEntity.builder().yaml(yaml0).build();
    assertThat(pipelineEntity0.getData()).isEqualTo(yaml0);

    String yaml1 = "pipeline:\n\tidentifier: name1\n";
    PipelineEntity pipelineEntity1 = PipelineEntity.builder().yaml(yaml1).storeType(StoreType.INLINE).build();
    assertThat(pipelineEntity1.getData()).isEqualTo(yaml1);
  }
}