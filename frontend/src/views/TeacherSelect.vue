<!--
  组件：独立老师选择页面
  说明：提供老师列表展示、搜索筛选、分页浏览、选择确认功能
  使用方式：通过路由访问 /teacher-select 或作为组件嵌入
-->
<template>
  <div class="teacher-select-container">
    <!-- 返回按钮 -->
    <button class="back-btn" @click="handleBack">
      ← 返回
    </button>

    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">👨‍🏫 选择指导老师</h1>
      <p class="page-subtitle">请从下方列表中选择一位指导老师</p>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="filter-bar">
      <div class="search-box">
        <input
          v-model="searchKeyword"
          type="text"
          class="search-input"
          placeholder="搜索老师姓名、职称或院系..."
          @input=""
        />
      </div>
      <div class="department-filter">
        <select
          v-model="selectedDepartment"
          class="filter-select"
          @change="handleDepartmentChange"
        >
          <option value="">全部院系</option>
          <option v-for="dept in departments" :key="dept" :value="dept">
            {{ dept }}
          </option>
        </select>
      </div>
    </div>

    <!-- 老师统计信息 -->
    <div class="stats-bar">
      <span class="stat-item">共 <strong>{{ total }}</strong> 位老师</span>
      <span class="stat-item">当前第 <strong>{{ currentPage }}</strong> / {{ totalPages }} 页</span>
    </div>

    <!-- 老师列表 -->
    <div class="teacher-list" v-if="teachers.length > 0">
      <div
        v-for="teacher in teachers"
        :key="teacher.userId"
        :class="['teacher-card', selectedTeacher?.userId === teacher.userId ? 'selected' : '']"
        @click="selectTeacher(teacher)"
      >
        <!-- 老师头像区域 -->
        <div class="teacher-avatar">
          <div class="avatar-icon">👤</div>
        </div>

        <!-- 老师信息区域 -->
        <div class="teacher-info">
          <div class="teacher-header">
            <span class="teacher-name">{{ teacher.realName }}</span>
            <span :class="['teacher-title', getTitleClass(teacher.title)]">
              {{ teacher.title }}
            </span>
          </div>
          <div class="teacher-department">📚 {{ teacher.department }}</div>
          <div class="teacher-contact">
            <span v-if="teacher.phone" class="contact-item">📱 {{ teacher.phone }}</span>
            <span v-if="teacher.email" class="contact-item">📧 {{ teacher.email }}</span>
          </div>
          <div class="teacher-quota">
            <span class="quota-label">带队名额：</span>
            <div class="quota-bar">
              <div 
                class="quota-fill" 
                :style="{ width: getQuotaPercent(teacher) + '%' }"
                :class="getQuotaClass(teacher)"
              ></div>
            </div>
            <span :class="['quota-text', getQuotaClass(teacher)]">
              {{ teacher.currentTeamCount }} / {{ teacher.maxTeamQuota }}
            </span>
          </div>
        </div>

        <!-- 选择标记 -->
        <div class="select-indicator">
          <div v-if="selectedTeacher?.userId === teacher.userId" class="check-circle">
            ✓
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon">🔍</div>
      <p>{{ isLoading ? '加载中...' : '暂无符合条件的老师' }}</p>
      <button v-if="!isLoading && (searchKeyword || selectedDepartment)" class="reset-btn" @click="resetFilter">
        重置筛选条件
      </button>
    </div>

    <!-- 分页组件 -->
    <div class="pagination" v-if="totalPages > 1">
      <button 
        class="page-btn prev-btn" 
        :disabled="currentPage <= 1"
        @click="goToPage(currentPage - 1)"
      >
        ←
      </button>
      
      <div class="page-numbers">
        <button
          v-for="page in visiblePages"
          :key="page"
          :class="['page-num', currentPage === page ? 'active' : '']"
          @click="goToPage(page)"
        >
          {{ page }}
        </button>
      </div>

      <button 
        class="page-btn next-btn" 
        :disabled="currentPage >= totalPages"
        @click="goToPage(currentPage + 1)"
      >
        →
      </button>
    </div>

    <!-- 底部操作栏 -->
    <div class="bottom-bar">
      <button class="cancel-btn" @click="handleBack">
        取消
      </button>
      <button 
        class="confirm-btn" 
        :disabled="!selectedTeacher"
        @click="handleConfirm"
      >
        确认选择
      </button>
    </div>

    <!-- 选中老师预览弹窗 -->
    <div v-if="showPreview" class="modal-overlay" @click="closePreview">
      <div class="preview-modal" @click.stop>
        <div class="modal-header">
          <h3>已选择指导老师</h3>
          <button class="close-btn" @click="closePreview">×</button>
        </div>
        <div class="modal-body" v-if="selectedTeacher">
          <div class="preview-avatar">👤</div>
          <div class="preview-info">
            <div class="preview-name">{{ selectedTeacher.realName }}</div>
            <div class="preview-title">{{ selectedTeacher.title }}</div>
            <div class="preview-department">{{ selectedTeacher.department }}</div>
            <div class="preview-contact">
              <span v-if="selectedTeacher.phone">📱 {{ selectedTeacher.phone }}</span>
              <span v-if="selectedTeacher.email">📧 {{ selectedTeacher.email }}</span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="confirm-modal-btn" @click="handleConfirm">
            确认选择
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { signupApi } from '@/api/signup'
import type { TeacherInfo } from '@/types/signup'

