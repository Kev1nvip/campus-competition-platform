<template>
  <div class="admin-wrap">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="logo">竞赛管理后台</div>
      <nav class="side-menu">
        <!-- 教师菜单 -->
        <template v-if="role === 'teacher'">
          <router-link to="/teacher/competition">发布竞赛</router-link>
          <router-link to="/teacher/apply">报名审核</router-link>
          <router-link to="/teacher/team">队伍管理</router-link>
          <router-link to="/teacher/award">录入获奖</router-link>
        </template>
        <!-- 管理员菜单 -->
        <template v-if="role === 'admin'">
          <router-link to="/admin/user">用户管理</router-link>
          <router-link to="/admin/competition">竞赛总管理</router-link>
          <router-link to="/admin/dept">院系管理</router-link>
          <router-link to="/admin/stat">数据统计</router-link>
        </template>
      </nav>
    </aside>
    <!-- 右侧页面区域 -->
    <div class="main-box">
      <header class="top-bar">
        <span>当前账号：{{ userName }}</span>
        <button @click="logout">退出登录</button>
      </header>
      <div class="page-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useTeacherStore } from '@/store/userTeacher'
import { useAdminStore } from '@/store/userAdmin'

const router = useRouter()
const teacherStore = useTeacherStore()
const adminStore = useAdminStore()

// 判断当前登录身份
const role = teacherStore.id ? 'teacher' : 'admin'
const userName = role === 'teacher' ? teacherStore.name : adminStore.name

// 退出登录
const logout = () => {
  if (role === 'teacher') teacherStore.clear()
  else adminStore.clear()
  router.push(`/${role}/login`)
}
</script>

<style scoped>
.admin-wrap {
  display: flex;
  height: 100vh;
}
.sidebar {
  width: 220px;
  background: #232734;
  color: #fff;
  padding-top: 20px;
}
.logo {
  text-align: center;
  font-size: 18px;
  padding-bottom: 30px;
  border-bottom: 1px solid #333;
}
.side-menu {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.side-menu a {
  color: #ccc;
  padding: 12px 24px;
  text-decoration: none;
}
.side-menu a.router-link-active {
  background: #409eff;
  color: #fff;
}
.main-box {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.top-bar {
  height: 60px;
  background: #fff;
  box-shadow: 0 1px 4px #eee;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 30px;
  gap: 20px;
}
.page-content {
  flex: 1;
  padding: 24px;
  background: #f5f7fa;
  overflow-y: auto;
}
</style>