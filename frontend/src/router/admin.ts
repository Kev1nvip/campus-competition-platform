import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/components/admin-layout/Layout.vue'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('@/views/admin/Login.vue')
  },
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/user',
    children: [
      {
        path: 'user',
        name: 'AdminUser',
        component: () => import('@/views/admin/UserManage.vue')
      },
      {
        path: 'competition',
        name: 'AdminComp',
        component: () => import('@/views/admin/CompManage.vue')
      },
      {
        path: 'dept',
        name: 'AdminDept',
        component: () => import('@/views/admin/DeptManage.vue')
      },
      {
        path: 'stat',
        name: 'AdminStat',
        component: () => import('@/views/admin/StatData.vue')
      }
    ]
  }
]