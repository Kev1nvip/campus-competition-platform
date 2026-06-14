/**
 * 认证模块类型定义
 * 定义登录注册相关的请求参数、响应数据和枚举类型
 */

// 登录请求参数
export interface LoginRequest {
  username: string
  password: string
}

// 注册请求参数
export interface RegisterRequest {
  username: string
  password: string
  realName: string
  role: 'STUDENT' | 'TEACHER'
  phone?: string
  email?: string
  studentNo?: string
  department?: string
  title?: string
}

// 用户信息
export interface UserInfo {
  userId: number
  username: string
  realName: string
  role: string
  department?: string
  avatarUrl?: string
}

// 登录响应数据
export interface LoginResponse {
  token: string
  tokenType: string
  expiresIn: number
  userInfo: UserInfo
}

// 注册响应数据
export interface RegisterResponse {
  userId: number
  username: string
  realName: string
  role: string
}

// 统一响应格式
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 分页返回格式
export interface PageVO<T> {
  list: T[]
  total: number
  page: number
  size: number
  totalPages: number
}