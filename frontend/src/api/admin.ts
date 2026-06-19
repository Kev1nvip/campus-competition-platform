import request from '@/utils/request'

export const adminLogin = (data: { username: string; password: string }) =>
  request({ url: '/v1/auth/login', method: 'POST', data })

// 用户管理 — 实际后端路由: GET /api/v1/user/list
export const getUserList = (params?: { page?: number; size?: number; keyword?: string }) =>
  request({ url: '/v1/user/list', method: 'GET', params })

// 新增用户 — 通过注册接口
export const addUser = (data: any) =>
  request({ url: '/v1/auth/register', method: 'POST', data })

// 禁用/启用用户
export const toggleUserStatus = (id: number, status: 'ACTIVE' | 'DISABLED') =>
  request({ url: `/v1/user/${id}/status`, method: 'PUT', params: { status } })

// 竞赛管理 — 实际后端路由: GET /api/v1/competitions
export const getCompAllList = (params?: { page?: number; size?: number }) =>
  request({ url: '/v1/competitions', method: 'GET', params: { page: 1, size: 100, ...params } })

// 院系管理
export const getDeptList = () =>
  request({ url: '/v1/department/list', method: 'GET' })

export const addDept = (name: string) =>
  request({ url: '/v1/department/add', method: 'POST', data: { name } })

export const renameDept = (oldName: string, newName: string) =>
  request({ url: `/v1/department/rename/${encodeURIComponent(oldName)}`, method: 'PUT', data: { name: newName } })

export const deleteDept = (name: string) =>
  request({ url: `/v1/department/${encodeURIComponent(name)}`, method: 'DELETE' })

// 数据统计 — 实际后端路由: GET /api/v1/statistics/dashboard
export const getStatData = () =>
  request({ url: '/v1/statistics/dashboard', method: 'GET' })
