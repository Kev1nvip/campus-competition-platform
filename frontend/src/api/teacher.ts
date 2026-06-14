import request from '@/utils/request'

export const teacherLogin = (data: { username: string; password: string }) =>
  request({ url: '/v1/auth/login', method: 'POST', data })

export const addCompetition = (data: any) =>
  request({ url: '/v1/teacher/comp/add', method: 'POST', data })

export const getApplyList = () =>
  request({ url: '/v1/teacher/apply/list', method: 'GET' })

export const auditApply = (data: { id: number; status: number }) =>
  request({ url: '/v1/teacher/apply/audit', method: 'POST', data })

export const getTeamList = () =>
  request({ url: '/v1/teacher/team/list', method: 'GET' })

export const addAward = (data: any) =>
  request({ url: '/v1/teacher/award/add', method: 'POST', data })
