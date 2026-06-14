import request from '@/utils/request'
import type { RecommendRequest, RecommendResponse } from '@/types/ai'
import type { Result } from '@/types/competition'

export const aiApi = {
  getRecommendations: (params: RecommendRequest): Promise<Result<RecommendResponse>> =>
    request({ url: '/v1/ai/recommend', method: 'POST', data: params }),

  getHotDirections: (): Promise<Result<string[]>> =>
    request({ url: '/v1/ai/hot-directions', method: 'GET' })
}
