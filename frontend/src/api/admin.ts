import request from '@/utils/request'

export const adminLogin = (data: { username: string; password: string }) =>
  request({ url: '/v1/auth/login', method: 'POST', data })

// 用户管理 — 实际后端路由: GET /api/v1/user/list
export const getUserList = (params?: { page?: number; size?: number; keyword?: string }) =>
  request({ url: '/v1/user/list', method: 'GET', params })

// 新增用户 — 通过注册接口
export const addUser = (data: any) =>
  request({ url: '/v1/auth/register', method: 'POST', data })

// 竞赛管理 — 实际后端路由: GET /api/v1/competitions
export const getCompAllList = (params?: { page?: number; size?: number }) =>
  request({ url: '/v1/competitions', method: 'GET', params: { page: 1, size: 100, ...params } })

// 院系管理 — 后端暂无，返回空
export const getDeptList = () =>
  Promise.resolve({ code: 0, message: 'success', data: [] })

// 数据统计 — 实际后端路由: GET /api/v1/statistics/dashboard
export const getStatData = () =>
  request({ url: '/v1/statistics/dashboard', method: 'GET' })
