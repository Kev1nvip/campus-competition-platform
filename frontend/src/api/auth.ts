/**
 * 认证模块API接口
 * 提供登录、注册等认证相关的接口调用
 */
import request from '@/utils/request'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  ApiResponse
} from '@/types/auth'

export function login(data: LoginRequest): Promise<ApiResponse<LoginResponse>> {
  return request({
    url: '/v1/auth/login',
    method: 'POST',
    data
  })
}

export function register(data: RegisterRequest): Promise<ApiResponse<void>> {
  return request({
    url: '/v1/auth/register',
    method: 'POST',
    data
  })
}
