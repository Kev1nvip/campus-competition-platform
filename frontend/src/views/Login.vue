<template>
  <div class="auth-page">
    <div class="auth-split">
      <!-- 左侧品牌区 -->
      <div class="auth-brand">
        <div class="brand-logo">◆</div>
        <div class="brand-name">校园竞赛平台</div>
        <div class="brand-desc">一站式校园学术竞赛管理系统</div>
      </div>

      <!-- 右侧表单区 -->
      <div class="auth-form-area">
        <div class="auth-card">
          <h2>登录</h2>
          <p class="auth-sub">学生、教师、管理员统一登录</p>

          <el-form :model="form" class="form">
            <el-form-item>
              <el-input
                v-model="form.username"
                placeholder="用户名"
                size="large"
                clearable
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-form-item>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>

            <div v-if="errorMessage" class="error-tip">{{ errorMessage }}</div>

            <el-button
              type="primary"
              size="large"
              :loading="loading"
              style="width:100%;margin-top:12px;"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </el-button>
          </el-form>

          <div class="auth-footer">
            还没有账号？<span class="link" @click="router.push('/register')">立即注册</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { useTeacherStore } from '@/store/userTeacher'
import { useAdminStore } from '@/store/userAdmin'

const router = useRouter()
const teacherStore = useTeacherStore()
const adminStore = useAdminStore()

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
      const { userInfo, token } = res.data
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))

      // 根据角色分流
      if (userInfo.role === 'TEACHER') {
        teacherStore.setUser({
          id: String(userInfo.userId),
          name: userInfo.realName,
          tid: userInfo.username
        })
        router.push('/teacher/competition')
      } else if (userInfo.role === 'ADMIN') {
        adminStore.setUser({ id: String(userInfo.userId), name: userInfo.realName })
        router.push('/admin/signup-audit')
      } else {
        // STUDENT
        router.push('/student/dashboard')
      }
    } else {
      errorMessage.value = res.message || '用户名或密码错误'
    }
  } catch {
    errorMessage.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: #fff;
  display: flex;
}

.auth-split {
  display: flex;
  width: 100%;
}

/* 左侧品牌 */
.auth-brand {
  width: 380px;
  flex-shrink: 0;
  background: #111;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px 48px;
  color: #fff;
}

.brand-logo {
  font-size: 28px;
  margin-bottom: 20px;
  color: #fff;
}

.brand-name {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 1.5px;
  margin-bottom: 12px;
}

.brand-desc {
  font-size: 14px;
  color: #888;
  line-height: 1.7;
}

/* 右侧表单 */
.auth-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
  padding: 40px 20px;
}

.auth-card {
  width: 360px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 40px;
}

h2 {
  font-size: 22px;
  font-weight: 700;
  color: #111;
  margin: 0 0 6px;
}

.auth-sub {
  font-size: 13px;
  color: #aaa;
  margin: 0 0 28px;
}

.form {
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

@media (max-width: 640px) {
  .auth-brand { display: none; }
}
</style>
