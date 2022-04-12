/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.authenticationsettings;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.VIKAS_M;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.authenticationsettings.impl.AuthenticationSettingsServiceImpl;
import io.harness.ng.authenticationsettings.remote.AuthSettingsManagerClient;
import io.harness.ng.core.api.UserGroupService;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.beans.sso.SamlSettings;
import software.wings.security.authentication.SSOConfig;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(PL)
public class AuthenticationSettingServiceImplTest extends CategoryTest {
  @Mock private AuthSettingsManagerClient managerClient;
  @Mock private UserGroupService userGroupService;
  @Inject @InjectMocks AuthenticationSettingsServiceImpl authenticationSettingsServiceImpl;

  private SamlSettings samlSettings;
  private static final String ACCOUNT_ID = "ACCOUNT_ID";

  @Before
  public void setup() {
    initMocks(this);
    samlSettings = SamlSettings.builder().accountId(ACCOUNT_ID).build();
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testDeleteSamlMetadata() throws IOException {
    Call<RestResponse<SamlSettings>> request = mock(Call.class);
    doReturn(request).when(managerClient).getSAMLMetadata(ACCOUNT_ID);
    RestResponse<SamlSettings> mockResponse = new RestResponse<>(samlSettings);
    doReturn(Response.success(mockResponse)).when(request).execute();
    List<String> userGroups = new ArrayList<>();
    doReturn(userGroups).when(userGroupService).getUserGroupsBySsoId(ACCOUNT_ID, samlSettings.getUuid());
    SSOConfig ssoConfig = SSOConfig.builder().accountId(ACCOUNT_ID).build();
    Call<RestResponse<SSOConfig>> config = mock(Call.class);
    doReturn(config).when(managerClient).deleteSAMLMetadata(ACCOUNT_ID);
    RestResponse<SSOConfig> mockConfig = new RestResponse<>(ssoConfig);
    doReturn(Response.success(mockConfig)).when(config).execute();
    SSOConfig expectedConfig = authenticationSettingsServiceImpl.deleteSAMLMetadata(ACCOUNT_ID);
    assertThat(expectedConfig.getAccountId()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testDeleteSamlMetadata_InvalidSSO_throwsException() throws IOException {
    Call<RestResponse<SamlSettings>> request = mock(Call.class);
    doReturn(request).when(managerClient).getSAMLMetadata(ACCOUNT_ID);
    RestResponse<SamlSettings> mockResponse = new RestResponse<>(null);
    doReturn(Response.success(mockResponse)).when(request).execute();
    try {
      authenticationSettingsServiceImpl.deleteSAMLMetadata(ACCOUNT_ID);
      fail("Deleting SAML metadata should fail.");
    } catch (InvalidRequestException e) {
      assertThat(e.getMessage()).isEqualTo("No Saml Metadata found for this account");
    }
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testDeleteSamlMetadata_WithExistingUserGroupsLinked_throwsException() throws IOException {
    Call<RestResponse<SamlSettings>> request = mock(Call.class);
    doReturn(request).when(managerClient).getSAMLMetadata(ACCOUNT_ID);
    RestResponse<SamlSettings> mockResponse = new RestResponse<>(samlSettings);
    doReturn(Response.success(mockResponse)).when(request).execute();
    List<String> userGroups = Collections.singletonList("userGroup1");
    doReturn(userGroups).when(userGroupService).getUserGroupsBySsoId(ACCOUNT_ID, samlSettings.getUuid());
    try {
      authenticationSettingsServiceImpl.deleteSAMLMetadata(ACCOUNT_ID);
      fail("Deleting SAML metadata should fail.");
    } catch (InvalidRequestException e) {
      assertThat(e.getMessage())
          .isEqualTo("Deleting Saml provider with linked user groups is not allowed. Unlink the user groups first");
    }
  }
}
