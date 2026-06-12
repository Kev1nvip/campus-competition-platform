<template>
  <div class="login-box">
    <h2>教师后台登录</h2>
    <input v-model="form.tid" placeholder="教师工号" />
    <input v-model="form.pwd" type="password" placeholder="密码" />
    <button @click="login">登录</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useTeacherStore } from '@/store/userTeacher'
import { teacherLogin } from '@/api/teacher'

const router = useRouter()
const teacherStore = useTeacherStore()
const form = ref({ tid: '', pwd: '' })

// 测试账号：工号111111，密码111111
const TEST_TEACHER = {
  tid: "111111",
  pwd: "111111",
  mockData: {
    id: "t001",
    name: "测试指导教师",
    tid: "111111"
  }
}

const login = async () => {
  if (!form.value.tid || !form.value.pwd) {
    alert('工号、密码不能为空')
    return
  }

  if (form.value.tid === TEST_TEACHER.tid && form.value.pwd === TEST_TEACHER.pwd) {
    teacherStore.setUser(TEST_TEACHER.mockData)
    router.push('/teacher/competition')
    return
  }

  try {
    const res = await teacherLogin(form.value)
    if (res.code === 200) {
      teacherStore.setUser(res.data)
      router.push('/teacher/competition')
    } else {
      alert(res.msg || '工号或密码错误')
    }
  } catch (err) {
    alert('后端服务未启动/接口请求失败，请检查服务')
    console.error('登录接口异常', err)
  }
}
</script>