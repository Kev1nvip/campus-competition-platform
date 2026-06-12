<!--
  组件：竞赛详情页面
  说明：展示竞赛的详细信息，提供报名入口
-->
<template>
  <div class="competition-detail-container">
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!detail" class="empty-state">
      <div class="empty-icon">📋</div>
      <p>竞赛信息不存在</p>
    </div>

    <!-- 详情内容 -->
    <div v-else class="detail-content">
      <!-- 返回按钮 -->
      <button class="back-btn" @click="handleBack">
        ← 返回列表
      </button>

      <!-- 竞赛基本信息 -->
      <div class="detail-header">
        <div class="header-top">
          <span :class="['badge', 'badge-type']">{{ typeMap[detail.type] }}</span>
          <span :class="['badge', 'badge-status', getStatusClass(detail.status)]">
            {{ statusMap[detail.status] }}
          </span>
        </div>
        <h1 class="detail-title">{{ detail.title }}</h1>
        <div class="header-info">
          <span class="info-item">主办方：{{ detail.organizer }}</span>
          <span class="info-item">发布人：{{ detail.createdByName }}</span>
          <span class="info-item">发布时间：{{ formatDate(detail.createdAt) }}</span>
        </div>
      </div>

      <!-- 报名状态提示 -->
      <div v-if="detail.mySignupStatus" :class="['signup-status-banner', getSignupBannerClass(detail.mySignupStatus)]">
        <span class="banner-icon">{{ getSignupBannerIcon(detail.mySignupStatus) }}</span>
        <span class="banner-text">{{ getSignupStatusText(detail.mySignupStatus) }}</span>
      </div>

      <!-- 时间安排 -->
      <div class="detail-section">
        <h2 class="section-title">📅 时间安排</h2>
        <div class="time-grid">
          <div class="time-item">
            <div class="time-label">报名开始</div>
            <div class="time-value">{{ formatDate(detail.signupStart) }}</div>
          </div>
          <div class="time-item">
            <div class="time-label">报名截止</div>
            <div class="time-value">{{ formatDate(detail.signupEnd) }}</div>
          </div>
          <div v-if="detail.competitionStart" class="time-item">
            <div class="time-label">比赛开始</div>
            <div class="time-value">{{ formatDate(detail.competitionStart) }}</div>
          </div>
          <div v-if="detail.competitionEnd" class="time-item">
            <div class="time-label">比赛结束</div>
            <div class="time-value">{{ formatDate(detail.competitionEnd) }}</div>
          </div>
        </div>
      </div>

      <!-- 名额信息 -->
      <div class="detail-section">
        <h2 class="section-title">👥 名额信息</h2>
        <div class="quota-grid">
          <div class="quota-item">
            <div class="quota-label">已报名人数</div>
            <div class="quota-value">{{ detail.enrolledCount }} 人</div>
          </div>
          <div class="quota-item">
            <div class="quota-label">剩余名额</div>
            <div :class="['quota-value', detail.hasQuota && detail.remainingQuota !== undefined && detail.remainingQuota < 10 ? 'quota-low' : '']">
              {{ detail.hasQuota && detail.remainingQuota !== undefined ? detail.remainingQuota + ' 人' : '不限' }}
            </div>
          </div>
          <div v-if="detail.maxQuota" class="quota-item">
            <div class="quota-label">总名额</div>
            <div class="quota-value">{{ detail.maxQuota }} 人</div>
          </div>
          <div v-if="detail.type === 'TEAM' && detail.minTeamSize" class="quota-item">
            <div class="quota-label">团队人数要求</div>
            <div class="quota-value">{{ detail.minTeamSize }}-{{ detail.maxTeamSize }} 人</div>
          </div>
        </div>
        <!-- 名额进度条 -->
        <div v-if="detail.hasQuota && detail.maxQuota" class="quota-progress">
          <div class="progress-bar">
            <div 
              class="progress-fill" 
              :style="{ width: getQuotaPercentage(detail) + '%' }"
            ></div>
          </div>
          <div class="progress-text">
            已报名 {{ detail.enrolledCount }} / {{ detail.maxQuota }} ({{ getQuotaPercentage(detail) }}%)
          </div>
        </div>
      </div>

      <!-- 参赛要求 -->
      <div v-if="detail.requirement" class="detail-section">
        <h2 class="section-title">📋 参赛要求</h2>
        <div class="requirement-content">
          {{ detail.requirement }}
        </div>
      </div>

      <!-- 竞赛详情 -->
      <div v-if="detail.description" class="detail-section">
        <h2 class="section-title">📝 竞赛详情</h2>
        <div class="description-content">
          {{ detail.description }}
        </div>
      </div>

      <!-- 附件下载 -->
      <div v-if="detail.attachmentUrl" class="detail-section">
        <h2 class="section-title">📎 附件下载</h2>
        <a 
          :href="detail.attachmentUrl" 
          target="_blank" 
          class="attachment-link"
        >
          <span class="link-icon">📄</span>
          <span>下载竞赛附件</span>
        </a>
      </div>

      <!-- 报名按钮 -->
      <div class="detail-footer">
        <button 
          :class="['signup-btn', getSignupBtnClass(detail)]"
          :disabled="!canSignup(detail)"
          @click="handleSignup"
        >
          {{ getSignupBtnText(detail) }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { competitionApi } from '@/api/competition'
import type { CompetitionDetailVO, CompetitionStatus, CompetitionType } from '@/types/competition'
import { statusMap, typeMap } from '@/types/competition'

// ================================
// 路由
// ================================
const route = useRoute()
const router = useRouter()

// ================================
// 响应式数据
// ================================

// 竞赛详情
const detail = ref<CompetitionDetailVO | null>(null)

// 加载状态
const isLoading = ref(false)

// ================================
// 方法定义
// ================================

/**
 * 获取状态样式类
 * @param status 竞赛状态
 * @returns 样式类名
 */
const getStatusClass = (status: CompetitionStatus) => {
  switch (status) {
    case 'SIGNING':
      return 'status-signing'
    case 'UPCOMING':
      return 'status-upcoming'
    case 'CLOSED':
      return 'status-closed'
    case 'ONGOING':
      return 'status-ongoing'
    case 'FINISHED':
      return 'status-finished'
    default:
      return 'status-offline'
  }
}

/**
 * 获取报名状态横幅样式类
 * @param status 报名状态
 * @returns 样式类名
 */
const getSignupBannerClass = (status: string) => {
  switch (status) {
    case 'SIGNED':
      return 'banner-signed'
    case 'APPROVED':
      return 'banner-approved'
    case 'REJECTED':
      return 'banner-rejected'
    default:
      return ''
  }
}

/**
 * 获取报名状态图标
 * @param status 报名状态
 * @returns 图标
 */
const getSignupBannerIcon = (status: string) => {
  switch (status) {
    case 'SIGNED':
      return '📝'
    case 'APPROVED':
      return '✅'
    case 'REJECTED':
      return '❌'
    default:
      return ''
  }
}

/**
 * 获取报名状态文本
 * @param status 报名状态
 * @returns 状态文本
 */
const getSignupStatusText = (status: string) => {
  switch (status) {
    case 'SIGNED':
      return '您已报名，等待审核'
    case 'APPROVED':
      return '报名已通过审核'
    case 'REJECTED':
      return '报名未通过审核'
    default:
      return ''
  }
}

/**
 * 是否可以报名
 * @param data 竞赛详情
 * @returns 是否可报名
 */
const canSignup = (data: CompetitionDetailVO) => {
  return data.status === 'SIGNING' && data.mySignupStatus !== 'SIGNED'
}

/**
 * 获取报名按钮样式类
 * @param data 竞赛详情
 * @returns 样式类名
 */
const getSignupBtnClass = (data: CompetitionDetailVO) => {
  if (data.mySignupStatus === 'SIGNED') return 'btn-signed'
  if (data.mySignupStatus === 'APPROVED') return 'btn-approved'
  if (data.mySignupStatus === 'REJECTED') return 'btn-rejected'
  
  switch (data.status) {
    case 'SIGNING':
      return 'btn-primary'
    default:
      return 'btn-disabled'
  }
}

/**
 * 获取报名按钮文本
 * @param data 竞赛详情
 * @returns 按钮文本
 */
const getSignupBtnText = (data: CompetitionDetailVO) => {
  if (data.mySignupStatus === 'SIGNED') return '已报名'
  if (data.mySignupStatus === 'APPROVED') return '报名已通过'
  if (data.mySignupStatus === 'REJECTED') return '报名未通过'
  
  switch (data.status) {
    case 'SIGNING':
      return '立即报名'
    case 'UPCOMING':
      return '报名未开始'
    case 'CLOSED':
      return '报名已截止'
    case 'ONGOING':
      return '竞赛进行中'
    case 'FINISHED':
      return '竞赛已结束'
    default:
      return '无法报名'
  }
}

/**
 * 获取名额百分比
 * @param data 竞赛详情
 * @returns 百分比
 */
const getQuotaPercentage = (data: CompetitionDetailVO) => {
  if (!data.hasQuota || data.maxQuota === undefined) {
    return 0
  }
  return Math.round((data.enrolledCount / data.maxQuota) * 100)
}

/**
 * 格式化日期时间
 * @param dateStr ISO格式日期字符串
 * @returns 格式化后的日期时间
 */
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 获取竞赛详情
 */
const fetchDetail = async () => {
  const id = Number(route.params.id)
  if (isNaN(id)) {
    console.error('无效的竞赛ID')
    return
  }
  
  isLoading.value = true
  try {
    const response = await competitionApi.getById(id)
    if (response.code === 0) {
      detail.value = response.data
    } else {
      console.error('获取竞赛详情失败:', response.message)
    }
  } catch (error) {
    console.error('获取竞赛详情失败:', error)
  } finally {
    isLoading.value = false
  }
}

/**
 * 返回列表页面
 */
const handleBack = () => {
  router.push('/competitions')
}

/**
 * 处理报名
 */
const handleSignup = () => {
  if (!detail.value || !canSignup(detail.value)) return
  router.push(`/competition/${detail.value.id}/signup`)
}

// ================================
// 生命周期
// ================================
onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.competition-detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 30px 20px;
}

