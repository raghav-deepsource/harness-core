package io.harness.gitsync.v2;

public enum StoreType {
  // Will be used when entity is not persisted on git but rather live in DATABASE
  INLINE,

  // Will be used when entity is in git repo
  REMOTE
}
