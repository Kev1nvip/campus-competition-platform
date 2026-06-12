/**
 * AI推荐模块API接口
 * 提供竞赛推荐相关接口调用
 */
import request from '@/utils/request'
import type { RecommendRequest, RecommendResponse } from '@/types/ai'
import type { Result } from '@/types/competition'

export const aiApi = {
  /**
   * 获取AI推荐竞赛
   * @param params 推荐请求参数
   * @returns 推荐结果列表
   */
  getRecommendations: async (params: RecommendRequest) => {
    const response = await request.post<Result<RecommendResponse>>(
      '/v1/ai/recommend',
      params
    )
    return response as unknown as Result<RecommendResponse>
  },

  /**
   * 获取热门竞赛方向
   * @returns 热门方向列表
   */
  getHotDirections: async () => {
    const response = await request.get<Result<string[]>>(
      '/v1/ai/hot-directions'
    )
    return response as unknown as Result<string[]>
  }
}