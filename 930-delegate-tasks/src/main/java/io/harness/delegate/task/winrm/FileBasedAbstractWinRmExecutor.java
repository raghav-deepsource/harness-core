/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.winrm;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.task.winrm.WinRmExecutorHelper.constructPSScriptWithCommands;
import static io.harness.delegate.task.winrm.WinRmExecutorHelper.constructPSScriptWithCommandsBulk;
import static io.harness.delegate.task.winrm.WinRmExecutorHelper.getScriptExecutingCommand;
import static io.harness.delegate.task.winrm.WinRmExecutorHelper.psWrappedCommandWithEncoding;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.windows.CmdUtils.escapeLineBreakChars;
import static io.harness.windows.CmdUtils.escapeWordBreakChars;
import static io.harness.winrm.WinRmHelperUtils.buildErrorDetailsFromWinRmClientException;

import static software.wings.beans.LogColor.Green;
import static software.wings.beans.LogColor.Yellow;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.encoding.EncodingUtils;
import io.harness.delegate.task.shell.ConfigFileMetaData;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.beans.LogColor;
import software.wings.utils.ExecutionLogWriter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
public abstract class FileBasedAbstractWinRmExecutor {
  public static final String WINDOWS_TEMPFILE_LOCATION = "%TEMP%";
  public static final String NOT_IMPLEMENTED = "Not implemented";
  protected static final String ERROR_WHILE_EXECUTING_COMMAND = "Error while executing command";
  /**
   * Size of one batch in bytes for copying files over to the host
   */
  protected static final int BATCH_SIZE_BYTES = 1024 * 4; // 4 KB

  protected LogCallback logCallback;
  protected final WinRmSessionConfig config;
  protected boolean disableCommandEncoding;
  protected boolean shouldSaveExecutionLogs;

  public FileBasedAbstractWinRmExecutor(LogCallback logCallback, boolean shouldSaveExecutionLogs,
      WinRmSessionConfig config, boolean disableCommandEncoding) {
    this.logCallback = logCallback;
    this.shouldSaveExecutionLogs = shouldSaveExecutionLogs;
    this.config = config;
    this.disableCommandEncoding = disableCommandEncoding;
  }

  public abstract byte[] getConfigFileBytes(ConfigFileMetaData configFileMetaData) throws IOException;

  public CommandExecutionStatus copyConfigFiles(ConfigFileMetaData configFileMetaData) {
    if (isBlank(configFileMetaData.getFileId()) || isBlank(configFileMetaData.getFilename())) {
      saveExecutionLog("There is no config file to copy. " + configFileMetaData.toString(), INFO);
      return CommandExecutionStatus.SUCCESS;
    }

    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try (WinRmSession session = new WinRmSession(config, this.logCallback);
         ExecutionLogWriter outputWriter = getExecutionLogWriter(INFO);
         ExecutionLogWriter errorWriter = getExecutionLogWriter(ERROR)) {
      saveExecutionLog(format("Connected to %s", config.getHostname()), INFO);
      saveExecutionLog(format("Executing command ...%n"), INFO);

      final int configFileLength = configFileMetaData.getLength().intValue();
      byte[] fileBytes = getConfigFileBytes(configFileMetaData);

      commandExecutionStatus =
          splitFileAndTransfer(configFileMetaData, session, outputWriter, errorWriter, configFileLength, fileBytes);

    } catch (Exception e) {
      log.error(ERROR_WHILE_EXECUTING_COMMAND, e);
      ResponseMessage details = buildErrorDetailsFromWinRmClientException(e);
      saveExecutionLog(
          format("Command execution failed. Error: %s", details.getMessage()), ERROR, commandExecutionStatus);
    }

    log.info("Copy Config command execution returned.", commandExecutionStatus);
    return commandExecutionStatus;
  }

  protected String getPowershell() {
    return this.config.isUseNoProfile() ? "Powershell -NoProfile " : "Powershell ";
  }

  private void saveExecutionLog(String line, LogLevel level) {
    saveExecutionLog(line, level, RUNNING);
  }

  private void saveExecutionLog(String line, LogLevel level, CommandExecutionStatus commandExecutionStatus) {
    if (shouldSaveExecutionLogs) {
      logCallback.saveExecutionLog(line, level, commandExecutionStatus);
    }
  }

