package io.harness.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL) public enum StaticAuditFilter { EXCLUDE_LOGIN_EVENTS, EXCLUDE_SYSTEM_EVENTS }
