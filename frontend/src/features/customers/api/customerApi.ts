import axiosInstance from '../../../utils/axiosInstance'
import type { CustomerResponse, CustomerPage } from '../../pos/types/Customer'

export interface CustomerUpdateRequest {
  name?: string
  phone?: string
  email?: string
  whatsapp?: string
  addressLine1?: string
  addressLine2?: string
  city?: string
  postalCode?: string
  deliveryNotes?: string
}

export const customerAdminApi = {
  getCustomers: (params: { search?: string; page?: number; size?: number } = {}): Promise<CustomerPage> =>
    axiosInstance
      .get<{ data: CustomerPage }>('/api/v1/customers', { params })
      .then((r) => r.data.data),

  updateCustomer: (id: string, body: CustomerUpdateRequest): Promise<CustomerResponse> =>
    axiosInstance
      .put<{ data: CustomerResponse }>(`/api/v1/customers/${id}`, body)
      .then((r) => r.data.data),
}
