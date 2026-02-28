import axiosInstance from '../../../utils/axiosInstance'
import type {
  ProductResponse,
  ProductPage,
  ProductCreateRequest,
  ProductUpdateRequest,
  CategoryResponse,
} from '../types/Product'

export interface ProductListParams {
  page?: number
  size?: number
  search?: string
  categoryId?: string
}

export const productApi = {
  getProducts: (params: ProductListParams = {}): Promise<ProductPage> =>
    axiosInstance
      .get<{ data: ProductPage }>('/api/v1/products', { params })
      .then((r) => r.data.data),

  getProduct: (id: string): Promise<ProductResponse> =>
    axiosInstance
      .get<{ data: ProductResponse }>(`/api/v1/products/${id}`)
      .then((r) => r.data.data),

  createProduct: (body: ProductCreateRequest): Promise<ProductResponse> =>
    axiosInstance
      .post<{ data: ProductResponse }>('/api/v1/products', body)
      .then((r) => r.data.data),

  updateProduct: (id: string, body: ProductUpdateRequest): Promise<ProductResponse> =>
    axiosInstance
      .put<{ data: ProductResponse }>(`/api/v1/products/${id}`, body)
      .then((r) => r.data.data),

  deleteProduct: (id: string): Promise<void> =>
    axiosInstance.delete(`/api/v1/products/${id}`).then(() => undefined),

  getCategories: (): Promise<CategoryResponse[]> =>
    axiosInstance
      .get<{ data: CategoryResponse[] }>('/api/v1/categories')
      .then((r) => r.data.data),
}
