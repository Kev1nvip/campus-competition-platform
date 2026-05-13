/**
 * 竞赛模块API接口
 * 提供竞赛列表查询、详情查询等接口调用
 */
import request from '@/utils/request'
import type { CompetitionVO, CompetitionDetailVO, PageVO, Result } from '@/types/competition'

export const competitionApi = {
  // 获取竞赛列表
  getList: async (params: {
    page: number
    size: number
    status?: string
    type?: string
    keyword?: string
  }) => {
    const response = await request.get<Result<PageVO<CompetitionVO>>>('/v1/competitions', { params })
    return response as unknown as Result<PageVO<CompetitionVO>>
  },

  // 获取竞赛详情
  getById: async (id: number) => {
    const response = await request.get<Result<CompetitionDetailVO>>(`/v1/competitions/${id}`)
    return response as unknown as Result<CompetitionDetailVO>
  }
}