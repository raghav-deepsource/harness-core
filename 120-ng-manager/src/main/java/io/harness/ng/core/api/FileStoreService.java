/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.node.FolderNodeDTO;

import java.io.File;
import java.io.InputStream;
import javax.validation.constraints.NotNull;

@OwnedBy(HarnessTeam.CDP)
public interface FileStoreService {
  /**
   * Create file.
   *
   * @param fileDto the file DTO object
   * @param content file content
   * @return created file DTO object
   */
  FileDTO create(@NotNull FileDTO fileDto, InputStream content);

  /**
   * Update file.
   *
   * @param fileDto the file DTO
   * @param content file content
   * @param identifier file identifier
   * @return updated file DTO
   */

  FileDTO update(@NotNull FileDTO fileDto, InputStream content, String identifier);

  /**
   * Download a file.
   *
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the organization identifier
   * @param projectIdentifier the project identifier
   * @param fileIdentifier the file identifier
   * @return file
   */
  File downloadFile(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull String fileIdentifier);

  /**
   * Delete file.
   *
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the organization identifier
   * @param projectIdentifier the project identifier
   * @param identifier the file identifier
   * @return whether the file is successfully deleted
   */
  boolean delete(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier);

  /**
   * Get the list of folder nodes.
   *
   * @param accountIdentifier the account identifier
   * @param orgIdentifier the organization identifier
   * @param projectIdentifier the project identifier
   * @param folderNodeDTO the folder for which to return the list of nodes
   * @return the folder populated with nodes
   */
  FolderNodeDTO listFolderNodes(@NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier,
      @NotNull FolderNodeDTO folderNodeDTO);
}
