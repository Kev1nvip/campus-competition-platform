/**
 * Axios请求封装
 * 统一配置请求拦截器自动添加Token
 * 统一配置响应拦截器处理错误
 */
import axios from 'axios'
import type { InternalAxiosRequestConfig, AxiosResponse } from 'axios'

// 创建axios实例，配置基础URL和超时时间
const request = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
// 在发送请求前从localStorage获取Token并添加到请求头
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token: string | null = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: Error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
// 统一处理响应数据，只返回data部分
request.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error: Error) => {
    return Promise.reject(error)
  }
)

export default request