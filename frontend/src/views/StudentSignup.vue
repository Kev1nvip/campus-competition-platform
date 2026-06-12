<!--
  组件：学生个人赛报名页面
  说明：提供个人赛报名表单，包含选择指导老师功能，提交报名申请
-->
<template>
  <div class="signup-container">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="handleBack">
      ← 返回竞赛详情
    </button>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">🏆 个人赛报名</h1>
      <p class="page-subtitle">请填写报名信息，完成竞赛报名</p>
    </div>

    <!-- 竞赛信息卡片 -->
    <div class="competition-card" v-if="competition">
      <div class="card-header">
        <h3 class="card-title">{{ competition.title }}</h3>
        <span :class="['badge', getStatusClass(competition.status)]">
          {{ statusMap[competition.status] }}
        </span>
      </div>
      <div class="card-body">
        <div class="info-row">
          <span class="info-label">主办方：</span>
          <span class="info-value">{{ competition.organizer }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">报名时间：</span>
          <span class="info-value">{{ formatDate(competition.signupStart) }} 至 {{ formatDate(competition.signupEnd) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">剩余名额：</span>
          <span :class="['info-value', competition.hasQuota && competition.remainingQuota !== undefined && competition.remainingQuota < 10 ? 'quota-low' : '']">
            {{ competition.hasQuota && competition.remainingQuota !== undefined ? competition.remainingQuota + ' 人' : '不限' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 报名表单 -->
    <div class="form-container">
      <form @submit.prevent="handleSubmit" class="signup-form">
        <!-- 联系方式 -->
        <div class="form-section">
          <h2 class="section-title">📱 联系方式</h2>
          
          <div class="form-item">
            <label class="form-label">联系电话 <span class="required">*</span></label>
            <input
              v-model="form.phone"
              type="tel"
              class="form-input"
              placeholder="请输入手机号码"
              maxlength="11"
              @blur="validatePhone"
            />
            <span v-if="errors.phone" class="error-message">{{ errors.phone }}</span>
          </div>

          <div class="form-item">
            <label class="form-label">电子邮箱 <span class="required">*</span></label>
            <input
              v-model="form.email"
              type="email"
              class="form-input"
              placeholder="请输入电子邮箱"
              @blur="validateEmail"
            />
            <span v-if="errors.email" class="error-message">{{ errors.email }}</span>
          </div>
        </div>

        <!-- 选择指导老师 -->
        <div class="form-section">
          <h2 class="section-title">👨‍🏫 选择指导老师</h2>
          <p class="section-hint">若竞赛需要指导老师，请选择一位老师作为您的指导教师</p>

          <!-- 老师搜索 -->
          <div class="search-box">
            <input
              v-model="teacherSearch"
              type="text"
              class="search-input"
              placeholder="搜索老师姓名或院系..."
              @input="handleTeacherSearch"
            />
          </div>

          <!-- 老师列表 -->
          <div class="teacher-list" v-if="teachers.length > 0">
            <div
              v-for="teacher in teachers"
              :key="teacher.userId"
              :class="['teacher-item', selectedTeacher?.userId === teacher.userId ? 'selected' : '']"
              @click="selectTeacher(teacher)"
            >
              <div class="teacher-info">
                <div class="teacher-name">
                  {{ teacher.realName }}
                  <span class="teacher-title">{{ teacher.title }}</span>
                </div>
                <div class="teacher-department">{{ teacher.department }}</div>
                <div class="teacher-quota">
                  已带队 {{ teacher.currentTeamCount }} / {{ teacher.maxTeamQuota }} 队
                </div>
              </div>
              <div class="teacher-select">
                <span v-if="selectedTeacher?.userId === teacher.userId" class="check-icon">✓</span>
              </div>
            </div>
          </div>

          <!-- 空状态 -->
          <div v-else class="empty-state">
            <div class="empty-icon">👤</div>
            <p>{{ isTeacherLoading ? '加载中...' : '暂无可选老师' }}</p>
          </div>

          <!-- 是否需要老师提示 -->
          <div class="form-item">
            <label class="checkbox-label">
              <input
                v-model="form.needTeacher"
                type="checkbox"
                class="form-checkbox"
              />
              <span>不需要指导老师</span>
            </label>
          </div>
        </div>

        <!-- 备注信息 -->
        <div class="form-section">
          <h2 class="section-title">📝 备注信息</h2>
          <textarea
            v-model="form.remark"
            class="form-textarea"
            placeholder="请输入其他需要说明的信息（选填）"
            rows="3"
          ></textarea>
        </div>

        <!-- 提交按钮 -->
        <div class="form-footer">
          <button
            type="submit"
            :class="['submit-btn', isSubmitting ? 'btn-loading' : '']"
            :disabled="isSubmitting"
          >
            <span v-if="isSubmitting" class="loading-spinner"></span>
            {{ isSubmitting ? '提交中...' : '提交报名' }}
          </button>
        </div>
      </form>
    </div>

    <!-- 成功提示弹窗 -->
    <div v-if="showSuccess" class="modal-overlay" @click="closeSuccess">
      <div class="success-modal" @click.stop>
        <div class="success-icon">🎉</div>
        <h3 class="success-title">报名成功！</h3>
        <p class="success-message">您的报名信息已提交，请等待审核结果</p>
        <button class="success-btn" @click="handleSuccessClose">
          确定
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { competitionApi } from '@/api/competition'
import { signupApi } from '@/api/signup'
import type { CompetitionDetailVO, CompetitionStatus } from '@/types/competition'
import type { IndividualSignupRequest, TeacherInfo } from '@/types/signup'
import { statusMap } from '@/types/competition'

// ================================
// 路由
// ================================
const route = useRoute()
const router = useRouter()

// ================================
// 响应式数据
// ================================

// 竞赛详情
const competition = ref<CompetitionDetailVO | null>(null)

// 加载状态
const isLoading = ref(false)
const isTeacherLoading = ref(false)
const isSubmitting = ref(false)

// 成功弹窗
const showSuccess = ref(false)

// 老师列表
const teachers = ref<TeacherInfo[]>([])

// 老师搜索关键词
const teacherSearch = ref('')

// 选中的老师
const selectedTeacher = ref<TeacherInfo | null>(null)

// 表单数据
const form = reactive({
  phone: '',
  email: '',
  remark: '',
  needTeacher: false
})

// 表单错误
const errors = reactive({
  phone: '',
  email: ''
})

// ================================
// 计算属性
// ================================

// 是否可以提交
const canSubmit = computed(() => {
  return form.phone && form.email && (form.needTeacher || selectedTeacher.value)
})

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
 * 验证手机号
 */
const validatePhone = () => {
  const phone = form.phone
  const phoneRegex = /^1[3-9]\d{9}$/
  if (!phone) {
    errors.phone = '请输入手机号码'
  } else if (!phoneRegex.test(phone)) {
    errors.phone = '请输入正确的手机号码'
  } else {
    errors.phone = ''
  }
}

/**
 * 验证邮箱
 */
const validateEmail = () => {
  const email = form.email
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!email) {
    errors.email = '请输入电子邮箱'
  } else if (!emailRegex.test(email)) {
    errors.email = '请输入正确的电子邮箱格式'
  } else {
    errors.email = ''
  }
}

/**
 * 获取竞赛详情
 */
const fetchCompetition = async () => {
  const id = Number(route.params.id)
  if (isNaN(id)) {
    console.error('无效的竞赛ID')
    return
  }

  isLoading.value = true
  try {
    const response = await competitionApi.getById(id)
    if (response.code === 0) {
      competition.value = response.data
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
 * 获取老师列表
 */
const fetchTeachers = async () => {
  isTeacherLoading.value = true
  try {
    const response = await signupApi.getAvailableTeachers({
      page: 1,
      size: 50,
      keyword: teacherSearch.value || undefined
    })
    if (response.code === 0) {
      teachers.value = response.data.list
    } else {
      console.error('获取老师列表失败:', response.message)
    }
  } catch (error) {
    console.error('获取老师列表失败:', error)
  } finally {
    isTeacherLoading.value = false
  }
}

/**
 * 搜索老师
 */
const handleTeacherSearch = () => {
  fetchTeachers()
}

/**
 * 选择老师
 * @param teacher 老师信息
 */
const selectTeacher = (teacher: TeacherInfo) => {
  if (selectedTeacher.value?.userId === teacher.userId) {
    selectedTeacher.value = null
  } else {
    selectedTeacher.value = teacher
  }
}

/**
 * 表单验证
 * @returns 是否验证通过
 */
const validateForm = (): boolean => {
  let isValid = true
  
  // 验证手机号
  validatePhone()
  if (errors.phone) isValid = false
  
  // 验证邮箱
  validateEmail()
  if (errors.email) isValid = false
  
  // 验证老师选择
  if (!form.needTeacher && !selectedTeacher.value) {
    alert('请选择指导老师，或勾选"不需要指导老师"')
    isValid = false
  }
  
  return isValid
}

/**
 * 提交报名
 */
const handleSubmit = async () => {
  if (!validateForm()) return
  
  isSubmitting.value = true
  
  try {
    const requestData: IndividualSignupRequest = {
      competitionId: Number(route.params.id),
      teacherId: selectedTeacher.value?.userId,
      phone: form.phone,
      email: form.email,
      remark: form.remark || undefined
    }
    
    const response = await signupApi.individualSignup(requestData)
    
    if (response.code === 0) {
      showSuccess.value = true
    } else {
      alert('报名失败：' + response.message)
    }
  } catch (error: any) {
    console.error('报名失败:', error)
    alert('报名失败：' + (error.response?.data?.message || '网络错误'))
  } finally {
    isSubmitting.value = false
  }
}

/**
 * 返回竞赛详情页
 */
const handleBack = () => {
  router.push(`/competition/${route.params.id}`)
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
  // 跳转回竞赛详情页
  router.push(`/competition/${route.params.id}`)
}

// ================================
// 生命周期
// ================================
onMounted(() => {
  fetchCompetition()
  fetchTeachers()
})
</script>

<style scoped>
.signup-container {
  max-width: 700px;
  margin: 0 auto;
  padding: 30px 20px;
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

/* 页面头部 */
.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 28px;
  color: #333;
  margin: 0 0 10px 0;
}

.page-subtitle {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 竞赛信息卡片 */
.competition-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.card-title {
  font-size: 20px;
  color: #333;
  margin: 0;
  flex: 1;
  margin-right: 15px;
}

.badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
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

.card-body {
  border-top: 1px solid #f0f0f0;
  padding-top: 15px;
}

.info-row {
  display: flex;
  margin-bottom: 10px;
}

.info-row:last-child {
  margin-bottom: 0;
}

.info-label {
  font-size: 14px;
  color: #999;
  width: 100px;
}

.info-value {
  font-size: 14px;
  color: #333;
}

.quota-low {
  color: #f57c00;
  font-weight: 500;
}

/* 表单容器 */
.form-container {
  background: #fff;
  border-radius: 10px;
  padding: 30px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.signup-form {
  display: flex;
  flex-direction: column;
  gap: 25px;
}

/* 表单区块 */
.form-section {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.section-title {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.section-hint {
  font-size: 14px;
  color: #999;
  margin: 0;
}

/* 表单项目 */
.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.required {
  color: #f57c00;
}

.form-input {
  padding: 12px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.form-textarea {
  padding: 12px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  resize: none;
  transition: border-color 0.2s;
}

.form-textarea:focus {
  outline: none;
  border-color: #667eea;
}

.error-message {
  font-size: 12px;
  color: #f44336;
}

/* 搜索框 */
.search-box {
  position: relative;
}

.search-input {
  width: 100%;
  padding: 12px 15px 12px 40px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: #f8f9fa;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%23999' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Ccircle cx='11' cy='11' r='8'%3E%3C/circle%3E%3Cpath d='m21 21-4.35-4.35'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: 12px center;
}

.search-input:focus {
  outline: none;
  border-color: #667eea;
  background-color: #fff;
}

/* 老师列表 */
.teacher-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 300px;
  overflow-y: auto;
}

.teacher-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.teacher-item:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.teacher-item.selected {
  border-color: #667eea;
  background: #f0f4ff;
}

.teacher-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.teacher-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.teacher-title {
  font-size: 12px;
  color: #667eea;
  padding: 2px 8px;
  background: #e8f0fe;
  border-radius: 4px;
}

.teacher-department {
  font-size: 14px;
  color: #666;
}

.teacher-quota {
  font-size: 12px;
  color: #999;
}

.teacher-select {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.teacher-item.selected .teacher-select {
  background: #667eea;
}

.check-icon {
  color: white;
  font-size: 14px;
  font-weight: bold;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px;
  gap: 10px;
}

.empty-icon {
  font-size: 48px;
}

.empty-state p {
  font-size: 14px;
  color: #999;
}

/* 复选框 */
.checkbox-label {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.form-checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

/* 表单底部 */
.form-footer {
  margin-top: 10px;
}

.submit-btn {
  width: 100%;
  padding: 15px;
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

.submit-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-loading {
  opacity: 0.8;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top: 2px solid white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
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