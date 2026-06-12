/**
 * 用户模块 API 接口
 * 提供用户搜索、信息查询、个人信息更新等接口调用
 */
import request from '@/utils/request'
import type { ApiResponse } from '@/types/auth'
import type { StudentProfile } from '@/types/profile'

// 学生信息（用于搜索）
export interface StudentInfo {
  userId: number
  studentNo: string
  realName: string
  department: string
}

// 更新个人信息请求参数
export interface ProfileUpdateRequest {
  realName?: string
  phone?: string
  email?: string
  department?: string
}

// 修改密码请求参数
export interface PasswordUpdateRequest {
  oldPassword: string
  newPassword: string
}

export const userApi = {
  /**
   * 根据学号或姓名搜索学生
   * @param keyword 学号或姓名关键字
   * @returns 学生列表
   */
  searchStudents: async (keyword: string) => {
    const response = await request.get<ApiResponse<StudentInfo[]>>(
      '/v1/users/search',
      { params: { keyword } }
    )
    return response as unknown as ApiResponse<StudentInfo[]>
  },

  /**
   * 获取当前登录用户信息
   * @returns 用户信息
   */
  getCurrentUser: async () => {
    const response = await request.get<ApiResponse<StudentProfile>>(
      '/v1/user/info'
    )
    return response as unknown as ApiResponse<StudentProfile>
  },

  /**
   * 更新个人信息
   * @param data 更新信息
   * @returns 操作结果
   */
  updateProfile: async (data: ProfileUpdateRequest) => {
    const response = await request.put<ApiResponse<void>>(
      '/v1/user/info',
      data
    )
    return response as unknown as ApiResponse<void>
  },

  /**
   * 修改密码
   * @param data 密码信息
   * @returns 操作结果
   */
  updatePassword: async (data: PasswordUpdateRequest) => {
    const response = await request.put<ApiResponse<void>>(
      '/v1/users/me/password',
      data
    )
    return response as unknown as ApiResponse<void>
  }
}