// ================================
// 路由
// ================================
const router = useRouter()
const route = useRoute()

// ================================
// 响应式数据
// ================================

// 老师列表
const teachers = ref<TeacherInfo[]>([])

// 加载状态
const isLoading = ref(false)

// 搜索关键词
const searchKeyword = ref('')

// 选中的院系
const selectedDepartment = ref('')

// 院系列表
const departments = ref<string[]>([])

// 分页信息
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = ref(1)

// 选中的老师
const selectedTeacher = ref<TeacherInfo | null>(null)

// 是否显示预览弹窗
const showPreview = ref(false)

// 用于接收外部传入的已选老师ID
const preSelectedTeacherId = ref<number | null>(null)

// ================================
// 计算属性
// ================================

// 可见的分页页码
const visiblePages = computed(() => {
  const pages: number[] = []
  const total = totalPages.value
  const current = currentPage.value
  
  // 最多显示5个页码
  let start = Math.max(1, current - 2)
  let end = Math.min(total, start + 4)
  
  // 如果末尾不够5个，向前延伸
  if (end - start < 4 && start > 1) {
    start = Math.max(1, end - 4)
  }
  
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  
  return pages
})

// ================================
// 方法定义
// ================================

/**
 * 获取职称样式类
 * @param title 职称
 * @returns 样式类名
 */
const getTitleClass = (title: string): string => {
  if (title.includes('教授')) return 'title-professor'
  if (title.includes('副教授')) return 'title-associate'
  if (title.includes('讲师')) return 'title-lecturer'
  if (title.includes('助教')) return 'title-assistant'
  return 'title-other'
}

/**
 * 计算名额使用百分比
 * @param teacher 老师信息
 * @returns 百分比
 */
const getQuotaPercent = (teacher: TeacherInfo): number => {
  if (teacher.maxTeamQuota <= 0) return 0
  return Math.round((teacher.currentTeamCount / teacher.maxTeamQuota) * 100)
}

/**
 * 获取名额状态样式类
 * @param teacher 老师信息
 * @returns 样式类名
 */
const getQuotaClass = (teacher: TeacherInfo): string => {
  const percent = getQuotaPercent(teacher)
  if (percent >= 100) return 'quota-full'
  if (percent >= 80) return 'quota-high'
  if (percent >= 50) return 'quota-medium'
  return 'quota-low'
}

/**
 * 获取老师列表
 */
