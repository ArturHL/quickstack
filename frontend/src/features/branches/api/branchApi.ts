import axiosInstance from '../../../utils/axiosInstance'
import type { BranchResponse, BranchCreateRequest, BranchUpdateRequest, AreaCreateRequest, TableCreateRequest } from '../types/Branch'
import type { AreaResponse, TableResponse } from '../../pos/types/Table'

export const branchApi = {
  getBranches: (): Promise<BranchResponse[]> =>
    axiosInstance
      .get<{ data: BranchResponse[] }>('/api/v1/branches')
      .then((r) => r.data.data),

  createBranch: (body: BranchCreateRequest): Promise<BranchResponse> =>
    axiosInstance
      .post<{ data: BranchResponse }>('/api/v1/branches', body)
      .then((r) => r.data.data),

  updateBranch: (id: string, body: BranchUpdateRequest): Promise<BranchResponse> =>
    axiosInstance
      .put<{ data: BranchResponse }>(`/api/v1/branches/${id}`, body)
      .then((r) => r.data.data),

  deleteBranch: (id: string): Promise<void> =>
    axiosInstance.delete(`/api/v1/branches/${id}`).then(() => undefined),

  getAreasByBranch: (branchId: string): Promise<AreaResponse[]> =>
    axiosInstance
      .get<{ data: AreaResponse[] }>(`/api/v1/branches/${branchId}/areas`)
      .then((r) => r.data.data),

  createArea: (branchId: string, body: AreaCreateRequest): Promise<AreaResponse> =>
    axiosInstance
      .post<{ data: AreaResponse }>(`/api/v1/branches/${branchId}/areas`, body)
      .then((r) => r.data.data),

  updateArea: (areaId: string, body: AreaCreateRequest): Promise<AreaResponse> =>
    axiosInstance
      .put<{ data: AreaResponse }>(`/api/v1/areas/${areaId}`, body)
      .then((r) => r.data.data),

  deleteArea: (areaId: string): Promise<void> =>
    axiosInstance.delete(`/api/v1/areas/${areaId}`).then(() => undefined),

  getTablesByArea: (areaId: string): Promise<TableResponse[]> =>
    axiosInstance
      .get<{ data: TableResponse[] }>(`/api/v1/areas/${areaId}/tables`)
      .then((r) => r.data.data),

  createTable: (areaId: string, body: TableCreateRequest): Promise<TableResponse> =>
    axiosInstance
      .post<{ data: TableResponse }>(`/api/v1/areas/${areaId}/tables`, body)
      .then((r) => r.data.data),

  updateTable: (tableId: string, body: TableCreateRequest): Promise<TableResponse> =>
    axiosInstance
      .put<{ data: TableResponse }>(`/api/v1/tables/${tableId}`, body)
      .then((r) => r.data.data),

  deleteTable: (tableId: string): Promise<void> =>
    axiosInstance.delete(`/api/v1/tables/${tableId}`).then(() => undefined),
}
