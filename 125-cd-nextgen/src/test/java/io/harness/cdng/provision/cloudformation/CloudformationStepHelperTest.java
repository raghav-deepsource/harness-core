/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.cloudformation;

import static io.harness.rule.OwnerRule.NGONZALEZ;
import static io.harness.rule.OwnerRule.TMACARI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.CDStepHelper;
import io.harness.cdng.common.beans.SetupAbstractionKeys;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.k8s.beans.StepExceptionPassThroughData;
import io.harness.cdng.manifest.yaml.GithubStore;
import io.harness.cdng.manifest.yaml.S3UrlStoreConfig;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.cdng.provision.cloudformation.beans.CloudFormationCreateStackPassThroughData;
import io.harness.cdng.provision.cloudformation.beans.CloudFormationInheritOutput;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.validator.scmValidators.GitConfigAuthenticationInfoHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.aws.s3.AwsS3FetchFilesResponse;
import io.harness.delegate.beans.aws.s3.S3FileDetailResponse;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.delegate.task.cloudformation.CloudformationTaskNGParameters;
import io.harness.delegate.task.cloudformation.CloudformationTaskType;
import io.harness.delegate.task.git.GitFetchResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.git.model.FetchFilesResult;
import io.harness.git.model.GitFile;
import io.harness.logging.UnitProgress;
import io.harness.logging.UnitStatus;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.expression.EngineExpressionService;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskChainResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.secretmanagerclient.services.api.SecretManagerClientService;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.steps.StepHelper;
import io.harness.steps.StepUtils;

import software.wings.beans.TaskType;
import software.wings.sm.states.provision.S3UriParser;

