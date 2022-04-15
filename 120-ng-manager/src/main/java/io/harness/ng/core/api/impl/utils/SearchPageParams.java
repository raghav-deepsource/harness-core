package io.harness.ng.core.api.impl.utils;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@SuperBuilder
@NoArgsConstructor
@OwnedBy(CDP)
public class SearchPageParams {
  @QueryParam("pageNumber") @DefaultValue("0") @NonNull int page;
  @QueryParam("pageSize") @DefaultValue("10") @NonNull int size;
  @QueryParam("searchTerm") String searchTerm;
}
