import request from '@/utils/request'
import type { CompetitionVO, CompetitionDetailVO, PageVO, Result } from '@/types/competition'

export const competitionApi = {
  getList: (params: {
    page: number
    size: number
    status?: string
    type?: string
    keyword?: string
  }): Promise<Result<PageVO<CompetitionVO>>> =>
    request({ url: '/v1/competitions', method: 'GET', params }),

  getById: (id: number): Promise<Result<CompetitionDetailVO>> =>
    request({ url: `/v1/competitions/${id}`, method: 'GET' })
}
