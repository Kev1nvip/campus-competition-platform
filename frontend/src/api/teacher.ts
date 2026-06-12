import axios from 'axios'

// 创建请求实例
const service = axios.create({
  baseURL: '/api',
  timeout: 6000
})

// 1.教师登录
export const teacherLogin = (data: { tid: string; pwd: string }) => {
  return service.post('/teacher/login', data)
}

// 2.发布竞赛提交
export const addCompetition = (data: any) => {
  return service.post('/teacher/comp/add', data)
}

// 3.获取报名审核列表
export const getApplyList = () => {
  return service.get('/teacher/apply/list')
}

// 4.审核通过/驳回
export const auditApply = (data: { id: number; status: number }) => {
  return service.post('/teacher/apply/audit', data)
}

// 5.队伍列表
export const getTeamList = () => {
  return service.get('/teacher/team/list')
}

// 6.录入获奖提交
export const addAward = (data: any) => {
  return service.post('/teacher/award/add', data)
}