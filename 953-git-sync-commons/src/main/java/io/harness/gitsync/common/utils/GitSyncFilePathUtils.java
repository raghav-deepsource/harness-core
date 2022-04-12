/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.utils.FilePathUtils;

import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.PL)
@UtilityClass
public class GitSyncFilePathUtils {
  private static final String FILE_PATH_SEPARATOR = "/";

  public static GitEntityFilePath getRootFolderAndFilePath(String completeFilePath) {
    String[] pathSplited = Pattern.compile("([.])(harness/)").split(completeFilePath);
    if (pathSplited.length != 2) {
      throw new InvalidRequestException(String.format(
          "The path %s doesn't contain the .harness folder, thus this file won't be processed", completeFilePath));
    }

    String folderPath = getGitFolderPath(pathSplited[0]);
    String filePath = pathSplited[1];
    return GitEntityFilePath.builder().rootFolder(folderPath).filePath(filePath).build();
  }

  public static String createFilePath(String folderPath, String filePath) {
    if (isEmpty(folderPath)) {
      throw new InvalidRequestException("Folder path cannot be empty");
    }
    if (isEmpty(filePath)) {
      throw new InvalidRequestException("File path cannot be empty");
    }
    String updatedFolderPath = getGitFolderPath(folderPath);
    String updatedFilePath = filePath.charAt(0) != '/' ? filePath : filePath.substring(1);
    return updatedFolderPath + updatedFilePath;
  }

  public static String formatFilePath(String path) {
    return FilePathUtils.addStartingSlashIfMissing(path);
  }

  private static String getGitFolderPath(String folderPath) {
    folderPath = addEndingSlashIfMissing(folderPath);
    String prefix = "";
    if (isBlank(folderPath) || folderPath.charAt(0) != '/') {
      prefix = "/";
    }

    if (folderPath.endsWith(GitSyncConstants.FOLDER_PATH)) {
      return prefix + folderPath;
    }

    return prefix + folderPath + GitSyncConstants.FOLDER_PATH;
  }

  private static String addEndingSlashIfMissing(String path) {
    if (!path.endsWith("/")) {
      return path + "/";
    }
    return path;
  }

  public static String getFilePathWithoutStartingSlash(String path) {
    if (isEmpty(path)) {
      throw new InvalidRequestException("File Path cannot be empty");
    }
    return StringUtils.stripStart(path, FILE_PATH_SEPARATOR);
  }
}
