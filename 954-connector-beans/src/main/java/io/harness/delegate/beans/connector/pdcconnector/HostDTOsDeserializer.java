/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.pdcconnector;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@OwnedBy(CDP)
public class HostDTOsDeserializer extends StdDeserializer<List<HostDTO>> {
  public HostDTOsDeserializer() {
    super(List.class);
  }

  protected HostDTOsDeserializer(StdDeserializer<List<HostDTO>> src) {
    super(src);
  }

  @Override
  public List<HostDTO> deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {
    JsonNode node = jp.getCodec().readTree(jp);

    if (node instanceof TextNode) {
      return getHostDTOs(node.asText());
    } else if (node instanceof ArrayNode) {
      ObjectMapper objectMapper = (ObjectMapper) jp.getCodec();
      JavaType customClassCollection = objectMapper.getTypeFactory().constructCollectionType(List.class, HostDTO.class);
      return objectMapper.readValue(node.toString(), customClassCollection);
    } else {
      throw new InvalidRequestException("No supported HostDTOs deserializable type");
    }
  }

  private List<HostDTO> getHostDTOs(final String hosts) {
    if (isBlank(hosts)) {
      return Collections.emptyList();
    }

    return parseHostDTOsFromPlanText(hosts);
  }

  private List<HostDTO> parseHostDTOsFromPlanText(final String hosts) {
    String[] hostsArr = hosts.replace("\n", ",").split(",");
    return getUnmodifiableHostDTOsList(hostsArr);
  }

  private List<HostDTO> getUnmodifiableHostDTOsList(final String[] hosts) {
    return Arrays.stream(hosts)
        .map(String::trim)
        .filter(StringUtils::isNoneBlank)
        .map(this::getHostDTO)
        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
  }

  @NotNull
  private HostDTO getHostDTO(final String host) {
    return new HostDTO(host, Collections.emptyMap());
  }
}
