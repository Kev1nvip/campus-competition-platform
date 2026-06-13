/**
 * Axios请求封装
 * 统一配置请求拦截器自动添加Token
 * 统一配置响应拦截器处理错误
 */
import axios from 'axios'
import type { InternalAxiosRequestConfig, AxiosRequestConfig, AxiosResponse } from 'axios'
import router from '@/router'

const instance = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

instance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token: string | null = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: Error) => Promise.reject(error)
)

// 响应拦截器 unwrap data，401 时清除登录状态并跳转登录页
instance.interceptors.response.use(
  (response: AxiosResponse) => response.data,
  (error: any) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

// 包装函数：让 TS 返回类型与拦截器实际行为一致（返回 T 而非 AxiosResponse<T>）
function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return instance.request<T, T>(config)
}

export default request