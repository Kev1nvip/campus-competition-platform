<!--
  组件：注册页面
  说明：用户填写信息进行注册，注册成功后跳转到登录页面
-->
<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import type { RegisterRequest } from '@/types/auth'

const router = useRouter()

const form = reactive<RegisterRequest>({
  username: '',
  password: '',
  realName: '',
  role: 'STUDENT',
  phone: '',
  email: '',
  studentNo: '',
  department: ''
})

const confirmPassword = ref('')
const loading = ref(false)
const errorMessage = ref('')

/**
 * 表单校验
 * 校验用户名、密码、角色等必填项和格式
 */
const validateForm = () => {
  // 用户名：4-64位，只能包含字母、数字、下划线
  if (!form.username.trim()) {
    errorMessage.value = '请输入用户名'
    return false
  }
  if (form.username.length < 4 || form.username.length > 64) {
    errorMessage.value = '用户名长度必须在4-64位之间'
    return false
  }
  if (!/^[a-zA-Z0-9_]+$/.test(form.username)) {
    errorMessage.value = '用户名只能包含字母、数字、下划线'
    return false
  }
  
  // 密码：8-32位，必须包含字母和数字
  if (!form.password) {
    errorMessage.value = '请输入密码'
    return false
  }
  if (form.password.length < 8 || form.password.length > 32) {
    errorMessage.value = '密码长度必须在8-32位之间'
    return false
  }
  if (!/[a-zA-Z]/.test(form.password) || !/[0-9]/.test(form.password)) {
    errorMessage.value = '密码必须包含字母和数字'
    return false
  }
  
  // 确认密码必须与密码一致
  if (form.password !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致'
    return false
  }
  
  // 真实姓名：必填
  if (!form.realName.trim()) {
    errorMessage.value = '请输入真实姓名'
    return false
  }
  
  // 学生角色必须填写学号
  if (form.role === 'STUDENT' && !form.studentNo?.trim()) {
    errorMessage.value = '请输入学号'
    return false
  }
  
  // 手机号：11位数字，格式校验
  if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) {
    errorMessage.value = '手机号格式不正确'
    return false
  }
  
  // 邮箱：格式校验
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errorMessage.value = '邮箱格式不正确'
    return false
  }
  
  errorMessage.value = ''
  return true
}

/**
 * 处理注册提交
 * 调用注册接口，注册成功则弹出提示并跳转登录页
 */
const handleRegister = async () => {
  if (!validateForm()) return
  
  loading.value = true
  errorMessage.value = ''
  
  try {
    const res: any = await register(form)
    
    if (res.code === 0) {
      alert('注册成功！请登录')
      router.push('/login')
    } else {
      // 后端返回业务错误，显示错误信息
      errorMessage.value = res.message || '注册失败'
    }
  } catch (error: any) {
    // 网络错误或后端服务异常，统一提示网络问题
    errorMessage.value = error.response?.data?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

/**
 * 跳转到登录页面
 */
const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="register-container">
    <div class="register-box">
      <h1>校园学术竞赛管理平台</h1>
      <h2>用户注册</h2>
      
      <form @submit.prevent="handleRegister" class="register-form">
        <div class="form-row">
          <div class="form-item">
            <label for="username">用户名 *</label>
            <input 
              id="username"
              v-model="form.username"
              type="text" 
              placeholder="4-64位字母、数字、下划线"
            />
          </div>
          
          <div class="form-item">
            <label for="realName">真实姓名 *</label>
            <input 
              id="realName"
              v-model="form.realName"
              type="text" 
              placeholder="请输入真实姓名"
            />
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-item">
            <label for="password">密码 *</label>
            <input 
              id="password"
              v-model="form.password"
              type="password" 
              placeholder="8-32位，必须包含字母和数字"
            />
          </div>
          
          <div class="form-item">
            <label for="confirmPassword">确认密码 *</label>
            <input 
              id="confirmPassword"
              v-model="confirmPassword"
              type="password" 
              placeholder="请再次输入密码"
            />
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-item">
            <label for="role">角色 *</label>
            <select id="role" v-model="form.role">
              <option value="STUDENT">学生</option>
              <option value="TEACHER">老师</option>
            </select>
          </div>
          
          <div class="form-item">
            <label for="studentNo">学号</label>
            <input 
              id="studentNo"
              v-model="form.studentNo"
              type="text" 
              placeholder="学生必填"
              :disabled="form.role !== 'STUDENT'"
            />
          </div>
        </div>
        
        <div class="form-row">
          <div class="form-item">
            <label for="phone">手机号</label>
            <input 
              id="phone"
              v-model="form.phone"
              type="text" 
              placeholder="11位手机号"
            />
          </div>
          
          <div class="form-item">
            <label for="email">邮箱</label>
            <input 
              id="email"
              v-model="form.email"
              type="email" 
              placeholder="请输入邮箱"
            />
          </div>
        </div>
        
        <div class="form-item">
          <label for="department">院系</label>
          <input 
            id="department"
            v-model="form.department"
            type="text" 
            placeholder="请输入院系"
          />
        </div>
        
        <div v-if="errorMessage" class="error-message">
          {{ errorMessage }}
        </div>
        
        <button type="submit" class="register-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        
        <div class="login-link">
          已有账号？<span @click="goToLogin">立即登录</span>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px 0;
}

.register-box {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  width: 600px;
  max-height: 90vh;
  overflow-y: auto;
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

.register-form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.form-row {
  display: flex;
  gap: 15px;
}

.form-row .form-item {
  flex: 1;
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

.form-item input,
.form-item select {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-item input:focus,
.form-item select:focus {
  outline: none;
  border-color: #667eea;
}

.form-item input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.error-message {
  color: #ff4444;
  font-size: 14px;
  text-align: center;
  padding: 10px;
  background: #fff0f0;
  border-radius: 5px;
}

.register-btn {
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

.register-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.register-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.login-link span {
  color: #667eea;
  cursor: pointer;
  font-weight: 500;
}

.login-link span:hover {
  text-decoration: underline;
}
</style>