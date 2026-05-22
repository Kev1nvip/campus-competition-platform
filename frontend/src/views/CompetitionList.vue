<!--
  组件：竞赛列表页面
  说明：展示所有竞赛列表，支持筛选、搜索和分页功能
-->
<template>
  <div class="competition-list-container">
    <!-- 页面标题 -->
    <div class="competition-list__header">
      <h1>竞赛列表</h1>
      <p class="competition-list__subtitle">浏览和搜索校园学术竞赛</p>
    </div>

    <!-- 筛选和搜索区域 -->
    <div class="competition-list__filter">
      <div class="filter-row">
        <div class="filter-item">
          <label>竞赛类型</label>
          <select v-model="filters.type">
            <option value="">全部类型</option>
            <option value="INDIVIDUAL">个人赛</option>
            <option value="TEAM">团队赛</option>
          </select>
        </div>
        
        <div class="filter-item">
          <label>竞赛状态</label>
          <select v-model="filters.status">
            <option value="">全部状态</option>
            <option value="UPCOMING">未开始</option>
            <option value="SIGNING">报名中</option>
            <option value="CLOSED">报名截止</option>
            <option value="ONGOING">进行中</option>
            <option value="FINISHED">已结束</option>
          </select>
        </div>
        
        <div class="search-item">
          <input 
            v-model="filters.keyword"
            type="text" 
            placeholder="搜索竞赛名称..."
            @keyup.enter="fetchList"
          />
          <button class="search-btn" @click="fetchList">搜索</button>
        </div>
        
        <button class="reset-btn" @click="handleReset">重置</button>
      </div>
    </div>

    <!-- 竞赛列表 -->
    <div class="competition-list__content">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading">
        <div class="spinner"></div>
        <span>加载中...</span>
      </div>
      
      <!-- 空状态 -->
      <div v-else-if="list.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <p>暂无竞赛信息</p>
      </div>
      
      <!-- 竞赛卡片列表 -->
      <div v-else class="competition-grid">
        <div 
          v-for="item in list" 
          :key="item.id" 
          class="competition-card"
          @click="handleCardClick(item.id)"
        >
          <div class="competition-card__header">
            <div class="competition-card__type">{{ typeMap[item.type] }}</div>
            <div :class="['competition-card__status', getStatusClass(item.status)]">
              {{ statusMap[item.status] }}
            </div>
          </div>
          
          <h3 class="competition-card__title">{{ item.title }}</h3>
          
          <div class="competition-card__body">
            <div class="competition-card__info">
              <span class="label">主办方：</span>
              <span class="value">{{ item.organizer }}</span>
            </div>
            <div class="competition-card__info">
              <span class="label">名额：</span>
              <span class="value">{{ getQuotaInfo(item) }}</span>
            </div>
            <div class="competition-card__info">
              <span class="label">报名时间：</span>
              <span class="value">{{ formatDate(item.signupStart) }} - {{ formatDate(item.signupEnd) }}</span>
            </div>
            <div v-if="item.competitionStart" class="competition-card__info">
              <span class="label">比赛时间：</span>
              <span class="value">{{ formatDate(item.competitionStart) }} - {{ formatDate(item.competitionEnd || '') }}</span>
            </div>
            <div class="competition-card__info">
              <span class="label">发布人：</span>
              <span class="value">{{ item.createdByName }}</span>
            </div>
          </div>
          
          <div class="competition-card__footer">
            <span class="competition-card__link">查看详情 →</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页组件 -->
    <div v-if="pagination.totalPages > 1" class="competition-list__pagination">
      <div class="pagination">
        <button 
          class="pagination__btn" 
          :disabled="pagination.page === 1"
          @click="handlePageChange(pagination.page - 1)"
        >
          ←
        </button>
        
        <button 
          v-for="page in pages" 
          :key="page"
          :class="['pagination__btn', { active: page === pagination.page, disabled: page === -1 }]"
          :disabled="page === -1"
          @click="page !== -1 && handlePageChange(page)"
        >
          {{ page === -1 ? '...' : page }}
        </button>
        
        <button 
          class="pagination__btn" 
          :disabled="pagination.page === pagination.totalPages"
          @click="handlePageChange(pagination.page + 1)"
        >
          →
        </button>
      </div>
      
      <div class="pagination__info">
        共 {{ pagination.total }} 条记录，当前第 {{ pagination.page }}/{{ pagination.totalPages }} 页
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { competitionApi } from '@/api/competition'
import type { CompetitionVO, CompetitionStatus, CompetitionType } from '@/types/competition'
import { statusMap, typeMap } from '@/types/competition'

// ================================
// 路由
// ================================
const router = useRouter()

// ================================
// 响应式数据
// ================================

// 列表数据
const list = ref<CompetitionVO[]>([])

// 加载状态
const isLoading = ref(false)

// 分页参数
const pagination = reactive({
  page: 1,
  size: 10,
  total: 0,
  totalPages: 0
})

