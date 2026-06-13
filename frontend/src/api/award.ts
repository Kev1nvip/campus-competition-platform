import request from '@/utils/request'
import type { AwardRecordVO, AwardSubmitRequest } from '@/types/award'
import type { ApiResponse, PageVO } from '@/types/auth'

export const awardApi = {
  getMyAwards: (params?: { page?: number; size?: number; status?: string }) =>
    request<ApiResponse<PageVO<AwardRecordVO>>>({ url: '/v1/awards/my', method: 'GET', params }),

  getAwardById: (id: number) =>
    request<ApiResponse<AwardRecordVO>>({ url: `/v1/awards/${id}`, method: 'GET' }),

  submitAward: (data: AwardSubmitRequest) =>
    request<ApiResponse<AwardRecordVO>>({ url: '/v1/awards', method: 'POST', data }),

  uploadCertificate: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request<ApiResponse<{ url: string }>>({
      url: '/v1/awards/upload',
      method: 'POST',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
