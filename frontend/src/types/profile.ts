/**
 * 学生个人中心类型定义
 * 定义个人信息、参赛记录、获奖记录等相关的类型
 */
import type { CompetitionType } from './competition'
import type { AwardStatus, AwardLevel } from './award'
import type { SignupStatus } from './signup'

// 个人信息
export interface StudentProfile {
  userId: number
  username: string
  realName: string
  studentNo?: string
  department?: string
  phone?: string
  email?: string
  avatarUrl?: string
  role: string
}

// 参赛记录视图对象（简化版，使用 IndividualSignupVO）
export interface SignupRecordVO {
  id: number
  competitionId: number
  competitionTitle: string
  competitionType?: CompetitionType
  status: SignupStatus | string
  signupDate: string
  teacherId?: number
  teacherName?: string
  teamId?: number
  teamName?: string
  awardStatus?: AwardStatus
  awardLevel?: AwardLevel
  awardName?: string
}

// 获奖统计
export interface AwardStatistics {
  total: number
  national: number
  provincial: number
  byLevel: {
    level: AwardLevel
    count: number
  }[]
}

// 上传证书请求参数
export interface CertificateUploadRequest {
  file: File
  awardId?: number
}

// 证书上传响应
export interface CertificateUploadResponse {
  url: string
  fileName: string
}
