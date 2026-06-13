<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="brand">◆ 校园竞赛平台</div>
        <h2>管理员登录</h2>
      </div>
      <el-form :model="form" class="auth-form">
        <el-form-item>
          <el-input v-model="form.username" placeholder="管理员账号" size="large" clearable />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
        </el-form-item>
        <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>
        <el-button type="primary" size="large" :loading="loading" style="width:100%;margin-top:8px;" @click="handleLogin">
          {{ loading ? '登录中...' : '登 录' }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/userAdmin'
import { login } from '@/api/auth'

const router = useRouter()
const adminStore = useAdminStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')

const handleLogin = async () => {
  if (!form.username || !form.password) return (errorMsg.value = '请输入账号和密码')
  loading.value = true
  errorMsg.value = ''
  try {
    const res: any = await login(form)
    if (res.code === 0) {
      const { userInfo, token } = res.data
      if (userInfo.role !== 'ADMIN') return (errorMsg.value = '该账号不是管理员账号')
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      adminStore.setUser({ id: String(userInfo.userId), name: userInfo.realName })
      router.push('/admin/user')
    } else {
      errorMsg.value = res.message || '登录失败'
    }
  } catch {
    errorMsg.value = '网络错误'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page { min-height: 100vh; background: #fafafa; display: flex; align-items: center; justify-content: center; }
.auth-card { width: 380px; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; padding: 40px; }
.auth-header { margin-bottom: 28px; }
.brand { font-size: 13px; font-weight: 700; color: #555; letter-spacing: 1px; margin-bottom: 16px; }
h2 { font-size: 22px; font-weight: 700; color: #111; margin: 0; }
.auth-form { display: flex; flex-direction: column; gap: 4px; }
.error-tip { font-size: 13px; color: #888; background: #f8f8f8; border: 1px solid #e8e8e8; border-radius: 4px; padding: 8px 12px; }
</style>
