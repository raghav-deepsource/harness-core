/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.remote;

import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.api.impl.FileStoreServiceImpl;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.dto.filestore.node.FolderNodeDTO;
import io.harness.rule.Owner;

import java.io.File;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class FileStoreResourceTest extends CategoryTest {
  private static final String ACCOUNT = "account";
  private static final String ORG = "org";
  private static final String PROJECT = "project";
  private static final String IDENTIFIER = "testFile";

  @Mock private FileStoreServiceImpl fileStoreService;

  private FileStoreResource fileStoreResource;

  @Before
  public void setup() {
    fileStoreResource = new FileStoreResource(fileStoreService);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFile() {
    fileStoreResource.delete(ACCOUNT, ORG, PROJECT, IDENTIFIER);
    verify(fileStoreService).delete(ACCOUNT, ORG, PROJECT, IDENTIFIER);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFile() {
    File file = new File("returnedFile-download-path");
    when(fileStoreService.downloadFile(ACCOUNT, ORG, PROJECT, IDENTIFIER)).thenReturn(file);
    Response response = fileStoreResource.downloadFile(ACCOUNT, ORG, PROJECT, IDENTIFIER);
    File returnedFile = (File) response.getEntity();

    assertThat(returnedFile).isNotNull();
    assertThat(returnedFile.getPath()).isEqualTo(file.getPath());
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFileWithException() {
    when(fileStoreService.downloadFile(ACCOUNT, ORG, PROJECT, IDENTIFIER))
        .thenThrow(new InvalidRequestException("Unable to download file"));

    assertThatThrownBy(() -> fileStoreResource.downloadFile(ACCOUNT, ORG, PROJECT, IDENTIFIER))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Unable to download file");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListFolderNodes() {
    FolderNodeDTO folderDTO = FolderNodeDTO.builder().build();
    when(fileStoreService.listFolderNodes(ACCOUNT, ORG, PROJECT, folderDTO))
        .thenReturn(FolderNodeDTO.builder().name("returnedFolderName").identifier("returnedFolderIdentifier").build());
    ResponseDTO<FolderNodeDTO> folderNodeDTOResponseDTO =
        fileStoreResource.listFolderNodes(ACCOUNT, ORG, PROJECT, folderDTO);
    FolderNodeDTO data = folderNodeDTOResponseDTO.getData();

    assertThat(data).isNotNull();
    assertThat(data.getName()).isEqualTo("returnedFolderName");
    assertThat(data.getIdentifier()).isEqualTo("returnedFolderIdentifier");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListFolderNodesWithException() {
    FolderNodeDTO folderDTO = FolderNodeDTO.builder().build();
    when(fileStoreService.listFolderNodes(ACCOUNT, ORG, PROJECT, folderDTO))
        .thenThrow(new InvalidRequestException("Unable to list folder nodes"));

    assertThatThrownBy(() -> fileStoreResource.listFolderNodes(ACCOUNT, ORG, PROJECT, folderDTO))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Unable to list folder nodes");
  }
}
