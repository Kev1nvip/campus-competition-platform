<template>
  <div>
    <div class="back-link" @click="router.push('/student/teams')">← 返回队伍</div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="!team" class="center-tip">队伍不存在</div>
    <div v-else>
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
      router.push('/student/teams')
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
.back-link { font-size: 13px; color: #888; cursor: pointer; margin-bottom: 20px; display: inline-block; }
.back-link:hover { color: #111; }
.center-tip { text-align: center; padding: 80px; color: #aaa; }
.detail-head { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
h2 { font-size: 20px; font-weight: 700; color: #111; margin: 0; }
.info-grid { display: flex; gap: 32px; flex-wrap: wrap; }
.info-block { min-width: 120px; }
.info-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.info-val { font-size: 14px; font-weight: 600; color: #111; }
</style>
