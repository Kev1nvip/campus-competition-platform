/**
 * 路由配置
 * 定义应用的所有路由路径和对应的组件
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
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
  }
]

// 使用createWebHistory实现HTML5 history模式
const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router