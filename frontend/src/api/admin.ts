import axios from 'axios'
const service = axios.create({
  baseURL: '/api',
  timeout: 6000
})

// 管理员登录
export const adminLogin = (data: { account: string; pwd: string }) => {
  return service.post('/admin/login', data)
}

// 用户管理-列表
export const getUserList = () => service.get('/admin/user/list')
// 新增用户
export const addUser = (data: any) => service.post('/admin/user/add', data)

// 竞赛管理列表
export const getCompAllList = () => service.get('/admin/comp/list')

// 院系、专业接口
export const getDeptList = () => service.get('/admin/dept/list')

// 统计数据
export const getStatData = () => service.get('/admin/stat')