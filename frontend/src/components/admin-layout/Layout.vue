<template>
  <el-container style="height:100vh;">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="sidebar">
      <div class="sidebar-logo">
        <span class="logo-icon">◆</span>
        <span>{{ role === 'teacher' ? '教师后台' : '管理后台' }}</span>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        class="sidebar-menu"
      >
        <template v-if="role === 'teacher'">
          <el-menu-item index="/teacher/competition">
            <el-icon><Edit /></el-icon>
            <span>发布竞赛</span>
          </el-menu-item>
          <el-menu-item index="/teacher/apply">
            <el-icon><Document /></el-icon>
            <span>报名审核</span>
          </el-menu-item>
          <el-menu-item index="/teacher/team">
            <el-icon><User /></el-icon>
            <span>队伍管理</span>
          </el-menu-item>
          <el-menu-item index="/teacher/award">
            <el-icon><Trophy /></el-icon>
            <span>录入获奖</span>
          </el-menu-item>
        </template>

        <template v-if="role === 'admin'">
          <el-menu-item index="/admin/user">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/competition">
            <el-icon><List /></el-icon>
            <span>竞赛管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/dept">
            <el-icon><OfficeBuilding /></el-icon>
            <span>院系管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/stat">
            <el-icon><DataLine /></el-icon>
            <span>数据统计</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 右侧 -->
    <el-container direction="vertical">
      <el-header class="topbar" height="56px">
        <div class="topbar-left">
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="topbar-right">
          <span class="username">{{ userName }}</span>
          <el-divider direction="vertical" />
          <el-button text @click="logout" class="logout-btn">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTeacherStore } from '@/store/userTeacher'
import { useAdminStore } from '@/store/userAdmin'

const router = useRouter()
const route = useRoute()
const teacherStore = useTeacherStore()
const adminStore = useAdminStore()

const role = computed(() => (teacherStore.id ? 'teacher' : 'admin'))
const userName = computed(() => role.value === 'teacher' ? teacherStore.name : adminStore.name)

const titleMap: Record<string, string> = {
  '/teacher/competition': '发布竞赛',
  '/teacher/apply': '报名审核',
  '/teacher/team': '队伍管理',
  '/teacher/award': '录入获奖',
  '/admin/user': '用户管理',
  '/admin/competition': '竞赛管理',
  '/admin/dept': '院系管理',
  '/admin/stat': '数据统计',
}
const currentTitle = computed(() => titleMap[route.path] ?? '后台管理')

const logout = () => {
  if (role.value === 'teacher') {
    teacherStore.clear()
    router.push('/teacher/login')
  } else {
    adminStore.clear()
    router.push('/admin/login')
  }
}
</script>

<style scoped>
.sidebar {
  background: #fafafa;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 20px;
  font-size: 15px;
  font-weight: 700;
  color: #111;
  border-bottom: 1px solid #e0e0e0;
  letter-spacing: 0.5px;
}

.logo-icon {
  font-size: 12px;
  color: #555;
}

.sidebar-menu {
  flex: 1;
  border-right: none !important;
  background: transparent;
}

.topbar {
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.topbar-left .page-title {
  font-size: 15px;
  font-weight: 600;
  color: #111;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #555;
}

.username {
  font-weight: 500;
  color: #1a1a1a;
}

.logout-btn {
  color: #555 !important;
  font-size: 13px !important;
}
.logout-btn:hover {
  color: #111 !important;
}

.main-content {
  background: #fafafa;
  padding: 24px;
  overflow-y: auto;
}
</style>
