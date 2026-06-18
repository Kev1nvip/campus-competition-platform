import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { studentRoutes } from './student'
import { teacherRoutes } from './teacher'
import { adminRoutes } from './admin'
import { useTeacherStore } from '../store/userTeacher'
import { useAdminStore } from '../store/userAdmin'

// 完整路由表
const routes: RouteRecordRaw[] = [
  // ── 公开页（未登录可访问） ──────────────────────────
  { path: '/',         name: 'Home',     component: () => import('../views/Home.vue') },
  { path: '/login',    name: 'Login',    component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },

  // ── 三端工作台 ────────────────────────────────────
  ...studentRoutes,
  ...teacherRoutes,
  ...adminRoutes,

  // ── 404 fallback ──────────────────────────────────
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// ── 统一路由守卫 ──────────────────────────────────────
const PUBLIC_PATHS = ['/', '/login', '/register']

router.beforeEach((to, _from, next) => {
  const teacherStore = useTeacherStore()
  const adminStore = useAdminStore()

  const rawUserInfo = localStorage.getItem('userInfo')
  const token = localStorage.getItem('token')
  const userInfo = rawUserInfo ? JSON.parse(rawUserInfo) : null
  const role: string = userInfo?.role ?? ''

  const isLoggedIn = !!token && !!userInfo

  // 1. 已登录用户访问公开页（/ /login /register）→ 跳到对应工作台
  //    但需要检查 Pinia Store 数据是否完整，避免因 persist 未恢复导致死循环
  if (isLoggedIn && PUBLIC_PATHS.includes(to.path)) {
    if (role === 'ADMIN' && adminStore.id) return next('/admin/signup-audit')
    if (role === 'TEACHER' && teacherStore.id) return next('/teacher/competition')
    if (role === 'STUDENT') return next('/student/dashboard')
    // 数据不一致时放行到登录页，让用户重新登录
    return next()
  }

  // 2. 学生端 /student/* → 需要 STUDENT 角色
  if (to.path.startsWith('/student')) {
    if (!isLoggedIn) return next('/login')
    if (role !== 'STUDENT') return next('/')
    return next()
  }

  // 3. 教师端 /teacher/* → 需要 TEACHER 角色 + teacherStore
  if (to.path.startsWith('/teacher')) {
    if (!isLoggedIn || role !== 'TEACHER') return next('/login')
    if (!teacherStore.id) {
      // 刷新页面后 persist 数据为空 → 引导重新登录
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      return next('/login')
    }
    return next()
  }

  // 4. 管理员端 /admin/* → 需要 ADMIN 角色 + adminStore
  if (to.path.startsWith('/admin')) {
    if (!isLoggedIn || role !== 'ADMIN') return next('/login')
    if (!adminStore.id) {
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      return next('/login')
    }
    return next()
  }

  // 5. 其余路径放行
  next()
})

export default router
