<!--
  组件：学生个人中心页面
  说明：展示个人信息、参赛记录、获奖记录，支持上传证书
-->
<template>
  <div class="profile-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">👤 个人中心</h1>
      <p class="page-subtitle">管理您的个人信息、参赛记录和获奖记录</p>
    </div>

    <!-- 选项卡导航 -->
    <div class="tab-nav">
      <button
        :class="['tab-btn', activeTab === 'profile' ? 'active' : '']"
        @click="activeTab = 'profile'"
      >
        个人信息
      </button>
      <button
        :class="['tab-btn', activeTab === 'signups' ? 'active' : '']"
        @click="activeTab = 'signups'"
      >
        参赛记录
      </button>
      <button
        :class="['tab-btn', activeTab === 'awards' ? 'active' : '']"
        @click="activeTab = 'awards'"
      >
        获奖记录
      </button>
    </div>

    <!-- 个人信息 -->
    <div v-if="activeTab === 'profile'" class="tab-content">
      <div class="profile-card">
        <div class="card-header">
          <h2 class="card-title">基本信息</h2>
          <button class="edit-btn" @click="isEditing = !isEditing">
            {{ isEditing ? '取消编辑' : '编辑资料' }}
          </button>
        </div>

        <div class="card-body">
          <div class="profile-row">
            <div class="profile-label">姓名</div>
            <div class="profile-value">
              <input
                v-if="isEditing"
                v-model="profileForm.realName"
                type="text"
                class="edit-input"
                placeholder="请输入姓名"
              />
              <span v-else>{{ profile.realName || '-' }}</span>
            </div>
          </div>

          <div class="profile-row">
            <div class="profile-label">学号</div>
            <div class="profile-value">{{ profile.studentNo || '-' }}</div>
          </div>

          <div class="profile-row">
            <div class="profile-label">院系</div>
            <div class="profile-value">
              <input
                v-if="isEditing"
                v-model="profileForm.department"
                type="text"
                class="edit-input"
                placeholder="请输入院系"
              />
              <span v-else>{{ profile.department || '-' }}</span>
            </div>
          </div>

          <div class="profile-row">
            <div class="profile-label">手机号</div>
            <div class="profile-value">
              <input
                v-if="isEditing"
                v-model="profileForm.phone"
                type="tel"
                class="edit-input"
                placeholder="请输入手机号"
              />
              <span v-else>{{ profile.phone || '-' }}</span>
            </div>
          </div>

          <div class="profile-row">
            <div class="profile-label">邮箱</div>
            <div class="profile-value">
              <input
                v-if="isEditing"
                v-model="profileForm.email"
                type="email"
                class="edit-input"
                placeholder="请输入邮箱"
              />
              <span v-else>{{ profile.email || '-' }}</span>
            </div>
          </div>

          <div v-if="isEditing" class="profile-actions">
            <button class="save-btn" @click="handleSaveProfile" :disabled="isSaving">
              {{ isSaving ? '保存中...' : '保存' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 修改密码 -->
      <div class="profile-card">
        <div class="card-header">
          <h2 class="card-title">修改密码</h2>
        </div>

        <div class="card-body">
          <div class="form-item">
            <label class="form-label">原密码</label>
            <input
              v-model="passwordForm.oldPassword"
              type="password"
              class="form-input"
              placeholder="请输入原密码"
            />
          </div>

          <div class="form-item">
            <label class="form-label">新密码</label>
            <input
              v-model="passwordForm.newPassword"
              type="password"
              class="form-input"
              placeholder="请输入新密码"
            />
          </div>

          <div class="form-item">
            <label class="form-label">确认新密码</label>
            <input
              v-model="passwordForm.confirmPassword"
              type="password"
              class="form-input"
              placeholder="请再次输入新密码"
            />
          </div>

          <button class="submit-btn" @click="handleChangePassword" :disabled="isChangingPassword">
            {{ isChangingPassword ? '修改中...' : '修改密码' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 参赛记录 -->
    <div v-if="activeTab === 'signups'" class="tab-content">
      <div class="table-container">
        <table class="data-table" v-if="signups.length > 0">
          <thead>
            <tr>
              <th>竞赛名称</th>
              <th>竞赛类型</th>
              <th>状态</th>
              <th>报名时间</th>
              <th>指导老师</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="signup in signups" :key="signup.id">
              <td class="text-left">{{ signup.competitionTitle }}</td>
              <td>个人赛</td>
              <td>
                <span :class="['status-badge', getStatusClass(signup.status)]">
                  {{ signupStatusMap[signup.status as keyof typeof signupStatusMap] }}
                </span>
              </td>
              <td>{{ formatDate(signup.createdAt) }}</td>
              <td>{{ signup.teacherName || '-' }}</td>
              <td>
                <button class="action-btn" @click="viewSignupDetail(signup.competitionId)">
                  查看详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-else class="empty-state">
          <div class="empty-icon">📝</div>
          <p>暂无参赛记录</p>
          <button class="primary-btn" @click="$router.push('/competitions')">
            浏览竞赛
          </button>
        </div>
      </div>
    </div>

    <!-- 获奖记录 -->
    <div v-if="activeTab === 'awards'" class="tab-content">
      <div class="awards-header">
        <h2 class="section-title">获奖记录</h2>
        <button class="primary-btn" @click="showUploadModal = true">
          ➕ 上传证书
        </button>
      </div>

      <div class="award-list" v-if="awards.length > 0">
        <div
          v-for="award in awards"
          :key="award.id"
          class="award-card"
        >
          <div class="award-header">
            <div class="award-info">
              <h3 class="award-name">{{ award.awardName }}</h3>
              <p class="award-competition">{{ award.competitionName || '竞赛名称' }}</p>
            </div>
            <span :class="['status-badge', getAwardStatusClass(award.status)]">
              {{ awardStatusMap[award.status] }}
            </span>
          </div>

          <div class="award-body">
            <div class="award-row">
              <span class="award-label">奖项等级：</span>
              <span class="award-value">{{ awardLevelMap[award.awardLevel] }}</span>
            </div>
            <div class="award-row">
              <span class="award-label">获奖时间：</span>
              <span class="award-value">{{ formatDate(award.awardDate) }}</span>
            </div>
            <div class="award-row">
              <span class="award-label">提交时间：</span>
              <span class="award-value">{{ formatDate(award.createdAt) }}</span>
            </div>
          </div>

          <div class="award-footer">
            <a
              v-if="award.certificateUrl"
              :href="award.certificateUrl"
              target="_blank"
              class="view-cert-btn"
            >
              📄 查看证书
            </a>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">🏆</div>
        <p>暂无获奖记录</p>
        <button class="primary-btn" @click="showUploadModal = true">
          上传第一份证书
        </button>
      </div>
    </div>

    <!-- 上传证书弹窗 -->
    <div v-if="showUploadModal" class="modal-overlay" @click="showUploadModal = false">
      <div class="upload-modal" @click.stop>
        <div class="modal-header">
          <h3 class="modal-title">📤 上传获奖证书</h3>
          <button class="close-btn" @click="showUploadModal = false">×</button>
        </div>

        <form @submit.prevent="handleUploadCertificate" class="upload-form">
          <div class="form-item">
            <label class="form-label">选择竞赛 <span class="required">*</span></label>
            <select v-model="uploadForm.competitionId" class="form-select" required>
              <option value="">请选择竞赛</option>
              <option
                v-for="comp in myCompetitions"
                :key="comp.id"
                :value="comp.id"
              >
                {{ comp.title }}
              </option>
            </select>
          </div>

          <div class="form-item">
            <label class="form-label">奖项等级 <span class="required">*</span></label>
            <select v-model="uploadForm.awardLevel" class="form-select" required>
              <option value="">请选择奖项等级</option>
              <option value="NATIONAL_FIRST">国家级一等奖</option>
              <option value="NATIONAL_SECOND">国家级二等奖</option>
              <option value="NATIONAL_THIRD">国家级三等奖</option>
              <option value="PROVINCIAL_FIRST">省级一等奖</option>
              <option value="PROVINCIAL_SECOND">省级二等奖</option>
              <option value="PROVINCIAL_THIRD">省级三等奖</option>
              <option value="OTHER">其他奖项</option>
            </select>
          </div>

          <div class="form-item">
            <label class="form-label">奖项名称 <span class="required">*</span></label>
            <input
              v-model="uploadForm.awardName"
              type="text"
              class="form-input"
              placeholder="例如：全国大学生数学建模竞赛一等奖"
              required
            />
          </div>

          <div class="form-item">
            <label class="form-label">获奖时间 <span class="required">*</span></label>
            <input
              v-model="uploadForm.awardDate"
              type="date"
              class="form-input"
              required
            />
          </div>

          <div class="form-item">
            <label class="form-label">证书图片 <span class="required">*</span></label>
            <div class="upload-area" @click="triggerFileInput">
              <input
                ref="fileInput"
                type="file"
                accept="image/*"
                class="file-input"
                @change="handleFileChange"
              />
              <div v-if="!uploadForm.file" class="upload-placeholder">
                <span class="upload-icon">📁</span>
                <p>点击选择证书图片</p>
                <p class="upload-hint">支持 JPG、PNG 格式，不超过 5MB</p>
              </div>
              <div v-else class="upload-preview">
                <img :src="previewUrl" alt="证书预览" class="preview-image" />
                <button type="button" class="remove-file-btn" @click.stop="removeFile">
                  ×
                </button>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button type="button" class="cancel-btn" @click="showUploadModal = false">
              取消
            </button>
            <button type="submit" class="submit-btn" :disabled="isUploading">
              {{ isUploading ? '上传中...' : '提交' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userApi } from '@/api/user'
import { signupApi } from '@/api/signup'
import { awardApi } from '@/api/award'
import type { StudentProfile } from '@/types/profile'
import type { AwardRecordVO, AwardLevel } from '@/types/award'
import type { CompetitionVO } from '@/types/competition'
import { awardLevelMap, awardStatusMap } from '@/types/award'
import type { IndividualSignupVO } from '@/types/signup'
import { signupStatusMap } from '@/types/signup'

// ================================
// 路由
// ================================
const router = useRouter()

// ================================
// 响应式数据
// ================================

// 当前激活的选项卡
const activeTab = ref<'profile' | 'signups' | 'awards'>('profile')

// 个人信息
const profile = ref<StudentProfile>({
  userId: 0,
  username: '',
  realName: '',
  studentNo: '',
  department: '',
  phone: '',
  email: '',
  role: 'STUDENT'
})

// 编辑表单
const profileForm = reactive({
  realName: '',
  phone: '',
  email: '',
  department: ''
})

// 是否正在编辑
const isEditing = ref(false)

// 是否正在保存
const isSaving = ref(false)

// 密码修改表单
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 是否正在修改密码
const isChangingPassword = ref(false)

// 参赛记录
const signups = ref<IndividualSignupVO[]>([])

// 获奖记录
const awards = ref<AwardRecordVO[]>([])

// 我的竞赛列表（用于上传证书时选择）
const myCompetitions = ref<CompetitionVO[]>([])

// 上传证书弹窗
const showUploadModal = ref(false)

// 上传表单
const uploadForm = reactive({
  competitionId: 0,
  awardLevel: '' as AwardLevel,
  awardName: '',
  awardDate: '',
  file: null as File | null
})

// 文件输入引用
const fileInput = ref<HTMLInputElement | null>(null)

// 预览 URL
const previewUrl = ref('')

// 是否正在上传
const isUploading = ref(false)

// ================================
// 方法定义
// ================================

/**
 * 获取个人信息
 */
const fetchProfile = async () => {
  try {
    const response = await userApi.getCurrentUser()
    if (response.code === 0 && response.data) {
      profile.value = response.data
      // 同步到编辑表单
      profileForm.realName = response.data.realName || ''
      profileForm.phone = response.data.phone || ''
      profileForm.email = response.data.email || ''
      profileForm.department = response.data.department || ''
    }
  } catch (error) {
    console.error('获取个人信息失败:', error)
  }
}

/**
 * 获取参赛记录
 */
const fetchSignups = async () => {
  try {
    const response = await signupApi.getMyIndividualSignups({
      page: 1,
      size: 50
    })
    if (response.code === 0 && response.data) {
      signups.value = response.data.list || []
    }
  } catch (error) {
    console.error('获取参赛记录失败:', error)
  }
}

/**
 * 获取获奖记录
 */
const fetchAwards = async () => {
  try {
    const response = await awardApi.getMyAwards({
      page: 1,
      size: 50
    })
    if (response.code === 0 && response.data) {
      awards.value = response.data.list || []
    }
  } catch (error) {
    console.error('获取获奖记录失败:', error)
  }
}

/**
 * 获取我的竞赛列表（用于上传证书）
 */
const fetchMyCompetitions = async () => {
  try {
    // 这里简化处理，实际应该有专门的接口获取我的竞赛
    myCompetitions.value = []
  } catch (error) {
    console.error('获取竞赛列表失败:', error)
  }
}

/**
 * 保存个人信息
 */
const handleSaveProfile = async () => {
  isSaving.value = true
  try {
    const response = await userApi.updateProfile({
      realName: profileForm.realName,
      phone: profileForm.phone,
      email: profileForm.email,
      department: profileForm.department
    })
    
    if (response.code === 0) {
      alert('保存成功')
      isEditing.value = false
      fetchProfile()
    } else {
      alert('保存失败：' + response.message)
    }
  } catch (error: any) {
    console.error('保存失败:', error)
    alert('保存失败：' + (error.response?.data?.message || '网络错误'))
  } finally {
    isSaving.value = false
  }
}

/**
 * 修改密码
 */
const handleChangePassword = async () => {
  // 验证密码
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    alert('请填写完整的密码信息')
    return
  }
  
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    alert('两次输入的新密码不一致')
    return
  }
  
  if (passwordForm.newPassword.length < 6) {
    alert('密码长度不能少于 6 位')
    return
  }
  
  isChangingPassword.value = true
  try {
    const response = await userApi.updatePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    
    if (response.code === 0) {
      alert('密码修改成功，请重新登录')
      localStorage.removeItem('token')
      router.push('/login')
    } else {
      alert('修改失败：' + response.message)
    }
  } catch (error: any) {
    console.error('修改密码失败:', error)
    alert('修改失败：' + (error.response?.data?.message || '网络错误'))
  } finally {
    isChangingPassword.value = false
  }
}

/**
 * 查看报名详情
 */
const viewSignupDetail = (competitionId: number) => {
  router.push(`/competition/${competitionId}`)
}

/**
 * 获取状态样式类
 */
const getStatusClass = (status: string) => {
  // 报名状态
  if (['SIGNED', 'APPROVED', 'REJECTED', 'CANCELLED'].includes(status)) {
    switch (status) {
      case 'SIGNED':
        return 'status-pending'
      case 'APPROVED':
        return 'status-approved'
      case 'REJECTED':
        return 'status-rejected'
      case 'CANCELLED':
        return 'status-offline'
      default:
        return 'status-offline'
    }
  }
  
  // 竞赛状态
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
 * 获取获奖状态样式类
 */
const getAwardStatusClass = (status: string) => {
  switch (status) {
    case 'PENDING':
      return 'status-pending'
    case 'APPROVED':
      return 'status-approved'
    case 'REJECTED':
      return 'status-rejected'
    default:
      return 'status-offline'
  }
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
    day: '2-digit'
  })
}

/**
 * 触发文件选择
 */
const triggerFileInput = () => {
  fileInput.value?.click()
}

/**
 * 处理文件选择
 */
const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  
  // 验证文件大小（5MB）
  if (file.size > 5 * 1024 * 1024) {
    alert('文件大小不能超过 5MB')
    target.value = ''
    return
  }
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    alert('请上传图片文件')
    target.value = ''
    return
  }
  
  uploadForm.file = file
  
  // 生成预览 URL
  const reader = new FileReader()
  reader.onload = (e) => {
    previewUrl.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

/**
 * 移除文件
 */
const removeFile = () => {
  uploadForm.file = null
  previewUrl.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

/**
 * 上传证书
 */
const handleUploadCertificate = async () => {
  // 验证必填项
  if (!uploadForm.competitionId || !uploadForm.awardLevel || !uploadForm.awardName || !uploadForm.awardDate || !uploadForm.file) {
    alert('请填写完整的获奖信息')
    return
  }
  
  isUploading.value = true
  try {
    // 1. 上传证书图片
    const uploadResponse = await awardApi.uploadCertificate(uploadForm.file)
    
    if (uploadResponse.code !== 0) {
      alert('上传失败：' + uploadResponse.message)
      return
    }
    
    // 2. 提交获奖记录
    const submitResponse = await awardApi.submitAward({
      competitionId: uploadForm.competitionId,
      bizType: 'INDIVIDUAL',
      bizId: 0, // 实际应该有报名记录 ID
      awardLevel: uploadForm.awardLevel,
      awardName: uploadForm.awardName,
      certificateUrl: uploadResponse.data?.url || '',
      awardDate: uploadForm.awardDate
    })
    
    if (submitResponse.code === 0) {
      alert('上传成功，请等待审核')
      showUploadModal.value = false
      resetUploadForm()
      fetchAwards()
    } else {
      alert('提交失败：' + submitResponse.message)
    }
  } catch (error: any) {
    console.error('上传失败:', error)
    alert('上传失败：' + (error.response?.data?.message || '网络错误'))
  } finally {
    isUploading.value = false
  }
}

/**
 * 重置上传表单
 */
const resetUploadForm = () => {
  uploadForm.competitionId = 0
  uploadForm.awardLevel = '' as AwardLevel
  uploadForm.awardName = ''
  uploadForm.awardDate = ''
  uploadForm.file = null
  previewUrl.value = ''
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// ================================
// 生命周期
// ================================
onMounted(() => {
  fetchProfile()
  fetchSignups()
  fetchAwards()
  fetchMyCompetitions()
})
</script>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 30px 20px;
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

/* 选项卡导航 */
.tab-nav {
  display: flex;
  gap: 10px;
  margin-bottom: 25px;
  border-bottom: 2px solid #f0f0f0;
}

.tab-btn {
  padding: 12px 24px;
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  font-size: 15px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: -2px;
}

.tab-btn:hover {
  color: #667eea;
}

.tab-btn.active {
  color: #667eea;
  border-bottom-color: #667eea;
  font-weight: 600;
}

/* 选项卡内容 */
.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 信息卡片 */
.profile-card {
  background: #fff;
  border-radius: 10px;
  padding: 25px;
  margin-bottom: 25px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.card-title {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.edit-btn {
  padding: 8px 16px;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-btn:hover {
  background: #e8e8e8;
}

/* 信息行 */
.profile-row {
  display: flex;
  padding: 15px 0;
  border-bottom: 1px solid #f5f5f5;
}

.profile-row:last-child {
  border-bottom: none;
}

.profile-label {
  width: 100px;
  font-size: 14px;
  color: #999;
}

.profile-value {
  flex: 1;
  font-size: 14px;
  color: #333;
}

.edit-input {
  width: 100%;
  max-width: 300px;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.edit-input:focus {
  outline: none;
  border-color: #667eea;
}

.profile-actions {
  padding-top: 15px;
}

.save-btn {
  padding: 10px 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.save-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 表单样式 */
.form-item {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #666;
  font-weight: 500;
}

.required {
  color: #f57c00;
  margin-left: 3px;
}

.form-input {
  width: 100%;
  max-width: 400px;
  padding: 10px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
}

.form-select {
  width: 100%;
  max-width: 400px;
  padding: 10px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 14px;
  background: white;
  cursor: pointer;
  transition: border-color 0.2s;
}

.form-select:focus {
  outline: none;
  border-color: #667eea;
}

.submit-btn {
  padding: 12px 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.submit-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 表格样式 */
.table-container {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #f5f5f5;
}

.data-table th {
  font-size: 14px;
  color: #666;
  font-weight: 600;
  background: #f8f9fa;
}

.data-table td {
  font-size: 14px;
  color: #333;
}

.text-left {
  text-align: left;
}

/* 状态徽章 */
.status-badge {
  display: inline-block;
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

.status-pending {
  background: #fff3e0;
  color: #f57c00;
}

.status-approved {
  background: #e8f5e9;
  color: #388e3c;
}

.status-rejected {
  background: #ffebee;
  color: #d32f2f;
}

/* 操作按钮 */
.action-btn {
  padding: 6px 12px;
  background: #f5f5f5;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #e8e8e8;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  gap: 15px;
}

.empty-icon {
  font-size: 64px;
}

.empty-state p {
  font-size: 14px;
  color: #999;
}

.primary-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.primary-btn:hover {
  opacity: 0.9;
}

/* 获奖记录 */
.awards-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 18px;
  color: #333;
  margin: 0;
}

.award-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.award-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;
}

.award-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.award-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.award-info {
  flex: 1;
}

.award-name {
  font-size: 18px;
  color: #333;
  margin: 0 0 8px 0;
}

.award-competition {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.award-body {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 15px;
}

.award-row {
  display: flex;
  gap: 8px;
}

.award-label {
  font-size: 14px;
  color: #999;
}

.award-value {
  font-size: 14px;
  color: #333;
}

.award-footer {
  display: flex;
  gap: 10px;
}

.view-cert-btn {
  padding: 8px 16px;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
}

.view-cert-btn:hover {
  background: #e8e8e8;
}

/* 上传弹窗 */
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

.upload-modal {
  background: white;
  border-radius: 12px;
  padding: 30px;
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow-y: auto;
  animation: modalIn 0.3s ease;
}

@keyframes modalIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.modal-title {
  font-size: 20px;
  color: #333;
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  background: #f5f5f5;
  border: none;
  border-radius: 50%;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: #e8e8e8;
}

.upload-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.upload-area {
  position: relative;
  width: 100%;
  height: 200px;
  border: 2px dashed #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
  overflow: hidden;
}

.upload-area:hover {
  border-color: #667eea;
}

.file-input {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 10px;
}

.upload-icon {
  font-size: 48px;
}

.upload-placeholder p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.upload-hint {
  font-size: 12px;
  color: #999;
}

.upload-preview {
  position: relative;
  width: 100%;
  height: 100%;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.remove-file-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  background: rgba(0, 0, 0, 0.6);
  border: none;
  border-radius: 50%;
  color: white;
  font-size: 20px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-file-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.cancel-btn {
  padding: 10px 24px;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #e8e8e8;
}
</style>
