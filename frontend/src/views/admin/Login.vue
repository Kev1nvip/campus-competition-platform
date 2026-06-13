<template>
  <div class="login-box">
    <h2>平台管理员登录</h2>
    <input v-model="form.username" placeholder="管理员账号" />
    <input v-model="form.password" type="password" placeholder="密码" />
    <button @click="handleLogin">登录</button>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/userAdmin'
import { login } from '@/api/auth'

const router = useRouter()
const adminStore = useAdminStore()
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.username || !form.password) {
    alert('账号、密码不能为空')
    return
  }

  try {
    const res: any = await login(form)
    if (res.code === 0) {
      const { userInfo, token } = res.data
      if (userInfo.role !== 'ADMIN') {
        alert('该账号不是管理员账号')
        return
      }
      localStorage.setItem('token', token)
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      adminStore.setUser({
        id: String(userInfo.userId),
        name: userInfo.realName
      })
      router.push('/admin/user')
    } else {
      alert(res.message || '账号或密码错误')
    }
  } catch (err) {
    alert('登录失败，请检查后端服务是否启动')
    console.error(err)
  }
}
</script>
