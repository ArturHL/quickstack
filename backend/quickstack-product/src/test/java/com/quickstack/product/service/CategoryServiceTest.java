package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.CategoryCreateRequest;
import com.quickstack.product.dto.request.CategoryUpdateRequest;
import com.quickstack.product.dto.response.CategoryResponse;
import com.quickstack.product.entity.Category;
import com.quickstack.product.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
@DisplayName("CategoryService")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    private CategoryService categoryService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository);
    }

    // -------------------------------------------------------------------------
    // createCategory
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createCategory")
    class CreateCategoryTests {

        @Test
        @DisplayName("1. Returns created category when valid - audit log verified")
        void shouldCreateCategorySuccessfully(CapturedOutput output) {
            CategoryCreateRequest request = new CategoryCreateRequest("Bebidas", "Todas", null, null, 1);

            when(categoryRepository.existsByNameAndTenantIdAndParentId("Bebidas", TENANT_ID, null))
                    .thenReturn(false);

            Category saved = new Category();
            saved.setId(CATEGORY_ID);
            saved.setName("Bebidas");
            saved.setTenantId(TENANT_ID);
            saved.setSortOrder(1);
            when(categoryRepository.save(any(Category.class))).thenReturn(saved);
            when(categoryRepository.countActiveProductsByCategory(CATEGORY_ID, TENANT_ID)).thenReturn(0L);

            CategoryResponse response = categoryService.createCategory(TENANT_ID, USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(CATEGORY_ID);
            assertThat(response.sortOrder()).isEqualTo(1);

            assertThat(output.getOut())
                    .contains("[CATALOG] ACTION=CATEGORY_CREATED")
                    .contains(TENANT_ID.toString());
        }

        @Test
        @DisplayName("should set tenantId and createdBy on new category")
        void shouldSetTenantIdAndCreatedBy() {
            CategoryCreateRequest request = new CategoryCreateRequest("Comidas", null, null, null, null);

            when(categoryRepository.existsByNameAndTenantIdAndParentId("Comidas", TENANT_ID, null))
                    .thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(CATEGORY_ID);
                return c;
            });
            when(categoryRepository.countActiveProductsByCategory(any(), any())).thenReturn(0L);

            categoryService.createCategory(TENANT_ID, USER_ID, request);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            Category saved = captor.getValue();

            assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should default sortOrder to 0 when null in request")
        void shouldDefaultSortOrderToZero() {
            CategoryCreateRequest request = new CategoryCreateRequest("Postres", null, null, null, null);

            when(categoryRepository.existsByNameAndTenantIdAndParentId("Postres", TENANT_ID, null))
                    .thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(CATEGORY_ID);
                return c;
            });
            when(categoryRepository.countActiveProductsByCategory(any(), any())).thenReturn(0L);

            categoryService.createCategory(TENANT_ID, USER_ID, request);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getSortOrder()).isEqualTo(0);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when name already exists at same parent level")
        void shouldThrowWhenDuplicateName() {
            CategoryCreateRequest request = new CategoryCreateRequest("Bebidas", null, null, null, null);

            when(categoryRepository.existsByNameAndTenantIdAndParentId("Bebidas", TENANT_ID, null))
                    .thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(TENANT_ID, USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(categoryRepository, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // updateCategory
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategoryTests {

        @Test
        @DisplayName("should update name when provided and unique")
        void shouldUpdateNameWhenUnique() {
            Category existing = buildCategory("Bebidas");
            CategoryUpdateRequest request = new CategoryUpdateRequest(
                    "Drinks", null, null, null, null, null);

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameAndTenantIdAndParentIdAndIdNot(
                    "Drinks", TENANT_ID, null, CATEGORY_ID))
                    .thenReturn(false);
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(categoryRepository.countActiveProductsByCategory(any(), any())).thenReturn(0L);

            CategoryResponse response = categoryService.updateCategory(TENANT_ID, USER_ID, CATEGORY_ID, request);

            assertThat(response.name()).isEqualTo("Drinks");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when category not found")
        void shouldThrowWhenCategoryNotFound() {
            CategoryUpdateRequest request = new CategoryUpdateRequest("NewName", null, null, null, null, null);

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(TENANT_ID, USER_ID, CATEGORY_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when new name conflicts with another category")
        void shouldThrowWhenNameConflicts() {
            Category existing = buildCategory("Bebidas");
            CategoryUpdateRequest request = new CategoryUpdateRequest("Comidas", null, null, null, null, null);

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameAndTenantIdAndParentIdAndIdNot(
                    "Comidas", TENANT_ID, null, CATEGORY_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> categoryService.updateCategory(TENANT_ID, USER_ID, CATEGORY_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("should set updatedBy to userId on update")
        void shouldSetUpdatedBy() {
            Category existing = buildCategory("Bebidas");
            CategoryUpdateRequest request = new CategoryUpdateRequest(
                    "Drinks", null, null, null, null, null);

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.existsByNameAndTenantIdAndParentIdAndIdNot(
                    "Drinks", TENANT_ID, null, CATEGORY_ID))
                    .thenReturn(false);
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(categoryRepository.countActiveProductsByCategory(any(), any())).thenReturn(0L);

            categoryService.updateCategory(TENANT_ID, USER_ID, CATEGORY_ID, request);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getUpdatedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should not update name when request name is null")
        void shouldNotUpdateNullFields() {
            Category existing = buildCategory("Bebidas");
            CategoryUpdateRequest request = new CategoryUpdateRequest(
                    null, null, null, null, 5, null);

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(categoryRepository.countActiveProductsByCategory(any(), any())).thenReturn(0L);

            CategoryResponse response = categoryService.updateCategory(TENANT_ID, USER_ID, CATEGORY_ID, request);

            assertThat(response.name()).isEqualTo("Bebidas");
            assertThat(response.sortOrder()).isEqualTo(5);
        }
    }

    // -------------------------------------------------------------------------
    // deleteCategory
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategoryTests {

        @Test
        @DisplayName("1. Soft deletes category when no active products - audit log verified")
        void shouldSoftDeleteWhenNoProducts(CapturedOutput output) {
            Category category = new Category();
            category.setId(CATEGORY_ID);
            category.setTenantId(TENANT_ID);

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.countActiveProductsByCategory(CATEGORY_ID, TENANT_ID)).thenReturn(0L);
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            categoryService.deleteCategory(TENANT_ID, USER_ID, CATEGORY_ID);

            verify(categoryRepository).save(category);
            assertThat(category.getDeletedAt()).isNotNull();
            assertThat(category.getDeletedBy()).isEqualTo(USER_ID);

            assertThat(output.getOut())
                    .contains("[CATALOG] ACTION=CATEGORY_DELETED")
                    .contains(CATEGORY_ID.toString());
        }

        @Test
        @DisplayName("should throw BusinessRuleException when category has active products")
        void shouldThrowWhenCategoryHasProducts() {
            Category existing = buildCategory("Bebidas");

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.countActiveProductsByCategory(CATEGORY_ID, TENANT_ID))
                    .thenReturn(3L);

            assertThatThrownBy(() -> categoryService.deleteCategory(TENANT_ID, USER_ID, CATEGORY_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("3");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("3. Throws exception when active products exist - warning log verified")
        void throwsExceptionWhenActiveProductsExist(CapturedOutput output) {
            Category category = new Category();
            category.setId(CATEGORY_ID);
            category.setTenantId(TENANT_ID);

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID)).thenReturn(Optional.of(category));
            when(categoryRepository.countActiveProductsByCategory(CATEGORY_ID, TENANT_ID)).thenReturn(5L);

            assertThatThrownBy(() -> categoryService.deleteCategory(TENANT_ID, USER_ID, CATEGORY_ID))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("has 5 active product(s)");

            assertThat(output.getOut())
                    .contains("WARN")
                    .contains("ACTION=CATEGORY_DELETED")
                    .contains("reason=\"Cannot delete category: it has 5 active product(s)\"");
            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when category not found")
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.deleteCategory(TENANT_ID, USER_ID, CATEGORY_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getCategory
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getCategory")
    class GetCategoryTests {

        @Test
        @DisplayName("should return category when found")
        void shouldReturnCategory() {
            Category existing = buildCategory("Bebidas");

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(categoryRepository.countActiveProductsByCategory(CATEGORY_ID, TENANT_ID))
                    .thenReturn(2L);

            CategoryResponse response = categoryService.getCategory(TENANT_ID, CATEGORY_ID);

            assertThat(response.name()).isEqualTo("Bebidas");
            assertThat(response.productCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when category not found or cross-tenant")
        void shouldThrowWhenNotFound() {
            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategory(TENANT_ID, CATEGORY_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // restoreCategory
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("restoreCategory")
    class RestoreCategoryTests {

        @Test
        @DisplayName("should restore deleted category")
        void shouldRestoreDeletedCategory() {
            Category deleted = buildCategory("Bebidas");
            deleted.softDelete(USER_ID);

            when(categoryRepository.findByIdAndTenantIdIncludingDeleted(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.of(deleted));
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(categoryRepository.countActiveProductsByCategory(any(), any())).thenReturn(0L);

            CategoryResponse response = categoryService.restoreCategory(TENANT_ID, USER_ID, CATEGORY_ID);

            assertThat(response.isActive()).isTrue();
            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().isDeleted()).isFalse();
            assertThat(captor.getValue().getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when category not found including deleted")
        void shouldThrowWhenNotFound() {
            when(categoryRepository.findByIdAndTenantIdIncludingDeleted(CATEGORY_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.restoreCategory(TENANT_ID, USER_ID, CATEGORY_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // reorderCategories
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("reorderCategories")
    class ReorderCategoriesTests {

        @Test
        @DisplayName("should do nothing when items list is null or empty")
        void shouldDoNothingWhenEmpty() {
            categoryService.reorderCategories(TENANT_ID, USER_ID, null);
            categoryService.reorderCategories(TENANT_ID, USER_ID, List.of());
            verify(categoryRepository, never()).findByIdInAndTenantId(any(), any());
            verify(categoryRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("should reorder categories within the same tenant")
        void shouldReorderCategories() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            Category c1 = buildCategory("Cat 1");
            c1.setId(id1);
            c1.setSortOrder(1);

            Category c2 = buildCategory("Cat 2");
            c2.setId(id2);
            c2.setSortOrder(2);

            List<com.quickstack.product.dto.request.ReorderItem> items = List.of(
                    new com.quickstack.product.dto.request.ReorderItem(id1, 5),
                    new com.quickstack.product.dto.request.ReorderItem(id2, 3));

            when(categoryRepository.findByIdInAndTenantId(anySet(), eq(TENANT_ID)))
                    .thenReturn(List.of(c1, c2));

            categoryService.reorderCategories(TENANT_ID, USER_ID, items);

            @SuppressWarnings({ "unchecked", "rawtypes" })
            ArgumentCaptor<List<Category>> captor = ArgumentCaptor.forClass((Class) List.class);
            verify(categoryRepository).saveAll(captor.capture());

            List<Category> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved).extracting(Category::getSortOrder).containsExactlyInAnyOrder(5, 3);
            assertThat(c1.getUpdatedBy()).isEqualTo(USER_ID);
            assertThat(c2.getUpdatedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("should throw BusinessRuleException when IDs cross tenants or are missing")
        void shouldThrowWhenCrossTenant() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            Category c1 = buildCategory("Cat 1");
            c1.setId(id1);

            List<com.quickstack.product.dto.request.ReorderItem> items = List.of(
                    new com.quickstack.product.dto.request.ReorderItem(id1, 5),
                    new com.quickstack.product.dto.request.ReorderItem(id2, 3));

            when(categoryRepository.findByIdInAndTenantId(anySet(), eq(TENANT_ID)))
                    .thenReturn(List.of(c1)); // Returned size = 1, requested = 2

            assertThatThrownBy(() -> categoryService.reorderCategories(TENANT_ID, USER_ID, items))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("One or more");

            verify(categoryRepository, never()).saveAll(any());
        }
    }

    // -------------------------------------------------------------------------
    // listCategories
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("listCategories")
    class ListCategoriesTests {

        @Test
        @DisplayName("should return only active categories when includeInactive is false")
        void shouldReturnOnlyActiveCategories() {
            Category active = buildCategory("Bebidas");
            Pageable pageable = PageRequest.of(0, 10);

            when(categoryRepository.findAllByTenantId(TENANT_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(active)));
            when(categoryRepository.countActiveProductsByCategory(any(), any())).thenReturn(0L);

            Page<CategoryResponse> result = categoryService.listCategories(TENANT_ID, false, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(categoryRepository).findAllByTenantId(TENANT_ID, pageable);
            verify(categoryRepository, never()).findAllByTenantIdIncludingInactive(any(), any());
        }

        @Test
        @DisplayName("should include inactive categories when includeInactive is true")
        void shouldIncludeInactiveCategoriesWhenRequested() {
            Category active = buildCategory("Bebidas");
            Category inactive = buildCategory("Comidas");
            inactive.setActive(false);
            Pageable pageable = PageRequest.of(0, 10);

            when(categoryRepository.findAllByTenantIdIncludingInactive(TENANT_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(active, inactive)));
            when(categoryRepository.countActiveProductsByCategory(any(), any())).thenReturn(0L);

            Page<CategoryResponse> result = categoryService.listCategories(TENANT_ID, true, pageable);

            assertThat(result.getContent()).hasSize(2);
            verify(categoryRepository).findAllByTenantIdIncludingInactive(TENANT_ID, pageable);
            verify(categoryRepository, never()).findAllByTenantId(any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Category buildCategory(String name) {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setTenantId(TENANT_ID);
        category.setName(name);
        category.setSortOrder(0);
        category.setActive(true);
        return category;
    }
}
