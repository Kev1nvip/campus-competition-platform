/**
 * 路由配置
 * 定义应用的所有路由路径和对应的组件
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
// 导入你写的教师、管理员路由模块
import { teacherRoutes } from './teacher'
import { adminRoutes } from './admin'
// 导入状态库用于路由权限拦截
import { useTeacherStore } from '../store/userTeacher'
import { useAdminStore } from '../store/userAdmin'
const routes: RouteRecordRaw[] = [
  // ========== 邓子恒负责：学生端路由（完全保留不动） ==========
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/competitions',
    name: 'CompetitionList',
    component: () => import('../views/CompetitionList.vue')
  },
  {
    path: '/competition/:id',
    name: 'CompetitionDetail',
    component: () => import('../views/CompetitionDetail.vue')
  },
  {
    path: '/competition/:id/signup',
    name: 'StudentSignup',
    component: () => import('../views/StudentSignup.vue')
  },
  {
    path: '/teacher-select',
    name: 'TeacherSelect',
    component: () => import('../views/TeacherSelect.vue')
  },
  {
    path: '/teams',
    name: 'TeamPage',
    component: () => import('../views/TeamPage.vue')
  },
  {
    path: '/team/:id',
    name: 'TeamDetail',
    component: () => import('../views/TeamDetail.vue')
  },
  {
    path: '/profile',
    name: 'StudentProfile',
    component: () => import('../views/StudentProfile.vue')
  },
  // ========== 你（队友B）负责：教师端、管理员路由 ==========
  ...teacherRoutes,
  ...adminRoutes
]

// 使用 createWebHistory 实现 HTML5 history 模式
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 全局路由守卫：未登录禁止访问后台页面 + 身份隔离（教师不能进管理后台，管理员不能进教师后台）
router.beforeEach((to, from, next) => {
  const teacherStore = useTeacherStore()
  const adminStore = useAdminStore()

  // 1. 访问教师后台分组
  if (to.path.startsWith('/teacher')) {
    // 访问登录页直接放行
    if (to.path === '/teacher/login') {
      next()
      return
    }
    // 未登录教师，跳教师登录
    if (!teacherStore.id) {
      return next('/teacher/login')
    }
    // 已登录教师，禁止进入管理员页面（手动篡改地址拦截）
    if (adminStore.id && !teacherStore.id) {
      return next('/teacher/competition')
    }
  }

  // 2. 访问管理员后台分组
  if (to.path.startsWith('/admin')) {
    // 访问登录页直接放行
    if (to.path === '/admin/login') {
      next()
      return
    }
    // 未登录管理员，跳管理员登录
    if (!adminStore.id) {
      return next('/admin/login')
    }
    // 已登录管理员，禁止进入教师页面
    if (teacherStore.id && !adminStore.id) {
      return next('/admin/user')
    }
  }

  // 3. 学生端/首页等公共页面全部直接放行
  next()
})

export default router