import axiosInstance from '../../../utils/axiosInstance'
import type { CustomerPage, CustomerCreateRequest, CustomerResponse } from '../types/Customer'

export const customerApi = {
  searchCustomers: (search: string): Promise<CustomerPage> =>
    axiosInstance
      .get<{ data: CustomerPage }>('/api/v1/customers', { params: { search, size: 20 } })
      .then((r) => r.data.data),

  createCustomer: (request: CustomerCreateRequest): Promise<CustomerResponse> =>
    axiosInstance
      .post<{ data: CustomerResponse }>('/api/v1/customers', request)
      .then((r) => r.data.data),
}
