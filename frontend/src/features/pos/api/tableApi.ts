import axiosInstance from '../../../utils/axiosInstance'
import type { AreaResponse, TableResponse } from '../types/Table'

export const tableApi = {
  getAreasByBranch: (branchId: string): Promise<AreaResponse[]> =>
    axiosInstance
      .get<{ data: AreaResponse[] }>(`/api/v1/branches/${branchId}/areas`)
      .then((r) => r.data.data),

  getTablesByArea: (areaId: string): Promise<TableResponse[]> =>
    axiosInstance
      .get<{ data: TableResponse[] }>(`/api/v1/areas/${areaId}/tables`)
      .then((r) => r.data.data),
}
