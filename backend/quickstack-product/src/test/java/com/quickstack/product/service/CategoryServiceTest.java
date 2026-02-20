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

@ExtendWith(MockitoExtension.class)
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
        @DisplayName("should create category and return response with generated id")
        void shouldCreateCategorySuccessfully() {
            CategoryCreateRequest request = new CategoryCreateRequest(
                "Bebidas", "Todas las bebidas", null, null, 1);

            when(categoryRepository.existsByNameAndTenantIdAndParentId("Bebidas", TENANT_ID, null))
                .thenReturn(false);
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(CATEGORY_ID);
                return c;
            });
            when(categoryRepository.countActiveProductsByCategory(any(), eq(TENANT_ID))).thenReturn(0L);

            CategoryResponse response = categoryService.createCategory(TENANT_ID, USER_ID, request);

            assertThat(response.id()).isEqualTo(CATEGORY_ID);
            assertThat(response.name()).isEqualTo("Bebidas");
            assertThat(response.sortOrder()).isEqualTo(1);
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
        @DisplayName("should soft-delete category when no active products")
        void shouldSoftDeleteWhenNoProducts() {
            Category existing = buildCategory("Bebidas");

            when(categoryRepository.findByIdAndTenantId(CATEGORY_ID, TENANT_ID))
                .thenReturn(Optional.of(existing));
            when(categoryRepository.countActiveProductsByCategory(CATEGORY_ID, TENANT_ID))
                .thenReturn(0L);
            when(categoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            categoryService.deleteCategory(TENANT_ID, USER_ID, CATEGORY_ID);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().isDeleted()).isTrue();
            assertThat(captor.getValue().getDeletedBy()).isEqualTo(USER_ID);
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
