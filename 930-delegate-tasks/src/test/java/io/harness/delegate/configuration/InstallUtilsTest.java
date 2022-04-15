/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.configuration;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.delegate.configuration.InstallUtils.getChartMuseumPath;
import static io.harness.delegate.configuration.InstallUtils.getDefaultKubectlPath;
import static io.harness.delegate.configuration.InstallUtils.getGoTemplateToolPath;
import static io.harness.delegate.configuration.InstallUtils.getHarnessPywinrmToolPath;
import static io.harness.delegate.configuration.InstallUtils.getHelm2Path;
import static io.harness.delegate.configuration.InstallUtils.getHelm380Path;
import static io.harness.delegate.configuration.InstallUtils.getHelm3Path;
import static io.harness.delegate.configuration.InstallUtils.getKustomizePath;
import static io.harness.delegate.configuration.InstallUtils.getNewKubectlPath;
import static io.harness.delegate.configuration.InstallUtils.getOcPath;
import static io.harness.delegate.configuration.InstallUtils.getOsPath;
import static io.harness.delegate.configuration.InstallUtils.getScmPath;
import static io.harness.delegate.configuration.InstallUtils.getTerraformConfigInspectPath;
import static io.harness.delegate.configuration.InstallUtils.helm2Version;
import static io.harness.delegate.configuration.InstallUtils.helm3Version;
import static io.harness.delegate.configuration.InstallUtils.installKustomize;
import static io.harness.delegate.configuration.InstallUtils.isCustomKustomizePath;
import static io.harness.delegate.configuration.InstallUtils.kustomizePath;
import static io.harness.delegate.configuration.InstallUtils.setupDefaultPaths;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.NAMAN_TALAYCHA;
import static io.harness.rule.OwnerRule.RIHAZ;
import static io.harness.rule.OwnerRule.SHUBHAM;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.VUK;
import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.FunctionalTests;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.io.IOException;
import java.nio.file.Paths;
import org.apache.commons.lang3.SystemUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(DEL)
public class InstallUtilsTest {
  DelegateConfiguration delegateConfiguration =
      DelegateConfiguration.builder().managerUrl("localhost").maxCachedArtifacts(10).build();
  String terraformConfigInspectVersion = "v1.0";

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void testGetTerraformConfigInspectDownloadUrlPath() {
    boolean useCdn = delegateConfiguration.isUseCdn();
    assertThat(useCdn).isFalse();
    final String osPath = getOsPath();

    assertThat(InstallUtils.getTerraformConfigInspectDownloadUrl(delegateConfiguration, terraformConfigInspectVersion))
        .isEqualTo("https://app.harness.io/storage/harness-download/harness-terraform-config-inspect/v1.0/" + osPath
            + "/amd64/terraform-config-inspect");
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void testGetScmDownloadUrlPath() {
    final boolean useCdn = delegateConfiguration.isUseCdn();
    final String osPath = getOsPath();

    assertThat(useCdn).isFalse();

    assertThat(InstallUtils.getScmDownloadUrl(delegateConfiguration))
        .isEqualTo("https://app.harness.io/storage/harness-download/harness-scm/release/98fc345b/bin/" + osPath
            + "/amd64/scm");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testisHelmV2() {
    assertThat(InstallUtils.isHelmV2("V2")).isTrue();
    assertThat(InstallUtils.isHelmV2("v2")).isTrue();
    assertThat(InstallUtils.isHelmV2("V3")).isFalse();
    assertThat(InstallUtils.isHelmV2("v3")).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testisHelmV3() {
    assertThat(InstallUtils.isHelmV3("V3")).isTrue();
    assertThat(InstallUtils.isHelmV3("v3")).isTrue();
    assertThat(InstallUtils.isHelmV3("V2")).isFalse();
    assertThat(InstallUtils.isHelmV3("v2")).isFalse();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetHelmPath() {
    assertThat(InstallUtils.getHelm2Path()).isNotNull();
    assertThat(InstallUtils.getHelm3Path()).isNotNull();
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(UnitTests.class)
  public void testGetKustomizePath() {
    kustomizePath = "usr/local/bin/kustomize";
    isCustomKustomizePath = true;
    assertThat(getKustomizePath(true)).isEqualTo(kustomizePath);
    assertThat(getKustomizePath(false)).isEqualTo(kustomizePath);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(FunctionalTests.class)
  public void shouldInstallKustomize() throws IOException {
    assumeThat(SystemUtils.IS_OS_WINDOWS).isFalse();
    assumeThat(SystemUtils.IS_OS_MAC).isFalse();

    deleteDirectoryAndItsContentIfExists("./client-tools/kustomize/");
    assertThat(installKustomize(delegateConfiguration)).isTrue();

    // Won't download this time
    assertThat(installKustomize(delegateConfiguration)).isTrue();
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(FunctionalTests.class)
  public void shouldInstallKustomizeCustom() {
    assumeThat(SystemUtils.IS_OS_WINDOWS).isFalse();
    assumeThat(SystemUtils.IS_OS_MAC).isFalse();
    delegateConfiguration.setKustomizePath("usr/local/bin/kustomize");
    assertThat(installKustomize(delegateConfiguration)).isTrue();
    assertThat(kustomizePath).isEqualTo("usr/local/bin/kustomize");
    assertThat(isCustomKustomizePath).isTrue();
  }

  @Test
  @Owner(developers = NAMAN_TALAYCHA)
  @Category(FunctionalTests.class)
  @Ignore("This test is flaky, but I need to write something more to satisfy ridiculous checkstyle")
  public void shouldsetupDefaultPaths() {
    assumeThat(SystemUtils.IS_OS_WINDOWS).isFalse();
    assumeThat(SystemUtils.IS_OS_MAC).isFalse();
    delegateConfiguration.setKustomizePath("local/bin/kustomize");
    delegateConfiguration.setKubectlPath("usr/local/bin/kubectl");
    delegateConfiguration.setHelm3Path("usr/local/bin/helm3");
    delegateConfiguration.setHelmPath("usr/local/bin/helm2");
    delegateConfiguration.setOcPath("usr/local/bin/oc");
    setupDefaultPaths(delegateConfiguration);
    assertThat(installKustomize(delegateConfiguration)).isTrue();
    assertThat(kustomizePath).isEqualTo("local/bin/kustomize");
    assertThat(getDefaultKubectlPath()).isEqualTo("usr/local/bin/kubectl");
    assertThat(getNewKubectlPath()).isEqualTo("usr/local/bin/kubectl");
    assertThat(getOcPath()).isEqualTo("usr/local/bin/oc");
    assertThat(getHelm2Path()).isEqualTo("usr/local/bin/helm2");
    assertThat(getHelm3Path()).isEqualTo("usr/local/bin/helm3");
    assertThat(getKustomizePath(true)).isEqualTo("local/bin/kustomize");
    assertThat(getKustomizePath(false)).isEqualTo("local/bin/kustomize");
    assertThat(isCustomKustomizePath).isTrue();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldNotInstallWhenPathSetInDelegateConfig() {
    DelegateConfiguration delegateConfiguration = DelegateConfiguration.builder().kustomizePath("RANDOM").build();
    assertThat(installKustomize(delegateConfiguration)).isTrue();
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  @Ignore("This test is flaky, but I need to write something more to satisfy ridiculous checkstyle")
  public void testHelm2Install() {
    final DelegateConfiguration helm2DelegateConfiguration =
        DelegateConfiguration.builder().helmPath("helm2Path").build();
    final String pwd = Paths.get(".").toAbsolutePath().normalize().toString();

    assertThat(InstallUtils.delegateConfigHasHelmPath(helm2DelegateConfiguration, helm2Version)).isTrue();
    assertThat(InstallUtils.getHelm2Path()).isEqualTo("helm2Path");
    assertThat(InstallUtils.getHelm3Path()).isEqualTo(pwd + "/client-tools/helm/v3.1.2/helm");
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  @Ignore("This test is flaky, but I need to write something more to satisfy ridiculous checkstyle")
  public void testHelm3Install() {
    final DelegateConfiguration helm3DelegateConfiguration =
        DelegateConfiguration.builder().helm3Path("helm3Path").build();
    final String pwd = Paths.get(".").toAbsolutePath().normalize().toString();

    assertThat(InstallUtils.delegateConfigHasHelmPath(helm3DelegateConfiguration, helm3Version)).isTrue();
    assertThat(InstallUtils.getHelm3Path()).isEqualTo("helm3Path");
    assertThat(InstallUtils.getHelm2Path()).isEqualTo(pwd + "/client-tools/helm/v2.13.1/helm");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testDefaultPathsSet() {
    final String osPath = InstallUtils.getOsPath();
    final String pwd = Paths.get(".").toAbsolutePath().normalize().toString();
    assertThat(getDefaultKubectlPath()).isEqualTo(pwd + "/client-tools/kubectl/v1.13.2/kubectl");
    assertThat(getNewKubectlPath()).isEqualTo(pwd + "/client-tools/kubectl/v1.19.2/kubectl");
    assertThat(getGoTemplateToolPath()).isEqualTo(pwd + "/client-tools/go-template/v0.4/go-template");
    assertThat(getHarnessPywinrmToolPath()).isEqualTo(pwd + "/client-tools/harness-pywinrm/v0.4-dev/harness-pywinrm");
    assertThat(getHelm2Path()).isEqualTo(pwd + "/client-tools/helm/v2.13.1/helm");
    assertThat(getHelm3Path()).isEqualTo(pwd + "/client-tools/helm/v3.1.2/helm");
    assertThat(getHelm380Path()).isEqualTo(pwd + "/client-tools/helm/v3.8.0/helm");
    assertThat(getChartMuseumPath(true)).isEqualTo(pwd + "/client-tools/chartmuseum/v0.12.0/chartmuseum");
    assertThat(getChartMuseumPath(false)).isEqualTo(pwd + "/client-tools/chartmuseum/v0.8.2/chartmuseum");
    assertThat(getTerraformConfigInspectPath(true))
        .isEqualTo(pwd + "/client-tools/tf-config-inspect/v1.1/" + osPath + "/amd64/terraform-config-inspect");
    assertThat(getTerraformConfigInspectPath(false))
        .isEqualTo(pwd + "/client-tools/tf-config-inspect/v1.0/" + osPath + "/amd64/terraform-config-inspect");
    assertThat(getOcPath()).isEqualTo(pwd + "/client-tools/oc/v4.2.16/oc");
    assertThat(getKustomizePath(true)).isEqualTo(pwd + "/client-tools/kustomize/v4.0.0/kustomize");
    assertThat(getKustomizePath(false)).isEqualTo(pwd + "/client-tools/kustomize/v3.5.4/kustomize");
    assertThat(getScmPath()).isEqualTo(pwd + "/client-tools/scm/98fc345b/" + osPath + "/amd64/scm");
  }
}
