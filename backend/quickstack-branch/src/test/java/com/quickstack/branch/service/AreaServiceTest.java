package com.quickstack.branch.service;

import com.quickstack.branch.dto.request.AreaCreateRequest;
import com.quickstack.branch.dto.request.AreaUpdateRequest;
import com.quickstack.branch.dto.response.AreaResponse;
import com.quickstack.branch.entity.Area;
import com.quickstack.branch.entity.Branch;
import com.quickstack.branch.repository.AreaRepository;
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
@DisplayName("AreaService")
class AreaServiceTest {

    @Mock
    private AreaRepository areaRepository;

    @Mock
    private BranchRepository branchRepository;

    private AreaService areaService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID AREA_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        areaService = new AreaService(areaRepository, branchRepository);
    }

    @Nested
    @DisplayName("createArea")
    class CreateAreaTests {

        @Test
        @DisplayName("1. Creates area successfully when branch belongs to tenant")
        void shouldCreateAreaSuccessfully() {
            AreaCreateRequest request = new AreaCreateRequest(BRANCH_ID, "Terraza", "Al aire libre", 1);

            when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(new Branch()));
            when(areaRepository.existsByNameAndBranchIdAndTenantId("Terraza", BRANCH_ID, TENANT_ID)).thenReturn(false);

            Area saved = buildArea("Terraza");
            when(areaRepository.save(any())).thenReturn(saved);

            AreaResponse response = areaService.createArea(TENANT_ID, USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Terraza");
        }

        @Test
        @DisplayName("2. Throws ResourceNotFoundException when branch doesn't belong to tenant")
        void shouldThrowWhenBranchNotInTenant() {
            AreaCreateRequest request = new AreaCreateRequest(BRANCH_ID, "Terraza", null, null);

            when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> areaService.createArea(TENANT_ID, USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(areaRepository, never()).save(any());
        }

        @Test
        @DisplayName("3. Throws DuplicateResourceException when name exists in branch")
        void shouldThrowWhenDuplicateName() {
            AreaCreateRequest request = new AreaCreateRequest(BRANCH_ID, "Terraza", null, null);

            when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(new Branch()));
            when(areaRepository.existsByNameAndBranchIdAndTenantId("Terraza", BRANCH_ID, TENANT_ID)).thenReturn(true);

            assertThatThrownBy(() -> areaService.createArea(TENANT_ID, USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(areaRepository, never()).save(any());
        }

        @Test
        @DisplayName("4. Sets tenantId, branchId, and createdBy on new area")
        void shouldSetAuditFields() {
            AreaCreateRequest request = new AreaCreateRequest(BRANCH_ID, "Interior", null, null);

            when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(new Branch()));
            when(areaRepository.existsByNameAndBranchIdAndTenantId(any(), any(), any())).thenReturn(false);
            when(areaRepository.save(any())).thenAnswer(inv -> {
                Area a = inv.getArgument(0);
                a.setId(AREA_ID);
                return a;
            });

            areaService.createArea(TENANT_ID, USER_ID, request);

            ArgumentCaptor<Area> captor = ArgumentCaptor.forClass(Area.class);
            verify(areaRepository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(captor.getValue().getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("getArea")
    class GetAreaTests {

        @Test
        @DisplayName("5. Returns area when found")
        void shouldReturnArea() {
            Area area = buildArea("Terraza");
            when(areaRepository.findByIdAndTenantId(AREA_ID, TENANT_ID)).thenReturn(Optional.of(area));

            AreaResponse response = areaService.getArea(TENANT_ID, AREA_ID);

            assertThat(response.name()).isEqualTo("Terraza");
        }

        @Test
        @DisplayName("6. Throws ResourceNotFoundException when area not found or cross-tenant")
        void shouldThrowWhenNotFound() {
            when(areaRepository.findByIdAndTenantId(AREA_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> areaService.getArea(TENANT_ID, AREA_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateArea")
    class UpdateAreaTests {

        @Test
        @DisplayName("7. Updates area name and sets updatedBy")
        void shouldUpdateArea() {
            Area existing = buildArea("Terraza");
            AreaUpdateRequest request = new AreaUpdateRequest("Terraza Actualizada", null, null, null);

            when(areaRepository.findByIdAndTenantId(AREA_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(areaRepository.existsByNameAndBranchIdAndTenantIdAndIdNot(
                    "Terraza Actualizada", BRANCH_ID, TENANT_ID, AREA_ID)).thenReturn(false);
            when(areaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AreaResponse response = areaService.updateArea(TENANT_ID, USER_ID, AREA_ID, request);

            assertThat(response.name()).isEqualTo("Terraza Actualizada");
            assertThat(existing.getUpdatedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("8. Soft deletes area and sets deletedBy")
        void shouldSoftDeleteArea() {
            Area area = buildArea("Terraza");
            when(areaRepository.findByIdAndTenantId(AREA_ID, TENANT_ID)).thenReturn(Optional.of(area));
            when(areaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            areaService.deleteArea(TENANT_ID, USER_ID, AREA_ID);

            assertThat(area.isDeleted()).isTrue();
            assertThat(area.getDeletedBy()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("listAreasByBranch")
    class ListAreasTests {

        @Test
        @DisplayName("9. Returns areas for branch when branch belongs to tenant")
        void shouldReturnAreasForBranch() {
            when(branchRepository.findByIdAndTenantId(BRANCH_ID, TENANT_ID)).thenReturn(Optional.of(new Branch()));
            when(areaRepository.findAllByBranchIdAndTenantId(BRANCH_ID, TENANT_ID))
                    .thenReturn(List.of(buildArea("Terraza"), buildArea("Interior")));

            List<AreaResponse> result = areaService.listAreasByBranch(TENANT_ID, BRANCH_ID);

            assertThat(result).hasSize(2);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Area buildArea(String name) {
        Area area = new Area();
        area.setId(AREA_ID);
        area.setTenantId(TENANT_ID);
        area.setBranchId(BRANCH_ID);
        area.setName(name);
        area.setSortOrder(0);
        area.setActive(true);
        return area;
    }
}
