import request from '@/utils/request'
import type {
  IndividualSignupRequest,
  IndividualSignupVO,
  TeacherInfo,
  ApiResponse
} from '@/types/signup'

export const signupApi = {
  individualSignup: (data: IndividualSignupRequest): Promise<ApiResponse<IndividualSignupVO>> =>
    request({ url: '/v1/signups/individual', method: 'POST', data }),

  getMyIndividualSignups: (params?: {
    page?: number
    size?: number
    status?: string
  }): Promise<ApiResponse<{ list: IndividualSignupVO[]; total: number; page: number; size: number; totalPages: number }>> =>
    request({ url: '/v1/signups/individual/my', method: 'GET', params }),

  getIndividualSignupById: (id: number): Promise<ApiResponse<IndividualSignupVO>> =>
    request({ url: `/v1/signups/individual/${id}`, method: 'GET' }),

  submitIndividualAudit: (id: number): Promise<ApiResponse<void>> =>
    request({ url: `/v1/signups/individual/${id}/submit`, method: 'POST' }),

  getAvailableTeachers: (params?: {
    page?: number
    size?: number
    department?: string
    keyword?: string
  }): Promise<ApiResponse<{ list: TeacherInfo[]; total: number; page: number; size: number; totalPages: number }>> =>
    request({ url: '/v1/users/teachers', method: 'GET', params })
}
