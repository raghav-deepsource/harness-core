/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.rule.OwnerRule.BOJAN;
import static io.harness.rule.OwnerRule.FILIP;
import static io.harness.rule.OwnerRule.IVAN;
import static io.harness.rule.OwnerRule.VLAD;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ChecksumType;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.FileUploadLimit;
import io.harness.exception.DuplicateEntityException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.file.beans.NGBaseFile;
import io.harness.filestore.FileStoreConstants;
import io.harness.filestore.NGFileType;
import io.harness.ng.core.dto.filestore.FileDTO;
import io.harness.ng.core.dto.filestore.node.FileNodeDTO;
import io.harness.ng.core.dto.filestore.node.FolderNodeDTO;
import io.harness.ng.core.entities.NGFile;
import io.harness.repositories.filestore.FileStoreRepositoryCriteriaCreator;
import io.harness.repositories.filestore.spring.FileStoreRepository;
import io.harness.rule.Owner;

import software.wings.app.MainConfiguration;
import software.wings.service.intfc.FileService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.dao.DuplicateKeyException;

@OwnedBy(HarnessTeam.CDP)
@RunWith(MockitoJUnitRunner.class)
public class FileStoreServiceImplTest extends CategoryTest {
  public static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
  private static final String ORG_IDENTIFIER = "orgIdentifier";
  public static final String PROJECT_IDENTIFIER = "projectIdentifier";
  public static final String PARENT_IDENTIFIER = "parentIdentifier";
  public static final String IDENTIFIER = "identifier";
  public static final String FILE_IDENTIFIER = "fileIdentifier";
  public static final String FILE_NAME = "fileName";

  @Mock private FileStoreRepository fileStoreRepository;
  @Mock private FileService fileService;
  @Mock private MainConfiguration configuration;

  @InjectMocks private FileStoreServiceImpl fileStoreService;

  @Before
  public void setup() {
    when(configuration.getFileUploadLimits()).thenReturn(new FileUploadLimit());

    when(fileStoreRepository.save(any())).thenAnswer(invocation -> invocation.getArguments()[0]);

    givenThatDatabaseIsEmpty();
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testUpdate() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.of(createNgFile()));

