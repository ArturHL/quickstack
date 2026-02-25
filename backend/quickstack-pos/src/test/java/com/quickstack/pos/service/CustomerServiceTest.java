package com.quickstack.pos.service;

import com.quickstack.common.exception.DuplicateResourceException;
import com.quickstack.common.exception.ResourceNotFoundException;
import com.quickstack.pos.dto.request.CustomerCreateRequest;
import com.quickstack.pos.dto.request.CustomerUpdateRequest;
import com.quickstack.pos.dto.response.CustomerResponse;
import com.quickstack.pos.entity.Customer;
import com.quickstack.pos.repository.CustomerRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository);
    }

    @Nested
    @DisplayName("createCustomer")
    class CreateCustomerTests {

        @Test
        @DisplayName("1. Creates customer successfully with phone")
        void shouldCreateCustomerSuccessfully() {
            CustomerCreateRequest request = new CustomerCreateRequest(
                    "Juan Perez", "5551234567", null, null, null, null, null, null, null);

            when(customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNull("5551234567", TENANT_ID))
                    .thenReturn(false);

            Customer saved = buildCustomer("Juan Perez", "5551234567", null);
            when(customerRepository.save(any())).thenReturn(saved);

            CustomerResponse response = customerService.createCustomer(TENANT_ID, USER_ID, request);

            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("Juan Perez");
            assertThat(response.phone()).isEqualTo("5551234567");
        }

        @Test
        @DisplayName("2. Throws DuplicateResourceException when phone already exists")
        void shouldThrowWhenDuplicatePhone() {
            CustomerCreateRequest request = new CustomerCreateRequest(
                    "Ana", "5551111111", null, null, null, null, null, null, null);

            when(customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNull("5551111111", TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> customerService.createCustomer(TENANT_ID, USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("3. Throws DuplicateResourceException when email already exists")
        void shouldThrowWhenDuplicateEmail() {
            CustomerCreateRequest request = new CustomerCreateRequest(
                    "Carlos", null, "carlos@test.com", null, null, null, null, null, null);

            when(customerRepository.existsByEmailAndTenantIdAndDeletedAtIsNull("carlos@test.com", TENANT_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> customerService.createCustomer(TENANT_ID, USER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("4. Creates customer successfully with only whatsapp (no phone or email)")
        void shouldCreateCustomerWithOnlyWhatsapp() {
            CustomerCreateRequest request = new CustomerCreateRequest(
                    "Maria", null, null, "5559876543", null, null, null, null, null);

            // No phone/email means no uniqueness checks run
            Customer saved = buildCustomer("Maria", null, null);
            saved.setWhatsapp("5559876543");
            when(customerRepository.save(any())).thenReturn(saved);

            CustomerResponse response = customerService.createCustomer(TENANT_ID, USER_ID, request);

            assertThat(response).isNotNull();
            // Phone and email checks should NOT have been called
            verify(customerRepository, never()).existsByPhoneAndTenantIdAndDeletedAtIsNull(any(), any());
            verify(customerRepository, never()).existsByEmailAndTenantIdAndDeletedAtIsNull(any(), any());
        }
    }

    @Nested
    @DisplayName("getCustomer")
    class GetCustomerTests {

        @Test
        @DisplayName("5. Returns customer when found")
        void shouldReturnCustomer() {
            Customer customer = buildCustomer("Rosa", "5550000001", null);
            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                    .thenReturn(Optional.of(customer));

            CustomerResponse response = customerService.getCustomer(TENANT_ID, CUSTOMER_ID);

            assertThat(response.name()).isEqualTo("Rosa");
        }

        @Test
        @DisplayName("6. Throws ResourceNotFoundException when customer not found")
        void shouldThrowWhenNotFound() {
            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomer(TENANT_ID, CUSTOMER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("7. Throws ResourceNotFoundException for cross-tenant access (IDOR returns 404)")
        void shouldThrowForCrossTenantAccess() {
            UUID otherTenant = UUID.randomUUID();
            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, otherTenant))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.getCustomer(otherTenant, CUSTOMER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateCustomer")
    class UpdateCustomerTests {

        @Test
        @DisplayName("8. Updates customer fields and sets updatedBy")
        void shouldUpdateCustomer() {
            Customer existing = buildCustomer("Pedro", "5550000002", null);
            CustomerUpdateRequest request = new CustomerUpdateRequest(
                    "Pedro Actualizado", null, null, null, null, null, null, null, null);

            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerResponse response = customerService.updateCustomer(TENANT_ID, USER_ID, CUSTOMER_ID, request);

            assertThat(response.name()).isEqualTo("Pedro Actualizado");
            assertThat(existing.getUpdatedBy()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("9. Throws DuplicateResourceException when updated phone conflicts with another customer")
        void shouldThrowWhenUpdatedPhoneConflicts() {
            Customer existing = buildCustomer("Luis", "5550000003", null);
            CustomerUpdateRequest request = new CustomerUpdateRequest(
                    null, "5550000999", null, null, null, null, null, null, null);

            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNullAndIdNot(
                    "5550000999", TENANT_ID, CUSTOMER_ID)).thenReturn(true);

            assertThatThrownBy(() -> customerService.updateCustomer(TENANT_ID, USER_ID, CUSTOMER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("10. Throws DuplicateResourceException when updated email conflicts with another customer")
        void shouldThrowWhenUpdatedEmailConflicts() {
            Customer existing = buildCustomer("Elena", "5550000004", null);
            CustomerUpdateRequest request = new CustomerUpdateRequest(
                    null, null, "taken@test.com", null, null, null, null, null, null);

            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(customerRepository.existsByEmailAndTenantIdAndDeletedAtIsNullAndIdNot(
                    "taken@test.com", TENANT_ID, CUSTOMER_ID)).thenReturn(true);

            assertThatThrownBy(() -> customerService.updateCustomer(TENANT_ID, USER_ID, CUSTOMER_ID, request))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("11. Can update with own existing phone (no self-conflict)")
        void shouldAllowUpdatingWithOwnPhone() {
            Customer existing = buildCustomer("Jorge", "5550000005", null);
            CustomerUpdateRequest request = new CustomerUpdateRequest(
                    "Jorge Updated", "5550000005", null, null, null, null, null, null, null);

            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(customerRepository.existsByPhoneAndTenantIdAndDeletedAtIsNullAndIdNot(
                    "5550000005", TENANT_ID, CUSTOMER_ID)).thenReturn(false);
            when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CustomerResponse response = customerService.updateCustomer(TENANT_ID, USER_ID, CUSTOMER_ID, request);

            assertThat(response.name()).isEqualTo("Jorge Updated");
        }
    }

    @Nested
    @DisplayName("deleteCustomer")
    class DeleteCustomerTests {

        @Test
        @DisplayName("12. Soft deletes customer and sets deletedBy")
        void shouldSoftDeleteCustomer() {
            Customer customer = buildCustomer("Sandra", "5550000006", null);
            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                    .thenReturn(Optional.of(customer));
            when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            customerService.deleteCustomer(TENANT_ID, USER_ID, CUSTOMER_ID);

            assertThat(customer.isDeleted()).isTrue();
            assertThat(customer.getDeletedBy()).isEqualTo(USER_ID);
            verify(customerRepository).save(customer);
        }

        @Test
        @DisplayName("13. Throws ResourceNotFoundException when customer not found")
        void shouldThrowWhenNotFoundOnDelete() {
            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customerService.deleteCustomer(TENANT_ID, USER_ID, CUSTOMER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listCustomers")
    class ListCustomersTests {

        @Test
        @DisplayName("14. Returns paginated customers when search is null")
        void shouldReturnCustomersWhenSearchIsNull() {
            Customer c1 = buildCustomer("Raul", "5550000010", null);
            Customer c2 = buildCustomer("Carmen", "5550000011", null);
            Page<Customer> page = new PageImpl<>(List.of(c1, c2));
            when(customerRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(page);

            Page<CustomerResponse> result = customerService.listCustomers(TENANT_ID, null, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            verify(customerRepository).findAllByTenantId(eq(TENANT_ID), any(Pageable.class));
            verify(customerRepository, never()).searchCustomers(any(), any(), any());
        }

        @Test
        @DisplayName("15. Uses searchCustomers when search term is provided")
        void shouldUseSearchWhenTermIsProvided() {
            Customer c1 = buildCustomer("Roberto", "5550000012", null);
            Page<Customer> page = new PageImpl<>(List.of(c1));
            when(customerRepository.searchCustomers(eq(TENANT_ID), eq("Roberto"), any(Pageable.class)))
                    .thenReturn(page);

            Page<CustomerResponse> result = customerService.listCustomers(TENANT_ID, "Roberto", PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(customerRepository).searchCustomers(eq(TENANT_ID), eq("Roberto"), any(Pageable.class));
            verify(customerRepository, never()).findAllByTenantId(any(), any());
        }

        @Test
        @DisplayName("16. Uses findAll when search is an empty string")
        void shouldUseFindAllWhenSearchIsEmpty() {
            Page<Customer> page = new PageImpl<>(List.of());
            when(customerRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(page);

            customerService.listCustomers(TENANT_ID, "", PageRequest.of(0, 10));

            verify(customerRepository).findAllByTenantId(eq(TENANT_ID), any(Pageable.class));
            verify(customerRepository, never()).searchCustomers(any(), any(), any());
        }

        @Test
        @DisplayName("17. Uses findAll when search is a blank string (spaces only)")
        void shouldUseFindAllWhenSearchIsBlank() {
            Page<Customer> page = new PageImpl<>(List.of());
            when(customerRepository.findAllByTenantId(eq(TENANT_ID), any(Pageable.class))).thenReturn(page);

            customerService.listCustomers(TENANT_ID, "   ", PageRequest.of(0, 10));

            verify(customerRepository).findAllByTenantId(eq(TENANT_ID), any(Pageable.class));
            verify(customerRepository, never()).searchCustomers(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("incrementOrderStats")
    class IncrementOrderStatsTests {

        @Test
        @DisplayName("18. Updates totalOrders, totalSpent, and lastOrderAt correctly")
        void shouldUpdateOrderStatsCorrectly() {
            Customer customer = buildCustomer("Beatriz", "5550000020", null);
            when(customerRepository.findByIdAndTenantId(CUSTOMER_ID, TENANT_ID))
                    .thenReturn(Optional.of(customer));
            when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            customerService.incrementOrderStats(TENANT_ID, CUSTOMER_ID, new BigDecimal("250.00"));

            ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
            verify(customerRepository).save(captor.capture());
            Customer saved = captor.getValue();
            assertThat(saved.getTotalOrders()).isEqualTo(1);
            assertThat(saved.getTotalSpent()).isEqualByComparingTo(new BigDecimal("250.00"));
            assertThat(saved.getLastOrderAt()).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Customer buildCustomer(String name, String phone, String email) {
        Customer customer = new Customer();
        customer.setId(CUSTOMER_ID);
        customer.setTenantId(TENANT_ID);
        customer.setName(name);
        customer.setPhone(phone);
        customer.setEmail(email);
        return customer;
    }
}
