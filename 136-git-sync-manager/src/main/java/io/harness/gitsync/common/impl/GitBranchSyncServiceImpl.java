/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.impl;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.gitsync.common.beans.BranchSyncStatus.SYNCING;
import static io.harness.gitsync.common.beans.BranchSyncStatus.UNSYNCED;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.git.YamlGitConfigDTO;
import io.harness.exception.UnexpectedException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.common.beans.BranchSyncMetadata;
import io.harness.gitsync.common.beans.GitBranch;
import io.harness.gitsync.common.beans.GitToHarnessFileProcessingRequest;
import io.harness.gitsync.common.beans.GitToHarnessProgressStatus;
import io.harness.gitsync.common.beans.YamlChangeSetEventType;
import io.harness.gitsync.common.dtos.GetFileDetailsFromBranchResponse;
import io.harness.gitsync.common.dtos.GitFileChangeDTO;
import io.harness.gitsync.common.dtos.GitToHarnessProcessMsvcStepResponse;
import io.harness.gitsync.common.helper.YamlGitConfigHelper;
import io.harness.gitsync.common.service.GitBranchService;
import io.harness.gitsync.common.service.GitBranchSyncService;
import io.harness.gitsync.common.service.GitToHarnessProgressService;
import io.harness.gitsync.common.service.ScmOrchestratorService;
import io.harness.gitsync.common.service.YamlGitConfigService;
import io.harness.gitsync.common.service.gittoharness.GitToHarnessProcessorService;
import io.harness.gitsync.core.dtos.YamlChangeSetDTO;
import io.harness.gitsync.core.dtos.YamlChangeSetSaveDTO;
import io.harness.gitsync.core.service.YamlChangeSetService;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class GitBranchSyncServiceImpl implements GitBranchSyncService {
  GitToHarnessProcessorService gitToHarnessProcessorService;
  ScmOrchestratorService scmOrchestratorService;
  GitToHarnessProgressService gitToHarnessProgressService;
  YamlGitConfigService yamlGitConfigService;
  YamlChangeSetService yamlChangeSetService;
  GitBranchService gitBranchService;

  @Override
  public void createBranchSyncEvent(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String yamlGitConfigIdentifier, String repoURL, String branch, List<String> filePathsToBeExcluded) {
    GitBranch gitBranch = gitBranchService.get(accountIdentifier, repoURL, branch);
    if (gitBranch == null) {
      log.info("No record found for the branch [{}] in the repo [{}]", repoURL, branch);
      return;
    } else if (gitBranch.getBranchSyncStatus() != UNSYNCED) {
      log.info("The branch sync for repoUrl [{}], branch [{}] has status [{}], hence skipping", repoURL, branch,
          gitBranch.getBranchSyncStatus());
      return;
    }
    final BranchSyncMetadata branchSyncMetadata = BranchSyncMetadata.builder()
                                                      .filesToBeExcluded(filePathsToBeExcluded)
                                                      .orgIdentifier(orgIdentifier)
                                                      .projectIdentifier(projectIdentifier)
                                                      .yamlGitConfigId(yamlGitConfigIdentifier)
                                                      .build();
    final YamlChangeSetSaveDTO yamlChangeSetSaveDTO = YamlChangeSetSaveDTO.builder()
                                                          .accountId(accountIdentifier)
                                                          .branch(branch)
                                                          .repoUrl(repoURL)
                                                          .eventType(YamlChangeSetEventType.BRANCH_SYNC)
                                                          .eventMetadata(branchSyncMetadata)
                                                          .build();
    final YamlChangeSetDTO savedChangeSet = yamlChangeSetService.save(yamlChangeSetSaveDTO);
    gitBranchService.updateBranchSyncStatus(accountIdentifier, repoURL, branch, SYNCING);
    log.info("Created the change set {} to process the branch {} in the repo {}", savedChangeSet.getChangesetId(),
        branch, repoURL);
  }

  @Override
  public GitToHarnessProcessMsvcStepResponse processBranchSyncEvent(YamlGitConfigDTO yamlGitConfig, String branchName,
      String accountIdentifier, List<String> filePathsToBeExcluded, String changeSetId,
      String gitToHarnessProgressRecordId) {
    List<YamlGitConfigDTO> yamlGitConfigDTOS =
        yamlGitConfigService.getByAccountAndRepo(accountIdentifier, yamlGitConfig.getRepo());
    Set<String> foldersList = YamlGitConfigHelper.getRootFolderList(yamlGitConfigDTOS);
    GetFileDetailsFromBranchResponse getFilesResponseOfBranch =
        getFilesToBeProcessed(yamlGitConfigDTOS, accountIdentifier, foldersList, branchName);
    String commitId = getFilesResponseOfBranch.getCommitId();
    final List<GitFileChangeDTO> harnessFilesOfBranch = getFilesResponseOfBranch.getFilesInGit();
    log.info("Received file paths: [{}] from git in harness folders.",
        emptyIfNull(harnessFilesOfBranch).stream().map(GitFileChangeDTO::getPath).collect(Collectors.toList()));
    List<GitFileChangeDTO> filteredFileList = getFilteredFiles(harnessFilesOfBranch, filePathsToBeExcluded);
    List<GitToHarnessFileProcessingRequest> gitToHarnessFilesToProcess =
        emptyIfNull(filteredFileList)
            .stream()
            .map(fileContent
                -> GitToHarnessFileProcessingRequest.builder()
                       .fileDetails(fileContent)
                       .changeType(ChangeType.ADD)
                       .build())
            .collect(toList());
    gitToHarnessProgressService.updateFilesInProgressRecord(gitToHarnessProgressRecordId, gitToHarnessFilesToProcess);
    String commitMessage = getCommitMessage(yamlGitConfig, commitId, accountIdentifier);
    GitToHarnessProgressStatus gitToHarnessProgressStatus =
        gitToHarnessProcessorService.processFiles(accountIdentifier, gitToHarnessFilesToProcess, branchName,
            yamlGitConfig.getRepo(), commitId, gitToHarnessProgressRecordId, changeSetId, commitMessage);
    return GitToHarnessProcessMsvcStepResponse.builder().gitToHarnessProgressStatus(gitToHarnessProgressStatus).build();
  }

  // todo deepak: if for loop is removed from here take care of branch push case
  private GetFileDetailsFromBranchResponse getFilesToBeProcessed(
      List<YamlGitConfigDTO> yamlGitConfigDTOs, String accountIdentifier, Set<String> foldersList, String branchName) {
    List<GitFileChangeDTO> filesInBranch = new ArrayList<>();
    int yamlGitConfigsCount = yamlGitConfigDTOs.size();
    for (int i = 0; i < yamlGitConfigsCount; i++) {
      YamlGitConfigDTO yamlGitConfigDTO = yamlGitConfigDTOs.get(i);
      try {
        log.info("Trying to get files using the yaml git config with the identifier {} in project {}",
            yamlGitConfigDTO.getIdentifier(), yamlGitConfigDTO.getProjectIdentifier());
        String commitId = getCommitId(filesInBranch);
        if (commitId == null) {
          commitId = getLatestCommitIdOfBranch(yamlGitConfigDTO, branchName);
        }
        filesInBranch = getFilesBelongingToThisBranch(accountIdentifier, yamlGitConfigDTO, foldersList, branchName);
        log.info("Completed get files using the yaml git config with the identifier {} in project {}",
            yamlGitConfigDTO.getIdentifier(), yamlGitConfigDTO.getProjectIdentifier());
        return GetFileDetailsFromBranchResponse.builder().filesInGit(filesInBranch).commitId(commitId).build();
      } catch (Exception ex) {
        log.error("Error doing get files using the yaml git config with the identifier {} in project {}",
            yamlGitConfigDTO.getIdentifier(), yamlGitConfigDTO.getProjectIdentifier(), ex);
        // If we are getting the exception for the last yaml git config too, then throw the exception
        if (i == yamlGitConfigsCount - 1) {
          throw ex;
        }
      }
    }
    throw new UnexpectedException("Could not get the files to do git branch sync");
  }

  private String getLatestCommitIdOfBranch(YamlGitConfigDTO yamlGitConfigDTO, String branchName) {
    return scmOrchestratorService.processScmRequest(scmClient
        -> scmClient.getLatestCommit(yamlGitConfigDTO, branchName).getSha(),
        yamlGitConfigDTO.getProjectIdentifier(), yamlGitConfigDTO.getOrganizationIdentifier(),
        yamlGitConfigDTO.getAccountIdentifier());
  }

  private String getCommitId(List<GitFileChangeDTO> harnessFilesOfBranch) {
    if (isEmpty(harnessFilesOfBranch)) {
      return null;
    }
    return harnessFilesOfBranch.get(0).getCommitId();
  }

  private List<GitFileChangeDTO> getFilesBelongingToThisBranch(
      String accountIdentifier, YamlGitConfigDTO yamlGitConfig, Set<String> foldersList, String branchName) {
    return scmOrchestratorService.processScmRequest(scmClientFacilitatorService
        -> scmClientFacilitatorService.listFilesOfBranches(accountIdentifier, yamlGitConfig.getOrganizationIdentifier(),
            yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getIdentifier(), foldersList, branchName),
        yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getOrganizationIdentifier(), accountIdentifier);
  }

  @VisibleForTesting
  protected List<GitFileChangeDTO> getFilteredFiles(
      List<GitFileChangeDTO> fileContents, List<String> filePathsToBeExcluded) {
    if (isEmpty(filePathsToBeExcluded)) {
      return fileContents;
    }
    List<GitFileChangeDTO> filteredFileContents = new ArrayList<>();
    for (GitFileChangeDTO fileContent : fileContents) {
      if (filePathsToBeExcluded.contains(fileContent.getPath())) {
        continue;
      }
      filteredFileContents.add(fileContent);
    }
    return filteredFileContents;
  }

  @VisibleForTesting
  protected String getCommitMessage(YamlGitConfigDTO yamlGitConfig, String commitId, String accountIdentifier) {
    if (commitId == null) {
      return null;
    }
    return scmOrchestratorService
        .processScmRequest(scmClientFacilitatorService
            -> scmClientFacilitatorService.findCommitById(yamlGitConfig, commitId),
            yamlGitConfig.getProjectIdentifier(), yamlGitConfig.getOrganizationIdentifier(), accountIdentifier)
        .getMessage();
  }
}
