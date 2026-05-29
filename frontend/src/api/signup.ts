/**
 * 报名模块API接口
 * 提供个人赛报名、团队赛报名等接口调用
 */
import request from '@/utils/request'
import type { 
  IndividualSignupRequest, 
  IndividualSignupVO, 
  TeacherInfo,
  ApiResponse 
} from '@/types/signup'

export const signupApi = {
  /**
   * 个人赛报名
   * @param data 报名请求参数
   * @returns 报名结果
   */
  individualSignup: async (data: IndividualSignupRequest) => {
    const response = await request.post<ApiResponse<IndividualSignupVO>>(
      '/v1/signups/individual',
      data
    )
    return response as unknown as ApiResponse<IndividualSignupVO>
  },

  /**
   * 获取我的个人赛报名列表
   * @param params 查询参数
   * @returns 报名列表
   */
  getMyIndividualSignups: async (params?: {
    page?: number
    size?: number
    status?: string
  }) => {
    const response = await request.get<ApiResponse<{
      list: IndividualSignupVO[]
      total: number
      page: number
      size: number
      totalPages: number
    }>>('/v1/signups/individual/my', { params })
    return response as unknown as ApiResponse<{
      list: IndividualSignupVO[]
      total: number
      page: number
      size: number
      totalPages: number
    }>
  },

  /**
   * 获取个人赛报名详情
   * @param id 报名记录ID
   * @returns 报名详情
   */
  getIndividualSignupById: async (id: number) => {
    const response = await request.get<ApiResponse<IndividualSignupVO>>(
      `/v1/signups/individual/${id}`
    )
    return response as unknown as ApiResponse<IndividualSignupVO>
  },

  /**
   * 提交个人赛审核（学生提交报名后触发审核流程）
   * @param id 报名记录ID
   * @returns 操作结果
   */
  submitIndividualAudit: async (id: number) => {
    const response = await request.post<ApiResponse<void>>(
      `/v1/signups/individual/${id}/submit`
    )
    return response as unknown as ApiResponse<void>
  },

  /**
   * 获取可选老师列表（用于选择指导老师）
   * @param params 查询参数
   * @returns 老师列表
   */
  getAvailableTeachers: async (params?: {
    page?: number
    size?: number
    department?: string
    keyword?: string
  }) => {
    const response = await request.get<ApiResponse<{
      list: TeacherInfo[]
      total: number
      page: number
      size: number
      totalPages: number
    }>>('/v1/users/teachers', { params })
    return response as unknown as ApiResponse<{
      list: TeacherInfo[]
      total: number
      page: number
      size: number
      totalPages: number
    }>
  }
}