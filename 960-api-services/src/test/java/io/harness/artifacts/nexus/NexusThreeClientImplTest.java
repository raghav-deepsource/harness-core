/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.artifacts.nexus;

import static io.harness.rule.OwnerRule.MLUKIC;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.fail;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.category.element.UnitTests;
import io.harness.exception.ExplanationException;
import io.harness.exception.HintException;
import io.harness.exception.InvalidArtifactServerException;
import io.harness.exception.NexusRegistryException;
import io.harness.nexus.NexusRequest;
import io.harness.nexus.NexusThreeClientImpl;
import io.harness.nexus.NexusThreeRestClient;
import io.harness.rule.Owner;

import software.wings.utils.RepositoryFormat;

import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@OwnedBy(HarnessTeam.CDP)
public class NexusThreeClientImplTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Rule
  public WireMockRule wireMockRule =
      new WireMockRule(WireMockConfiguration.wireMockConfig()
                           .usingFilesUnderDirectory("960-api-services/src/test/resources")
                           .port(Options.DYNAMIC_PORT),
          false);
  @InjectMocks NexusThreeClientImpl nexusThreeService;
  @Mock NexusThreeRestClient nexusThreeRestClient;

  private static String url;
  private static String artifactRepoUrl;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    url = "http://localhost:" + wireMockRule.port();
    artifactRepoUrl = "http://localhost:999";
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetRepositories() {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username("username")
                                   .password("password".toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(url)
                                   .version("3.x")
                                   .build();

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("["
                                 + "{\n"
                                 + "        \"name\": \"docker-test1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus.dev.harness.io/repository/docker-test1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },"
                                 + "{\n"
                                 + "        \"name\": \"docker-test2\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus.dev.harness.io/repository/docker-test2\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }"
                                 + "]")));
    try {
      Map<String, String> response = nexusThreeService.getRepositories(nexusConfig, RepositoryFormat.docker.name());

      assertThat(response).isNotNull();
      assertThat(response).size().isEqualTo(2);
    } catch (Exception e) {
      fail("This point should not have been reached!", e);
    }
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testIsServerValid() {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username("username")
                                   .password("password".toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(url)
                                   .version("3.x")
                                   .build();

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("["
                                 + "{\n"
                                 + "        \"name\": \"docker-test1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus.dev.harness.io/repository/docker-test1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },"
                                 + "{\n"
                                 + "        \"name\": \"docker-test2\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus.dev.harness.io/repository/docker-test2\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }"
                                 + "]")));
    try {
      boolean response = nexusThreeService.isServerValid(nexusConfig);

      assertThat(response).isNotNull();
      assertThat(response).isEqualTo(true);
    } catch (Exception e) {
      fail("This point should not have been reached!", e);
    }

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories")).willReturn(aResponse().withStatus(404)));
    try {
      assertThatThrownBy(() -> nexusThreeService.isServerValid(nexusConfig))
          .isInstanceOf(HintException.class)
          .getCause()
          .isInstanceOf(ExplanationException.class)
          .getCause()
          .isInstanceOf(InvalidArtifactServerException.class);
    } catch (Exception e) {
      fail("This point should not have been reached!", e);
    }
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetArtifactsVersionsInvalidPort() {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username("username")
                                   .password("password".toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(url)
                                   .version("3.x")
                                   .build();

    assertThatThrownBy(()
                           -> nexusThreeService.getArtifactsVersions(
                               nexusConfig, "todolist", null, "todolist", RepositoryFormat.docker.name()))
        .isInstanceOf(HintException.class)
        .getCause()
        .isInstanceOf(ExplanationException.class)
        .getCause()
        .isInstanceOf(NexusRegistryException.class);
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetArtifactsVersionsSuccess() {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username("username")
                                   .password("password".toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version("3.x")
                                   .build();

    String repoKey = "TestRepoKey";
    String artifactPath = "test/artifact";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?repository=" + repoKey + "&name=" + artifactPath + "&format=docker"))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "        {\n"
                + "            \"id\": \"dG9kb2xpc3Q6OTNiOWI5ZWI5YTdlY2IwNjMyMDJhMTYwMzhmMTZkODk\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"a1new\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/a1new\",\n"
                + "                    \"path\": \"v2/todolist/manifests/a1new\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6N2Y2Mzc5ZDMyZjhkZDc4ZmRjMWY0MTM4NDI0M2JmOTE\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "],\n"
                + "\"continuationToken\": null\n"
                + "}")));
    List<BuildDetailsInternal> response = nexusThreeService.getArtifactsVersions(
        nexusConfig, repoKey, null, artifactPath, RepositoryFormat.docker.name());

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(2);
    List<String> dockerPullCommands =
        response.stream().map(bdi -> bdi.getMetadata().get(ArtifactMetadataKeys.IMAGE)).collect(Collectors.toList());

    String fullRepoPath = "localhost:999/" + artifactPath;
    assertThat(dockerPullCommands).contains(fullRepoPath + ":latest2", fullRepoPath + ":a1new");
  }

  @Test
  @Owner(developers = MLUKIC)
  @Category(UnitTests.class)
  public void testGetBuildDetails() {
    NexusRequest nexusConfig = NexusRequest.builder()
                                   .nexusUrl(url)
                                   .username("username")
                                   .password("password".toCharArray())
                                   .hasCredentials(true)
                                   .artifactRepositoryUrl(artifactRepoUrl)
                                   .version("3.x")
                                   .build();

    String repoKey = "TestRepoKey";
    String artifactPath = "test/artifact";
    String tag = "latest2";

    wireMockRule.stubFor(get(urlEqualTo("/service/rest/v1/repositories"))
                             .willReturn(aResponse().withStatus(200).withBody("[\n"
                                 + "    {\n"
                                 + "        \"name\": \"repo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"group\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/repo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"testrepo1\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/testrepo1\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    },\n"
                                 + "    {\n"
                                 + "        \"name\": \"" + repoKey + "\",\n"
                                 + "        \"format\": \"docker\",\n"
                                 + "        \"type\": \"hosted\",\n"
                                 + "        \"url\": \"https://nexus3.dev.harness.io/repository/" + repoKey + "\",\n"
                                 + "        \"attributes\": {}\n"
                                 + "    }\n"
                                 + "]")));

    wireMockRule.stubFor(get(urlEqualTo("/repository/" + repoKey + "/v2/_catalog"))
                             .willReturn(aResponse().withStatus(200).withBody("{\n"
                                 + "    \"repositories\": [\n"
                                 + "        \"busybox\",\n"
                                 + "        \"nginx\",\n"
                                 + "        \"" + artifactPath + "\"\n"
                                 + "    ]\n"
                                 + "}")));

    wireMockRule.stubFor(
        get(urlEqualTo("/service/rest/v1/search?repository=" + repoKey + "&name=" + artifactPath
                + "&format=docker&version=" + tag))
            .willReturn(aResponse().withStatus(200).withBody("{\n"
                + "    \"items\": [\n"
                + "{\n"
                + "            \"id\": \"dG9kb2xpc3Q6NzFhZmVhNTQwZTIzZGRlNTdiODg3MThiYzBmNWY3M2Q\",\n"
                + "            \"repository\": \"" + repoKey + "\",\n"
                + "            \"format\": \"docker\",\n"
                + "            \"group\": null,\n"
                + "            \"name\": \"" + artifactPath + "\",\n"
                + "            \"version\": \"latest2\",\n"
                + "            \"assets\": [\n"
                + "                {\n"
                + "                    \"downloadUrl\": \"https://nexus3.dev.harness.io/repository/todolist/v2/todolist/manifests/latest2\",\n"
                + "                    \"path\": \"v2/todolist/manifests/latest2\",\n"
                + "                    \"id\": \"dG9kb2xpc3Q6OTEyZDBmZTdiODE5MjM5MjcxODliMGYyNmQxMDE3NTQ\",\n"
                + "                    \"repository\": \"" + repoKey + "\",\n"
                + "                    \"format\": \"docker\",\n"
                + "                    \"checksum\": {\n"
                + "                        \"sha1\": \"0d0793de2da200fd2a821357adffe89438bbc9be\",\n"
                + "                        \"sha256\": \"90659bf80b44ce6be8234e6ff90a1ac34acbeb826903b02cfa0da11c82cbc042\"\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "        ],\n"
                + "\"continuationToken\": null"
                + "}")));

    List<BuildDetailsInternal> response = nexusThreeService.getBuildDetails(
        nexusConfig, repoKey, null, artifactPath, RepositoryFormat.docker.name(), tag);

    assertThat(response).isNotNull();
    assertThat(response).size().isEqualTo(1);
    assertThat(response.get(0).getMetadata().get(ArtifactMetadataKeys.IMAGE))
        .isEqualTo("localhost:999/" + artifactPath + ":" + tag);
  }
}