// 筛选参数
const filters = reactive({
  status: '',
  type: '',
  keyword: ''
})

// ================================
// 计算属性
// ================================

// 分页页码数组
const pages = computed(() => {
  const arr: number[] = []
  const current = pagination.page
  const totalVal = pagination.totalPages

  if (totalVal <= 7) {
    for (let i = 1; i <= totalVal; i++) {
      arr.push(i)
    }
  } else {
    if (current <= 3) {
      arr.push(1, 2, 3, 4, 5, -1, totalVal)
    } else if (current >= totalVal - 2) {
      arr.push(1, -1, totalVal - 4, totalVal - 3, totalVal - 2, totalVal - 1, totalVal)
    } else {
      arr.push(1, -1, current - 1, current, current + 1, -1, totalVal)
    }
  }
  return arr
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
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/**
 * 获取名额信息文本
 * @param item 竞赛列表项
 * @returns 名额信息
 */
const getQuotaInfo = (item: CompetitionVO) => {
  if (!item.hasQuota || item.maxQuota === undefined || item.remainingQuota === undefined) {
    return '不限名额'
  }
  return `${item.remainingQuota}/${item.maxQuota} 剩余`
}

/**
 * 获取竞赛列表
 * 根据筛选条件和分页参数获取竞赛列表数据
 */
const fetchList = async () => {
  isLoading.value = true
  try {
    const response = await competitionApi.getList({
      page: pagination.page,
      size: pagination.size,
      status: filters.status || undefined,
      type: filters.type || undefined,
      keyword: filters.keyword.trim() || undefined
    })
    
    if (response.code === 0) {
      list.value = response.data.list
      pagination.total = response.data.total
      pagination.totalPages = response.data.totalPages
    } else {
      console.error('获取竞赛列表失败:', response.message)
    }
  } catch (error) {
    console.error('获取竞赛列表失败:', error)
  } finally {
    isLoading.value = false
  }
}

/**
 * 重置筛选条件
 * 清空所有筛选条件并重新查询
 */
const handleReset = () => {
  filters.status = ''
  filters.type = ''
  filters.keyword = ''
  pagination.page = 1
  fetchList()
}

/**
 * 分页跳转
 * @param page 目标页码
 */
const handlePageChange = (page: number) => {
  pagination.page = page
  fetchList()
}

/**
 * 查看竞赛详情
 * @param id 竞赛ID
 */
const handleCardClick = (id: number) => {
  router.push(`/competition/${id}`)
}

// ================================
// 生命周期
// ================================
onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.competition-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
}

.competition-list__header {
  margin-bottom: 30px;
}

.competition-list__header h1 {
  font-size: 28px;
  color: #333;
  margin: 0 0 10px 0;
}

.competition-list__subtitle {
  font-size: 14px;
  color: #666;
  margin: 0;
}

/* 筛选区域 */
.competition-list__filter {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-item label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.filter-item select {
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
  min-width: 150px;
}

.search-item {
  display: flex;
  gap: 10px;
  flex: 1;
  min-width: 300px;
}

.search-item input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 14px;
}

.search-btn, .reset-btn {
  padding: 10px 20px;
  border: none;
  border-radius: 5px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.3s;
}

.search-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.reset-btn {
  background: #fff;
  color: #666;
  border: 1px solid #ddd;
}

.search-btn:hover, .reset-btn:hover {
  opacity: 0.9;
}

/* 列表区域 */
.competition-list__content {
  margin-bottom: 30px;
}

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

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px;
  gap: 15px;
}

.empty-icon {
  font-size: 48px;
}

.empty-state p {
  font-size: 16px;
  color: #666;
}

/* 竞赛卡片网格 */
.competition-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
}

.competition-card {
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.competition-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12);
}

.competition-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.competition-card__type, .competition-card__status {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.competition-card__type {
  background: #e8f4fd;
  color: #1e88e5;
}

.competition-card__status {
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

.competition-card__title {
  font-size: 18px;
  color: #333;
  margin: 0 0 15px 0;
  line-height: 1.4;
}

.competition-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.competition-card__info {
  display: flex;
  font-size: 14px;
  color: #666;
}

.competition-card__info .label {
  color: #999;
  min-width: 70px;
}

.competition-card__info .value {
  flex: 1;
}

.competition-card__footer {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.competition-card__link {
  font-size: 14px;
  color: #667eea;
  font-weight: 500;
}

/* 分页组件 */
.competition-list__pagination {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.pagination {
  display: flex;
  align-items: center;
  gap: 5px;
}

.pagination__btn {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  border-radius: 5px;
  background: white;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination__btn:hover:not(:disabled) {
  background: #f5f5f5;
}

.pagination__btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.pagination__btn.disabled {
  color: #ddd;
  cursor: not-allowed;
}

.pagination__info {
  font-size: 14px;
  color: #666;
}
</style>