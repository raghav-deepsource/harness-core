/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.dto.filestore;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.validator.Condition;
import io.harness.data.validator.EntityIdentifier;
import io.harness.filestore.FileUsage;
import io.harness.filestore.NGFileType;
import io.harness.ng.core.common.beans.NGTag;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotBlank;

@OwnedBy(CDP)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "File", description = "This is details of the file entity defined in Harness.")
@Condition(property = "type", propertyValue = "FILE", requiredProperties = {"fileUsage"},
    message = "FileUsage is required for file.")
public class FileDTO {
  @ApiModelProperty(required = true)
  @Schema(description = NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE)
  private String accountIdentifier;
  @Schema(description = NGCommonEntityConstants.ORG_PARAM_MESSAGE) private String orgIdentifier;
  @Schema(description = NGCommonEntityConstants.PROJECT_PARAM_MESSAGE) private String projectIdentifier;

  @EntityIdentifier
  @Schema(description = "Identifier of the File")
  @FormDataParam("identifier")
  private String identifier;

  @NotBlank @Schema(description = "Name of the File") @FormDataParam("name") private String name;
  @Schema(description = "This specifies the file usage") @FormDataParam("fileUsage") private FileUsage fileUsage;
  @NotNull @Schema(description = "This specifies the type of the File") @FormDataParam("type") private NGFileType type;
  @NotBlank
  @Schema(description = "This specifies parent identifier")
  @FormDataParam("parentIdentifier")
  private String parentIdentifier;
  @Schema(description = "Description of the File") @FormDataParam("description") private String description;
  @Schema(description = "Tags") @Valid private List<NGTag> tags;
  @Schema(description = "Mime type of the File") @FormDataParam("mimeType") private String mimeType;

  @JsonIgnore
  public boolean isFile() {
    return type == NGFileType.FILE;
  }

  @JsonIgnore
  public boolean isFolder() {
    return type == NGFileType.FOLDER;
  }
}
