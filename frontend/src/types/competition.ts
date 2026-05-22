/**
 * 竞赛模块类型定义
 * 定义竞赛列表、详情等相关的请求参数、响应数据和枚举类型
 */

// 竞赛状态枚举
export type CompetitionStatus = 
  | 'UPCOMING'   // 未开始
  | 'SIGNING'    // 报名中
  | 'CLOSED'     // 报名截止
  | 'ONGOING'    // 进行中
  | 'FINISHED'   // 已结束
  | 'OFFLINE'    // 已下架

// 竞赛类型枚举
export type CompetitionType = 
  | 'INDIVIDUAL' // 个人赛
  | 'TEAM'       // 团队赛

// 竞赛视图对象（接口返回）
export interface CompetitionVO {
  id: number
  title: string
  type: CompetitionType
  organizer: string
  status: CompetitionStatus
  signupStart: string
  signupEnd: string
  competitionStart?: string
  competitionEnd?: string
  hasQuota: boolean
  maxQuota?: number
  remainingQuota?: number
  createdBy: number
  createdByName: string
  createdAt: string
}

// 竞赛列表查询参数
export interface CompetitionListRequest {
  page?: number
  size?: number
  status?: CompetitionStatus
  type?: CompetitionType
  keyword?: string
}

// 分页返回
export interface PageVO<T> {
  list: T[]
  total: number
  page: number
  size: number
  totalPages: number
}

// 竞赛详情
export interface CompetitionDetailVO {
  id: number
  title: string
  type: CompetitionType
  organizer: string
  requirement?: string
  signupStart: string
  signupEnd: string
  competitionStart?: string
  competitionEnd?: string
  hasQuota: boolean
  maxQuota?: number
  enrolledCount: number
  remainingQuota?: number
  minTeamSize?: number
  maxTeamSize?: number
  maxTeachQuota?: number
  description?: string
  attachmentUrl?: string
  status: CompetitionStatus
  createdBy: number
  createdByName: string
  createdAt: string
  updatedAt: string
  mySignupStatus?: string
}

// 状态中文映射
export const statusMap: Record<CompetitionStatus, string> = {
  UPCOMING: '未开始',
  SIGNING: '报名中',
  CLOSED: '报名截止',
  ONGOING: '进行中',
  FINISHED: '已结束',
  OFFLINE: '已下架'
}

// 类型中文映射
export const typeMap: Record<CompetitionType, string> = {
  INDIVIDUAL: '个人赛',
  TEAM: '团队赛'
}

// 统一返回体
export interface Result<T> {
  code: number
  message: string
  data: T
}