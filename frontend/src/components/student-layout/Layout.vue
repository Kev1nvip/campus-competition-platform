<template>
  <div class="student-layout">
    <header class="topbar">
      <div class="topbar-left">
        <span class="brand" @click="router.push('/student/dashboard')">◆ 校园竞赛平台</span>
        <nav class="nav-menu">
          <router-link to="/student/competitions" class="nav-link" active-class="active">竞赛</router-link>
          <router-link to="/student/team-hall" class="nav-link" active-class="active">队伍大厅</router-link>
          <router-link to="/student/my-teams" class="nav-link" active-class="active">我的队伍</router-link>
          <router-link to="/student/signups" class="nav-link" active-class="active">我的报名</router-link>
          <router-link to="/student/ai" class="nav-link" active-class="active">AI 推荐</router-link>
        </nav>
      </div>
      <div class="topbar-right">
        <NotificationBell />
        <el-divider direction="vertical" />
        <router-link to="/student/profile" class="nav-link">{{ userName }}</router-link>
        <el-divider direction="vertical" />
        <span class="logout-btn" @click="handleLogout">退出</span>
      </div>
    </header>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import NotificationBell from '@/components/NotificationBell.vue'

const router = useRouter()

const userName = computed(() => {
  const info = localStorage.getItem('userInfo')
  if (!info) return '我的'
  return JSON.parse(info).realName || '我的'
})

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
}
</script>

<style scoped>
.student-layout { min-height: 100vh; background: #fafafa; display: flex; flex-direction: column; }

.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  position: sticky;
  top: 0;
  z-index: 100;
}

.topbar-left { display: flex; align-items: center; gap: 32px; }
.brand { font-size: 14px; font-weight: 800; letter-spacing: 1px; color: #111; cursor: pointer; white-space: nowrap; }
.nav-menu { display: flex; gap: 4px; }
.nav-link { padding: 6px 14px; font-size: 13px; color: #555; text-decoration: none; border-radius: 4px; transition: all 0.12s; }
.nav-link:hover, .nav-link.active { color: #111; background: #f4f4f4; font-weight: 600; }

.topbar-right { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.logout-btn { color: #888; cursor: pointer; font-size: 13px; }
.logout-btn:hover { color: #111; }

.main-content {
  flex: 1;
  padding: 32px 40px;
  max-width: 1100px;
  width: 100%;
  margin: 0 auto;
  box-sizing: border-box;
}
</style>
