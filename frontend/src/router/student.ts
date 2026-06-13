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
      // 队伍大厅（所有招募中的队伍）
      {
        path: 'team-hall',
        name: 'TeamHall',
        component: () => import('@/views/TeamHall.vue')
      },
      // 我的队伍（自己参与的队伍）
      {
        path: 'my-teams',
        name: 'MyTeams',
        component: () => import('@/views/MyTeams.vue')
      },
      // 队伍详情（保留旧路径兼容）
      {
        path: 'teams',
        redirect: '/student/team-hall'
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
