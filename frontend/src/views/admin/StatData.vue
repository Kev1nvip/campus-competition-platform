<template>
  <div class="page-box">
    <h2>平台参赛数据统计</h2>
    <div class="stat-card">
      <p>总竞赛数：{{ stat.totalComp }}</p>
      <p>总参赛队伍：{{ stat.totalTeam }}</p>
      <p>获奖记录：{{ stat.totalAward }}</p>
      <p>教师总数：{{ stat.totalTeacher }}</p>
      <p>学生总数：{{ stat.totalStudent }}</p>
    </div>
    <div style="margin-top:30px;">
      <h3>年度竞赛趋势图表区域</h3>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getStatData } from '@/api/admin'
const stat = ref({
  totalComp: 0,
  totalTeam: 0,
  totalAward: 0,
  totalTeacher: 0,
  totalStudent: 0
})

const loadStat = async () => {
  try {
    const res = await getStatData()
    stat.value = res.data.data
  } catch {}
}
onMounted(loadStat)
</script>

<style scoped>
.page-box { padding:30px; }
.stat-card {
  display: flex;
  gap: 30px;
  padding:20px;
  border:1px solid #eee;
}
</style>