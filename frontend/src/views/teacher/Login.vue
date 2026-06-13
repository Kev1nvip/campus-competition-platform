<template>
  <div class="login-box">
    <h2>教师后台登录</h2>
    <input v-model="form.username" placeholder="用户名" />
    <input v-model="form.password" type="password" placeholder="密码" />
    <button @click="handleLogin">登录</button>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useTeacherStore } from '@/store/userTeacher'
import { login } from '@/api/auth'

const router = useRouter()
const teacherStore = useTeacherStore()
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.username || !form.password) {
    alert('用户名、密码不能为空')
    return
  }

  try {
    const res: any = await login(form)
    if (res.code === 0) {
      const { userInfo, token } = res.data
      if (userInfo.role !== 'TEACHER') {
        alert('该账号不是教师账号')
        return
      }
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      teacherStore.setUser({
        id: String(userInfo.userId),
        name: userInfo.realName,
        tid: userInfo.username
      })
      router.push('/teacher/competition')
    } else {
      alert(res.message || '用户名或密码错误')
    }
  } catch (err) {
    alert('登录失败，请检查后端服务是否启动')
    console.error('登录接口异常', err)
  }
}
</script>
