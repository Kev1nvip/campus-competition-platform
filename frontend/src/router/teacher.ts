import type { RouteRecordRaw } from 'vue-router'
import Layout from '@/components/admin-layout/Layout.vue'

export const teacherRoutes: RouteRecordRaw[] = [
  {
    path: '/teacher',
    component: Layout,
    redirect: '/teacher/competition',
    children: [
      {
        path: 'competition',
        name: 'TeacherCompetition',
        component: () => import('@/views/teacher/PublishComp.vue')
      },
      {
        path: 'apply',
        name: 'TeacherApply',
        component: () => import('@/views/teacher/ApplyList.vue')
      },
      {
        path: 'team',
        name: 'TeacherTeam',
        component: () => import('@/views/teacher/TeamManage.vue')
      },
      {
        path: 'award',
        name: 'TeacherAward',
        component: () => import('@/views/teacher/AwardInput.vue')
      },
      {
        path: 'profile',
        name: 'TeacherProfile',
        component: () => import('@/views/teacher/Profile.vue')
      }
    ]
  }
]
