package com.quickstack.pos.service;

import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.pos.dto.request.CustomerCreateRequest;
import com.quickstack.pos.dto.request.CustomerUpdateRequest;
import com.quickstack.pos.dto.response.CustomerResponse;
import com.quickstack.pos.entity.Customer;
import com.quickstack.pos.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for customer management operations.
 * <p>
 * Enforces business rules:
 * - Phone must be unique within the same tenant (if provided)
 * - Email must be unique within the same tenant (if provided)
 * - All operations are scoped to a tenant (multi-tenancy enforcement)
 * - Cross-tenant access returns 404 to prevent resource enumeration (ASVS V4.1)
 * - At least one contact method must be present at all times
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Creates a new customer for the given tenant.
     *
     * @param tenantId the tenant that owns the customer
     * @param userId   the user creating the customer (for audit)
     * @param request  the creation request with customer details
     * @return the created customer as a response DTO
     * @throws DuplicateResourceException if phone or email already exists for this tenant
     */
    @Transactional
    public CustomerResponse createCustomer(UUID tenantId, UUID userId, CustomerCreateRequest request) {
        validatePhoneUniqueness(request.phone(), tenantId, null);
        validateEmailUniqueness(request.email(), tenantId, null);

        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setName(request.name());
        customer.setPhone(request.phone());
        customer.setEmail(request.email());
        customer.setWhatsapp(request.whatsapp());
        customer.setAddressLine1(request.addressLine1());
        customer.setAddressLine2(request.addressLine2());
        customer.setCity(request.city());
        customer.setPostalCode(request.postalCode());
        customer.setDeliveryNotes(request.deliveryNotes());
        customer.setCreatedBy(userId);

        Customer saved = customerRepository.save(customer);
        log.info("[POS] ACTION=CUSTOMER_CREATED tenantId={} userId={} resourceId={} resourceType=CUSTOMER",
                tenantId, userId, saved.getId());

        return CustomerResponse.from(saved);
    }

    /**
     * Updates an existing customer.
     * <p>
     * Only non-null fields from the request are applied.
     *
     * @param tenantId   the tenant that owns the customer
     * @param userId     the user performing the update (for audit)
     * @param customerId the customer to update
     * @param request    the update request with changed fields
     * @return the updated customer as a response DTO
     * @throws ResourceNotFoundException  if the customer is not found or belongs to another tenant
     * @throws DuplicateResourceException if the new phone or email conflicts with an existing customer
     */
    @Transactional
    public CustomerResponse updateCustomer(UUID tenantId, UUID userId, UUID customerId, CustomerUpdateRequest request) {
        Customer customer = findActiveByIdAndTenant(customerId, tenantId);

        if (request.phone() != null) {
            validatePhoneUniqueness(request.phone(), tenantId, customerId);
            customer.setPhone(request.phone());
        }
        if (request.email() != null) {
            validateEmailUniqueness(request.email(), tenantId, customerId);
            customer.setEmail(request.email());
        }
        if (request.name() != null) {
            customer.setName(request.name());
        }
        if (request.whatsapp() != null) {
            customer.setWhatsapp(request.whatsapp());
        }
        if (request.addressLine1() != null) {
            customer.setAddressLine1(request.addressLine1());
        }
        if (request.addressLine2() != null) {
            customer.setAddressLine2(request.addressLine2());
        }
        if (request.city() != null) {
            customer.setCity(request.city());
        }
        if (request.postalCode() != null) {
            customer.setPostalCode(request.postalCode());
        }
        if (request.deliveryNotes() != null) {
            customer.setDeliveryNotes(request.deliveryNotes());
        }
        customer.setUpdatedBy(userId);

        Customer saved = customerRepository.save(customer);
        log.info("[POS] ACTION=CUSTOMER_UPDATED tenantId={} userId={} resourceId={} resourceType=CUSTOMER",
                tenantId, userId, saved.getId());

        return CustomerResponse.from(saved);
    }

    /**
     * Soft-deletes a customer.
     *
     * @param tenantId   the tenant that owns the customer
     * @param userId     the user performing the deletion (for audit)
     * @param customerId the customer to delete
     * @throws ResourceNotFoundException if the customer is not found or belongs to another tenant
     */
    @Transactional
    public void deleteCustomer(UUID tenantId, UUID userId, UUID customerId) {
        Customer customer = findActiveByIdAndTenant(customerId, tenantId);
        customer.softDelete(userId);
        customerRepository.save(customer);
        log.info("[POS] ACTION=CUSTOMER_DELETED tenantId={} userId={} resourceId={} resourceType=CUSTOMER",
                tenantId, userId, customerId);
    }

    /**
     * Retrieves a customer by ID within the tenant's scope.
     *
     * @param tenantId   the tenant that owns the customer
     * @param customerId the customer to retrieve
     * @return the customer as a response DTO
     * @throws ResourceNotFoundException if the customer is not found or belongs to another tenant
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID tenantId, UUID customerId) {
        Customer customer = findActiveByIdAndTenant(customerId, tenantId);
        return CustomerResponse.from(customer);
    }

    /**
     * Lists customers for a tenant with optional search filtering.
     * <p>
     * When search is null or blank, returns all non-deleted customers.
     * Otherwise, filters by name (case-insensitive), phone, or email.
     *
     * @param tenantId the tenant whose customers to list
     * @param search   optional search term (null or blank means no filter)
     * @param pageable pagination parameters
     * @return page of customer response DTOs
     */
    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(UUID tenantId, String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return customerRepository.findAllByTenantId(tenantId, pageable)
                    .map(CustomerResponse::from);
        }
        return customerRepository.searchCustomers(tenantId, search, pageable)
                .map(CustomerResponse::from);
    }

    /**
     * Increments order statistics for a customer after an order is placed.
     * Intended to be called from OrderService after an order is confirmed.
     *
     * @param tenantId   the tenant that owns the customer
     * @param customerId the customer to update
     * @param orderTotal the total amount of the completed order
     * @throws ResourceNotFoundException if the customer is not found or belongs to another tenant
     */
    @Transactional
    public void incrementOrderStats(UUID tenantId, UUID customerId, BigDecimal orderTotal) {
        Customer customer = findActiveByIdAndTenant(customerId, tenantId);
        customer.incrementOrderStats(orderTotal);
        customerRepository.save(customer);
        log.debug("[POS] ACTION=CUSTOMER_STATS_UPDATED tenantId={} customerId={} newTotal={}",
                tenantId, customerId, customer.getTotalSpent());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Finds a non-deleted customer by ID within the tenant scope.
     * Returns 404 for both missing and cross-tenant resources (IDOR protection).
     */
    private Customer findActiveByIdAndTenant(UUID customerId, UUID tenantId) {
        return customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    /**
     * Validates that a phone number is unique within the tenant.
     * No-op if phone is null or blank.
     *
     * @param phone     the phone to check
     * @param tenantId  the tenant scope
     * @param excludeId the customer ID to exclude (for updates; null = no exclusion)
     * @throws DuplicateResourceException if another customer with this phone exists
     */
    private void validatePhoneUniqueness(String phone, UUID tenantId, UUID excludeId) {
        if (phone == null || phone.isBlank()) {
            return;
        }
        boolean exists = excludeId != null
                ? customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNullAndIdNot(phone, tenantId, excludeId)
                : customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNull(phone, tenantId);

        if (exists) {
            log.warn("[POS] ACTION=DUPLICATE_PHONE tenantId={} phone=\"{}\"", tenantId, phone);
            throw new DuplicateResourceException("Customer", "phone", phone);
        }
    }

    /**
     * Validates that an email is unique within the tenant.
     * No-op if email is null or blank.
     *
     * @param email     the email to check
     * @param tenantId  the tenant scope
     * @param excludeId the customer ID to exclude (for updates; null = no exclusion)
     * @throws DuplicateResourceException if another customer with this email exists
     */
    private void validateEmailUniqueness(String email, UUID tenantId, UUID excludeId) {
        if (email == null || email.isBlank()) {
            return;
        }
        boolean exists = excludeId != null
                ? customerRepository.existsByEmailAndTenantIdAndDeletedAtIsNullAndIdNot(email, tenantId, excludeId)
                : customerRepository.existsByEmailAndTenantIdAndDeletedAtIsNull(email, tenantId);

        if (exists) {
            log.warn("[POS] ACTION=DUPLICATE_EMAIL tenantId={} email=\"{}\"", tenantId, email);
            throw new DuplicateResourceException("Customer", "email", email);
        }
    }
}
