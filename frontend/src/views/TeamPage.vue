<!--
  组件：组队页面
  说明：提供创建队伍、查看队伍状态、邀请队友等功能
-->
<template>
  <div class="team-page-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">我的队伍</h1>
      <p class="page-subtitle">管理你的队伍，邀请队友参加竞赛</p>
    </div>

    <!-- 创建队伍区域 -->
    <div class="create-team-section">
      <div class="create-team-card">
        <div class="card-header">
          <h2 class="card-title">创建新队伍</h2>
          <span class="card-hint">选择一个团队赛，创建你的队伍</span>
        </div>
        
        <div class="card-body">
          <!-- 选择竞赛 -->
          <div class="form-item">
            <label class="form-label">选择竞赛 <span class="required">*</span></label>
            <select v-model="createForm.competitionId" class="form-select" @change="onCompetitionChange">
              <option value="">请选择团队赛</option>
              <option v-for="comp in teamCompetitions" :key="comp.id" :value="comp.id">
                {{ comp.title }}
              </option>
            </select>
          </div>

          <!-- 队伍名称 -->
          <div class="form-item">
            <label class="form-label">队伍名称 <span class="required">*</span></label>
            <input
              v-model="createForm.teamName"
              type="text"
              class="form-input"
              placeholder="给你的队伍起个响亮的名字"
              maxlength="64"
            />
            <span v-if="errors.teamName" class="error-message">{{ errors.teamName }}</span>
          </div>

          <!-- 竞赛信息展示 -->
          <div v-if="selectedCompetition" class="competition-info-box">
            <div class="info-title">竞赛信息</div>
            <div class="info-item">
              <span class="info-label">竞赛名称：</span>
              <span class="info-value">{{ selectedCompetition.title }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">队伍人数：</span>
              <span class="info-value">{{ selectedCompetition.minTeamSize }} - {{ selectedCompetition.maxTeamSize }} 人</span>
            </div>
            <div class="info-item">
              <span class="info-label">报名截止：</span>
              <span class="info-value">{{ formatDate(selectedCompetition.signupEnd) }}</span>
            </div>
          </div>

          <!-- 创建按钮 -->
          <button
            class="create-btn"
            :disabled="isCreating || !canCreateTeam"
            @click="handleCreateTeam"
          >
            <span v-if="isCreating" class="loading-spinner"></span>
            {{ isCreating ? '创建中...' : '创建队伍' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 我的队伍列表 -->
    <div class="team-list-section">
      <div class="section-header">
        <h2 class="section-title">我的队伍</h2>
      </div>

      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="teams.length === 0" class="empty-state">
        <div class="empty-icon">👥</div>
        <p>你还没有创建或加入任何队伍</p>
        <p class="empty-hint">选择上面的竞赛创建你的第一个队伍吧！</p>
      </div>

      <!-- 队伍卡片列表 -->
      <div v-else class="team-grid">
        <div v-for="team in teams" :key="team.id" class="team-card">
          <div class="team-card-header">
            <h3 class="team-name">{{ team.teamName }}</h3>
            <span :class="['team-status', getStatusClass(team.status)]">
              {{ teamStatusMap[team.status] }}
            </span>
          </div>

          <div class="team-card-body">
            <div class="team-info">
              <div class="info-row">
                <span class="info-label">竞赛：</span>
                <span class="info-value">{{ team.competitionTitle }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">队长：</span>
                <span class="info-value">{{ team.leaderName }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">人数：</span>
                <span class="info-value">{{ team.memberCount }}{{ team.maxTeamSize ? `/${team.maxTeamSize}` : '' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">指导老师：</span>
                <span class="info-value">{{ team.teacherName || (team.teacherConfirmed ? '已确认' : '待确认') }}</span>
              </div>
            </div>
          </div>

          <div class="team-card-footer">
            <button class="view-btn" @click="handleViewTeam(team.id)">查看详情</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建成功弹窗 -->
    <div v-if="showSuccess" class="modal-overlay" @click="closeSuccess">
      <div class="success-modal" @click.stop>
        <div class="success-icon">🎉</div>
        <h3 class="success-title">队伍创建成功！</h3>
        <p class="success-message">你现在可以邀请队友加入你的队伍了</p>
        <button class="success-btn" @click="handleSuccessClose">确定</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { teamApi } from '@/api/team'
import { competitionApi } from '@/api/competition'
import type { TeamVO, TeamStatus } from '@/types/team'
import { teamStatusMap } from '@/types/team'
import type { CompetitionVO, CompetitionDetailVO } from '@/types/competition'

// ================================
// 路由
// ================================
const router = useRouter()

// ================================
// 响应式数据
// ================================

// 队伍列表
const teams = ref<TeamVO[]>([])

// 团队赛列表（用于选择）
const teamCompetitions = ref<CompetitionVO[]>([])

// 选中的竞赛详情
const selectedCompetition = ref<CompetitionDetailVO | null>(null)

// 加载状态
const isLoading = ref(false)
const isCreating = ref(false)

// 成功弹窗
const showSuccess = ref(false)

// 创建队伍表单
const createForm = reactive({
  competitionId: '' as number | string,
  teamName: ''
})

// 表单错误
const errors = reactive({
  teamName: ''
})

// ================================
// 计算属性
// ================================

// 是否可以创建队伍
const canCreateTeam = computed(() => {
  return createForm.competitionId && createForm.teamName.trim().length >= 2
})

// ================================
// 方法定义
// ================================

/**
 * 获取状态样式类
 * @param status 队伍状态
 * @returns 样式类名
 */
const getStatusClass = (status: TeamStatus) => {
  switch (status) {
    case 'FORMING':
      return 'status-forming'
    case 'FULL':
      return 'status-full'
    case 'SUBMITTED':
      return 'status-submitted'
    case 'APPROVED':
      return 'status-approved'
    case 'REJECTED':
      return 'status-rejected'
    case 'DISMISSED':
      return 'status-dismissed'
    default:
      return 'status-default'
  }
}

/**
 * 格式化日期时间
 * @param dateStr ISO格式日期字符串
 * @returns 格式化后的日期时间
 */
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

/**
 * 获取团队赛列表
 */
const fetchTeamCompetitions = async () => {
  try {
    const response = await competitionApi.getList({
      page: 1,
      size: 100,
      type: 'TEAM',
      status: 'SIGNING'
    })
    
    if (response.code === 0) {
      teamCompetitions.value = response.data.list
    }
  } catch (error) {
    console.error('获取团队赛列表失败:', error)
  }
}

/**
 * 获取我的队伍列表
 */
const fetchMyTeams = async () => {
  isLoading.value = true
  try {
    const response = await teamApi.getMyTeams({
      page: 1,
      size: 50
    })
    
    if (response.code === 0) {
      teams.value = response.data.list
    }
  } catch (error) {
    console.error('获取队伍列表失败:', error)
  } finally {
    isLoading.value = false
  }
}

/**
 * 选择竞赛变更
 */
const onCompetitionChange = async () => {
  if (createForm.competitionId) {
    try {
      const response = await competitionApi.getById(Number(createForm.competitionId))
      if (response.code === 0) {
        selectedCompetition.value = response.data
      }
    } catch (error) {
      console.error('获取竞赛详情失败:', error)
    }
  } else {
    selectedCompetition.value = null
  }
}

/**
 * 验证表单
 * @returns 是否验证通过
 */
const validateForm = (): boolean => {
  let isValid = true

  if (!createForm.teamName.trim()) {
    errors.teamName = '请输入队伍名称'
    isValid = false
  } else if (createForm.teamName.trim().length < 2) {
    errors.teamName = '队伍名称至少2个字符'
    isValid = false
  } else {
    errors.teamName = ''
  }

  return isValid
}

/**
 * 创建队伍
 */
const handleCreateTeam = async () => {
  if (!validateForm()) return

  isCreating.value = true
  try {
    const response = await teamApi.createTeam({
      competitionId: Number(createForm.competitionId),
      teamName: createForm.teamName.trim()
    })

    if (response.code === 0) {
      showSuccess.value = true
      // 重置表单
      createForm.competitionId = ''
      createForm.teamName = ''
      selectedCompetition.value = null
      // 刷新队伍列表
      await fetchMyTeams()
    } else {
      alert('创建队伍失败: ' + response.message)
    }
  } catch (error: any) {
    console.error('创建队伍失败:', error)
    alert('创建队伍失败: ' + (error.response?.data?.message || '网络错误'))
  } finally {
    isCreating.value = false
  }
}

/**
 * 查看队伍详情
 * @param teamId 队伍ID
 */
const handleViewTeam = (teamId: number) => {
  router.push(`/team/${teamId}`)
}

/**
 * 关闭成功弹窗
 */
const closeSuccess = () => {
  showSuccess.value = false
}

/**
 * 处理成功弹窗关闭
 */
const handleSuccessClose = () => {
  showSuccess.value = false
}

// ================================
// 生命周期
// ================================
onMounted(() => {
  fetchTeamCompetitions()
  fetchMyTeams()
})
</script>

<style scoped>
.team-page-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 30px 20px;
}

/* 页面标题 */
.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 32px;
  color: #333;
  margin: 0 0 10px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 创建队伍区域 */
.create-team-section {
  margin-bottom: 40px;
}

.create-team-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.card-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px 30px;
}

.card-title {
  font-size: 20px;
  color: white;
  margin: 0 0 5px 0;
}

.card-hint {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
}

.card-body {
  padding: 30px;
}

.form-item {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 8px;
}

.required {
  color: #f57c00;
}

.form-input,
.form-select {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #667eea;
}

.error-message {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #f44336;
}

.competition-info-box {
  background: #f8f9ff;
  border: 1px solid #e8e4ff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
}

.info-title {
  font-size: 14px;
  font-weight: 600;
  color: #667eea;
  margin-bottom: 12px;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  color: #999;
  min-width: 80px;
}

.info-value {
  color: #333;
  flex: 1;
}

.create-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.create-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.create-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 队伍列表区域 */
.team-list-section {
}

.section-header {
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  color: #333;
  margin: 0;
}

/* 加载状态 */
.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
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
  padding: 80px;
  gap: 10px;
  text-align: center;
}

.empty-icon {
  font-size: 64px;
}

.empty-state p {
  font-size: 16px;
  color: #666;
  margin: 0;
}

.empty-hint {
  font-size: 14px;
  color: #999;
}

/* 队伍卡片网格 */
.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.team-card {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.team-card-header {
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.team-name {
  font-size: 18px;
  color: #333;
  margin: 0;
  font-weight: 600;
}

.team-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.status-forming {
  background: #e3f2fd;
  color: #1976d2;
}

.status-full {
  background: #fff3e0;
  color: #f57c00;
}

.status-submitted {
  background: #f3e5f5;
  color: #7b1fa2;
}

.status-approved {
  background: #e8f5e9;
  color: #388e3c;
}

.status-rejected {
  background: #ffebee;
  color: #d32f2f;
}

.status-dismissed {
  background: #f5f5f5;
  color: #999;
}

.status-default {
  background: #f5f5f5;
  color: #666;
}

.team-card-body {
  padding: 20px;
}

.team-info {
}

.info-row {
  display: flex;
  margin-bottom: 10px;
  font-size: 14px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.team-card-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
}

.view-btn {
  width: 100%;
  padding: 10px;
  background: white;
  color: #667eea;
  border: 1px solid #667eea;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.view-btn:hover {
  background: #f8f9ff;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 成功弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.success-modal {
  background: white;
  border-radius: 16px;
  padding: 40px;
  text-align: center;
  max-width: 400px;
  width: 90%;
  animation: modalIn 0.3s ease;
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.success-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.success-title {
  font-size: 24px;
  color: #333;
  margin: 0 0 10px 0;
}

.success-message {
  font-size: 14px;
  color: #666;
  margin: 0 0 30px 0;
}

.success-btn {
  padding: 12px 40px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.success-btn:hover {
  opacity: 0.9;
}
</style>
