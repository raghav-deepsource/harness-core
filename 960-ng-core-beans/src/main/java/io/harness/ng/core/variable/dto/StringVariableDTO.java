/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.variable.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.ng.core.variable.VariableValueType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import javax.annotation.RegEx;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.PL)
@Getter
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "StringVariableDTOKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StringVariableDTO extends VariableConfigDTO {
  String fixedValue;
  String defaultValue;
  Set<String> allowedValues;
  @RegEx String regex;

  @Override
  String getValue() {
    switch (getVariableValueType()) {
      case FIXED:
        return fixedValue;
      case FIXED_SET:
        return String.format(FIXED_SET_VALUE_FORMAT, String.join(",", allowedValues));
      case REGEX:
        return String.format(REGEX_VALUE_FORMAT, regex);
      default:
        throw new UnknownEnumTypeException("variableValueType", getVariableValueType().name());
    }
  }

  @Override
  public void validate() {
    switch (getVariableValueType()) {
      case FIXED:
        validateForFixedValueType();
        break;
      case FIXED_SET:
        validateForFixedSetValueType();
        break;
      case REGEX:
        // No custom validation is required
        break;
      default:
        throw new UnknownEnumTypeException("variableValueType", getVariableValueType().name());
    }
  }

  private void validateForFixedValueType() {
    if (StringUtils.isBlank(fixedValue)) {
      throw new InvalidRequestException(
          String.format("Value for field [%s] must be provide when validation is of type [%s]",
              StringVariableDTOKeys.fixedValue, VariableValueType.FIXED));
    }
  }

  private void validateForFixedSetValueType() {
    if (CollectionUtils.isEmpty(allowedValues)) {
      throw new InvalidRequestException(
          String.format("Value(s) for field [%s] must be provide when validation is of type [%s]",
              StringVariableDTOKeys.allowedValues, VariableValueType.FIXED_SET));
    }
  }
}