/* 加载状态 */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px;
  gap: 15px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px;
  gap: 15px;
}

.empty-icon {
  font-size: 48px;
}

.empty-state p {
  font-size: 16px;
  color: #666;
}

/* 返回按钮 */
.back-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  background: #f5f5f5;
  border: none;
  border-radius: 5px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  margin-bottom: 20px;
  transition: background 0.2s;
}

.back-btn:hover {
  background: #eee;
}

/* 详情头部 */
.detail-header {
  margin-bottom: 20px;
}

.header-top {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.badge-type {
  background: #e8f4fd;
  color: #1e88e5;
}

.badge-status {
  background: #f5f5f5;
  color: #666;
}

.status-signing {
  background: #e3f2fd;
  color: #1976d2;
}

.status-upcoming {
  background: #f3e5f5;
  color: #7b1fa2;
}

.status-closed {
  background: #fff3e0;
  color: #f57c00;
}

.status-ongoing {
  background: #e8f5e9;
  color: #388e3c;
}

.status-finished {
  background: #f5f5f5;
  color: #999;
}

.detail-title {
  font-size: 28px;
  color: #333;
  margin: 0 0 15px 0;
  line-height: 1.4;
}

.header-info {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.info-item {
  display: flex;
  align-items: center;
}

/* 报名状态横幅 */
.signup-status-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}

.banner-signed {
  background: #fff3e0;
  color: #f57c00;
}

.banner-approved {
  background: #e8f5e9;
  color: #388e3c;
}

.banner-rejected {
  background: #ffebee;
  color: #c62828;
}

.banner-icon {
  font-size: 18px;
}

.banner-text {
  font-weight: 500;
}

/* 详情区块 */
.detail-section {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 18px;
  color: #333;
  margin: 0 0 15px 0;
}

/* 时间网格 */
.time-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
}