  private CommandExecutionStatus transferFileAsIs(ConfigFileMetaData configFileMetaData, WinRmSession session,
      ExecutionLogWriter outputWriter, ExecutionLogWriter errorWriter, byte[] fileBytes) throws IOException {
    int exitCode;
    String command;
    String psScriptFile = null;

    if (disableCommandEncoding) {
      // Keep the temp script in working directory or in Temp is working directory is not set.
      psScriptFile = config.getWorkingDirectory() == null
          ? WINDOWS_TEMPFILE_LOCATION
          : config.getWorkingDirectory() + "harness-" + this.config.getExecutionId() + ".ps1";
      command = getCopyConfigCommandBehindFF(configFileMetaData, fileBytes);
      exitCode = session.executeCommandsList(constructPSScriptWithCommands(command, psScriptFile, getPowershell()),
          outputWriter, errorWriter, false, getScriptExecutingCommand(psScriptFile, getPowershell()));
    } else {
      String encodedFile = EncodingUtils.encodeBase64(fileBytes);
      command = getCopyConfigCommand(configFileMetaData, encodedFile);

      exitCode = session.executeCommandString(
          WinRmExecutorHelper.psWrappedCommandWithEncoding(command, getPowershell()), outputWriter, errorWriter, false);
    }
    log.info("Execute Command String returned exit code.", exitCode);
    WinRmExecutorHelper.cleanupFiles(session, psScriptFile, getPowershell(), disableCommandEncoding);
    return exitCode == 0 ? SUCCESS : FAILURE;
  }

  private CommandExecutionStatus splitFileAndTransfer(ConfigFileMetaData configFileMetaData, WinRmSession session,
      ExecutionLogWriter outputWriter, ExecutionLogWriter errorWriter, int configFileLength, byte[] fileBytes)
      throws IOException {
    final List<List<Byte>> partitions = Lists.partition(Bytes.asList(fileBytes), BATCH_SIZE_BYTES);
    clearTargetFile(configFileMetaData, session, outputWriter, errorWriter);
    logFileSizeAndOtherMetadata(configFileMetaData, configFileLength, partitions.size());

    CommandExecutionStatus commandExecutionStatus = SUCCESS;
    int chunkNumber = 1;
    for (List<Byte> partition : partitions) {
      final byte[] bytesToCopy = Bytes.toArray(partition);
      String command = getCopyConfigCommand(configFileMetaData, bytesToCopy);
      commandExecutionStatus = executeRemoteCommand(session, outputWriter, errorWriter, command, true);
      if (FAILURE == commandExecutionStatus) {
        saveExecutionLog(format("Failed to copy chunk #%d. Discontinuing", chunkNumber), ERROR, RUNNING);
        break;
      }
      saveExecutionLog(format("Transferred %s data for config file...\n",
                           calcPercentage(chunkNumber * BATCH_SIZE_BYTES, configFileLength)),
          INFO, RUNNING);
      chunkNumber++;
    }
    return commandExecutionStatus;
  }

  private void logFileSizeAndOtherMetadata(
      ConfigFileMetaData configFileMetaData, int configFileLength, int nPartitions) {
    saveExecutionLog(format("Size of file (%s) to be transferred %.2f %s", configFileMetaData.getFilename(),
                         configFileLength > 1024 ? configFileLength / 1024.0 : configFileLength,
                         configFileLength > 1024 ? "(KB) KiloBytes" : "(B) Bytes"),
        INFO, RUNNING);
    if (nPartitions > 1) {
      saveExecutionLog(format("splitting file into %s %s for transfer\n", color(valueOf(nPartitions), LogColor.Cyan),
                           color("chunks", LogColor.Cyan)),
          INFO, RUNNING);
    }
  }

  private String calcPercentage(int dataTransferred, int configFileLength) {
    NumberFormat defaultFormat = NumberFormat.getPercentInstance();
    defaultFormat.setMinimumFractionDigits(2);
    final float fraction = min(1, (float) dataTransferred / configFileLength);
    return color(defaultFormat.format(fraction), (fraction < 1) ? Yellow : Green, Bold);
  }

  private void clearTargetFile(ConfigFileMetaData configFileMetaData, WinRmSession session,
      ExecutionLogWriter outputWriter, ExecutionLogWriter errorWriter) throws IOException {
    String command = getDeleteFileCommandStr(configFileMetaData);
    final CommandExecutionStatus commandExecutionStatus =
        executeRemoteCommand(session, outputWriter, errorWriter, command, false);
    if (commandExecutionStatus != SUCCESS) {
      final String messsage = format("File %s could not cleared before writing",
          Paths.get(configFileMetaData.getDestinationDirectoryPath(), configFileMetaData.getFilename()));
      saveExecutionLog(messsage, ERROR, FAILURE);
      throw new InvalidRequestException(messsage, USER);
    }
  }

  @VisibleForTesting
  public String getDeleteFileCommandStr(ConfigFileMetaData configFileMetaData) {
    return disableCommandEncoding ? getDeleteFileCommandBehindFF(configFileMetaData)
                                  : getDeleteFileCommand(configFileMetaData);
  }

  @VisibleForTesting
  public String getCopyConfigCommand(ConfigFileMetaData configFileMetaData, byte[] bytesToCopy) {
    return disableCommandEncoding ? getCopyConfigCommandBehindFF(configFileMetaData, bytesToCopy)
                                  : getCopyConfigCommand(configFileMetaData, EncodingUtils.encodeBase64(bytesToCopy));
  }

