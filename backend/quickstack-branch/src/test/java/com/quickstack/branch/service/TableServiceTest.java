package com.quickstack.branch.service;

import com.quickstack.branch.dto.request.TableCreateRequest;
import com.quickstack.branch.dto.request.TableUpdateRequest;
import com.quickstack.branch.dto.response.TableResponse;
import com.quickstack.branch.entity.Area;
import com.quickstack.branch.entity.RestaurantTable;
import com.quickstack.branch.entity.TableStatus;
import com.quickstack.branch.repository.AreaRepository;
import com.quickstack.branch.repository.TableRepository;
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
@DisplayName("TableService")
class TableServiceTest {

    @Mock
    private TableRepository tableRepository;

    @Mock
    private AreaRepository areaRepository;

    private TableService tableService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID AREA_ID = UUID.randomUUID();
    private static final UUID TABLE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tableService = new TableService(tableRepository, areaRepository);
    }

    @Nested
    @DisplayName("createTable")
    class CreateTableTests {

        @Test
        @DisplayName("1. Creates table successfully when area belongs to tenant")
        void shouldCreateTableSuccessfully() {
            TableCreateRequest request = new TableCreateRequest(AREA_ID, "1", "Mesa 1", 4, 0);

            when(areaRepository.findByIdAndTenantId(AREA_ID, TENANT_ID)).thenReturn(Optional.of(new Area()));
            when(tableRepository.existsByNumberAndAreaIdAndTenantId("1", AREA_ID, TENANT_ID)).thenReturn(false);

            RestaurantTable saved = buildTable("1");
            when(tableRepository.save(any())).thenReturn(saved);

            TableResponse response = tableService.createTable(TENANT_ID, USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.number()).isEqualTo("1");
        }

        @Test
        @DisplayName("2. Throws ResourceNotFoundException when area doesn't belong to tenant")
        void shouldThrowWhenAreaNotInTenant() {
            TableCreateRequest request = new TableCreateRequest(AREA_ID, "1", null, null, null);

            when(areaRepository.findByIdAndTenantId(AREA_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.createTable(TENANT_ID, USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(tableRepository, never()).save(any());
        }

        @Test
        @DisplayName("3. Throws DuplicateResourceException when number exists in area")
        void shouldThrowWhenDuplicateNumber() {
            TableCreateRequest request = new TableCreateRequest(AREA_ID, "1", null, null, null);

            when(areaRepository.findByIdAndTenantId(AREA_ID, TENANT_ID)).thenReturn(Optional.of(new Area()));
            when(tableRepository.existsByNumberAndAreaIdAndTenantId("1", AREA_ID, TENANT_ID)).thenReturn(true);

            assertThatThrownBy(() -> tableService.createTable(TENANT_ID, USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(tableRepository, never()).save(any());
        }

        @Test
        @DisplayName("4. Sets tenantId, areaId, and createdBy on new table")
        void shouldSetAuditFields() {
            TableCreateRequest request = new TableCreateRequest(AREA_ID, "2", null, null, null);

            when(areaRepository.findByIdAndTenantId(AREA_ID, TENANT_ID)).thenReturn(Optional.of(new Area()));
            when(tableRepository.existsByNumberAndAreaIdAndTenantId(any(), any(), any())).thenReturn(false);
            when(tableRepository.save(any())).thenAnswer(inv -> {
                RestaurantTable t = inv.getArgument(0);
                t.setId(TABLE_ID);
                return t;
            });

            tableService.createTable(TENANT_ID, USER_ID, request);

            ArgumentCaptor<RestaurantTable> captor = ArgumentCaptor.forClass(RestaurantTable.class);
            verify(tableRepository).save(captor.capture());
            assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
            assertThat(captor.getValue().getAreaId()).isEqualTo(AREA_ID);
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("getTable")
    class GetTableTests {

        @Test
        @DisplayName("5. Returns table when found")
        void shouldReturnTable() {
            RestaurantTable table = buildTable("1");
            when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID)).thenReturn(Optional.of(table));

            TableResponse response = tableService.getTable(TENANT_ID, TABLE_ID);

            assertThat(response.number()).isEqualTo("1");
        }

        @Test
        @DisplayName("6. Throws ResourceNotFoundException when table not found or cross-tenant")
        void shouldThrowWhenNotFound() {
            when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.getTable(TENANT_ID, TABLE_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateTable")
    class UpdateTableTests {

        @Test
        @DisplayName("7. Updates table number and sets updatedBy")
        void shouldUpdateTable() {
            RestaurantTable existing = buildTable("1");
            TableUpdateRequest request = new TableUpdateRequest("1A", null, null, null, null, null, null);

            when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID)).thenReturn(Optional.of(existing));
            when(tableRepository.existsByNumberAndAreaIdAndTenantIdAndIdNot("1A", AREA_ID, TENANT_ID, TABLE_ID)).thenReturn(false);
            when(tableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TableResponse response = tableService.updateTable(TENANT_ID, USER_ID, TABLE_ID, request);

            assertThat(response.number()).isEqualTo("1A");
            assertThat(existing.getUpdatedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("8. Soft deletes table and sets deletedBy")
        void shouldSoftDeleteTable() {
            RestaurantTable table = buildTable("1");
            when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID)).thenReturn(Optional.of(table));
            when(tableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            tableService.deleteTable(TENANT_ID, USER_ID, TABLE_ID);

            assertThat(table.isDeleted()).isTrue();
            assertThat(table.getDeletedBy()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("updateTableStatus")
    class UpdateTableStatusTests {

        @Test
        @DisplayName("9. Updates table status to OCCUPIED")
        void shouldUpdateTableStatusToOccupied() {
            RestaurantTable table = buildTable("1");
            when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID)).thenReturn(Optional.of(table));
            when(tableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TableResponse response = tableService.updateTableStatus(TENANT_ID, TABLE_ID, TableStatus.OCCUPIED, USER_ID);

            assertThat(response.status()).isEqualTo(TableStatus.OCCUPIED);
            assertThat(table.getUpdatedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("10. Throws ResourceNotFoundException when table not found for status update")
        void shouldThrowWhenTableNotFoundForStatusUpdate() {
            when(tableRepository.findByIdAndTenantId(TABLE_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tableService.updateTableStatus(TENANT_ID, TABLE_ID, TableStatus.OCCUPIED, USER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listTablesByArea")
    class ListTablesTests {

        @Test
        @DisplayName("11. Returns tables for area when area belongs to tenant")
        void shouldReturnTablesForArea() {
            when(areaRepository.findByIdAndTenantId(AREA_ID, TENANT_ID)).thenReturn(Optional.of(new Area()));
            when(tableRepository.findAllByAreaIdAndTenantId(AREA_ID, TENANT_ID))
                    .thenReturn(List.of(buildTable("1"), buildTable("2")));

            List<TableResponse> result = tableService.listTablesByArea(TENANT_ID, AREA_ID);

            assertThat(result).hasSize(2);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private RestaurantTable buildTable(String number) {
        RestaurantTable table = new RestaurantTable();
        table.setId(TABLE_ID);
        table.setTenantId(TENANT_ID);
        table.setAreaId(AREA_ID);
        table.setNumber(number);
        table.setStatus(TableStatus.AVAILABLE);
        table.setSortOrder(0);
        table.setActive(true);
        return table;
    }
}
