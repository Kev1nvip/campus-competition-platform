/**
 * 报名模块类型定义
 * 定义个人赛报名、团队赛报名相关的请求参数、响应数据和枚举类型
 */

// 报名状态枚举
export type SignupStatus =
  | 'SIGNED'    // 已报名，待审核
  | 'APPROVED'  // 审核通过
  | 'REJECTED'  // 审核驳回
  | 'CANCELLED' // 已取消

// 个人赛报名请求参数
export interface IndividualSignupRequest {
  competitionId: number
  teacherId?: number  // 选择的指导老师ID
  phone?: string      // 联系电话
  email?: string      // 邮箱
  remark?: string     // 备注信息
}

// 个人赛报名详情
export interface IndividualSignupVO {
  id: number
  competitionId: number
  competitionTitle: string
  studentId: number
  studentName: string
  studentNo: string
  department: string
  teacherId?: number
  teacherName?: string
  phone?: string
  email?: string
  remark?: string
  status: SignupStatus
  auditRemark?: string
  createdAt: string
  updatedAt: string
}

// 老师信息（用于选择老师）
export interface TeacherInfo {
  id: number        // 后端返回字段名是 id
  realName: string
  title: string
  department: string
  email?: string
  avatarUrl?: string
}

// 报名状态中文映射
export const signupStatusMap: Record<SignupStatus, string> = {
  SIGNED: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已取消'
}

// 统一响应格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}