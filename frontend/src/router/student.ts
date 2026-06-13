import type { RouteRecordRaw } from 'vue-router'
import StudentLayout from '@/components/student-layout/Layout.vue'

export const studentRoutes: RouteRecordRaw[] = [
  {
    path: '/student',
    component: StudentLayout,
    redirect: '/student/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'StudentDashboard',
        component: () => import('@/views/student/Dashboard.vue')
      },
      {
        path: 'competitions',
        name: 'StudentCompetitions',
        component: () => import('@/views/CompetitionList.vue')
      },
      {
        path: 'competition/:id',
        name: 'StudentCompetitionDetail',
        component: () => import('@/views/CompetitionDetail.vue')
      },
      {
        path: 'competition/:id/signup',
        name: 'StudentSignup',
        component: () => import('@/views/StudentSignup.vue')
      },
      {
        path: 'teams',
        name: 'StudentTeams',
        component: () => import('@/views/TeamPage.vue')
      },
      {
        path: 'team/:id',
        name: 'StudentTeamDetail',
        component: () => import('@/views/TeamDetail.vue')
      },
      {
        path: 'signups',
        name: 'StudentSignups',
        component: () => import('@/views/StudentProfile.vue')
      },
      {
        path: 'profile',
        name: 'StudentProfile',
        component: () => import('@/views/StudentProfile.vue')
      },
      {
        path: 'ai',
        name: 'StudentAi',
        component: () => import('@/views/AiRecommend.vue')
      }
    ]
  }
]