.time-item {
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.time-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}

.time-value {
  font-size: 16px;
  color: #333;
  font-weight: 500;
}

/* 名额网格 */
.quota-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.quota-item {
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  text-align: center;
}

.quota-label {
  font-size: 14px;
  color: #999;
  margin-bottom: 8px;
}

.quota-value {
  font-size: 20px;
  color: #333;
  font-weight: 600;
}

.quota-low {
  color: #f57c00;
}

/* 名额进度条 */
.quota-progress {
  margin-top: 15px;
}

.progress-bar {
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 4px;
  transition: width 0.3s;
}

.progress-text {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
  text-align: right;
}

/* 内容区域 */
.requirement-content, .description-content {
  font-size: 15px;
  color: #555;
  line-height: 1.8;
  white-space: pre-wrap;
}

/* 附件链接 */
.attachment-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: #667eea;
  color: white;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  transition: opacity 0.2s;
}

.attachment-link:hover {
  opacity: 0.9;
}

.link-icon {
  font-size: 16px;
}

/* 底部报名按钮 */
.detail-footer {
  position: sticky;
  bottom: 0;
  padding: 20px 0;
  background: rgba(255, 255, 255, 0.95);
  border-top: 1px solid #f0f0f0;
  margin-top: 30px;
}

.signup-btn {
  display: block;
  width: 100%;
  padding: 15px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  opacity: 0.9;
}

.btn-disabled {
  background: #e0e0e0;
  color: #999;
  cursor: not-allowed;
}

.btn-signed {
  background: #fff3e0;
  color: #f57c00;
}

.btn-approved {
  background: #e8f5e9;
  color: #388e3c;
}

.btn-rejected {
  background: #ffebee;
  color: #c62828;
}
</style>