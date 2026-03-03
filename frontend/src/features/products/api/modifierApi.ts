import axiosInstance from '../../../utils/axiosInstance'
import type {
  ModifierGroupResponse,
  ModifierGroupCreateRequest,
  ModifierGroupUpdateRequest,
  ModifierResponse,
  ModifierCreateRequest,
  ModifierUpdateRequest,
} from '../types/Product'

export const modifierApi = {
  getModifierGroups: (productId: string): Promise<ModifierGroupResponse[]> =>
    axiosInstance
      .get<{ data: ModifierGroupResponse[] }>(`/api/v1/products/${productId}/modifier-groups`)
      .then((r) => r.data.data),

  createModifierGroup: (productId: string, body: ModifierGroupCreateRequest): Promise<ModifierGroupResponse> =>
    axiosInstance
      .post<{ data: ModifierGroupResponse }>(`/api/v1/products/${productId}/modifier-groups`, { ...body, productId })
      .then((r) => r.data.data),

  updateModifierGroup: (groupId: string, body: ModifierGroupUpdateRequest): Promise<ModifierGroupResponse> =>
    axiosInstance
      .put<{ data: ModifierGroupResponse }>(`/api/v1/modifier-groups/${groupId}`, body)
      .then((r) => r.data.data),

  deleteModifierGroup: (groupId: string): Promise<void> =>
    axiosInstance.delete(`/api/v1/modifier-groups/${groupId}`).then(() => undefined),

  getModifiers: (groupId: string): Promise<ModifierResponse[]> =>
    axiosInstance
      .get<{ data: ModifierResponse[] }>(`/api/v1/modifier-groups/${groupId}/modifiers`)
      .then((r) => r.data.data),

  createModifier: (groupId: string, body: ModifierCreateRequest): Promise<ModifierResponse> =>
    axiosInstance
      .post<{ data: ModifierResponse }>(`/api/v1/modifier-groups/${groupId}/modifiers`, body)
      .then((r) => r.data.data),

  updateModifier: (modifierId: string, body: ModifierUpdateRequest): Promise<ModifierResponse> =>
    axiosInstance
      .put<{ data: ModifierResponse }>(`/api/v1/modifiers/${modifierId}`, body)
      .then((r) => r.data.data),

  deleteModifier: (modifierId: string): Promise<void> =>
    axiosInstance.delete(`/api/v1/modifiers/${modifierId}`).then(() => undefined),
}
