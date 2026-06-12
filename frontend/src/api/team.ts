/**
 * 队伍模块API接口
 * 提供队伍创建、管理、队员邀请等接口调用
 */
import request from '@/utils/request'
import type {
  TeamVO,
  TeamDetailVO,
  CreateTeamRequest,
  UpdateTeamRequest,
  InviteMemberRequest,
  HandleInviteRequest,
  ApplyJoinTeamRequest,
  TeamInviteVO,
  ApiResponse
} from '@/types/team'

export const teamApi = {
  /**
   * 获取我所在的队伍列表
   * @param params 查询参数
   * @returns 队伍列表
   */
  getMyTeams: async (params?: {
    page?: number
    size?: number
    competitionId?: number
  }) => {
    const response = await request.get<ApiResponse<{
      list: TeamVO[]
      total: number
      page: number
      size: number
      totalPages: number
    }>>('/v1/teams/my', { params })
    return response as unknown as ApiResponse<{
      list: TeamVO[]
      total: number
      page: number
      size: number
      totalPages: number
    }>
  },

  /**
   * 获取队伍详情
   * @param teamId 队伍ID
   * @returns 队伍详情
   */
  getTeamDetail: async (teamId: number) => {
    const response = await request.get<ApiResponse<TeamDetailVO>>(
      `/v1/teams/${teamId}`
    )
    return response as unknown as ApiResponse<TeamDetailVO>
  },

  /**
   * 创建队伍
   * @param data 创建请求参数
   * @returns 创建的队伍信息
   */
  createTeam: async (data: CreateTeamRequest) => {
    const response = await request.post<ApiResponse<TeamVO>>(
      '/v1/teams',
      data
    )
    return response as unknown as ApiResponse<TeamVO>
  },

  /**
   * 更新队伍信息
   * @param data 更新请求参数
   * @returns 更新后的队伍信息
   */
  updateTeam: async (data: UpdateTeamRequest) => {
    const response = await request.put<ApiResponse<TeamVO>>(
      `/v1/teams/${data.teamId}`,
      data
    )
    return response as unknown as ApiResponse<TeamVO>
  },

  /**
   * 解散队伍
   * @param teamId 队伍ID
   * @returns 操作结果
   */
  dismissTeam: async (teamId: number) => {
    const response = await request.delete<ApiResponse<void>>(
      `/v1/teams/${teamId}`
    )
    return response as unknown as ApiResponse<void>
  },

  /**
   * 邀请队员
   * @param data 邀请请求参数
   * @returns 操作结果
   */
  inviteMember: async (data: InviteMemberRequest) => {
    const response = await request.post<ApiResponse<TeamInviteVO>>(
      `/v1/teams/${data.teamId}/invite`,
      { studentId: data.studentId }
    )
    return response as unknown as ApiResponse<TeamInviteVO>
  },

  /**
   * 处理邀请（接受/拒绝）
   * @param data 处理请求参数
   * @returns 操作结果
   */
  handleInvite: async (data: HandleInviteRequest) => {
    const response = await request.post<ApiResponse<void>>(
      `/v1/teams/invites/${data.applyId}`,
      { action: data.action }
    )
    return response as unknown as ApiResponse<void>
  },

  /**
   * 获取我收到的邀请列表
   * @param params 查询参数
   * @returns 邀请列表
   */
  getMyInvites: async (params?: {
    page?: number
    size?: number
    status?: string
  }) => {
    const response = await request.get<ApiResponse<{
      list: TeamInviteVO[]
      total: number
      page: number
      size: number
      totalPages: number
    }>>('/v1/teams/invites/my', { params })
    return response as unknown as ApiResponse<{
      list: TeamInviteVO[]
      total: number
      page: number
      size: number
      totalPages: number
    }>
  },

  /**
   * 申请加入队伍
   * @param data 申请请求参数
   * @returns 操作结果
   */
  applyJoinTeam: async (data: ApplyJoinTeamRequest) => {
    const response = await request.post<ApiResponse<void>>(
      `/v1/teams/${data.teamId}/apply`,
      data
    )
    return response as unknown as ApiResponse<void>
  },

  /**
   * 处理加入申请（队长）
   * @param applyId 申请ID
   * @param action 处理动作
   * @returns 操作结果
   */
  handleJoinApply: async (applyId: number, action: 'ACCEPT' | 'REJECT') => {
    const response = await request.post<ApiResponse<void>>(
      `/v1/teams/applies/${applyId}`,
      { action }
    )
    return response as unknown as ApiResponse<void>
  },

  /**
   * 退出队伍
   * @param teamId 队伍ID
   * @returns 操作结果
   */
  leaveTeam: async (teamId: number) => {
    const response = await request.post<ApiResponse<void>>(
      `/v1/teams/${teamId}/leave`
    )
    return response as unknown as ApiResponse<void>
  },

  /**
   * 移除队员（队长）
   * @param teamId 队伍ID
   * @param memberId 队员ID
   * @returns 操作结果
   */
  removeMember: async (teamId: number, memberId: number) => {
    const response = await request.delete<ApiResponse<void>>(
      `/v1/teams/${teamId}/members/${memberId}`
    )
    return response as unknown as ApiResponse<void>
  }
}