    FileDTO result = fileStoreService.update(createFileDto(), null, "identifier1");

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("updatedName");
    assertThat(result.getDescription()).isEqualTo("updatedDescription");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testUpdateShouldThrowException() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             any(), any(), any(), any()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> fileStoreService.update(createFileDto(), null, "identifier1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("File with identifier: identifier1 not found.");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFileWithNullIdentifier() {
    assertThatThrownBy(
        () -> fileStoreService.downloadFile(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, null))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("File identifier cannot be null or empty");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFileWithNotExistingFile() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> fileStoreService.downloadFile(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining(format("Unable to find file, fileIdentifier: %s", FILE_IDENTIFIER));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFileWithFolder() {
    NGFile ngFile = createNgFile();
    ngFile.setType(NGFileType.FOLDER);
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .thenReturn(Optional.of(ngFile));

    assertThatThrownBy(
        () -> fileStoreService.downloadFile(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining(format("Downloading folder not supported, fileIdentifier: %s", FILE_IDENTIFIER));
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testDownloadFile() {
    NGFile ngFile = createNgFile();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER))
        .thenReturn(Optional.of(ngFile));
    File fileToReturn = new File("download-file-path");
    when(fileService.download(eq(ngFile.getFileUuid()), any(File.class), any(FileBucket.class)))
        .thenReturn(fileToReturn);

    File file = fileStoreService.downloadFile(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, FILE_IDENTIFIER);

    assertThat(file).isNotNull();
    assertThat(file.getPath()).isEqualTo(fileToReturn.getPath());
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldSaveNgFile() {
    // Given
    final FileDTO fileDto = aFileDto();

    // When
    fileStoreService.create(fileDto, getStreamWithDummyContent());

    // Then
    NGFile expected = NGFile.builder()
                          .identifier(fileDto.getIdentifier())
                          .accountIdentifier(fileDto.getAccountIdentifier())
                          .description(fileDto.getDescription())
                          .name(fileDto.getName())
                          .type(fileDto.getType())
                          .checksumType(ChecksumType.MD5)
                          .tags(Collections.emptyList())
                          .size(0L)
                          .build();

    verify(fileStoreRepository).save(expected);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldSaveFileUsingFileService() {
    // Given
    final FileDTO fileDto = aFileDto();

    // When
    fileStoreService.create(fileDto, getStreamWithDummyContent());

    // Then
    NGBaseFile baseFile = new NGBaseFile();
    baseFile.setFileName(fileDto.getName());
    baseFile.setAccountId(fileDto.getAccountIdentifier());

    verify(fileService).saveFile(eq(baseFile), notNull(InputStream.class), eq(FileBucket.FILE_STORE));
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldNotInvokeSaveFileOnFileServiceForFolder() {
    // Given
    final FileDTO folderDto = aFolderDto();

    // When
    fileStoreService.create(folderDto, null);

    // Then
    verifyZeroInteractions(fileService);
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenFolderAlreadyExistsInDatabase() {
    givenThatFileExistsInDatabase();

    FileDTO folderDto = aFolderDto();

    assertThatThrownBy(() -> fileStoreService.create(folderDto, null))
        .isInstanceOf(DuplicateEntityException.class)
        .hasMessageContaining(
            "Try creating another folder, folder with identifier [%s] already exists in the parent folder",
            folderDto.getIdentifier());
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenFileAlreadyExistsInDatabase() {
    givenThatFileExistsInDatabase();

    FileDTO fileDTO = aFileDto();

    assertThatThrownBy(() -> fileStoreService.create(fileDTO, getStreamWithDummyContent()))
        .isInstanceOf(DuplicateEntityException.class)
        .hasMessageContaining(
            "Try creating another file, file with identifier [%s] already exists in the parent folder",
            fileDTO.getIdentifier());
  }

  @Test
  @Owner(developers = FILIP)
  @Category(UnitTests.class)
  public void shouldHandleDuplicateKeyExceptionForFile() {
    when(fileStoreRepository.save(any())).thenThrow(DuplicateKeyException.class);

    FileDTO fileDTO = aFileDto();

    assertThatThrownBy(() -> fileStoreService.create(fileDTO, getStreamWithDummyContent()))
        .isInstanceOf(DuplicateEntityException.class)
        .hasMessageContaining(
            "Try creating another file, file with identifier [%s] already exists in the parent folder",
            fileDTO.getIdentifier());
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void shouldHandleEmptyFileExceptionForFile() {
    FileDTO fileDTO = aFileDto();

    assertThatThrownBy(() -> fileStoreService.create(fileDTO, null))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessageContaining("File content is empty. Identifier: " + fileDTO.getIdentifier(), fileDTO.getIdentifier());
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFileDoesNotExist() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.empty());
    assertThatThrownBy(
        () -> fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage(
            "File or folder with identifier [identifier], account [accountIdentifier], org [orgIdentifier] and project [projectIdentifier] could not be retrieved from file store.");
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFile() {
    String fileUuid = "fileUUID";
    NGFile file = NGFile.builder().name(IDENTIFIER).identifier(IDENTIFIER).fileUuid(fileUuid).build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(file));
    boolean result = fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER);
    assertThat(result).isTrue();
    verify(fileStoreRepository).delete(file);
    verify(fileService).deleteFile(fileUuid, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFolder() {
    String fileUuid = "fileUUID";
    NGFile file = NGFile.builder().name(IDENTIFIER).fileUuid(fileUuid).build();
    String folder1 = "folder1";
    NGFile parentFolder = NGFile.builder()
                              .name(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                              .orgIdentifier(ORG_IDENTIFIER)
                              .projectIdentifier(PROJECT_IDENTIFIER)
                              .build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Arrays.asList(file));
    boolean result = fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1);
    assertThat(result).isTrue();
    verify(fileStoreRepository).delete(file);
    verify(fileStoreRepository).delete(parentFolder);
    verify(fileService).deleteFile(fileUuid, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFolderWithSubfolder() {
    String fileUuid1 = "fileUUID1";
    NGFile file = NGFile.builder().name(IDENTIFIER).identifier(IDENTIFIER).fileUuid(fileUuid1).build();
    String folder1 = "folder1";
    NGFile parentFolder = NGFile.builder()
                              .name(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                              .orgIdentifier(ORG_IDENTIFIER)
                              .projectIdentifier(PROJECT_IDENTIFIER)
                              .build();
    String folder2 = "folder2";
    NGFile childFolder = NGFile.builder()
                             .name(folder2)
                             .identifier(folder2)
                             .type(NGFileType.FOLDER)
                             .accountIdentifier(ACCOUNT_IDENTIFIER)
                             .orgIdentifier(ORG_IDENTIFIER)
                             .projectIdentifier(PROJECT_IDENTIFIER)
                             .build();
    String file2 = "file2";
    String fileUuid2 = "fileUUID2";
    NGFile childFile = NGFile.builder().name(file2).fileUuid(fileUuid2).build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder2))
        .thenReturn(Optional.of(childFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, file2))
        .thenReturn(Optional.of(childFile));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1))
        .thenReturn(Arrays.asList(file, childFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder2))
        .thenReturn(Arrays.asList(childFile));
    boolean result = fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folder1);
    assertThat(result).isTrue();
    verify(fileStoreRepository).delete(file);
    verify(fileStoreRepository).delete(parentFolder);
    verify(fileStoreRepository).delete(childFile);
    verify(fileStoreRepository).delete(childFolder);
    verify(fileService).deleteFile(fileUuid1, FileBucket.FILE_STORE);
    verify(fileService).deleteFile(fileUuid2, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteFolderWithScopePrefix() {
    String fileUuid = "fileUUID";
    NGFile file = NGFile.builder().name(IDENTIFIER).identifier(IDENTIFIER).fileUuid(fileUuid).build();
    String folder1 = "folder1";
    NGFile parentFolder = NGFile.builder()
                              .name(folder1)
                              .identifier(folder1)
                              .type(NGFileType.FOLDER)
                              .accountIdentifier(ACCOUNT_IDENTIFIER)
                              .build();
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, null, null, IDENTIFIER))
        .thenReturn(Optional.of(file));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             ACCOUNT_IDENTIFIER, null, null, folder1))
        .thenReturn(Optional.of(parentFolder));
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndParentIdentifier(
             ACCOUNT_IDENTIFIER, null, null, folder1))
        .thenReturn(Arrays.asList(file));
    boolean result =
        fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, "account." + folder1);
    assertThat(result).isTrue();
    verify(fileStoreRepository).delete(file);
    verify(fileStoreRepository).delete(parentFolder);
    verify(fileService).deleteFile(fileUuid, FileBucket.FILE_STORE);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldDeleteRootFolder() {
    String rootIdentifier = FileStoreConstants.ROOT_FOLDER_IDENTIFIER;
    assertThatThrownBy(
        () -> fileStoreService.delete(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, rootIdentifier))
        .isInstanceOf(InvalidArgumentsException.class)
        .hasMessage("Root folder [Root] can not be deleted.");
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testListFolderNodes() {
    FolderNodeDTO folderNodeDTO = FolderNodeDTO.builder().identifier(FILE_IDENTIFIER).name(FILE_NAME).build();
    when(fileStoreRepository.findAllAndSort(
             FileStoreRepositoryCriteriaCreator.createCriteriaByScopeAndParentIdentifier(
                 Scope.of(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER), FILE_IDENTIFIER),
             FileStoreRepositoryCriteriaCreator.createSortByLastModifiedAtDesc()))
        .thenReturn(Arrays.asList(NGFile.builder()
                                      .type(NGFileType.FOLDER)
                                      .name("folderName1")
                                      .identifier("folderIdentifier1")
                                      .parentId(FILE_IDENTIFIER)
                                      .build(),
            NGFile.builder()
                .type(NGFileType.FOLDER)
                .name("folderName2")
                .identifier("folderIdentifier2")
                .parentId(FILE_IDENTIFIER)
                .build(),
            NGFile.builder()
                .type(NGFileType.FILE)
                .name("fileName")
                .identifier("fileIdentifier")
                .parentId(FILE_IDENTIFIER)
                .build()));

    FolderNodeDTO populatedFolderNodeDTO =
        fileStoreService.listFolderNodes(ACCOUNT_IDENTIFIER, ORG_IDENTIFIER, PROJECT_IDENTIFIER, folderNodeDTO);

    assertThat(populatedFolderNodeDTO).isNotNull();
    assertThat(populatedFolderNodeDTO.getName()).isEqualTo(FILE_NAME);
    assertThat(populatedFolderNodeDTO.getIdentifier()).isEqualTo(FILE_IDENTIFIER);
    assertThat(populatedFolderNodeDTO.getChildren().size()).isEqualTo(3);
    assertThat(populatedFolderNodeDTO.getChildren())
        .contains(FileNodeDTO.builder().name("fileName").identifier("fileIdentifier").build(),
            FolderNodeDTO.builder().name("folderName1").identifier("folderIdentifier1").build(),
            FolderNodeDTO.builder().name("folderName2").identifier("folderIdentifier2").build());
  }

  private static FileDTO aFileDto() {
    return FileDTO.builder()
        .identifier("identifier")
        .accountIdentifier("account-ident")
        .description("some description")
        .name("file-name")
        .type(NGFileType.FILE)
        .build();
  }

  private static FileDTO aFolderDto() {
    return FileDTO.builder()
        .identifier("identifier")
        .accountIdentifier("account-ident")
        .description("some description")
        .name("folder-name")
        .type(NGFileType.FOLDER)
        .build();
  }

  private void givenThatFileExistsInDatabase() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.of(NGFile.builder().build()));
  }

  private void givenThatDatabaseIsEmpty() {
    when(fileStoreRepository.findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
             anyString(), anyString(), anyString(), anyString()))
        .thenReturn(Optional.empty());
  }

  private static InputStream getStreamWithDummyContent() {
    return new ByteArrayInputStream("File content".getBytes());
  }

  private FileDTO createFileDto() {
    return FileDTO.builder()
        .type(NGFileType.FILE)
        .identifier("identifier1")
        .name("updatedName")
        .description("updatedDescription")
        .build();
  }
  private NGFile createNgFile() {
    return NGFile.builder()
        .type(NGFileType.FILE)
        .name("oldName")
        .description("oldDescription")
        .identifier("identifier1")
        .build();
  }
}
