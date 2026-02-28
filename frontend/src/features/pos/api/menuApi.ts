import axiosInstance from '../../../utils/axiosInstance'
import type { MenuResponse } from '../types/Menu'

export const menuApi = {
  getMenu: (): Promise<MenuResponse> =>
    axiosInstance.get<{ data: MenuResponse }>('/api/v1/menu').then((r) => r.data.data),
}
