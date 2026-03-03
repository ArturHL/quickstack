import axiosInstance from '../../../utils/axiosInstance'
import type { CategoryResponse } from '../types/Product'

export interface CategoryCreateRequest {
  name: string
  description?: string
  sortOrder?: number
}

export interface CategoryUpdateRequest extends CategoryCreateRequest {
  isActive?: boolean
}

export interface CategoryPage {
  content: CategoryResponse[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const categoryApi = {
  getCategories: (): Promise<CategoryResponse[]> =>
    axiosInstance
      .get<{ data: CategoryPage }>('/api/v1/categories')
      .then((r) => r.data.data.content),

  createCategory: (body: CategoryCreateRequest): Promise<CategoryResponse> =>
    axiosInstance
      .post<{ data: CategoryResponse }>('/api/v1/categories', body)
      .then((r) => r.data.data),

  updateCategory: (id: string, body: CategoryUpdateRequest): Promise<CategoryResponse> =>
    axiosInstance
      .put<{ data: CategoryResponse }>(`/api/v1/categories/${id}`, body)
      .then((r) => r.data.data),

  deleteCategory: (id: string): Promise<void> =>
    axiosInstance.delete(`/api/v1/categories/${id}`).then(() => undefined),
}
