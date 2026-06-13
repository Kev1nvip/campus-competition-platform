<template>
  <div>
    <div class="page-title">数据统计</div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else class="stats-wrap">
      <!-- 用户统计 -->
      <div class="stat-section">
        <div class="section-label">用户</div>
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-num">{{ stats.userStats?.totalUsers ?? 0 }}</div>
            <div class="stat-name">总用户数</div>
          </div>
          <div class="stat-card">
            <div class="stat-num">{{ stats.userStats?.studentCount ?? 0 }}</div>
            <div class="stat-name">学生数</div>
          </div>
          <div class="stat-card">
            <div class="stat-num">{{ stats.userStats?.teacherCount ?? 0 }}</div>
            <div class="stat-name">教师数</div>
          </div>
        </div>
      </div>

      <el-divider />

      <!-- 竞赛统计 -->
      <div class="stat-section">
        <div class="section-label">竞赛</div>
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-num">{{ stats.competitionStats?.totalCompetitions ?? 0 }}</div>
            <div class="stat-name">总竞赛数</div>
          </div>
          <div class="stat-card">
            <div class="stat-num">{{ stats.competitionStats?.signingCount ?? 0 }}</div>
            <div class="stat-name">报名中</div>
          </div>
          <div class="stat-card">
            <div class="stat-num">{{ stats.competitionStats?.judgingCount ?? 0 }}</div>
            <div class="stat-name">进行中</div>
          </div>
          <div class="stat-card">
            <div class="stat-num">{{ stats.competitionStats?.endedCount ?? 0 }}</div>
            <div class="stat-name">已结束</div>
          </div>
        </div>
      </div>

      <el-divider />

      <!-- 获奖统计 -->
      <div class="stat-section">
        <div class="section-label">获奖</div>
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-num">{{ stats.awardStats?.totalAwards ?? 0 }}</div>
            <div class="stat-name">总获奖数</div>
          </div>
          <div class="stat-card">
            <div class="stat-num">{{ stats.awardStats?.approvedAwards ?? 0 }}</div>
            <div class="stat-name">已通过</div>
          </div>
          <div class="stat-card">
            <div class="stat-num">{{ stats.awardStats?.pendingAwards ?? 0 }}</div>
            <div class="stat-name">待审核</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStatData } from '@/api/admin'

const isLoading = ref(false)
const stats = ref<any>({})

const loadData = async () => {
  isLoading.value = true
  try {
    const res: any = await getStatData()
    if (res.code === 0) stats.value = res.data
  } finally {
    isLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.center-tip { text-align: center; padding: 40px; color: #aaa; }
.stats-wrap { display: flex; flex-direction: column; gap: 4px; }
.stat-section { padding: 8px 0; }
.section-label { font-size: 12px; font-weight: 700; color: #999; letter-spacing: 1px; text-transform: uppercase; margin-bottom: 14px; }
.stat-cards { display: flex; gap: 16px; flex-wrap: wrap; }
.stat-card { background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 20px 28px; min-width: 120px; transition: border-color 0.15s; }
.stat-card:hover { border-color: #aaa; }
.stat-num { font-size: 28px; font-weight: 800; color: #111; line-height: 1; margin-bottom: 6px; }
.stat-name { font-size: 12px; color: #888; }
</style>
