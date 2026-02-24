package com.quickstack.product.service;

import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.ComboCreateRequest;
import com.quickstack.product.dto.request.ComboItemRequest;
import com.quickstack.product.dto.request.ComboUpdateRequest;
import com.quickstack.product.dto.response.ComboResponse;
import com.quickstack.product.entity.Combo;
import com.quickstack.product.entity.ComboItem;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.repository.ComboItemRepository;
import com.quickstack.product.repository.ComboRepository;
import com.quickstack.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComboService")
class ComboServiceTest {

    @Mock private ComboRepository comboRepository;
    @Mock private ComboItemRepository comboItemRepository;
    @Mock private ProductRepository productRepository;

    private ComboService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMBO_ID = UUID.randomUUID();
    private static final UUID PRODUCT_A_ID = UUID.randomUUID();
    private static final UUID PRODUCT_B_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ComboService(comboRepository, comboItemRepository, productRepository);
    }

    // -------------------------------------------------------------------------
    // createCombo
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createCombo")
    class CreateTests {

        @Test
        @DisplayName("Should create combo when valid")
        void shouldCreateWhenValid() {
            when(comboRepository.existsByNameAndTenantId("Combo 1", TENANT_ID)).thenReturn(false);
            when(productRepository.findByIdInAndTenantId(anyCollection(), eq(TENANT_ID)))
                    .thenReturn(List.of(buildProduct(PRODUCT_A_ID), buildProduct(PRODUCT_B_ID)));
            when(comboRepository.save(any())).thenAnswer(inv -> {
                Combo c = inv.getArgument(0);
                c.setId(COMBO_ID);
                return c;
            });
            when(comboItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            ComboCreateRequest request = new ComboCreateRequest(
                    "Combo 1", null, null, new BigDecimal("99.00"),
                    List.of(
                            new ComboItemRequest(PRODUCT_A_ID, 1, null, null, null),
                            new ComboItemRequest(PRODUCT_B_ID, 1, null, null, null)
                    ), null);

            ComboResponse response = service.createCombo(TENANT_ID, USER_ID, request);

            assertThat(response.name()).isEqualTo("Combo 1");
            assertThat(response.items()).hasSize(2);
            verify(comboRepository).save(any(Combo.class));
            verify(comboItemRepository).saveAll(any());
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when name already exists")
        void shouldThrowWhenNameDuplicate() {
            when(comboRepository.existsByNameAndTenantId("Combo 1", TENANT_ID)).thenReturn(true);

            ComboCreateRequest request = new ComboCreateRequest(
                    "Combo 1", null, null, new BigDecimal("99.00"),
                    List.of(
                            new ComboItemRequest(PRODUCT_A_ID, 1, null, null, null),
                            new ComboItemRequest(PRODUCT_B_ID, 1, null, null, null)
                    ), null);

            assertThatThrownBy(() -> service.createCombo(TENANT_ID, USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when a product does not exist")
        void shouldThrowWhenProductNotFound() {
            when(comboRepository.existsByNameAndTenantId(any(), any())).thenReturn(false);
            when(productRepository.findByIdInAndTenantId(anyCollection(), eq(TENANT_ID)))
                    .thenReturn(List.of(buildProduct(PRODUCT_A_ID))); // Only 1 of 2 found

            ComboCreateRequest request = new ComboCreateRequest(
                    "Combo 1", null, null, new BigDecimal("99.00"),
                    List.of(
                            new ComboItemRequest(PRODUCT_A_ID, 1, null, null, null),
                            new ComboItemRequest(PRODUCT_B_ID, 1, null, null, null)
                    ), null);

            assertThatThrownBy(() -> service.createCombo(TENANT_ID, USER_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // updateCombo
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateCombo")
    class UpdateTests {

        @Test
        @DisplayName("Should update name when valid")
        void shouldUpdateNameWhenValid() {
            Combo combo = buildCombo("Combo Viejo");
            when(comboRepository.findByIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(Optional.of(combo));
            when(comboRepository.existsByNameAndTenantIdAndIdNot("Combo Nuevo", TENANT_ID, COMBO_ID)).thenReturn(false);
            when(comboRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(comboItemRepository.findAllByComboIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(List.of());

            ComboUpdateRequest request = new ComboUpdateRequest("Combo Nuevo", null, null, null, null, null, null);

            ComboResponse response = service.updateCombo(TENANT_ID, USER_ID, COMBO_ID, request);

            assertThat(response.name()).isEqualTo("Combo Nuevo");
        }

        @Test
        @DisplayName("Should replace items when items are provided in update")
        void shouldReplaceItemsWhenProvided() {
            Combo combo = buildCombo("Combo 1");
            when(comboRepository.findByIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(Optional.of(combo));
            when(comboRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(productRepository.findByIdInAndTenantId(anyCollection(), eq(TENANT_ID)))
                    .thenReturn(List.of(buildProduct(PRODUCT_A_ID), buildProduct(PRODUCT_B_ID)));
            when(comboItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<ComboItemRequest> newItems = List.of(
                    new ComboItemRequest(PRODUCT_A_ID, 1, null, null, null),
                    new ComboItemRequest(PRODUCT_B_ID, 2, null, null, null)
            );
            ComboUpdateRequest request = new ComboUpdateRequest(null, null, null, null, null, newItems, null);

            ComboResponse response = service.updateCombo(TENANT_ID, USER_ID, COMBO_ID, request);

            verify(comboItemRepository).deleteAllByComboIdAndTenantId(COMBO_ID, TENANT_ID);
            verify(comboItemRepository).saveAll(any());
            assertThat(response.items()).hasSize(2);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when combo not found")
        void shouldThrowWhenNotFound() {
            when(comboRepository.findByIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateCombo(TENANT_ID, USER_ID, COMBO_ID,
                    new ComboUpdateRequest(null, null, null, null, null, null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when new name conflicts")
        void shouldThrowWhenNewNameConflicts() {
            Combo combo = buildCombo("Combo Viejo");
            when(comboRepository.findByIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(Optional.of(combo));
            when(comboRepository.existsByNameAndTenantIdAndIdNot("Combo Nuevo", TENANT_ID, COMBO_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.updateCombo(TENANT_ID, USER_ID, COMBO_ID,
                    new ComboUpdateRequest("Combo Nuevo", null, null, null, null, null, null)))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    // -------------------------------------------------------------------------
    // deleteCombo
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteCombo")
    class DeleteTests {

        @Test
        @DisplayName("Should soft-delete combo")
        void shouldSoftDeleteCombo() {
            Combo combo = buildCombo("Combo 1");
            when(comboRepository.findByIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(Optional.of(combo));

            service.deleteCombo(TENANT_ID, USER_ID, COMBO_ID);

            assertThat(combo.isDeleted()).isTrue();
            assertThat(combo.getDeletedAt()).isNotNull();
            assertThat(combo.getDeletedBy()).isEqualTo(USER_ID);
            verify(comboRepository).save(combo);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when combo not found")
        void shouldThrowWhenNotFound() {
            when(comboRepository.findByIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteCombo(TENANT_ID, USER_ID, COMBO_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getCombo
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getCombo")
    class GetTests {

        @Test
        @DisplayName("Should return combo with items")
        void shouldReturnComboWithItems() {
            Combo combo = buildCombo("Combo 1");
            ComboItem item = buildItem(PRODUCT_A_ID);
            when(comboRepository.findByIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(Optional.of(combo));
            when(comboItemRepository.findAllByComboIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(List.of(item));
            when(productRepository.findByIdInAndTenantId(anyCollection(), eq(TENANT_ID)))
                    .thenReturn(List.of(buildProduct(PRODUCT_A_ID)));

            ComboResponse response = service.getCombo(TENANT_ID, COMBO_ID);

            assertThat(response.name()).isEqualTo("Combo 1");
            assertThat(response.items()).hasSize(1);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when combo not found")
        void shouldThrowWhenNotFound() {
            when(comboRepository.findByIdAndTenantId(COMBO_ID, TENANT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getCombo(TENANT_ID, COMBO_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // listCombos
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listCombos")
    class ListTests {

        @Test
        @DisplayName("Should return all combos for tenant")
        void shouldReturnAllCombos() {
            Combo c1 = buildCombo("Combo 1");
            c1.setId(UUID.randomUUID());
            Combo c2 = buildCombo("Combo 2");
            c2.setId(UUID.randomUUID());

            when(comboRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(c1, c2));
            when(comboItemRepository.findAllByTenantIdAndComboIdIn(eq(TENANT_ID), anyCollection()))
                    .thenReturn(List.of());

            List<ComboResponse> responses = service.listCombos(TENANT_ID);

            assertThat(responses).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty list when no combos")
        void shouldReturnEmptyListWhenNoCombos() {
            when(comboRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of());

            List<ComboResponse> responses = service.listCombos(TENANT_ID);

            assertThat(responses).isEmpty();
            verifyNoInteractions(comboItemRepository);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Combo buildCombo(String name) {
        Combo combo = new Combo();
        combo.setId(COMBO_ID);
        combo.setTenantId(TENANT_ID);
        combo.setName(name);
        combo.setPrice(new BigDecimal("99.00"));
        combo.setActive(true);
        return combo;
    }

    private Product buildProduct(UUID productId) {
        Product product = new Product();
        product.setId(productId);
        product.setTenantId(TENANT_ID);
        product.setName("Producto " + productId);
        product.setBasePrice(new BigDecimal("50.00"));
        product.setProductType(ProductType.SIMPLE);
        return product;
    }

    private ComboItem buildItem(UUID itemProductId) {
        ComboItem item = new ComboItem();
        item.setId(UUID.randomUUID());
        item.setTenantId(TENANT_ID);
        item.setComboId(COMBO_ID);
        item.setProductId(itemProductId);
        item.setQuantity(1);
        return item;
    }
}
