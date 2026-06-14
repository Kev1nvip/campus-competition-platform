import request from '@/utils/request'
import type { ApiResponse } from '@/types/auth'
import type { StudentProfile } from '@/types/profile'

export interface StudentInfo {
  userId: number
  studentNo: string
  realName: string
  department: string
}

export interface ProfileUpdateRequest {
  realName?: string
  phone?: string
  email?: string
  department?: string
}

export interface PasswordUpdateRequest {
  oldPassword: string
  newPassword: string
}

export const userApi = {
  searchStudents: (keyword: string): Promise<ApiResponse<StudentInfo[]>> =>
    request({ url: '/v1/users/search', method: 'GET', params: { keyword } }),

  getCurrentUser: (): Promise<ApiResponse<StudentProfile>> =>
    request({ url: '/v1/user/info', method: 'GET' }),

  updateProfile: (data: ProfileUpdateRequest): Promise<ApiResponse<void>> =>
    request({ url: '/v1/user/info', method: 'PUT', data }),

  updatePassword: (data: PasswordUpdateRequest): Promise<ApiResponse<void>> =>
    request({ url: '/v1/users/me/password', method: 'PUT', data })
}
