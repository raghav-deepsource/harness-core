/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.service;

import io.harness.cdng.manifest.ManifestConfigType;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigType;
import io.harness.cdng.manifest.yaml.storeConfig.StoreConfigWrapper;
import io.harness.ngmigration.beans.ManifestProvidedEntitySpec;
import io.harness.ngmigration.beans.NgEntityDetail;
import io.harness.pms.yaml.ParameterField;

import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.ngmigration.CgEntityId;
import software.wings.ngmigration.NGMigrationEntityType;

import com.google.inject.Inject;
import java.util.Map;

public class NgManifestFactory {
  @Inject ManifestMigrationService manifestMigrationService;

  public ManifestConfigWrapper getManifestConfigWrapper(ApplicationManifest applicationManifest,
      Map<CgEntityId, NgEntityDetail> migratedEntities, ManifestProvidedEntitySpec entitySpec) {
    switch (applicationManifest.getKind()) {
      case K8S_MANIFEST:
        return k8sManifestConfigWrapper(applicationManifest, migratedEntities, entitySpec);
      case VALUES:
        return valuesManifestConfigWrapper(applicationManifest, migratedEntities, entitySpec);
      default:
        throw new IllegalStateException();
    }
  }

  private ManifestConfigWrapper valuesManifestConfigWrapper(ApplicationManifest applicationManifest,
      Map<CgEntityId, NgEntityDetail> migratedEntities, ManifestProvidedEntitySpec entitySpec) {
    GitFileConfig gitFileConfig = applicationManifest.getGitFileConfig();
    NgEntityDetail connector = migratedEntities.get(
        CgEntityId.builder().id(gitFileConfig.getConnectorId()).type(NGMigrationEntityType.CONNECTOR).build());

    ValuesManifest valuesManifest =
        ValuesManifest.builder()
            .identifier(MigratorUtility.generateIdentifier(applicationManifest.getUuid()))
            .store(ParameterField.createValueField(
                StoreConfigWrapper.builder()
                    .type(StoreConfigType.GIT)
                    .spec(manifestMigrationService.getGitStore(gitFileConfig, entitySpec, connector.getIdentifier()))
                    .build()))
            .build();
    return ManifestConfigWrapper.builder()
        .manifest(ManifestConfig.builder()
                      .identifier(MigratorUtility.generateIdentifier(applicationManifest.getUuid()))
                      .type(ManifestConfigType.VALUES)
                      .spec(valuesManifest)
                      .build())
        .build();
  }

  private ManifestConfigWrapper k8sManifestConfigWrapper(ApplicationManifest applicationManifest,
      Map<CgEntityId, NgEntityDetail> migratedEntities, ManifestProvidedEntitySpec entitySpec) {
    // TODO: get store from migrated connector entity
    GitFileConfig gitFileConfig = applicationManifest.getGitFileConfig();
    NgEntityDetail connector = migratedEntities.get(
        CgEntityId.builder().id(gitFileConfig.getConnectorId()).type(NGMigrationEntityType.CONNECTOR).build());

    K8sManifest k8sManifest =
        K8sManifest
            .builder()
            // TODO: There needs to be a logic to build identifier of the manifest
            .identifier(MigratorUtility.generateIdentifier(applicationManifest.getUuid()))
            .skipResourceVersioning(
                ParameterField.createValueField(applicationManifest.getSkipVersioningForAllK8sObjects()))
            .store(ParameterField.createValueField(
                StoreConfigWrapper.builder()
                    .type(StoreConfigType.GIT)
                    .spec(manifestMigrationService.getGitStore(gitFileConfig, entitySpec, connector.getIdentifier()))
                    .build()))
            .build();
    return ManifestConfigWrapper.builder()
        .manifest(ManifestConfig.builder()
                      .identifier(MigratorUtility.generateIdentifier(applicationManifest.getUuid()))
                      .type(ManifestConfigType.K8_MANIFEST)
                      .spec(k8sManifest)
                      .build())
        .build();
  }
}
