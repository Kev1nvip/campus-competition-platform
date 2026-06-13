import request from '@/utils/request'

export const adminLogin = (data: { username: string; password: string }) =>
  request({ url: '/v1/auth/login', method: 'POST', data })

export const getUserList = () =>
  request({ url: '/v1/admin/user/list', method: 'GET' })

export const addUser = (data: any) =>
  request({ url: '/v1/admin/user/add', method: 'POST', data })

export const getCompAllList = () =>
  request({ url: '/v1/admin/comp/list', method: 'GET' })

export const getDeptList = () =>
  request({ url: '/v1/admin/dept/list', method: 'GET' })

export const getStatData = () =>
  request({ url: '/v1/admin/stat', method: 'GET' })
