/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.beans.yaml.extended.infrastrucutre;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("Pool")
@TypeAlias("VmPoolYaml")
@OwnedBy(CI)
public class VmPoolYaml implements VmInfraSpec {
  @Builder.Default @NotNull private VmInfraSpec.Type type = VmInfraSpec.Type.POOL;
  @NotNull private VmPoolYamlSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VmPoolYamlSpec {
    @Deprecated private String identifier;
    @ApiModelProperty(dataType = STRING_CLASSPATH) private ParameterField<String> name;
  }
}
