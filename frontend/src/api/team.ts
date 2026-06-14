import request from '@/utils/request'
import type {
  TeamVO, TeamDetailVO, CreateTeamRequest, UpdateTeamRequest,
  InviteMemberRequest, HandleInviteRequest, ApplyJoinTeamRequest,
  TeamInviteVO, ApiResponse
} from '@/types/team'

export const teamApi = {
  getMyTeams: (params?: { page?: number; size?: number; competitionId?: number }) =>
    request<ApiResponse<{ list: TeamVO[]; total: number; page: number; size: number; totalPages: number }>>({
      url: '/v1/teams/my', method: 'GET', params
    }),

  getTeamDetail: (teamId: number) =>
    request<ApiResponse<TeamDetailVO>>({ url: `/v1/teams/${teamId}`, method: 'GET' }),

  createTeam: (data: CreateTeamRequest) =>
    request<ApiResponse<TeamVO>>({ url: '/v1/teams', method: 'POST', data }),

  updateTeam: (data: UpdateTeamRequest) =>
    request<ApiResponse<TeamVO>>({ url: `/v1/teams/${data.teamId}`, method: 'PUT', data }),

  dismissTeam: (teamId: number) =>
    request<ApiResponse<void>>({ url: `/v1/teams/${teamId}`, method: 'DELETE' }),

  inviteMember: (data: InviteMemberRequest) =>
    request<ApiResponse<TeamInviteVO>>({ url: `/v1/teams/${data.teamId}/invite`, method: 'POST', data: { studentId: data.studentId } }),

  handleInvite: (data: HandleInviteRequest) =>
    request<ApiResponse<void>>({ url: `/v1/teams/invites/${data.applyId}`, method: 'POST', data: { action: data.action } }),

  getMyInvites: (params?: { page?: number; size?: number; status?: string }) =>
    request<ApiResponse<{ list: TeamInviteVO[]; total: number; page: number; size: number; totalPages: number }>>({
      url: '/v1/teams/invites/my', method: 'GET', params
    }),

  applyJoinTeam: (data: ApplyJoinTeamRequest) =>
    request<ApiResponse<void>>({ url: `/v1/teams/${data.teamId}/apply`, method: 'POST', data }),

  handleJoinApply: (applyId: number, action: 'ACCEPT' | 'REJECT') =>
    request<ApiResponse<void>>({ url: `/v1/teams/applies/${applyId}`, method: 'POST', data: { action } }),

  leaveTeam: (teamId: number) =>
    request<ApiResponse<void>>({ url: `/v1/teams/${teamId}/leave`, method: 'POST' }),

  removeMember: (teamId: number, memberId: number) =>
    request<ApiResponse<void>>({ url: `/v1/teams/${teamId}/members/${memberId}`, method: 'DELETE' })
}
