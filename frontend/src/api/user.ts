/**
 * 用户模块API接口
 * 提供用户搜索、信息查询等接口调用
 */
import request from '@/utils/request'
import type { ApiResponse } from '@/types/auth'

// 学生信息
export interface StudentInfo {
  userId: number
  studentNo: string
  realName: string
  department: string
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
    const response = await request.get<ApiResponse<{ userId: number; realName: string; role: string }>>(
      '/v1/users/me'
    )
    return response as unknown as ApiResponse<{ userId: number; realName: string; role: string }>
  }
}