import com.amazonaws.services.s3.AmazonS3URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@PrepareForTest({StepUtils.class})
@RunWith(PowerMockRunner.class)
public class CloudformationStepHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock private EngineExpressionService engineExpressionService;
  @Mock private K8sStepHelper k8sStepHelper;
  @Mock private SecretManagerClientService secretManagerClientService;
  @Mock private CDStepHelper cdStepHelper;
  @Mock private GitConfigAuthenticationInfoHelper gitConfigAuthenticationInfoHelper;
  @Mock private S3UriParser s3UriParser;
  @Mock private StepHelper stepHelper;
  @Mock private CloudformationStepExecutor cloudformationStepExecutor;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputService;
  @InjectMocks private final CloudformationStepHelper cloudformationStepHelper = new CloudformationStepHelper();
  private static final String TAGS = "[\n"
      + "  {\n"
      + "    \"tag\": \"tag1\",\n"
      + "    \"tag2\": \"tag3\"\n"
      + "  }\n"
      + "]"
      + "";

  @Test
  @Owner(developers = TMACARI)
  @Category(UnitTests.class)
  public void testRenderValue() {
    Ambiance ambiance = getAmbiance();
    String expression = "expression";
    cloudformationStepHelper.renderValue(ambiance, expression);
    verify(engineExpressionService).renderExpression(ambiance, expression);
  }

  public Ambiance getAmbiance() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put(SetupAbstractionKeys.accountId, "account1");
    setupAbstractions.put(SetupAbstractionKeys.orgIdentifier, "org1");
    setupAbstractions.put(SetupAbstractionKeys.projectIdentifier, "project1");
    return Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).build();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithoutAwsConnector() {
    StepElementParameters stepElementParameters = createStepParametersWithGit(false);
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorConfig(AppDynamicsConnectorDTO.builder().build()).build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    cloudformationStepHelper.startChainLink(cloudformationStepExecutor, getAmbiance(), stepElementParameters);
  }

  /*
  File templates stored in git

   */
  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithGit() {
    StepElementParameters stepElementParameters = createStepParametersWithGit(false);
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder().connectorConfig(AwsConnectorDTO.builder().build()).build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    ConnectorInfoDTO connectorInfoDTO1 =
        ConnectorInfoDTO.builder()
            .connectorConfig(GitConfigDTO.builder().gitConnectionType(GitConnectionType.REPO).build())
            .build();
    doReturn(connectorInfoDTO1).when(k8sStepHelper).getConnector(any(), any());
    SSHKeySpecDTO sshKeySpecDTO = SSHKeySpecDTO.builder().build();

    doReturn(sshKeySpecDTO).when(gitConfigAuthenticationInfoHelper).getSSHKey(any(), any(), any(), any());
    List<EncryptedDataDetail> apiEncryptedDataDetails = new ArrayList<>();
    doReturn(apiEncryptedDataDetails).when(secretManagerClientService).getEncryptionDetails(any(), any());

    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);
    TaskChainResponse response =
        cloudformationStepHelper.startChainLink(cloudformationStepExecutor, getAmbiance(), stepElementParameters);

    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(TaskType.GIT_FETCH_NEXT_GEN_TASK.name());
    assertThat(response.getTaskRequest()).isNotNull();
    assertThat(response.getPassThroughData()).isNotNull();

    StepElementParameters stepElementParametersWithTags = createStepParametersWithGit(true);
    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptorWithTags = ArgumentCaptor.forClass(TaskData.class);
    TaskChainResponse responseWithTags = cloudformationStepHelper.startChainLink(
        cloudformationStepExecutor, getAmbiance(), stepElementParametersWithTags);

    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptorWithTags.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptorWithTags.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptorWithTags.getValue().getParameters()).isNotNull();
    assertThat(taskDataArgumentCaptorWithTags.getValue().getTaskType())
        .isEqualTo(TaskType.GIT_FETCH_NEXT_GEN_TASK.name());
    assertThat(responseWithTags.getTaskRequest()).isNotNull();
    assertThat(responseWithTags.getPassThroughData()).isNotNull();
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithS3() {
    StepElementParameters stepElementParameters = createStepParametersWithS3(false);
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(
                AwsConnectorDTO.builder()
                    .credential(AwsCredentialDTO.builder().config(AwsManualConfigSpecDTO.builder().build()).build())
                    .build())
            .build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());
    doReturn(new ArrayList<>()).when(secretManagerClientService).getEncryptionDetails(any(), any());
    AmazonS3URI s3URI = new AmazonS3URI("s3://bucket/key");
    doReturn(s3URI).when(s3UriParser).parseUrl(anyString());
    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);
    TaskChainResponse response =
        cloudformationStepHelper.startChainLink(cloudformationStepExecutor, getAmbiance(), stepElementParameters);

    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptor.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(TaskType.FETCH_S3_FILE_TASK_NG.name());
    assertThat(response.getTaskRequest()).isNotNull();
    assertThat(response.getPassThroughData()).isNotNull();

    StepElementParameters stepElementParametersWithTags = createStepParametersWithS3(true);
    mockStatic(StepUtils.class);
    PowerMockito.when(StepUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptorWithTags = ArgumentCaptor.forClass(TaskData.class);
    TaskChainResponse responseWithTags = cloudformationStepHelper.startChainLink(
        cloudformationStepExecutor, getAmbiance(), stepElementParametersWithTags);

    PowerMockito.verifyStatic(StepUtils.class, times(1));
    StepUtils.prepareCDTaskRequest(any(), taskDataArgumentCaptorWithTags.capture(), any(), any(), any(), any(), any());
    assertThat(taskDataArgumentCaptorWithTags.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptorWithTags.getValue().getParameters()).isNotNull();
    assertThat(taskDataArgumentCaptorWithTags.getValue().getTaskType())
        .isEqualTo(TaskType.FETCH_S3_FILE_TASK_NG.name());
    assertThat(responseWithTags.getTaskRequest()).isNotNull();
    assertThat(responseWithTags.getPassThroughData()).isNotNull();
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithInline() {
    StepElementParameters stepElementParameters = createStepParameterInline(false);
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(
                AwsConnectorDTO.builder()
                    .credential(AwsCredentialDTO.builder().config(AwsManualConfigSpecDTO.builder().build()).build())
                    .build())
            .build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());

    ArgumentCaptor<CloudformationTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(CloudformationTaskNGParameters.class);
    cloudformationStepHelper.startChainLink(cloudformationStepExecutor, getAmbiance(), stepElementParameters);

    doReturn("test-template").when(engineExpressionService).renderExpression(any(), eq("test-template"));
    verify(cloudformationStepExecutor).executeCloudformationTask(any(), any(), taskDataArgumentCaptor.capture());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(CloudformationTaskType.CREATE_STACK);
    assertThat(taskDataArgumentCaptor.getValue().getRegion()).isEqualTo("region");
    assertThat(taskDataArgumentCaptor.getValue().getStackName()).isEqualTo("stack-name");

    StepElementParameters stepElementParametersWithTags = createStepParameterInline(true);
    ArgumentCaptor<CloudformationTaskNGParameters> taskDataArgumentCaptorWithTags =
        ArgumentCaptor.forClass(CloudformationTaskNGParameters.class);
    cloudformationStepHelper.startChainLink(cloudformationStepExecutor, getAmbiance(), stepElementParametersWithTags);
    verify(cloudformationStepExecutor, times(2))
        .executeCloudformationTask(any(), any(), taskDataArgumentCaptorWithTags.capture());
    assertThat(taskDataArgumentCaptorWithTags.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptorWithTags.getValue().getTaskType()).isEqualTo(CloudformationTaskType.CREATE_STACK);
    assertThat(taskDataArgumentCaptorWithTags.getValue().getRegion()).isEqualTo("region");
    assertThat(taskDataArgumentCaptorWithTags.getValue().getStackName()).isEqualTo("stack-name");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void testStartChainLinkWithS3Template() {
    StepElementParameters stepElementParameters = createStepParameterS3WithNoParameterFiles();
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(
                AwsConnectorDTO.builder()
                    .credential(AwsCredentialDTO.builder().config(AwsManualConfigSpecDTO.builder().build()).build())
                    .build())
            .build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());

    ArgumentCaptor<CloudformationTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(CloudformationTaskNGParameters.class);
    cloudformationStepHelper.startChainLink(cloudformationStepExecutor, getAmbiance(), stepElementParameters);

    verify(cloudformationStepExecutor).executeCloudformationTask(any(), any(), taskDataArgumentCaptor.capture());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getTemplateUrl()).isEqualTo("test-url");
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(CloudformationTaskType.CREATE_STACK);
    assertThat(taskDataArgumentCaptor.getValue().getRegion()).isEqualTo("region");
    assertThat(taskDataArgumentCaptor.getValue().getStackName()).isEqualTo("stack-name");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  // This test is going to test the executeNextLink with the param files been only in github and with the template files
  // being in git, inline and s3
  public void executeNextLinkGitNoS3() throws Exception {
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(
                AwsConnectorDTO.builder()
                    .credential(AwsCredentialDTO.builder().config(AwsManualConfigSpecDTO.builder().build()).build())
                    .build())
            .build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());

    StepElementParameters stepElementParameters = createStepParametersWithGit(false);

    LinkedHashMap<String, List<String>> parameters = new LinkedHashMap<>();

    parameters.put("param1", null);

    CloudFormationCreateStackPassThroughData passThroughData = CloudFormationCreateStackPassThroughData.builder()
                                                                   .hasGitFiles(true)
                                                                   .hasS3Files(false)
                                                                   .parametersFilesContent(parameters)
                                                                   .build();

    Map<String, FetchFilesResult> filesFromMultiRepo = new HashMap<>();

    filesFromMultiRepo.put("param1",
        FetchFilesResult.builder()
            .files(Arrays.asList(GitFile.builder()
                                     .fileContent("[\n"
                                         + "  {\n"
                                         + "    \"ParameterKey\": \"AlarmEMail\",\n"
                                         + "    \"ParameterValue\": \"nasser.gonzalez@harness.io\"\n"
                                         + "  }\n"
                                         + "]")
                                     .filePath("file-path")
                                     .build()))
            .build());
    filesFromMultiRepo.put("tagsFile",
        FetchFilesResult.builder()
            .files(Arrays.asList(GitFile.builder().fileContent(TAGS).filePath("file-path").build()))
            .build());
    filesFromMultiRepo.put("templateFile",
        FetchFilesResult.builder()
            .files(Arrays.asList(GitFile.builder().fileContent("foobar").filePath("file-path").build()))
            .build());

    GitFetchResponse response = GitFetchResponse.builder().filesFromMultipleRepo(filesFromMultiRepo).build();

    ArgumentCaptor<CloudformationTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(CloudformationTaskNGParameters.class);
    doReturn("foobar").when(engineExpressionService).renderExpression(any(), eq("foobar"));
    doReturn(TAGS).when(engineExpressionService).renderExpression(any(), eq(TAGS));

    cloudformationStepHelper.executeNextLink(
        cloudformationStepExecutor, getAmbiance(), stepElementParameters, passThroughData, () -> response);

    verify(cloudformationStepExecutor).executeCloudformationTask(any(), any(), taskDataArgumentCaptor.capture());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getTemplateBody()).isEqualTo("foobar");
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(CloudformationTaskType.CREATE_STACK);
    assertThat(taskDataArgumentCaptor.getValue().getRegion()).isEqualTo("region");
    assertThat(taskDataArgumentCaptor.getValue().getStackName()).isEqualTo("stack-name");
    assertThat(taskDataArgumentCaptor.getValue().getParameters().get("AlarmEMail"))
        .isEqualTo("nasser.gonzalez@harness.io");
    assertThat(taskDataArgumentCaptor.getValue().getTags().equals(TAGS));

    filesFromMultiRepo.remove("templateFile");

    // Test now the same scenario but with the template been Inline
    StepElementParameters stepElementParametersInline = createStepParameterInline(false);
    ArgumentCaptor<CloudformationTaskNGParameters> taskDataArgumentCaptorInline =
        ArgumentCaptor.forClass(CloudformationTaskNGParameters.class);
    doReturn("test-template").when(engineExpressionService).renderExpression(any(), eq("test-template"));

    cloudformationStepHelper.executeNextLink(
        cloudformationStepExecutor, getAmbiance(), stepElementParametersInline, passThroughData, () -> response);
    verify(cloudformationStepExecutor, times(2))
        .executeCloudformationTask(any(), any(), taskDataArgumentCaptorInline.capture());
    assertThat(taskDataArgumentCaptorInline.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptorInline.getValue().getTemplateBody()).isEqualTo("test-template");
    assertThat(taskDataArgumentCaptorInline.getValue().getTaskType()).isEqualTo(CloudformationTaskType.CREATE_STACK);
    assertThat(taskDataArgumentCaptorInline.getValue().getRegion()).isEqualTo("region");
    assertThat(taskDataArgumentCaptorInline.getValue().getStackName()).isEqualTo("stack-name");
    assertThat(taskDataArgumentCaptorInline.getValue().getParameters().get("AlarmEMail"))
        .isEqualTo("nasser.gonzalez@harness.io");
    assertThat(taskDataArgumentCaptor.getValue().getTags().equals(TAGS));

    // Test now the same scenario but with the template been in S3 URL
    StepElementParameters stepElementParametersS3Url = createStepParameterS3WithNoParameterFiles();
    ArgumentCaptor<CloudformationTaskNGParameters> taskDataArgumentCaptorS3Url =
        ArgumentCaptor.forClass(CloudformationTaskNGParameters.class);

    cloudformationStepHelper.executeNextLink(
        cloudformationStepExecutor, getAmbiance(), stepElementParametersS3Url, passThroughData, () -> response);
    verify(cloudformationStepExecutor, times(3))
        .executeCloudformationTask(any(), any(), taskDataArgumentCaptorS3Url.capture());
    assertThat(taskDataArgumentCaptorS3Url.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptorS3Url.getValue().getTemplateUrl()).isEqualTo("test-url");
    assertThat(taskDataArgumentCaptorS3Url.getValue().getTaskType()).isEqualTo(CloudformationTaskType.CREATE_STACK);
    assertThat(taskDataArgumentCaptorS3Url.getValue().getRegion()).isEqualTo("region");
    assertThat(taskDataArgumentCaptorS3Url.getValue().getStackName()).isEqualTo("stack-name");
    assertThat(taskDataArgumentCaptorS3Url.getValue().getParameters().get("AlarmEMail"))
        .isEqualTo("nasser.gonzalez@harness.io");
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  // This test is going to test the executeNextLink with the param files been only in S3 and with the template files
  // being in git, inline and s3
  public void executeNextLinkS3NoGit() throws Exception {
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(
                AwsConnectorDTO.builder()
                    .credential(AwsCredentialDTO.builder().config(AwsManualConfigSpecDTO.builder().build()).build())
                    .build())
            .build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());

    StepElementParameters stepElementParameters = createStepParametersWithS3(false);

    LinkedHashMap<String, List<String>> parameters = new LinkedHashMap<>();

    parameters.put("param1", null);

    CloudFormationCreateStackPassThroughData passThroughData = CloudFormationCreateStackPassThroughData.builder()
                                                                   .hasGitFiles(false)
                                                                   .hasS3Files(true)
                                                                   .parametersFilesContent(parameters)
                                                                   .build();

    Map<String, List<S3FileDetailResponse>> filesFromMultiRepo = new HashMap<>();

    filesFromMultiRepo.put("param1",
        Arrays.asList(S3FileDetailResponse.builder()
                          .fileContent("[\n"
                              + "  {\n"
                              + "    \"ParameterKey\": \"AlarmEMail\",\n"
                              + "    \"ParameterValue\": \"nasser.gonzalez@harness.io\"\n"
                              + "  }\n"
                              + "]")
                          .build()));

    filesFromMultiRepo.put("tagsFile", Arrays.asList(S3FileDetailResponse.builder().fileContent(TAGS).build()));
    filesFromMultiRepo.put(
        "templateFile", Arrays.asList(S3FileDetailResponse.builder().fileContent("template-content").build()));

    AwsS3FetchFilesResponse response = AwsS3FetchFilesResponse.builder().s3filesDetails(filesFromMultiRepo).build();

    ArgumentCaptor<CloudformationTaskNGParameters> taskDataArgumentCaptor =
        ArgumentCaptor.forClass(CloudformationTaskNGParameters.class);
    doReturn("template-content").when(engineExpressionService).renderExpression(any(), eq("template-content"));
    doReturn(TAGS).when(engineExpressionService).renderExpression(any(), eq(TAGS));

    cloudformationStepHelper.executeNextLink(
        cloudformationStepExecutor, getAmbiance(), stepElementParameters, passThroughData, () -> response);

    verify(cloudformationStepExecutor).executeCloudformationTask(any(), any(), taskDataArgumentCaptor.capture());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getTemplateBody()).isEqualTo("template-content");
    assertThat(taskDataArgumentCaptor.getValue().getTaskType()).isEqualTo(CloudformationTaskType.CREATE_STACK);
    assertThat(taskDataArgumentCaptor.getValue().getRegion()).isEqualTo("region");
    assertThat(taskDataArgumentCaptor.getValue().getStackName()).isEqualTo("stack-name");
    assertThat(taskDataArgumentCaptor.getValue().getParameters().get("AlarmEMail"))
        .isEqualTo("nasser.gonzalez@harness.io");
    assertThat(taskDataArgumentCaptor.getValue().getTags().equals(TAGS));

    filesFromMultiRepo.remove("templateFile");

    // Test now the same scenario but with the template been Inline
    StepElementParameters stepElementParametersInline = createStepParameterInline(false);
    ArgumentCaptor<CloudformationTaskNGParameters> taskDataArgumentCaptorInline =
        ArgumentCaptor.forClass(CloudformationTaskNGParameters.class);
    doReturn("test-template").when(engineExpressionService).renderExpression(any(), eq("test-template"));

    cloudformationStepHelper.executeNextLink(
        cloudformationStepExecutor, getAmbiance(), stepElementParametersInline, passThroughData, () -> response);
    verify(cloudformationStepExecutor, times(2))
        .executeCloudformationTask(any(), any(), taskDataArgumentCaptorInline.capture());
    assertThat(taskDataArgumentCaptorInline.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptorInline.getValue().getTemplateBody()).isEqualTo("test-template");
    assertThat(taskDataArgumentCaptorInline.getValue().getTaskType()).isEqualTo(CloudformationTaskType.CREATE_STACK);
    assertThat(taskDataArgumentCaptorInline.getValue().getRegion()).isEqualTo("region");
    assertThat(taskDataArgumentCaptorInline.getValue().getStackName()).isEqualTo("stack-name");
    assertThat(taskDataArgumentCaptorInline.getValue().getParameters().get("AlarmEMail"))
        .isEqualTo("nasser.gonzalez@harness.io");
    assertThat(taskDataArgumentCaptor.getValue().getTags().equals(TAGS));

    // Test now the same scenario but with the template been in S3 URL
    StepElementParameters stepElementParametersS3Url = createStepParameterS3WithNoParameterFiles();
    ArgumentCaptor<CloudformationTaskNGParameters> taskDataArgumentCaptorS3Url =
        ArgumentCaptor.forClass(CloudformationTaskNGParameters.class);

    cloudformationStepHelper.executeNextLink(
        cloudformationStepExecutor, getAmbiance(), stepElementParametersS3Url, passThroughData, () -> response);
    verify(cloudformationStepExecutor, times(3))
        .executeCloudformationTask(any(), any(), taskDataArgumentCaptorS3Url.capture());
    assertThat(taskDataArgumentCaptorS3Url.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptorS3Url.getValue().getTemplateUrl()).isEqualTo("test-url");
    assertThat(taskDataArgumentCaptorS3Url.getValue().getTaskType()).isEqualTo(CloudformationTaskType.CREATE_STACK);
    assertThat(taskDataArgumentCaptorS3Url.getValue().getRegion()).isEqualTo("region");
    assertThat(taskDataArgumentCaptorS3Url.getValue().getStackName()).isEqualTo("stack-name");
    assertThat(taskDataArgumentCaptorS3Url.getValue().getParameters().get("AlarmEMail"))
        .isEqualTo("nasser.gonzalez@harness.io");
    assertThat(taskDataArgumentCaptor.getValue().getTags().equals(TAGS));
  }

  @Test
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  // This test is going to test the executeNextLink with the param files been stored in an unsupported store
  public void executeNextLinkWithoutS3OrGit() throws Exception {
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(
                AwsConnectorDTO.builder()
                    .credential(AwsCredentialDTO.builder().config(AwsManualConfigSpecDTO.builder().build()).build())
                    .build())
            .build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());

    StepElementParameters stepElementParameters = createStepParameterInline(false);

    LinkedHashMap<String, List<String>> parameters = new LinkedHashMap<>();

    parameters.put("param1", null);

    CloudFormationCreateStackPassThroughData passThroughData = CloudFormationCreateStackPassThroughData.builder()
                                                                   .hasGitFiles(false)
                                                                   .hasS3Files(true)
                                                                   .parametersFilesContent(parameters)
                                                                   .build();

    ArtifactTaskResponse artifactTaskResponse = ArtifactTaskResponse.builder().build();

    TaskChainResponse response = cloudformationStepHelper.executeNextLink(
        cloudformationStepExecutor, getAmbiance(), stepElementParameters, passThroughData, () -> artifactTaskResponse);
    assertThat(response).isNotNull();
    assertThat(response.isChainEnd()).isTrue();
  }

  @Test()
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  // This test is going to test the executeNextLink with exceptions scenarios
  public void executeNextLinkWithExceptions() throws Exception {
    ConnectorInfoDTO connectorInfoDTO =
        ConnectorInfoDTO.builder()
            .connectorConfig(
                AwsConnectorDTO.builder()
                    .credential(AwsCredentialDTO.builder().config(AwsManualConfigSpecDTO.builder().build()).build())
                    .build())
            .build();
    doReturn(connectorInfoDTO).when(cdStepHelper).getConnector(any(), any());

    StepElementParameters stepElementParameters = createStepParametersWithS3(false);

    LinkedHashMap<String, List<String>> parameters = new LinkedHashMap<>();

    parameters.put("param1", null);

    CloudFormationCreateStackPassThroughData passThroughData = CloudFormationCreateStackPassThroughData.builder()
                                                                   .hasGitFiles(false)
                                                                   .hasS3Files(true)
                                                                   .parametersFilesContent(parameters)
                                                                   .build();

    AwsS3FetchFilesResponse awsS3FetchFilesResponse =
        AwsS3FetchFilesResponse.builder()
            .unitProgressData(
                UnitProgressData.builder()
                    .unitProgresses(Arrays.asList(
                        UnitProgress.newBuilder().setUnitName("name").setStatus(UnitStatus.FAILURE).build()))
                    .build())
            .build();
    doReturn(UnitProgressData.builder()
                 .unitProgresses(
                     Arrays.asList(UnitProgress.newBuilder().setUnitName("name").setStatus(UnitStatus.FAILURE).build()))
                 .build())
        .when(cdStepHelper)
        .completeUnitProgressData(any(), any(), any());
    TaskChainResponse response = cloudformationStepHelper.executeNextLink(cloudformationStepExecutor, getAmbiance(),
        stepElementParameters, passThroughData, () -> awsS3FetchFilesResponse);
    assertThat(response).isNotNull();
    assertThat(response.isChainEnd()).isTrue();
    StepExceptionPassThroughData stepExceptionPassThroughData =
        (StepExceptionPassThroughData) response.getPassThroughData();
    assertThat(stepExceptionPassThroughData.getUnitProgressData().getUnitProgresses().get(0).getStatus())
        .isEqualTo(UnitStatus.FAILURE);
  }

  @Test()
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void saveCloudformationInheritInputTest() {
    RemoteCloudformationTemplateFileSpec templateFileSpec = new RemoteCloudformationTemplateFileSpec();
    CloudformationParametersFileSpec parametersFileSpec = new CloudformationParametersFileSpec();
    CloudformationParametersFileSpec parametersFileSpec2 = new CloudformationParametersFileSpec();

    StoreConfigWrapper storeConfigWrapper =
        StoreConfigWrapper.builder()
            .spec(GithubStore.builder()
                      .repoName(ParameterField.createValueField("repoName"))
                      .paths(ParameterField.createValueField(Arrays.asList("path1")))
                      .branch(ParameterField.createValueField("branch1"))
                      .gitFetchType(FetchType.BRANCH)
                      .connectorRef(ParameterField.createValueField("test-connector"))
                      .build())
            .build();
    parametersFileSpec.setStore(storeConfigWrapper);
    parametersFileSpec2.setStore(storeConfigWrapper);
    templateFileSpec.setStore(storeConfigWrapper);
    CloudformationCreateStackStepConfiguration config =
        CloudformationCreateStackStepConfiguration.builder()
            .connectorRef(ParameterField.createValueField("aws-connector"))
            .region(ParameterField.createValueField("region"))
            .stackName(ParameterField.createValueField("stack-name"))
            .parametersFilesSpecs(Arrays.asList(parametersFileSpec, parametersFileSpec2))
            .templateFile(CloudformationTemplateFile.builder()
                              .spec(templateFileSpec)
                              .type(CloudformationTemplateFileTypes.Remote)
                              .build())
            .build();

    ArgumentCaptor<CloudFormationInheritOutput> dataCaptor = ArgumentCaptor.forClass(CloudFormationInheritOutput.class);
    cloudformationStepHelper.saveCloudFormationInheritOutput(config, "id", getAmbiance());
    verify(executionSweepingOutputService).consume(any(), any(), dataCaptor.capture(), any());
    assertThat(dataCaptor.getValue()).isNotNull();
    assertThat(dataCaptor.getValue().getStackName()).isEqualTo("stack-name");
    assertThat(dataCaptor.getValue().getRegion()).isEqualTo("region");
    assertThat(dataCaptor.getValue().getConnectorRef()).isEqualTo("aws-connector");
  }

  @Test()
  @Owner(developers = NGONZALEZ)
  @Category(UnitTests.class)
  public void getSavedCloudformationInheritInputTest() {
    CloudFormationInheritOutput cloudFormationInheritOutput = CloudFormationInheritOutput.builder()
                                                                  .stackName("stack-name")
                                                                  .connectorRef("aws-connector")
                                                                  .region("region")
                                                                  .roleArn("role-arn")
                                                                  .build();

    doReturn(OptionalSweepingOutput.builder().output(cloudFormationInheritOutput).found(true).build())
        .when(executionSweepingOutputService)
        .resolveOptional(any(), any());
    CloudFormationInheritOutput output =
        cloudformationStepHelper.getSavedCloudFormationInheritOutput("id", getAmbiance());
    assertThat(output).isNotNull();
    assertThat(output.getStackName()).isEqualTo("stack-name");
    assertThat(output.getConnectorRef()).isEqualTo("aws-connector");
    assertThat(output.getRegion()).isEqualTo("region");
    assertThat(output.getRoleArn()).isEqualTo("role-arn");
  }

  public StepElementParameters createStepParametersWithS3(boolean tags) {
    CloudformationCreateStackStepParameters parameters = new CloudformationCreateStackStepParameters();
    RemoteCloudformationTemplateFileSpec templateFileSpec = new RemoteCloudformationTemplateFileSpec();
    CloudformationParametersFileSpec parametersFileSpec = new CloudformationParametersFileSpec();
    CloudformationParametersFileSpec parametersFileSpec2 = new CloudformationParametersFileSpec();
    RemoteCloudformationTagsFileSpec tagsFileSpec = new RemoteCloudformationTagsFileSpec();

    StoreConfigWrapper storeConfigWrapper =
        StoreConfigWrapper.builder()
            .type(StoreConfigType.S3URL)
            .spec(S3UrlStoreConfig.builder()
                      .urls(ParameterField.createValueField(Arrays.asList("url1")))
                      .region(ParameterField.createValueField("region"))
                      .connectorRef(ParameterField.createValueField("test-connector"))
                      .build())
            .build();
    parametersFileSpec.setStore(storeConfigWrapper);
    parametersFileSpec2.setStore(storeConfigWrapper);
    templateFileSpec.setStore(storeConfigWrapper);
    tagsFileSpec.setStore(storeConfigWrapper);

    parameters.setConfiguration(
        CloudformationCreateStackStepConfiguration.builder()
            .tags(tags
                    ? CloudformationTags.builder().type(CloudformationTagsFileTypes.Remote).spec(tagsFileSpec).build()
                    : null)
            .region(ParameterField.createValueField("region"))
            .stackName(ParameterField.createValueField("stack-name"))
            .parametersFilesSpecs(Arrays.asList(parametersFileSpec, parametersFileSpec2))
            .templateFile(CloudformationTemplateFile.builder()
                              .spec(templateFileSpec)
                              .type(CloudformationTemplateFileTypes.Remote)
                              .build())
            .build());
    return StepElementParameters.builder().spec(parameters).build();
  }

  public StepElementParameters createStepParameterInline(boolean tags) {
    CloudformationCreateStackStepParameters parameters = new CloudformationCreateStackStepParameters();
    InlineCloudformationTemplateFileSpec templateFileSpec = new InlineCloudformationTemplateFileSpec();
    InlineCloudformationTagsFileSpec tagsFileSpec = new InlineCloudformationTagsFileSpec();

    templateFileSpec.setTemplateBody(ParameterField.createValueField("test-template"));
    tagsFileSpec.setContent(ParameterField.createValueField(TAGS));
    parameters.setConfiguration(
        CloudformationCreateStackStepConfiguration.builder()
            .tags(tags
                    ? CloudformationTags.builder().type(CloudformationTagsFileTypes.Inline).spec(tagsFileSpec).build()
                    : null)
            .region(ParameterField.createValueField("region"))
            .stackName(ParameterField.createValueField("stack-name"))
            .templateFile(CloudformationTemplateFile.builder()
                              .spec(templateFileSpec)
                              .type(CloudformationTemplateFileTypes.Inline)
                              .build())
            .build());
    return StepElementParameters.builder().spec(parameters).build();
  }

  public StepElementParameters createStepParameterS3WithNoParameterFiles() {
    CloudformationCreateStackStepParameters parameters = new CloudformationCreateStackStepParameters();
    S3UrlCloudformationTemplateFileSpec templateFileSpec = new S3UrlCloudformationTemplateFileSpec();
    templateFileSpec.setTemplateUrl(ParameterField.createValueField("test-url"));
    parameters.setConfiguration(CloudformationCreateStackStepConfiguration.builder()
                                    .region(ParameterField.createValueField("region"))
                                    .stackName(ParameterField.createValueField("stack-name"))
                                    .templateFile(CloudformationTemplateFile.builder()
                                                      .spec(templateFileSpec)
                                                      .type(CloudformationTemplateFileTypes.S3Url)
                                                      .build())
                                    .build());
    return StepElementParameters.builder().spec(parameters).build();
  }

  public StepElementParameters createStepParametersWithGit(boolean tags) {
    CloudformationCreateStackStepParameters parameters = new CloudformationCreateStackStepParameters();
    RemoteCloudformationTemplateFileSpec templateFileSpec = new RemoteCloudformationTemplateFileSpec();
    CloudformationParametersFileSpec parametersFileSpec = new CloudformationParametersFileSpec();
    CloudformationParametersFileSpec parametersFileSpec2 = new CloudformationParametersFileSpec();
    RemoteCloudformationTagsFileSpec tagsFileSpec = new RemoteCloudformationTagsFileSpec();

    StoreConfigWrapper storeConfigWrapper =
        StoreConfigWrapper.builder()
            .type(StoreConfigType.GIT)
            .spec(GithubStore.builder()
                      .repoName(ParameterField.createValueField("repoName"))
                      .paths(ParameterField.createValueField(Arrays.asList("path1")))
                      .branch(ParameterField.createValueField("branch1"))
                      .gitFetchType(FetchType.BRANCH)
                      .connectorRef(ParameterField.createValueField("test-connector"))
                      .build())
            .build();
    parametersFileSpec.setStore(storeConfigWrapper);
    parametersFileSpec.setIdentifier("test-identifier");
    parametersFileSpec2.setStore(storeConfigWrapper);
    parametersFileSpec2.setIdentifier("test-identifier2");
    templateFileSpec.setStore(storeConfigWrapper);
    tagsFileSpec.setStore(storeConfigWrapper);
    parameters.setConfiguration(
        CloudformationCreateStackStepConfiguration.builder()
            .tags(tags
                    ? CloudformationTags.builder().type(CloudformationTagsFileTypes.Remote).spec(tagsFileSpec).build()
                    : null)
            .parametersFilesSpecs(Arrays.asList(parametersFileSpec, parametersFileSpec2))
            .region(ParameterField.createValueField("region"))
            .stackName(ParameterField.createValueField("stack-name"))
            .templateFile(CloudformationTemplateFile.builder()
                              .spec(templateFileSpec)
                              .type(CloudformationTemplateFileTypes.Remote)
                              .build())
            .build());
    return StepElementParameters.builder().spec(parameters).build();
  }
}
