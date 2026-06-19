<template>
  <div>
    <div class="back-link" @click="router.push('/student/competitions')">← 返回竞赛列表</div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="!detail" class="center-tip">竞赛不存在或已下架</div>

    <div v-else class="page-inner">
      <!-- 标题区 -->
      <div class="detail-head">
        <div class="tags">
          <el-tag size="small" effect="plain">{{ typeMap[detail.type] }}</el-tag>
          <el-tag :type="statusTagType(detail.status)" size="small">{{ statusMap[detail.status] }}</el-tag>
        </div>
        <h1>{{ detail.title }}</h1>
        <div class="meta">
          <span>主办方：{{ detail.organizer }}</span>
          <span>发布人：{{ detail.createdByName }}</span>
        </div>
      </div>

      <el-divider />

      <!-- 时间和名额 -->
      <div class="info-grid">
        <div class="info-block">
          <div class="info-label">报名时间</div>
          <div class="info-val">{{ formatDate(detail.signupStart) }} — {{ formatDate(detail.signupEnd) }}</div>
        </div>
        <div v-if="detail.competitionStart" class="info-block">
          <div class="info-label">比赛时间</div>
          <div class="info-val">{{ formatDate(detail.competitionStart) }} — {{ formatDate(detail.competitionEnd ?? '') }}</div>
        </div>
        <div class="info-block">
          <div class="info-label">已报名</div>
          <div class="info-val">{{ detail.enrolledCount }} 人</div>
        </div>
        <div v-if="detail.hasQuota" class="info-block">
          <div class="info-label">剩余名额</div>
          <div class="info-val" :class="{ 'low': detail.remainingQuota !== undefined && detail.remainingQuota < 10 }">
            {{ detail.remainingQuota ?? '不限' }}
          </div>
        </div>
        <div v-if="detail.minTeamSize" class="info-block">
          <div class="info-label">队伍人数</div>
          <div class="info-val">{{ detail.minTeamSize }} — {{ detail.maxTeamSize }} 人</div>
        </div>
      </div>

      <el-divider />

      <!-- 简介 -->
      <div v-if="detail.description" class="section">
        <h3>竞赛简介</h3>
        <p class="desc-text">{{ detail.description }}</p>
      </div>

      <div v-if="detail.requirement" class="section">
        <h3>参赛要求</h3>
        <p class="desc-text">{{ detail.requirement }}</p>
      </div>

<!-- 报名按钮 -->
      <div class="action-area">
        <el-button
          v-if="detail.status === 'SIGNING'"
          type="primary"
          size="large"
          @click="router.push(`/student/competition/${detail.id}/signup`)"
        >
          立即报名
        </el-button>
        <span v-else class="signup-closed">{{ statusMap[detail.status] }}，暂不可报名</span>
      </div>

      <!-- 相关文档 -->
      <el-divider v-if="detail.id" />
      <div v-if="detail.id" class="section">
        <CompetitionDoc :competition-id="detail.id" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { competitionApi } from '@/api/competition'
import type { CompetitionDetailVO } from '@/types/competition'
import { statusMap, typeMap } from '@/types/competition'
import CompetitionDoc from '@/components/CompetitionDoc.vue'


const route = useRoute()
const router = useRouter()
const detail = ref<CompetitionDetailVO | null>(null)
const isLoading = ref(false)

const statusTagType = (status: string) => {
  const map: Record<string, any> = { SIGNING: 'success', UPCOMING: 'info', CLOSED: 'warning', ONGOING: '', FINISHED: 'danger' }
  return map[status] ?? 'info'
}

const formatDate = (str: string) => {
  if (!str) return '-'
  return new Date(str).toLocaleDateString('zh-CN')
}

onMounted(async () => {
  isLoading.value = true
  try {
    const res = await competitionApi.getById(Number(route.params.id))
    if (res.code === 0) detail.value = res.data
  } finally {
    isLoading.value = false
  }
})
</script>

<style scoped>
.back-link {
  font-size: 13px;
  color: #888;
  cursor: pointer;
  margin-bottom: 20px;
  display: inline-block;
}
.back-link:hover { color: #111; }

.center-tip { text-align: center; padding: 80px; color: #aaa; }

.detail-head { margin-bottom: 20px; }
.tags { display: flex; gap: 8px; margin-bottom: 12px; }
h1 { font-size: 24px; font-weight: 800; color: #111; margin: 0 0 12px; }
.meta { display: flex; gap: 24px; font-size: 13px; color: #888; }

.info-grid { display: flex; flex-wrap: wrap; gap: 20px; padding: 4px 0; }
.info-block { min-width: 160px; }
.info-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.info-val { font-size: 14px; font-weight: 600; color: #111; }
.info-val.low { color: #555; }

.section { margin: 20px 0; }
.section h3 { font-size: 15px; font-weight: 700; color: #111; margin-bottom: 10px; }
.desc-text { font-size: 14px; color: #555; line-height: 1.8; white-space: pre-wrap; }

.action-area { margin-top: 32px; }
.signup-closed { font-size: 14px; color: #aaa; }
</style>
