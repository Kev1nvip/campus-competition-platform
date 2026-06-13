<template>
  <div class="page-wrap">
    <header class="nav">
      <span class="nav-brand" @click="router.push('/')">◆ 校园竞赛平台</span>
      <span class="nav-item" @click="router.push('/teams')">← 返回队伍</span>
    </header>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="!team" class="center-tip">队伍不存在</div>
    <div v-else class="page-inner">
      <div class="detail-head">
        <h2>{{ team.teamName }}</h2>
        <el-tag size="small" :type="team.status === 'FORMING' ? 'success' : 'info'">
          {{ statusText(team.status) }}
        </el-tag>
      </div>

      <el-divider />

      <div class="info-grid">
        <div class="info-block">
          <div class="info-label">成员人数</div>
          <div class="info-val">{{ team.memberCount }} 人</div>
        </div>
        <div class="info-block">
          <div class="info-label">指导老师</div>
          <div class="info-val">{{ team.teacherConfirmed ? '已确认' : '待确认' }}</div>
        </div>
      </div>

      <div class="section" style="margin-top:24px;">
        <el-button
          v-if="team.status === 'FORMING'"
          type="primary"
          size="small"
          @click="handleQuit"
        >
          退出队伍
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const team = ref<any>(null)
const isLoading = ref(false)

const statusText = (s: string) => (({ FORMING: '招募中', FULL: '已满员', SUBMITTED: '已提交', APPROVED: '已通过', REJECTED: '已驳回', DISMISSED: '已解散' } as Record<string, string>)[s] ?? s)

const fetchTeam = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: `/v1/team/${route.params.id}`, method: 'GET' })
    if (res.code === 0) team.value = res.data
  } finally {
    isLoading.value = false
  }
}

const handleQuit = async () => {
  try {
    const res: any = await request({ url: `/v1/team/${route.params.id}/quit`, method: 'DELETE' })
    if (res.code === 0) {
      ElMessage.success('已退出队伍')
      router.push('/teams')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

onMounted(fetchTeam)
</script>

<style scoped>
.page-wrap { min-height: 100vh; background: #fff; }
.nav { height: 56px; border-bottom: 1px solid #e0e0e0; display: flex; align-items: center; justify-content: space-between; padding: 0 40px; }
.nav-brand { font-size: 14px; font-weight: 800; letter-spacing: 1px; color: #111; cursor: pointer; }
.nav-item { font-size: 13px; color: #555; cursor: pointer; padding: 6px 12px; border-radius: 4px; }
.nav-item:hover { background: #f4f4f4; color: #111; }
.center-tip { text-align: center; padding: 80px; color: #aaa; }
.page-inner { max-width: 680px; margin: 0 auto; padding: 32px 20px; }
.detail-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
h2 { font-size: 20px; font-weight: 700; color: #111; margin: 0; }
.info-grid { display: flex; gap: 32px; flex-wrap: wrap; }
.info-block { min-width: 120px; }
.info-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.info-val { font-size: 14px; font-weight: 600; color: #111; }
</style>
