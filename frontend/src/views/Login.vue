<!--
  组件：登录页面
  说明：用户输入账号密码进行登录，登录成功后存储Token并跳转到首页
-->
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import type { LoginRequest } from '@/types/auth'

const router = useRouter()

const form = reactive<LoginRequest>({
  username: '',
  password: ''
})

const loading = ref(false)
const errorMessage = ref('')

/**
 * 表单校验
 * 确保用户名和密码不为空
 */
const validateForm = () => {
  if (!form.username.trim()) {
    errorMessage.value = '请输入用户名'
    return false
  }
  if (!form.password) {
    errorMessage.value = '请输入密码'
    return false
  }
  errorMessage.value = ''
  return true
}

/**
 * 处理登录提交
 * 调用登录接口，登录成功则存储Token并跳转首页
 */
const handleLogin = async () => {
  if (!validateForm()) return
  
  loading.value = true
  errorMessage.value = ''
  
  try {
    const res: any = await login(form)
    
    if (res.code === 0) {
      // 登录成功，存储Token和用户信息到localStorage
      // 后续请求通过请求拦截器自动携带Token
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
      
      router.push('/')
    } else {
      // 后端返回业务错误，显示错误信息
      errorMessage.value = res.message || '登录失败'
    }
  } catch (error: any) {
    // 网络错误或后端服务异常，统一提示网络问题
    errorMessage.value = error.response?.data?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到注册页面
 */
const goToRegister = () => {
  router.push('/register')
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <h1>校园学术竞赛管理平台</h1>
      <h2>用户登录</h2>
      
      <form @submit.prevent="handleLogin" class="login-form">
        <div class="form-item">
          <label for="username">用户名</label>
          <input 
            id="username"
            v-model="form.username"
            type="text" 
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </div>
        
        <div class="form-item">
          <label for="password">密码</label>
          <input 
            id="password"
            v-model="form.password"
            type="password" 
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>
        
        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
        
        <button type="submit" class="login-btn" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        
        <div class="register-link">
          还没有账号？<span @click="goToRegister">立即注册</span>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 400px;
}

h1 {
  font-size: 24px;
  color: #333;
  text-align: center;
  margin-bottom: 10px;
}

h2 {
  font-size: 18px;
  color: #666;
  text-align: center;
  margin-bottom: 30px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-item input {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-item input:focus {
  outline: none;
  border-color: #667eea;
}

.error-message {
  color: #ff4444;
  font-size: 14px;
  text-align: center;
  padding: 10px;
  background: #fff0f0;
  border-radius: 5px;
}

.login-btn {
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.3s;
}

.login-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.register-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.register-link span {
  color: #667eea;
  cursor: pointer;
  font-weight: 500;
}

.register-link span:hover {
  text-decoration: underline;
}
</style>