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
import com.quickstack.product.repository.ComboItemRepository;
import com.quickstack.product.repository.ComboRepository;
import com.quickstack.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for combo management.
 * <p>
 * Business rules:
 * - Combo name must be unique within a tenant
 * - A combo must contain at least 2 items
 * - All products in a combo must belong to the same tenant
 * - Updating items replaces all existing items in a single transaction
 * <p>
 * ASVS Compliance:
 * - V4.1: tenantId always extracted from JWT, never from request body
 * - V4.1: Cross-tenant access returns 404 to prevent resource enumeration (IDOR protection)
 */
@Service
public class ComboService {

    private static final Logger log = LoggerFactory.getLogger(ComboService.class);

    private final ComboRepository comboRepository;
    private final ComboItemRepository comboItemRepository;
    private final ProductRepository productRepository;

    public ComboService(
            ComboRepository comboRepository,
            ComboItemRepository comboItemRepository,
            ProductRepository productRepository) {
        this.comboRepository = comboRepository;
        this.comboItemRepository = comboItemRepository;
        this.productRepository = productRepository;
    }

    /**
     * Creates a new combo with its items.
     *
     * @param tenantId the tenant creating the combo
     * @param userId   the user creating the combo (for audit)
     * @param request  the creation request
     * @return the created combo as a response DTO
     * @throws DuplicateResourceException if a combo with the same name exists in the tenant
     * @throws ResourceNotFoundException  if any product in items doesn't exist or belongs to another tenant
     */
    @Transactional
    public ComboResponse createCombo(UUID tenantId, UUID userId, ComboCreateRequest request) {
        validateNameUniqueness(request.name(), tenantId, null);
        Map<UUID, Product> productMap = validateAndLoadProducts(request.items(), tenantId);

        Combo combo = new Combo();
        combo.setTenantId(tenantId);
        combo.setName(request.name());
        combo.setDescription(request.description());
        combo.setImageUrl(request.imageUrl());
        combo.setPrice(request.price());
        combo.setActive(true);
        combo.setSortOrder(request.effectiveSortOrder());
        combo.setCreatedBy(userId);
        combo.setUpdatedBy(userId);

        Combo saved = comboRepository.save(combo);

        List<ComboItem> items = buildItems(request.items(), saved.getId(), tenantId);
        comboItemRepository.saveAll(items);

        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                CatalogAction.COMBO_CREATED, tenantId, userId, saved.getId(), "COMBO");

