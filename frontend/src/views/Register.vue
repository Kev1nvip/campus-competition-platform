<template>
  <div class="auth-page">
    <div class="auth-card wide">
      <div class="auth-header">
        <div class="brand">◆ 校园竞赛平台</div>
        <h2>注册账号</h2>
      </div>

      <el-form :model="form" label-width="0" class="reg-form">
        <div class="form-row">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名（4-64位字母/数字/下划线）" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.realName" placeholder="真实姓名" />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码（8-32位，含字母和数字）" show-password />
          </el-form-item>
          <el-form-item>
            <el-input v-model="confirmPassword" type="password" placeholder="确认密码" show-password />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item>
            <el-select v-model="form.role" style="width:100%">
              <el-option label="学生" value="STUDENT" />
              <el-option label="教师" value="TEACHER" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.role === 'STUDENT'">
            <el-input v-model="form.studentNo" placeholder="学号（学生必填）" />
          </el-form-item>
          <el-form-item v-if="form.role === 'TEACHER'">
            <el-input v-model="form.title" placeholder="职称（如：讲师/副教授/教授）" />
          </el-form-item>
        </div>

        <div class="form-row">
          <el-form-item>
            <el-input v-model="form.phone" placeholder="手机号（选填）" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.email" placeholder="邮箱（选填）" />
          </el-form-item>
        </div>

        <el-form-item>
          <el-input v-model="form.department" placeholder="所在院系（选填）" />
        </el-form-item>

        <div v-if="errorMessage" class="error-tip">{{ errorMessage }}</div>

        <el-button
          type="primary"
          size="large"
          :loading="loading"
          style="width:100%;margin-top:8px;"
          @click="handleRegister"
        >
          {{ loading ? '注册中...' : '注 册' }}
        </el-button>
      </el-form>

      <div class="auth-footer">
        已有账号？<span class="link" @click="router.push('/login')">立即登录</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import type { RegisterRequest } from '@/types/auth'

const router = useRouter()
const form = reactive<RegisterRequest>({
  username: '', password: '', realName: '', role: 'STUDENT',
  phone: '', email: '', studentNo: '', department: '', title: ''
})
const confirmPassword = ref('')
const loading = ref(false)
const errorMessage = ref('')

const handleRegister = async () => {
  if (!form.username.trim()) return (errorMessage.value = '请输入用户名')
  if (form.username.length < 4 || form.username.length > 64) return (errorMessage.value = '用户名长度需 4-64 位')
  if (!/^[a-zA-Z0-9_]+$/.test(form.username)) return (errorMessage.value = '用户名只能包含字母、数字、下划线')
  if (!form.password) return (errorMessage.value = '请输入密码')
  if (form.password.length < 8 || form.password.length > 32) return (errorMessage.value = '密码长度需 8-32 位')
  if (!/[a-zA-Z]/.test(form.password) || !/[0-9]/.test(form.password)) return (errorMessage.value = '密码必须包含字母和数字')
  if (form.password !== confirmPassword.value) return (errorMessage.value = '两次密码不一致')
  if (!form.realName.trim()) return (errorMessage.value = '请输入真实姓名')
  if (form.role === 'STUDENT' && !form.studentNo?.trim()) return (errorMessage.value = '请输入学号')
  if (form.role === 'TEACHER' && !form.title?.trim()) return (errorMessage.value = '请输入职称')
  if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) return (errorMessage.value = '手机号格式不正确')
  if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) return (errorMessage.value = '邮箱格式不正确')

  errorMessage.value = ''
  loading.value = true
  try {
    const res: any = await register(form)
    if (res.code === 0) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } else {
      errorMessage.value = res.message || '注册失败'
    }
  } catch (e: any) {
    errorMessage.value = e.response?.data?.message || '网络错误'
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
  padding: 24px;
}

.auth-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 40px;
  width: 600px;
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

.reg-form {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-row .el-form-item {
  flex: 1;
  margin-bottom: 12px;
}

.error-tip {
  font-size: 13px;
  color: #888;
  background: #f8f8f8;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 4px;
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
