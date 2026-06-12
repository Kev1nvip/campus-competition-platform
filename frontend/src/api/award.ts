/**
 * 获奖模块 API 接口
 * 提供获奖记录查询、提交、证书上传等接口调用
 */
import request from '@/utils/request'
import type { AwardRecordVO, AwardSubmitRequest } from '@/types/award'
import type { ApiResponse, PageVO } from '@/types/auth'

export const awardApi = {
  /**
   * 获取我的获奖记录列表
   * @param params 查询参数（分页、状态等）
   * @returns 获奖记录列表
   */
  getMyAwards: async (params?: {
    page?: number
    size?: number
    status?: string
  }) => {
    const response = await request.get<ApiResponse<PageVO<AwardRecordVO>>>(
      '/v1/awards/my',
      { params }
    )
    return response as unknown as ApiResponse<PageVO<AwardRecordVO>>
  },

  /**
   * 获取获奖记录详情
   * @param id 获奖记录 ID
   * @returns 获奖记录详情
   */
  getAwardById: async (id: number) => {
    const response = await request.get<ApiResponse<AwardRecordVO>>(
      `/v1/awards/${id}`
    )
    return response as unknown as ApiResponse<AwardRecordVO>
  },

  /**
   * 提交获奖记录
   * @param data 获奖记录信息
   * @returns 提交结果
   */
  submitAward: async (data: AwardSubmitRequest) => {
    const response = await request.post<ApiResponse<AwardRecordVO>>(
      '/v1/awards',
      data
    )
    return response as unknown as ApiResponse<AwardRecordVO>
  },

  /**
   * 上传证书图片
   * @param file 证书图片文件
   * @returns 上传后的 URL
   */
  uploadCertificate: async (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await request.post<ApiResponse<{ url: string }>>(
      '/v1/awards/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      }
    )
    return response as unknown as ApiResponse<{ url: string }>
  }
}
