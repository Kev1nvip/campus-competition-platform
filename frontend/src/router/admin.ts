import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/components/admin-layout/Layout.vue'

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/signup-audit',
    children: [
      {
        path: 'signup-audit',
        name: 'AdminSignupAudit',
        component: () => import('@/views/admin/SignupAudit.vue')
      },
      {
        path: 'award-audit',
        name: 'AdminAwardAudit',
        component: () => import('@/views/admin/AwardAudit.vue')
      },
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