const fetchTeachers = async () => {
  isLoading.value = true
  try {
    const response = await signupApi.getAvailableTeachers({
      page: currentPage.value,
      size: pageSize.value,
      department: selectedDepartment.value || undefined,
      keyword: searchKeyword.value || undefined
    })
    
    if (response.code === 0) {
      teachers.value = response.data.list
      total.value = response.data.total
      totalPages.value = response.data.totalPages
      
      // 提取院系列表
      extractDepartments(response.data.list)
      
      // 如果有预选中的老师，自动选中
      if (preSelectedTeacherId.value) {
        selectedTeacher.value = teachers.value.find(
          t => t.userId === preSelectedTeacherId.value
        ) || null
      }
    } else {
      console.error('获取老师列表失败:', response.message)
    }
  } catch (error) {
    console.error('获取老师列表失败:', error)
  } finally {
    isLoading.value = false
  }
}

/**
 * 提取院系列表
 * @param teacherList 老师列表
 */
const extractDepartments = (teacherList: TeacherInfo[]) => {
  const deptSet = new Set<string>()
  teacherList.forEach(teacher => {
    if (teacher.department) {
      deptSet.add(teacher.department)
    }
  })
  departments.value = Array.from(deptSet).sort()
}

/**
 * 处理搜索
 */
const handleSearch = () => {
  currentPage.value = 1
  fetchTeachers()
}

/**
 * 处理院系筛选变化
 */
const handleDepartmentChange = () => {
  currentPage.value = 1
  fetchTeachers()
}

/**
 * 重置筛选条件
 */