        return ComboResponse.from(saved, items, productMap);
    }

    /**
     * Updates an existing combo. If items are provided, they fully replace the existing items.
     *
     * @param tenantId the tenant that owns the combo
     * @param userId   the user updating the combo (for audit)
     * @param comboId  the combo to update
     * @param request  the update request with changed fields
     * @return the updated combo as a response DTO
     * @throws ResourceNotFoundException  if the combo is not found or belongs to another tenant
     * @throws DuplicateResourceException if the new name conflicts with an existing combo
     */
    @Transactional
    public ComboResponse updateCombo(UUID tenantId, UUID userId, UUID comboId, ComboUpdateRequest request) {
        Combo combo = findByIdAndTenant(comboId, tenantId);

        if (request.name() != null) {
            validateNameUniqueness(request.name(), tenantId, comboId);
            combo.setName(request.name());
        }
        if (request.description() != null) {
            combo.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            combo.setImageUrl(request.imageUrl());
        }
        if (request.price() != null) {
            combo.setPrice(request.price());
        }
        if (request.isActive() != null) {
            combo.setActive(request.isActive());
        }
        if (request.sortOrder() != null) {
            combo.setSortOrder(request.sortOrder());
        }
        combo.setUpdatedBy(userId);

        Combo saved = comboRepository.save(combo);

        List<ComboItem> currentItems;
        Map<UUID, Product> productMap;

        if (request.items() != null) {
            Map<UUID, Product> newProductMap = validateAndLoadProducts(request.items(), tenantId);
            comboItemRepository.deleteAllByComboIdAndTenantId(comboId, tenantId);
            List<ComboItem> newItems = buildItems(request.items(), comboId, tenantId);
            comboItemRepository.saveAll(newItems);
            currentItems = newItems;
            productMap = newProductMap;
        } else {
            currentItems = comboItemRepository.findAllByComboIdAndTenantId(comboId, tenantId);
            productMap = loadProductMap(currentItems, tenantId);
        }

        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                CatalogAction.COMBO_UPDATED, tenantId, userId, saved.getId(), "COMBO");

        return ComboResponse.from(saved, currentItems, productMap);
    }

    /**
     * Soft-deletes a combo.
     *
     * @param tenantId the tenant that owns the combo
     * @param userId   the user deleting the combo (for audit)
     * @param comboId  the combo to delete
     * @throws ResourceNotFoundException if not found or belongs to another tenant
     */
    @Transactional
    public void deleteCombo(UUID tenantId, UUID userId, UUID comboId) {
        Combo combo = findByIdAndTenant(comboId, tenantId);

        combo.setDeletedAt(Instant.now());
        combo.setDeletedBy(userId);
        comboRepository.save(combo);

        log.info("[CATALOG] ACTION={} tenantId={} userId={} resourceId={} resourceType={}",
                CatalogAction.COMBO_DELETED, tenantId, userId, comboId, "COMBO");
    }

    /**
     * Retrieves a combo by ID with its items.
     *
     * @param tenantId the tenant that owns the combo
     * @param comboId  the combo to retrieve
     * @return the combo response with items
     * @throws ResourceNotFoundException if not found or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public ComboResponse getCombo(UUID tenantId, UUID comboId) {
        Combo combo = findByIdAndTenant(comboId, tenantId);
        List<ComboItem> items = comboItemRepository.findAllByComboIdAndTenantId(comboId, tenantId);
        Map<UUID, Product> productMap = loadProductMap(items, tenantId);
        return ComboResponse.from(combo, items, productMap);
    }

    /**
     * Lists all combos for a tenant, ordered by sort_order. Uses batch loading to avoid N+1.
     *
     * @param tenantId the tenant whose combos to list
     * @return list of combo responses (each with their items)
     */
    @Transactional(readOnly = true)
    public List<ComboResponse> listCombos(UUID tenantId) {
        List<Combo> combos = comboRepository.findAllByTenantId(tenantId);
        if (combos.isEmpty()) {
            return List.of();
        }

        List<UUID> comboIds = combos.stream().map(Combo::getId).toList();
        List<ComboItem> allItems = comboItemRepository.findAllByTenantIdAndComboIdIn(tenantId, comboIds);

        Map<UUID, Product> productMap = loadProductMap(allItems, tenantId);
        Map<UUID, List<ComboItem>> itemsByCombo = allItems.stream()
                .collect(Collectors.groupingBy(ComboItem::getComboId));

        return combos.stream()
                .map(c -> ComboResponse.from(c, itemsByCombo.getOrDefault(c.getId(), List.of()), productMap))
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Combo findByIdAndTenant(UUID comboId, UUID tenantId) {
        return comboRepository.findByIdAndTenantId(comboId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo", comboId));
    }

    private void validateNameUniqueness(String name, UUID tenantId, UUID excludeId) {
        boolean exists = excludeId != null
                ? comboRepository.existsByNameAndTenantIdAndIdNot(name, tenantId, excludeId)
                : comboRepository.existsByNameAndTenantId(name, tenantId);

        if (exists) {
            throw new DuplicateResourceException("Combo", "name", name);
        }
    }

    private Map<UUID, Product> validateAndLoadProducts(List<ComboItemRequest> itemRequests, UUID tenantId) {
        Set<UUID> productIds = itemRequests.stream()
                .map(ComboItemRequest::productId)
                .collect(Collectors.toSet());

        List<Product> products = productRepository.findByIdInAndTenantId(productIds, tenantId);

        if (products.size() < productIds.size()) {
            Set<UUID> foundIds = products.stream().map(Product::getId).collect(Collectors.toSet());
            UUID missingId = productIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .findFirst()
                    .orElse(null);
            throw new ResourceNotFoundException("Product", missingId);
        }

        return products.stream().collect(Collectors.toMap(Product::getId, p -> p));
    }

    private List<ComboItem> buildItems(List<ComboItemRequest> itemRequests, UUID comboId, UUID tenantId) {
        return itemRequests.stream().map(req -> {
            ComboItem item = new ComboItem();
            item.setTenantId(tenantId);
            item.setComboId(comboId);
            item.setProductId(req.productId());
            item.setQuantity(req.quantity());
            item.setAllowSubstitutes(req.effectiveAllowSubstitutes());
            item.setSubstituteGroup(req.substituteGroup());
            item.setSortOrder(req.effectiveSortOrder());
            return item;
        }).toList();
    }

    private Map<UUID, Product> loadProductMap(Collection<ComboItem> items, UUID tenantId) {
        if (items.isEmpty()) {
            return Map.of();
        }
        Set<UUID> productIds = items.stream()
                .map(ComboItem::getProductId)
                .collect(Collectors.toSet());
        return productRepository.findByIdInAndTenantId(productIds, tenantId)
                .stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
    }
}
