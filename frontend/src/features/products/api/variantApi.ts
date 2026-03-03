import axiosInstance from '../../../utils/axiosInstance'
import type { VariantResponse, VariantCreateRequest, VariantUpdateRequest } from '../types/Product'

export const variantApi = {
  getVariants: (productId: string): Promise<VariantResponse[]> =>
    axiosInstance
      .get<{ data: VariantResponse[] }>(`/api/v1/products/${productId}/variants`)
      .then((r) => r.data.data),

  createVariant: (productId: string, body: VariantCreateRequest): Promise<VariantResponse> =>
    axiosInstance
      .post<{ data: VariantResponse }>(`/api/v1/products/${productId}/variants`, body)
      .then((r) => r.data.data),

  updateVariant: (productId: string, variantId: string, body: VariantUpdateRequest): Promise<VariantResponse> =>
    axiosInstance
      .put<{ data: VariantResponse }>(`/api/v1/products/${productId}/variants/${variantId}`, body)
      .then((r) => r.data.data),

  deleteVariant: (productId: string, variantId: string): Promise<void> =>
    axiosInstance
      .delete(`/api/v1/products/${productId}/variants/${variantId}`)
      .then(() => undefined),
}
