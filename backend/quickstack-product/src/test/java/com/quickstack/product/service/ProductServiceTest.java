package com.quickstack.product.service;

import com.quickstack.common.exception.BusinessRuleException;
import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.product.dto.request.ProductCreateRequest;
import com.quickstack.product.dto.request.ProductUpdateRequest;
import com.quickstack.product.dto.request.VariantCreateRequest;
import com.quickstack.product.dto.response.ProductResponse;
import com.quickstack.product.dto.response.ProductSummaryResponse;
import com.quickstack.product.entity.Category;
import com.quickstack.product.entity.Product;
import com.quickstack.product.entity.ProductType;
import com.quickstack.product.repository.CategoryRepository;
import com.quickstack.product.repository.ProductRepository;
import com.quickstack.product.repository.VariantRepository;
import com.quickstack.product.entity.ProductVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private VariantRepository variantRepository;

    @InjectMocks
    private ProductService productService;

    private UUID tenantId;
    private UUID userId;
    private UUID categoryId;
    private Category category;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        categoryId = UUID.randomUUID();

        category = new Category();
        category.setId(categoryId);
        category.setTenantId(tenantId);
        category.setName("Bebidas");
    }

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create simple product successfully")
        void shouldCreateSimpleProductSuccessfully() {
            // Given
            ProductCreateRequest request = new ProductCreateRequest(
                "Coca Cola", "Refresco", categoryId, "COCA-001",
                new BigDecimal("15.00"), new BigDecimal("8.00"),
                "http://image.com/coca.png", ProductType.SIMPLE, 1, null
            );

            when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
            when(productRepository.existsBySkuAndTenantId(request.sku(), tenantId)).thenReturn(false);
            when(productRepository.existsByNameAndTenantIdAndCategoryId(request.name(), tenantId, categoryId)).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product p = (Product) inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            // When
            ProductResponse response = productService.createProduct(tenantId, userId, request);

            // Then
            assertThat(response.name()).isEqualTo("Coca Cola");
            assertThat(response.productType()).isEqualTo(ProductType.SIMPLE);
            assertThat(response.category().id()).isEqualTo(categoryId);
            verify(productRepository).save(any(Product.class));
        }

        @Test
        @DisplayName("Should create variant product successfully")
        void shouldCreateVariantProductSuccessfully() {
            // Given
            VariantCreateRequest v1 = new VariantCreateRequest("Chico", "COF-S", BigDecimal.ZERO, true, 1);
            VariantCreateRequest v2 = new VariantCreateRequest("Grande", "COF-L", new BigDecimal("10.00"), false, 2);

            ProductCreateRequest request = new ProductCreateRequest(
                "Cafe", "Cafe caliente", categoryId, "COF-001",
                new BigDecimal("30.00"), new BigDecimal("10.00"),
                null, ProductType.VARIANT, 1, List.of(v1, v2)
            );

            when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product p = (Product) inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> {
                ProductVariant v = inv.getArgument(0);
                v.setId(UUID.randomUUID());
                return v;
            });

            // When
            ProductResponse response = productService.createProduct(tenantId, userId, request);

            // Then
            assertThat(response.productType()).isEqualTo(ProductType.VARIANT);
            assertThat(response.variants()).hasSize(2);
            assertThat(response.variants().get(0).name()).isEqualTo("Chico");
            assertThat(response.variants().get(1).effectivePrice()).isEqualByComparingTo("40.00");
        }

        @Test
        @DisplayName("Should throw exception when category not found")
        void shouldThrowExceptionWhenCategoryNotFound() {
            ProductCreateRequest request = new ProductCreateRequest(
                "Coca Cola", null, categoryId, null,
                new BigDecimal("15.00"), null, null, ProductType.SIMPLE, null, null
            );

            when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(tenantId, userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when SKU duplicated")
        void shouldThrowExceptionWhenSkuDuplicated() {
            ProductCreateRequest request = new ProductCreateRequest(
                "Coca Cola", null, categoryId, "DUPLICATED",
                new BigDecimal("15.00"), null, null, ProductType.SIMPLE, null, null
            );

            when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
            when(productRepository.existsBySkuAndTenantId("DUPLICATED", tenantId)).thenReturn(true);

            assertThatThrownBy(() -> productService.createProduct(tenantId, userId, request))
                .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("Should throw exception for VARIANT product without variants")
        void shouldThrowExceptionForVariantProductWithoutVariants() {
            ProductCreateRequest request = new ProductCreateRequest(
                "Cafe", null, categoryId, null,
                new BigDecimal("30.00"), null, null, ProductType.VARIANT, null, List.of()
            );

            when(categoryRepository.findByIdAndTenantId(categoryId, tenantId)).thenReturn(Optional.of(category));
            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product p = (Product) inv.getArgument(0);
                p.setId(UUID.randomUUID());
                return p;
            });

            assertThatThrownBy(() -> productService.createProduct(tenantId, userId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Product of type VARIANT must have at least one variant");
        }
    }

    @Nested
    @DisplayName("Update Product Tests")
    class UpdateProductTests {

        private UUID productId;
        private Product product;

        @BeforeEach
        void setUp() {
            productId = UUID.randomUUID();
            product = new Product();
            product.setId(productId);
            product.setTenantId(tenantId);
            product.setCategoryId(categoryId);
            product.setName("Old Name");
            product.setBasePrice(new BigDecimal("10.00"));
            product.setProductType(ProductType.SIMPLE);
        }

        @Test
        @DisplayName("Should update product successfully")
        void shouldUpdateProductSuccessfully() {
            ProductUpdateRequest request = new ProductUpdateRequest(
                "New Name", "New Desc", null, "NEW-SKU",
                new BigDecimal("12.00"), null, null, null, 2, true, null
            );

            when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
            when(productRepository.existsBySkuAndTenantIdAndIdNot("NEW-SKU", tenantId, productId)).thenReturn(false);
            when(productRepository.existsByNameAndTenantIdAndCategoryIdAndIdNot("New Name", tenantId, categoryId, productId)).thenReturn(false);
            when(productRepository.save(any(Product.class))).thenReturn(product);

            ProductResponse response = productService.updateProduct(tenantId, userId, productId, request);

            assertThat(response.name()).isEqualTo("New Name");
            assertThat(response.basePrice()).isEqualByComparingTo("12.00");
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Should change type to VARIANT and set new variants")
        void shouldChangeTypeToVariantAndSetNewVariants() {
            VariantCreateRequest v1 = new VariantCreateRequest("Normal", "NOR", BigDecimal.ZERO, true, 1);
            ProductUpdateRequest request = new ProductUpdateRequest(
                null, null, null, null, null, null, null,
                ProductType.VARIANT, null, null, List.of(v1)
            );

            when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(inv -> inv.getArgument(0));

            ProductResponse response = productService.updateProduct(tenantId, userId, productId, request);

            assertThat(product.getProductType()).isEqualTo(ProductType.VARIANT);
            assertThat(product.getVariants()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Reorder Product Tests")
    class ReorderProductsTests {

        @Test
        @DisplayName("Should do nothing when items list is null or empty")
        void shouldDoNothingWhenEmpty() {
            productService.reorderProducts(tenantId, userId, null);
            productService.reorderProducts(tenantId, userId, List.of());
            verify(productRepository, never()).findByIdInAndTenantId(any(), any());
            verify(productRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Should reorder products within the same tenant")
        void shouldReorderProducts() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            
            Product p1 = new Product();
            p1.setId(id1);
            p1.setTenantId(tenantId);
            p1.setSortOrder(1);

            Product p2 = new Product();
            p2.setId(id2);
            p2.setTenantId(tenantId);
            p2.setSortOrder(2);

            List<com.quickstack.product.dto.request.ReorderItem> items = List.of(
                new com.quickstack.product.dto.request.ReorderItem(id1, 5),
                new com.quickstack.product.dto.request.ReorderItem(id2, 3)
            );

            when(productRepository.findByIdInAndTenantId(anySet(), eq(tenantId)))
                .thenReturn(List.of(p1, p2));

            productService.reorderProducts(tenantId, userId, items);

            ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass((Class)List.class);
            verify(productRepository).saveAll(captor.capture());
            
            List<Product> saved = captor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved).extracting(Product::getSortOrder).containsExactlyInAnyOrder(5, 3);
            assertThat(p1.getUpdatedBy()).isEqualTo(userId);
            assertThat(p2.getUpdatedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw exception when IDs cross tenants or are missing")
        void shouldThrowWhenCrossTenant() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            
            Product p1 = new Product();
            p1.setId(id1);

            List<com.quickstack.product.dto.request.ReorderItem> items = List.of(
                new com.quickstack.product.dto.request.ReorderItem(id1, 5),
                new com.quickstack.product.dto.request.ReorderItem(id2, 3)
            );

            when(productRepository.findByIdInAndTenantId(anySet(), eq(tenantId)))
                .thenReturn(List.of(p1)); // Returned size = 1, requested = 2

            assertThatThrownBy(() -> productService.reorderProducts(tenantId, userId, items))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("One or more");

            verify(productRepository, never()).saveAll(any());
        }
    }

    @Nested
    @DisplayName("Get and List Product Tests")
    class GetAndListTests {

        @Test
        @DisplayName("Should return product details")
        void shouldReturnProductDetails() {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setTenantId(tenantId);
            product.setCategoryId(categoryId);
            product.setName("Coca Cola");
            product.setBasePrice(new BigDecimal("15.00"));

            when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));
            when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

            ProductResponse response = productService.getProduct(tenantId, productId);

            assertThat(response.name()).isEqualTo("Coca Cola");
            assertThat(response.category().id()).isEqualTo(categoryId);
        }

        @Test
        @DisplayName("Should list products with filters")
        void shouldListProductsWithFilters() {
            Product product = new Product();
            product.setId(UUID.randomUUID());
            product.setName("Coca Cola");
            product.setBasePrice(new BigDecimal("15.00"));
            
            Page<Product> page = new PageImpl<>(List.of(product));
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.findAllByTenantIdWithFilters(eq(tenantId), any(), any(), any(), any(), eq(pageable)))
                .thenReturn(page);

            Page<ProductSummaryResponse> result = productService.listProducts(
                tenantId, categoryId, true, "coca", false, pageable
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Coca Cola");
            verify(productRepository).findAllByTenantIdWithFilters(tenantId, categoryId, true, true, "coca", pageable);
        }
    }

    @Nested
    @DisplayName("Delete and Restore Tests")
    class DeleteAndRestoreTests {

        @Test
        @DisplayName("Should soft delete product")
        void shouldSoftDeleteProduct() {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setTenantId(tenantId);

            when(productRepository.findByIdAndTenantId(productId, tenantId)).thenReturn(Optional.of(product));

            productService.deleteProduct(tenantId, userId, productId);

            assertThat(product.getDeletedAt()).isNotNull();
            assertThat(product.getDeletedBy()).isEqualTo(userId);
            assertThat(product.isActive()).isFalse();
            verify(productRepository).save(product);
        }

        @Test
        @DisplayName("Should restore deleted product")
        void shouldRestoreDeletedProduct() {
            UUID productId = UUID.randomUUID();
            Product product = new Product();
            product.setId(productId);
            product.setTenantId(tenantId);
            product.softDelete(userId);

            when(productRepository.findByIdAndTenantIdIncludingDeleted(productId, tenantId)).thenReturn(Optional.of(product));
            when(productRepository.save(any(Product.class))).thenReturn(product);

            ProductResponse response = productService.restoreProduct(tenantId, userId, productId);

            assertThat(product.getDeletedAt()).isNull();
            assertThat(product.isActive()).isTrue();
            verify(productRepository).save(product);
        }
    }
}
