package io.harness.accesscontrol.acl.api;

import static io.harness.rule.OwnerRule.UTKARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import io.harness.accesscontrol.AccessControlTestBase;
import io.harness.accesscontrol.acl.ACLService;
import io.harness.accesscontrol.preference.services.AccessControlPreferenceService;
import io.harness.accesscontrol.roleassignments.privileged.PrivilegedRoleAssignmentService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
public class ACLResourceImplTest extends AccessControlTestBase {
  private ACLResourceImpl aclResource;

  @Before
  public void setup() {
    ACLService aclService = mock(ACLService.class);
    AccessControlPreferenceService accessControlPreferenceService = mock(AccessControlPreferenceService.class);
    PrivilegedRoleAssignmentService privilegedRoleAssignmentService = mock(PrivilegedRoleAssignmentService.class);
    aclResource = new ACLResourceImpl(aclService, accessControlPreferenceService, privilegedRoleAssignmentService);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testAccessCheckWithNullPermissionCheckDTO() {
    Principal principal = Principal.builder().build();
    AccessCheckRequestDTO accessCheckRequestDTO =
        AccessCheckRequestDTO.builder().principal(Principal.builder().build()).permissions(null).build();
    ResponseDTO<AccessCheckResponseDTO> accessCheckResponse = aclResource.get(accessCheckRequestDTO);
    assertThat(principal).isEqualTo(accessCheckResponse.getData().getPrincipal());
    assertThat(accessCheckResponse.getData().getAccessControlList()).isNotNull();
    assertThat(accessCheckResponse.getData().getAccessControlList()).isEmpty();
  }
}
