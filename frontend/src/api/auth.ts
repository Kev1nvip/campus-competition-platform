/**
 * 认证模块API接口
 * 提供登录、注册等认证相关的接口调用
 */
import request from '@/utils/request'
import type { 
  LoginRequest, 
  LoginResponse, 
  RegisterRequest, 
  RegisterResponse,
  ApiResponse 
} from '@/types/auth'

/**
 * 用户登录接口
 * 调用后端登录API，验证用户名密码后返回JWT Token
 */
export function login(data: LoginRequest) {
  return request<ApiResponse<LoginResponse>>({
    url: '/v1/auth/login',
    method: 'POST',
    data
  })
}

/**
 * 用户注册接口
 * 调用后端注册API，创建新用户账号
 */
export function register(data: RegisterRequest) {
  return request<ApiResponse<RegisterResponse>>({
    url: '/v1/auth/register',
    method: 'POST',
    data
  })
}