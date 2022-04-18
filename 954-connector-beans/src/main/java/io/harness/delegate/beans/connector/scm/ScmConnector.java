/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.connector.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;

/**
 * Marker interface for all scm connectors.
 */

@OwnedBy(DX)
public interface ScmConnector {
  void setUrl(String url);
  String getUrl();
  ConnectorType getConnectorType();

  /**
   * This method is used to fetch final git connnection url to the repo
   * If its a REPO level connector, it should return its url directly
   * If its an ACCOUNT level connector, it should construct corresponding connection url for input repo
   */
  String getGitConnectionUrl(String repoName);
}
