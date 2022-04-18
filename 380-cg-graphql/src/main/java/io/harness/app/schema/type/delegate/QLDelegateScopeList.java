package io.harness.app.schema.type.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(DEL)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.DELEGATE)
public class QLDelegateScopeList {
  private QLPageInfo pageInfo;
  List<QLDelegateScope> nodes;
}
