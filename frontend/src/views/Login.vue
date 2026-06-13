<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="brand">◆ 校园竞赛平台</div>
        <h2>登录</h2>
      </div>

      <el-form :model="form" @submit.prevent="handleLogin" class="auth-form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>

        <div v-if="errorMessage" class="error-tip">{{ errorMessage }}</div>

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          style="width:100%;margin-top:8px;"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </el-button>
      </el-form>

      <div class="auth-footer">
        没有账号？<span class="link" @click="router.push('/register')">立即注册</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'

const router = useRouter()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMessage = ref('')

const handleLogin = async () => {
  if (!form.username.trim() || !form.password) {
    errorMessage.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const res: any = await login(form)
    if (res.code === 0) {
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))
      router.push('/')
    } else {
      errorMessage.value = res.message || '登录失败'
    }
  } catch (e: any) {
    errorMessage.value = e.response?.data?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-card {
  width: 380px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 40px;
}

.auth-header {
  margin-bottom: 28px;
}

.brand {
  font-size: 13px;
  font-weight: 700;
  color: #555;
  letter-spacing: 1px;
  margin-bottom: 16px;
}

h2 {
  font-size: 22px;
  font-weight: 700;
  color: #111;
  margin: 0;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.error-tip {
  font-size: 13px;
  color: #888;
  background: #f8f8f8;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px 12px;
  margin-top: 4px;
}

.auth-footer {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: #888;
}

.link {
  color: #111;
  font-weight: 600;
  cursor: pointer;
  text-decoration: underline;
}
</style>
