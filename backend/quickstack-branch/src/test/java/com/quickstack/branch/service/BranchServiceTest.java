package com.quickstack.branch.service;

import com.quickstack.branch.dto.request.BranchCreateRequest;
import com.quickstack.branch.dto.request.BranchUpdateRequest;
import com.quickstack.branch.dto.response.BranchResponse;
import com.quickstack.branch.entity.Branch;
import com.quickstack.branch.repository.BranchRepository;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BranchService")
class BranchServiceTest {

    @Mock
    private BranchRepository branchRepository;

    private BranchService branchService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        branchService = new BranchService(branchRepository);
    }

    @Nested
    @DisplayName("createBranch")
    class CreateBranchTests {

        @Test
        @DisplayName("1. Creates branch successfully with valid data")
        void shouldCreateBranchSuccessfully() {
            BranchCreateRequest request = new BranchCreateRequest(
                    "Sucursal Centro", "CENTRO", "Av. Principal 1", "CDMX", "5551234567", "centro@rest.com");

            when(branchRepository.existsByNameAndTenantId("Sucursal Centro", TENANT_ID)).thenReturn(false);
            when(branchRepository.existsByCodeAndTenantId("CENTRO", TENANT_ID)).thenReturn(false);

            Branch saved = buildBranch("Sucursal Centro", "CENTRO");
            when(branchRepository.save(any())).thenReturn(saved);

            BranchResponse response = branchService.createBranch(TENANT_ID, USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Sucursal Centro");
            assertThat(response.code()).isEqualTo("CENTRO");
        }

        @Test
        @DisplayName("2. Throws DuplicateResourceException when name already exists")
        void shouldThrowWhenDuplicateName() {
            BranchCreateRequest request = new BranchCreateRequest(
                    "Sucursal Centro", "CENTRO", null, null, null, null);

            when(branchRepository.existsByNameAndTenantId("Sucursal Centro", TENANT_ID)).thenReturn(true);

            assertThatThrownBy(() -> branchService.createBranch(TENANT_ID, USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(branchRepository, never()).save(any());
        }

        @Test
        @DisplayName("3. Throws DuplicateResourceException when code already exists")
        void shouldThrowWhenDuplicateCode() {
            BranchCreateRequest request = new BranchCreateRequest(
                    "Sucursal Norte", "CENTRO", null, null, null, null);

            when(branchRepository.existsByNameAndTenantId("Sucursal Norte", TENANT_ID)).thenReturn(false);
            when(branchRepository.existsByCodeAndTenantId("CENTRO", TENANT_ID)).thenReturn(true);

            assertThatThrownBy(() -> branchService.createBranch(TENANT_ID, USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(branchRepository, never()).save(any());
        }

        @Test
        @DisplayName("4. Sets tenantId and createdBy on new branch")
        void shouldSetAuditFields() {
            BranchCreateRequest request = new BranchCreateRequest(
                    "Sucursal Sur", "SUR", null, null, null, null);

            when(branchRepository.existsByNameAndTenantId(any(), any())).thenReturn(false);
            when(branchRepository.existsByCodeAndTenantId(any(), any())).thenReturn(false);
            when(branchRepository.save(any())).thenAnswer(inv -> {
                Branch b = inv.getArgument(0);
                b.setId(BRANCH_ID);
                return b;
            });

            branchService.createBranch(TENANT_ID, USER_ID, request);

            ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
            verify(branchRepository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("getBranch")
    class GetBranchTests {

        @Test
        @DisplayName("5. Returns branch when found")
        void shouldReturnBranch() {
            Branch branch = buildBranch("Centro", "CE");
            when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(branch));

            BranchResponse response = branchService.getBranch(TENANT_ID, BRANCH_ID);

            assertThat(response.name()).isEqualTo("Centro");
        }

        @Test
        @DisplayName("6. Throws ResourceNotFoundException when branch not found or cross-tenant")
        void shouldThrowWhenNotFound() {
            when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> branchService.getBranch(TENANT_ID, BRANCH_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateBranch")
    class UpdateBranchTests {

        @Test
        @DisplayName("7. Updates branch fields and sets updatedBy")
        void shouldUpdateBranch() {
            Branch existing = buildBranch("Centro", "CE");
            BranchUpdateRequest request = new BranchUpdateRequest("Centro Actualizado", null, null, null, null, null, null);

            when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(branchRepository.existsByNameAndTenantIdAndIdNot("Centro Actualizado", TENANT_ID, BRANCH_ID)).thenReturn(false);
            when(branchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BranchResponse response = branchService.updateBranch(TENANT_ID, USER_ID, BRANCH_ID, request);

            assertThat(response.name()).isEqualTo("Centro Actualizado");
            assertThat(existing.getUpdatedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("8. Soft deletes branch and sets deletedBy")
        void shouldSoftDeleteBranch() {
            Branch branch = buildBranch("Centro", "CE");
            when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(branch));
            when(branchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            branchService.deleteBranch(TENANT_ID, USER_ID, BRANCH_ID);

            assertThat(branch.isDeleted()).isTrue();
            assertThat(branch.getDeletedBy()).isEqualTo(USER_ID);
            verify(branchRepository).save(branch);
        }
    }

    @Nested
    @DisplayName("listBranches")
    class ListBranchesTests {

        @Test
        @DisplayName("9. Returns all branches for tenant")
        void shouldReturnBranchesForTenant() {
            Branch b1 = buildBranch("Centro", "CE");
            Branch b2 = buildBranch("Norte", "NO");
            when(branchRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(b1, b2));

            List<BranchResponse> result = branchService.listBranches(TENANT_ID);

            assertThat(result).hasSize(2);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Branch buildBranch(String name, String code) {
        Branch branch = new Branch();
        branch.setId(BRANCH_ID);
        branch.setTenantId(TENANT_ID);
        branch.setName(name);
        branch.setCode(code);
        branch.setActive(true);
        return branch;
    }
}
