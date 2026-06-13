<template>
  <div class="login-box">
    <h2>平台管理员登录</h2>
    <input v-model="form.account" placeholder="管理员账号" />
    <input v-model="form.pwd" type="password" placeholder="密码" />
    <button @click="login">登录</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAdminStore } from '@/store/userAdmin'
import { adminLogin } from '@/api/admin'

const router = useRouter()
const adminStore = useAdminStore()
const form = ref({ account: '', pwd: '' })

const TEST_ADMIN = {
  account: "admin",
  pwd: "111111",
  mockData: {
    id: "a001",
    name: "系统超级管理员"
  }
}

const login = async () => {
  if (!form.value.account || !form.value.pwd) {
    alert('账号、密码不能为空')
    return
  }

  if (form.value.account === TEST_ADMIN.account && form.value.pwd === TEST_ADMIN.pwd) {
    adminStore.setUser(TEST_ADMIN.mockData)
    router.push('/admin/user')
    return
  }

  try {
    const res = await adminLogin(form.value)
    if (res.code === 200) {
      adminStore.setUser(res.data)
      router.push('/admin/user')
    } else {
      alert(res.msg || '账号或密码错误')
    }
  } catch (err) {
    alert('后端服务未启动，无法连接登录接口')
    console.error(err)
  }
}
</script>