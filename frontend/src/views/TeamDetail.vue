<!--
  组件：队伍详情页面
  说明：提供队伍详情展示、队员管理、邀请队友等功能
-->
<template>
  <div class="team-detail-container">
    <!-- 返回按钮 -->
    <div class="back-btn-wrapper">
      <button class="back-btn" @click="goBack">
        <span class="back-icon">←</span>
        返回队伍列表
      </button>
    </div>

    <!-- 队伍信息卡片 -->
    <div class="team-info-card">
      <div class="card-header">
        <div class="team-name-wrapper">
          <h1 class="team-name">{{ teamDetail.teamName }}</h1>
          <span :class="['team-status', getStatusClass(teamDetail.status)]">
            {{ teamStatusMap[teamDetail.status] }}
          </span>
        </div>
      </div>
      
      <div class="card-body">
        <!-- 基本信息 -->
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">所属竞赛</span>
            <span class="info-value">{{ teamDetail.competitionTitle }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">队长</span>
            <span class="info-value">{{ teamDetail.leaderName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">指导老师</span>
            <span class="info-value">{{ teamDetail.teacherName || '未指定' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">队伍人数</span>
            <span class="info-value">{{ teamDetail.memberCount }} / {{ teamDetail.maxTeamSize ?? '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatDate(teamDetail.createdAt) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">更新时间</span>
            <span class="info-value">{{ formatDate(teamDetail.updatedAt) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 队员列表 -->
    <div class="members-section">
      <div class="section-header">
        <h2 class="section-title">队员列表</h2>
        <button 
          v-if="canInviteMember"
          class="invite-btn"
          @click="showInviteModal = true"
        >
          邀请队友
        </button>
      </div>
      
      <div class="members-list">
        <div 
          v-for="member in teamDetail.members" 
          :key="member.id" 
          class="member-card"
        >
          <div class="member-info">
            <div class="member-avatar">
              {{ member.studentName.charAt(0) }}
            </div>
            <div class="member-detail">
              <div class="member-name">
                {{ member.studentName }}
                <span v-if="member.role === 'LEADER'" class="leader-badge">队长</span>
              </div>
              <div class="member-meta">
                {{ member.studentNo }} · {{ member.department }}
              </div>
              <div class="member-joined">加入于 {{ formatDate(member.joinedAt) }}</div>
            </div>
          </div>
          
          <div class="member-actions">
            <button 
              v-if="isLeader && member.role !== 'LEADER'"
              class="action-btn remove-btn"
              @click="handleRemoveMember(member)"
            >
              移除
            </button>
            <button 
              v-if="!isLeader && member.role === 'MEMBER'"
              class="action-btn leave-btn"
              @click="handleLeaveTeam"
            >
              退出队伍
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 队伍操作 -->
    <div class="team-actions-section">
      <button 
        v-if="isLeader && teamDetail.status === 'FORMING'"
        class="action-btn primary-btn"
        @click="handleDismissTeam"
      >
        解散队伍
      </button>
    </div>

    <!-- 邀请队友弹窗 -->
    <div v-if="showInviteModal" class="modal-overlay" @click.self="showInviteModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>邀请队友</h3>
          <button class="close-btn" @click="showInviteModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-item">
            <label class="form-label">学号 <span class="required">*</span></label>
            <input
              v-model="inviteForm.studentNo"
              type="text"
              class="form-input"
              placeholder="请输入队友学号"
              maxlength="20"
            />
            <span v-if="inviteErrors.studentNo" class="error-message">{{ inviteErrors.studentNo }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-cancel" @click="showInviteModal = false">取消</button>
          <button 
            class="btn-confirm"
            :disabled="isInviting || !inviteForm.studentNo.trim()"
            @click="handleInviteMember"
          >
            {{ isInviting ? '邀请中...' : '发送邀请' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { teamApi } from '@/api/team'
import type { TeamDetailVO, TeamMemberVO, TeamStatus } from '@/types/team'
import { teamStatusMap } from '@/types/team'

// ================================
// 路由
// ================================
const route = useRoute()
const router = useRouter()

// ================================
// 状态
// ================================
const teamDetail = ref<TeamDetailVO>({
  id: 0,
  competitionId: 0,
  competitionTitle: '',
  teamName: '',
  leaderId: 0,
  leaderName: '',
  teacherId: undefined,
  teacherName: undefined,
  teacherConfirmed: false,
  memberCount: 0,
  minTeamSize: 1,
  maxTeamSize: 5,
  status: 'FORMING',
  createdAt: '',
  updatedAt: '',
  members: []
})

const isLoading = ref(false)
const showInviteModal = ref(false)
const isInviting = ref(false)

// 邀请表单
const inviteForm = ref({
  studentNo: ''
})

const inviteErrors = ref({
  studentNo: ''
})

// ================================
// 计算属性
// ================================
// 获取当前用户ID（从全局状态获取，这里假设当前用户ID为1）
const currentUserId = ref(1)

// 是否是队长
const isLeader = computed(() => {
  return teamDetail.value.leaderId === currentUserId.value
})

// 是否可以邀请队友
const canInviteMember = computed(() => {
  return isLeader.value && 
         teamDetail.value.status === 'FORMING' && 
         teamDetail.value.memberCount < (teamDetail.value.maxTeamSize ?? Infinity)
})

// ================================
// 方法
// ================================
/**
 * 获取状态样式类
 */
const getStatusClass = (status: TeamStatus) => {
  const classMap: Record<TeamStatus, string> = {
    FORMING: 'status-forming',
    FULL: 'status-full',
    SUBMITTED: 'status-submitted',
    APPROVED: 'status-approved',
    REJECTED: 'status-rejected',
    DISMISSED: 'status-dismissed'
  }
  return classMap[status]
}

/**
 * 格式化日期
 */
const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 返回队伍列表
 */
const goBack = () => {
  router.push('/teams')
}

/**
 * 获取队伍详情
 */
const fetchTeamDetail = async () => {
  const teamId = Number(route.params.id)
  if (!teamId) return
  
  isLoading.value = true
  try {
    const response = await teamApi.getTeamDetail(teamId)
    if (response.code === 0) {
      teamDetail.value = response.data
    }
  } catch (error) {
    console.error('获取队伍详情失败:', error)
  } finally {
    isLoading.value = false
  }
}

/**
 * 邀请队员
 */
const handleInviteMember = async () => {
  inviteErrors.value = { studentNo: '' }
  
  if (!inviteForm.value.studentNo.trim()) {
    inviteErrors.value.studentNo = '请输入学号'
    return
  }
  
  isInviting.value = true
  try {
    // 这里需要后端提供根据学号查询学生ID的接口
    // 暂时模拟一个学生ID
    const studentId = 2
    
    const response = await teamApi.inviteMember({
      teamId: teamDetail.value.id,
      studentId
    })
    
    if (response.code === 0) {
      alert('邀请已发送')
      showInviteModal.value = false
      inviteForm.value.studentNo = ''
      fetchTeamDetail()
    } else {
      alert(response.message || '邀请失败')
    }
  } catch (error) {
    console.error('邀请失败:', error)
    alert('邀请失败，请稍后重试')
  } finally {
    isInviting.value = false
  }
}

/**
 * 移除队员
 */
const handleRemoveMember = async (member: TeamMemberVO) => {
  if (!confirm(`确定要移除 ${member.studentName} 吗？`)) return
  
  try {
    const response = await teamApi.removeMember(teamDetail.value.id, member.studentId)
    if (response.code === 0) {
      alert('移除成功')
      fetchTeamDetail()
    } else {
      alert(response.message || '移除失败')
    }
  } catch (error) {
    console.error('移除队员失败:', error)
    alert('移除失败，请稍后重试')
  }
}

/**
 * 退出队伍
 */
const handleLeaveTeam = async () => {
  if (!confirm('确定要退出队伍吗？')) return
  
  try {
    const response = await teamApi.leaveTeam(teamDetail.value.id)
    if (response.code === 0) {
      alert('退出成功')
      router.push('/teams')
    } else {
      alert(response.message || '退出失败')
    }
  } catch (error) {
    console.error('退出队伍失败:', error)
    alert('退出失败，请稍后重试')
  }
}

/**
 * 解散队伍
 */
const handleDismissTeam = async () => {
  if (!confirm('确定要解散队伍吗？此操作不可撤销！')) return
  
  try {
    const response = await teamApi.dismissTeam(teamDetail.value.id)
    if (response.code === 0) {
      alert('队伍已解散')
      router.push('/teams')
    } else {
      alert(response.message || '解散失败')
    }
  } catch (error) {
    console.error('解散队伍失败:', error)
    alert('解散失败，请稍后重试')
  }
}

// ================================
// 生命周期
// ================================
onMounted(() => {
  fetchTeamDetail()
})
</script>

<style scoped>
.team-detail-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.back-btn-wrapper {
  margin-bottom: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  font-size: 14px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #eee;
  color: #333;
}

.back-icon {
  font-size: 16px;
}

.team-info-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  margin-bottom: 24px;
  overflow: hidden;
}

.card-header {
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.team-name-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.team-name {
  font-size: 24px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.team-status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
}

.status-forming {
  background: rgba(255, 255, 255, 0.25);
}

.status-full {
  background: #4CAF50;
}

.status-submitted {
  background: #FF9800;
}

.status-approved {
  background: #2196F3;
}

.status-rejected {
  background: #f44336;
}

.status-dismissed {
  background: #9E9E9E;
}

.card-body {
  padding: 24px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 13px;
  color: #999;
}

.info-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.members-section {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 24px;
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.invite-btn {
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 6px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: transform 0.2s;
}

.invite-btn:hover {
  transform: translateY(-1px);
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 8px;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.member-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
}

.leader-badge {
  padding: 2px 8px;
  background: #FF9800;
  color: #fff;
  font-size: 11px;
  border-radius: 4px;
}

.member-meta {
  font-size: 13px;
  color: #666;
}

.member-joined {
  font-size: 12px;
  color: #999;
}

.member-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.remove-btn {
  background: #fff;
  color: #f44336;
  border: 1px solid #f44336;
}

.remove-btn:hover {
  background: #fff5f5;
}

.leave-btn {
  background: #fff;
  color: #999;
  border: 1px solid #ddd;
}

.leave-btn:hover {
  background: #f5f5f5;
  color: #666;
}

.primary-btn {
  background: #f44336;
  color: #fff;
}

.primary-btn:hover {
  background: #d32f2f;
}

.team-actions-section {
  display: flex;
  justify-content: center;
  gap: 12px;
}

/* 弹窗样式 */
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

.modal-content {
  background: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.close-btn {
  width: 28px;
  height: 28px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 20px;
  color: #666;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #eee;
}

.btn-cancel {
  padding: 8px 20px;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  color: #666;
  font-size: 14px;
  cursor: pointer;
}

.btn-confirm {
  padding: 8px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 6px;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-item {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.required {
  color: #f44336;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.error-message {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #f44336;
}
</style>