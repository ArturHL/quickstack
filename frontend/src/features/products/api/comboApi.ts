import axiosInstance from '../../../utils/axiosInstance'
import type { ComboResponse, ComboPage, ComboCreateRequest, ComboUpdateRequest } from '../types/Product'

export const comboApi = {
  getCombos: (params: { page?: number; size?: number } = {}): Promise<ComboPage> =>
    axiosInstance
      .get<{ data: ComboPage }>('/api/v1/combos', { params })
      .then((r) => r.data.data),

  getCombo: (id: string): Promise<ComboResponse> =>
    axiosInstance
      .get<{ data: ComboResponse }>(`/api/v1/combos/${id}`)
      .then((r) => r.data.data),

  createCombo: (body: ComboCreateRequest): Promise<ComboResponse> =>
    axiosInstance
      .post<{ data: ComboResponse }>('/api/v1/combos', body)
      .then((r) => r.data.data),

  updateCombo: (id: string, body: ComboUpdateRequest): Promise<ComboResponse> =>
    axiosInstance
      .put<{ data: ComboResponse }>(`/api/v1/combos/${id}`, body)
      .then((r) => r.data.data),

  deleteCombo: (id: string): Promise<void> =>
    axiosInstance.delete(`/api/v1/combos/${id}`).then(() => undefined),
}