  @VisibleForTesting
  public CommandExecutionStatus executeRemoteCommand(WinRmSession session, ExecutionLogWriter outputWriter,
      ExecutionLogWriter errorWriter, String command, boolean bulkMode) throws IOException {
    String psScriptFile = null;
    int exitCode = 0;
    if (disableCommandEncoding) {
      // Keep the temp script in working directory or in Temp is working directory is not set.
      psScriptFile = config.getWorkingDirectory() == null
          ? WINDOWS_TEMPFILE_LOCATION
          : config.getWorkingDirectory() + "harness-" + this.config.getExecutionId() + ".ps1";
      exitCode =
          executeCommandsWithoutEncoding(session, outputWriter, errorWriter, command, bulkMode, psScriptFile, exitCode);
    } else {
      exitCode = session.executeCommandString(
          psWrappedCommandWithEncoding(command, getPowershell()), outputWriter, errorWriter, false);
    }
    log.info("Execute Command String returned exit code.", exitCode);
    io.harness.delegate.task.winrm.WinRmExecutorHelper.cleanupFiles(
        session, psScriptFile, getPowershell(), disableCommandEncoding);
    return exitCode == 0 ? SUCCESS : FAILURE;
  }

  private int executeCommandsWithoutEncoding(WinRmSession session, ExecutionLogWriter outputWriter,
      ExecutionLogWriter errorWriter, String command, boolean bulkMode, String psScriptFile, int exitCode)
      throws IOException {
    // Commands are not split up per line in bulk mode. Hence, we want to run them individually to avoid issues with
    // quoting
    if (bulkMode) {
      final List<String> commands = constructPSScriptWithCommandsBulk(command, psScriptFile, getPowershell());
      for (String commandStr : commands) {
        exitCode = session.executeCommandString(commandStr, outputWriter, errorWriter, false);
        if (exitCode != 0) {
          return exitCode;
        }
      }
    } else {
      exitCode = session.executeCommandsList(constructPSScriptWithCommands(command, psScriptFile, getPowershell()),
          outputWriter, errorWriter, false, getScriptExecutingCommand(psScriptFile, getPowershell()));
    }
    return exitCode;
  }

  private ExecutionLogWriter getExecutionLogWriter(LogLevel logLevel) {
    return ExecutionLogWriter.builder()
        .accountId(config.getAccountId())
        .appId(config.getAppId())
        .commandUnitName(config.getCommandUnitName())
        .executionId(config.getExecutionId())
        .logCallback(logCallback)
        .stringBuilder(new StringBuilder(1024))
        .logLevel(logLevel)
        .build();
  }

  @VisibleForTesting
  public String getCopyConfigCommandBehindFF(ConfigFileMetaData configFileMetaData, byte[] fileBytes) {
    final String breakCharsEscapedStr = escapeWordBreakChars(escapeLineBreakChars(new String(fileBytes)));
    return "$fileName = \"" + configFileMetaData.getDestinationDirectoryPath() + "\\" + configFileMetaData.getFilename()
        + "\"\n"
        + "$commandString = @'\n" + breakCharsEscapedStr + "\n'@"
        + "\n[IO.File]::AppendAllText($fileName, $commandString,   [Text.Encoding]::UTF8)\n"
        + "Write-Host \"Appended to config file on the host.\"";
  }

  @VisibleForTesting
  public String getCopyConfigCommand(ConfigFileMetaData configFileMetaData, String encodedFile) {
    return "#### Convert Base64 string back to config file ####\n"
        + "\n"
        + "$DecodedString = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String(\"" + encodedFile
        + "\"))\n"
        + "Write-Host \"Decoding config file on the host.\"\n"
        + "$decodedFile = \'" + configFileMetaData.getDestinationDirectoryPath() + "\\"
        + configFileMetaData.getFilename() + "\'\n"
        + "[IO.File]::AppendAllText($decodedFile, $DecodedString) \n"
        + "Write-Host \"Appended to config file on the host.\"\n";
  }

  private String getDeleteFileCommandBehindFF(ConfigFileMetaData configFileMetaData) {
    return "$fileName = \"" + configFileMetaData.getDestinationDirectoryPath() + "\\" + configFileMetaData.getFilename()
        + "\"\n"
        + "Write-Host \"Clearing target config file $fileName on the host.\""
        + "\n[IO.File]::Delete($fileName)";
  }

  private String getDeleteFileCommand(ConfigFileMetaData configFileMetaData) {
    return "$decodedFile = \'" + configFileMetaData.getDestinationDirectoryPath() + "\\"
        + configFileMetaData.getFilename() + "\'\n"
        + "Write-Host \"Clearing target config file $decodedFile  on the host.\"\n"
        + "[IO.File]::Delete($decodedFile)";
  }
}
