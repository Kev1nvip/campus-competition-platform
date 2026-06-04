/**
 * 队伍模块类型定义
 * 定义队伍、队员相关的请求参数、响应数据和枚举类型
 */

import type { UserInfo } from './auth'

// 队伍状态枚举
export type TeamStatus = 
  | 'FORMING'
  | 'FULL'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'DISMISSED'

// 队员角色枚举
export type TeamMemberRole = 
  | 'LEADER'
  | 'MEMBER'

// 队伍成员信息
export interface TeamMemberVO {
  id: number
  teamId: number
  studentId: number
  studentName: string
  studentNo: string
  department: string
  role: TeamMemberRole
  joinedAt: string
}

// 队伍信息
export interface TeamVO {
  id: number
  competitionId: number
  competitionTitle: string
  teamName: string
  leaderId: number
  leaderName: string
  teacherId?: number
  teacherName?: string
  teacherConfirmed: boolean
  memberCount: number
  minTeamSize?: number
  maxTeamSize?: number
  status: TeamStatus
  createdAt: string
  updatedAt: string
}

// 队伍详情（包含成员列表）
export interface TeamDetailVO extends TeamVO {
  members: TeamMemberVO[]
}

// 创建队伍请求参数
export interface CreateTeamRequest {
  competitionId: number
  teamName: string
}

// 更新队伍请求参数
export interface UpdateTeamRequest {
  teamId: number
  teamName?: string
}

// 邀请队员请求参数
export interface InviteMemberRequest {
  teamId: number
  studentId: number
}

// 处理邀请请求参数
export interface HandleInviteRequest {
  applyId: number
  action: 'ACCEPT' | 'REJECT'
}

// 邀请记录信息
export interface TeamInviteVO {
  id: number
  teamId: number
  teamName: string
  inviterId: number
  inviterName: string
  inviteeId: number
  inviteeName: string
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED'
  createdAt: string
}

// 申请加入队伍请求参数
export interface ApplyJoinTeamRequest {
  teamId: number
  introduction?: string
}

// 队伍状态中文映射
export const teamStatusMap: Record<TeamStatus, string> = {
  FORMING: '组建中',
  FULL: '人数已满',
  SUBMITTED: '已提交审核',
  APPROVED: '审核通过',
  REJECTED: '审核驳回',
  DISMISSED: '已解散'
}

// 队员角色中文映射
export const teamMemberRoleMap: Record<TeamMemberRole, string> = {
  LEADER: '队长',
  MEMBER: '队员'
}

// 统一响应格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}
