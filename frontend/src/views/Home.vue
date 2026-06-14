<template>
  <div class="landing">
    <!-- 顶栏 -->
    <header class="nav">
      <div class="nav-brand">◆ 校园竞赛平台</div>
      <nav class="nav-right">
        <el-button size="small" @click="router.push('/login')">登录</el-button>
        <el-button size="small" type="primary" @click="router.push('/register')">注册</el-button>
      </nav>
    </header>

    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <h1>校园学术竞赛<br />管理平台</h1>
        <p class="hero-desc">
          为学生、教师、管理员提供一站式竞赛报名、队伍组建、获奖管理服务
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/login')">立即登录</el-button>
          <el-button size="large" @click="router.push('/register')">注册账号</el-button>
        </div>
      </div>
    </section>

    <!-- 三端说明 -->
    <section class="roles">
      <div class="role-card">
        <div class="role-icon">▣</div>
        <div class="role-title">学生</div>
        <div class="role-desc">浏览竞赛、报名参赛、组建队伍、查看个人获奖记录</div>
      </div>
      <div class="role-card">
        <div class="role-icon">▦</div>
        <div class="role-title">教师</div>
        <div class="role-desc">发布竞赛、审核报名、管理指导队伍、录入获奖信息</div>
      </div>
      <div class="role-card">
        <div class="role-icon">▧</div>
        <div class="role-title">管理员</div>
        <div class="role-desc">管理平台用户、统一审核竞赛、查看全平台数据统计</div>
      </div>
    </section>

    <footer class="foot">© 2026 校园竞赛平台</footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTeacherStore } from '@/store/userTeacher'
import { useAdminStore } from '@/store/userAdmin'

const router = useRouter()
const teacherStore = useTeacherStore()
const adminStore = useAdminStore()

// 已登录用户直接跳到对应工作台
onMounted(() => {
  const userInfo = localStorage.getItem('userInfo')
  if (!userInfo) return

  const info = JSON.parse(userInfo)
  if (info.role === 'ADMIN' && adminStore.id) {
    router.replace('/admin/user')
  } else if (info.role === 'TEACHER' && teacherStore.id) {
    router.replace('/teacher/competition')
  } else if (info.role === 'STUDENT') {
    router.replace('/student/dashboard')
  }
})
</script>

<style scoped>
.landing {
  min-height: 100vh;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.nav {
  height: 56px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
}

.nav-brand {
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #111;
}

.nav-right {
  display: flex;
  gap: 8px;
}

.hero {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  border-bottom: 1px solid #e0e0e0;
}

.hero-inner {
  text-align: center;
  max-width: 600px;
}

h1 {
  font-size: 44px;
  font-weight: 900;
  color: #111;
  line-height: 1.2;
  margin: 0 0 20px;
  letter-spacing: -0.5px;
}

.hero-desc {
  font-size: 16px;
  color: #666;
  line-height: 1.8;
  margin: 0 0 36px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.roles {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
}

.role-card {
  padding: 48px 40px;
  border-right: 1px solid #e0e0e0;
}
.role-card:last-child { border-right: none; }

.role-icon {
  font-size: 20px;
  color: #333;
  margin-bottom: 14px;
}

.role-title {
  font-size: 16px;
  font-weight: 700;
  color: #111;
  margin-bottom: 10px;
}

.role-desc {
  font-size: 13px;
  color: #888;
  line-height: 1.7;
}

.foot {
  border-top: 1px solid #e0e0e0;
  padding: 20px 40px;
  text-align: center;
  font-size: 12px;
  color: #bbb;
}
</style>