const resetFilter = () => {
  searchKeyword.value = ''
  selectedDepartment.value = ''
  currentPage.value = 1
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
 * 跳转到指定页码
 * @param page 页码
 */
const goToPage = (page: number) => {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchTeachers()
}

/**
 * 返回上一页
 */
const handleBack = () => {
  // 如果有来源页面，返回来源页面
  const from = route.query.from || '/competitions'
  router.push(from as string)
}

/**
 * 确认选择
 */
const handleConfirm = () => {
  if (!selectedTeacher.value) return
  
  // 通过URL参数返回选中结果
  const callbackUrl = route.query.callback
  if (callbackUrl) {
    // 如果有回调URL，拼接选中的老师信息
    const teacher = selectedTeacher.value
    const resultUrl = `${callbackUrl}?teacherId=${teacher.userId}&teacherName=${encodeURIComponent(teacher.realName)}`
    router.push(resultUrl)
  } else {
    // 否则返回来源页面，并存储选中状态到sessionStorage
    sessionStorage.setItem('selectedTeacher', JSON.stringify(selectedTeacher.value))
    handleBack()
  }
}

/**
 * 打开预览弹窗
 */
const openPreview = () => {
  if (selectedTeacher.value) {
    showPreview.value = true
  }
}

/**
 * 关闭预览弹窗
 */
const closePreview = () => {
  showPreview.value = false
}

// ================================
// 监听事件
// ================================

// 防抖定时器
let searchTimer: number | null = null

// 监听搜索关键词变化
watch(searchKeyword, () => {
  // 使用防抖避免频繁请求
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(() => {
    currentPage.value = 1
    fetchTeachers()
  }, 300)
})

// ================================
// 生命周期
// ================================
onMounted(() => {
  // 获取URL参数中的预选中老师ID
  const teacherId = route.query.teacherId
  if (teacherId) {
    preSelectedTeacherId.value = Number(teacherId)
  }
  
  fetchTeachers()
})
</script>

<style scoped>
.teacher-select-container {
  max-width: 800px;
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

/* 筛选栏 */
.filter-bar {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-box {
  flex: 1;
  min-width: 250px;
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

.department-filter {
  min-width: 150px;
}

.filter-select {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
}

.filter-select:focus {
  outline: none;
  border-color: #667eea;
}

/* 统计信息栏 */
.stats-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 12px 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.stat-item {
  font-size: 14px;
  color: #666;
}

.stat-item strong {
  color: #333;
}

/* 老师列表 */
.teacher-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.teacher-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background: #fff;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.teacher-card:hover {
  border-color: #667eea;
  background: #f8f9ff;
}

.teacher-card.selected {
  border-color: #667eea;
  background: #f0f4ff;
}

/* 老师头像 */
.teacher-avatar {
  flex-shrink: 0;
}

.avatar-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}

/* 老师信息 */
.teacher-info {
  flex: 1;
  min-width: 0;
}

.teacher-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.teacher-name {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.teacher-title {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.title-professor {
  background: #e3f2fd;
  color: #1976d2;
}

.title-associate {
  background: #e8f5e9;
  color: #388e3c;
}

.title-lecturer {
  background: #fff3e0;
  color: #f57c00;
}

.title-assistant {
  background: #f3e5f5;
  color: #7b1fa2;
}

.title-other {
  background: #f5f5f5;
  color: #666;
}

.teacher-department {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.teacher-contact {
  display: flex;
  gap: 15px;
  margin-bottom: 10px;
}

.contact-item {
  font-size: 13px;
  color: #999;
}

/* 名额进度条 */
.teacher-quota {
  display: flex;
  align-items: center;
  gap: 10px;
}

.quota-label {
  font-size: 13px;
  color: #666;
  min-width: 60px;
}

.quota-bar {
  flex: 1;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.quota-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.3s;
}

.quota-fill.quota-low {
  background: #4caf50;
}

.quota-fill.quota-medium {
  background: #ff9800;
}

.quota-fill.quota-high {
  background: #ff5722;
}

.quota-fill.quota-full {
  background: #f44336;
}

.quota-text {
  font-size: 13px;
  font-weight: 500;
  min-width: 60px;
  text-align: right;
}

.quota-text.quota-low {
  color: #4caf50;
}

.quota-text.quota-medium {
  color: #ff9800;
}

.quota-text.quota-high {
  color: #ff5722;
}

.quota-text.quota-full {
  color: #f44336;
}

/* 选择标记 */
.select-indicator {
  flex-shrink: 0;
}

.check-circle {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
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
  font-size: 16px;
  color: #999;
  margin: 0;
}

.reset-btn {
  padding: 10px 24px;
  background: #f5f5f5;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
}

.reset-btn:hover {
  background: #eee;
}

/* 分页组件 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 30px;
}

.page-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  background: #fff;
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #667eea;
  color: #667eea;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-numbers {
  display: flex;
  gap: 5px;
}

.page-num {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.page-num:hover {
  background: #f0f0f0;
}

.page-num.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 15px;
  padding: 15px 20px;
  background: #fff;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);
}

.bottom-bar button {
  flex: 1;
  padding: 15px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.cancel-btn {
  background: #f5f5f5;
  border: none;
  color: #666;
}

.cancel-btn:hover {
  background: #eee;
}

.confirm-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
}

.confirm-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 弹窗遮罩 */
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

/* 预览弹窗 */
.preview-modal {
  background: white;
  border-radius: 16px;
  padding: 0;
  max-width: 400px;
  width: 90%;
  animation: modalIn 0.3s ease;
  overflow: hidden;
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

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: #f5f5f5;
  font-size: 20px;
  color: #666;
  cursor: pointer;
}

.close-btn:hover {
  background: #eee;
}

.modal-body {
  padding: 30px 20px;
  text-align: center;
}

.preview-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  color: white;
  margin: 0 auto 20px;
}

.preview-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-name {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.preview-title {
  font-size: 14px;
  color: #667eea;
}

.preview-department {
  font-size: 14px;
  color: #666;
}

.preview-contact {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 10px;
}

.preview-contact span {
  font-size: 13px;
  color: #999;
}

.modal-footer {
  padding: 20px;
  border-top: 1px solid #f0f0f0;
}

.confirm-modal-btn {
  width: 100%;
  padding: 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  color: white;
  cursor: pointer;
}

.confirm-modal-btn:hover {
  opacity: 0.9;
}
</style>