<!--
  组件：AI推荐页面
  说明：根据用户输入的方向，展示AI推荐的竞赛列表
-->
<template>
  <div class="ai-recommend-container">
    <!-- 页面标题 -->
    <div class="ai-recommend__header">
      <h1 class="ai-recommend__title">🤖 AI 竞赛推荐</h1>
      <p class="ai-recommend__subtitle">智能推荐适合您的学术竞赛</p>
    </div>

    <!-- 输入区域 -->
    <div class="ai-recommend__input-section">
      <div class="input-card">
        <div class="input-card__header">
          <h2>输入您感兴趣的方向</h2>
        </div>
        
        <div class="input-card__body">
          <div class="input-wrapper">
            <input 
              v-model="inputDirection"
              type="text"
              class="direction-input"
              placeholder="例如：人工智能、数据科学、数学建模、创新创业..."
              @keyup.enter="handleRecommend"
            />
            <button 
              class="recommend-btn" 
              @click="handleRecommend"
              :disabled="isLoading || !inputDirection.trim()"
            >
              {{ isLoading ? '推荐中...' : '🔍 智能推荐' }}
            </button>
          </div>

          <!-- 热门方向标签 -->
          <div class="hot-tags">
            <span class="hot-tags__label">热门方向：</span>
            <button 
              v-for="tag in hotDirections" 
              :key="tag"
              class="hot-tag"
              @click="selectDirection(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 推荐结果 -->
    <div v-if="recommendResult && recommendResult.items.length > 0" class="ai-recommend__result-section">
      <div class="result-header">
        <h2>📊 为您推荐的竞赛</h2>
        <div class="result-source">
          <span class="source-label">数据来源：</span>
          <span class="source-value">{{ recommendResult.source }}</span>
          <span class="source-time">{{ formatDate(recommendResult.recommendTime) }}</span>
        </div>
      </div>

      <div class="recommend-list">
        <div 
          v-for="(item, index) in recommendResult.items" 
          :key="item.competition.id"
          class="recommend-card"
        >
          <div class="recommend-card__rank" :class="getRankClass(index)">
            {{ index + 1 }}
          </div>
          
          <div class="recommend-card__content">
            <div class="recommend-card__header">
              <div class="competition-info">
                <h3 class="competition-title">{{ item.competition.title }}</h3>
                <div class="competition-meta">
                  <span :class="['type-badge', item.competition.type === 'INDIVIDUAL' ? 'type-individual' : 'type-team']">
                    {{ item.competition.type === 'INDIVIDUAL' ? '个人赛' : '团队赛' }}
                  </span>
                  <span :class="['status-badge', getStatusClass(item.competition.status)]">
                    {{ statusMap[item.competition.status] }}
                  </span>
                  <span class="match-score">匹配度 {{ item.matchScore }}%</span>
                </div>
              </div>
              
              <div class="match-bar">
                <div class="match-bar__fill" :style="{ width: item.matchScore + '%' }"></div>
              </div>
            </div>

            <div class="recommend-card__body">
              <div class="recommend-reason">
                <span class="reason-label">推荐理由：</span>
                <span class="reason-text">{{ item.reason }}</span>
              </div>
              
              <div class="recommend-tags">
                <span 
                  v-for="tag in item.tags" 
                  :key="tag"
                  class="tag"
                >
                  {{ tag }}
                </span>
              </div>
            </div>

            <div class="recommend-card__footer">
              <div class="competition-details">
                <span class="detail-item">主办方：{{ item.competition.organizer }}</span>
                <span class="detail-item">报名时间：{{ formatDate(item.competition.signupStart) }} - {{ formatDate(item.competition.signupEnd) }}</span>
              </div>
              <button class="view-detail-btn" @click="viewCompetition(item.competition.id)">
                查看详情 →
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else-if="hasSearched && (!recommendResult || recommendResult.items.length === 0)" class="empty-state">
      <div class="empty-icon">🔍</div>
      <p>暂无相关推荐</p>
      <p class="empty-hint">请尝试其他方向关键词</p>
    </div>

    <!-- 初始状态 -->
    <div v-else class="initial-state">
      <div class="initial-icon">✨</div>
      <h3>发现适合您的竞赛</h3>
      <p>输入您感兴趣的方向，AI将为您智能推荐相关竞赛</p>
      <div class="initial-features">
        <div class="feature-item">
          <span class="feature-icon">🎯</span>
          <span>精准匹配</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">📚</span>
          <span>智能分析</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">🔄</span>
          <span>实时更新</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { aiApi } from '@/api/ai'
import type { RecommendResponse } from '@/types/ai'
import { statusMap } from '@/types/competition'
import type { CompetitionStatus } from '@/types/competition'

// 路由
const router = useRouter()

// 响应式数据
const inputDirection = ref('')
const isLoading = ref(false)
const hasSearched = ref(false)
const recommendResult = ref<RecommendResponse | null>(null)
const hotDirections = ref<string[]>([])

/**
 * 获取热门方向
 */
const fetchHotDirections = async () => {
  try {
    const response = await aiApi.getHotDirections()
    if (response.code === 0 && response.data) {
      hotDirections.value = response.data
    }
  } catch (error) {
    console.error('获取热门方向失败:', error)
    // 使用默认热门方向
    hotDirections.value = ['人工智能', '数据科学', '数学建模', '创新创业', '计算机视觉', '机器学习']
  }
}

/**
 * 选择方向标签
 */
const selectDirection = (direction: string) => {
  inputDirection.value = direction
  handleRecommend()
}

/**
 * 获取推荐结果
 */
const handleRecommend = async () => {
  if (!inputDirection.value.trim()) {
    return
  }
  
  isLoading.value = true
  hasSearched.value = true
  
  try {
    const response = await aiApi.getRecommendations({
      direction: inputDirection.value.trim(),
      count: 10
    })
    
    if (response.code === 0 && response.data) {
      recommendResult.value = response.data
    } else {
      recommendResult.value = null
    }
  } catch (error) {
    console.error('获取推荐失败:', error)
    recommendResult.value = null
  } finally {
    isLoading.value = false
  }
}

/**
 * 查看竞赛详情
 */
const viewCompetition = (id: number) => {
  router.push(`/competition/${id}`)
}

/**
 * 获取排名样式类
 */
const getRankClass = (index: number) => {
  switch (index) {
    case 0:
      return 'rank-gold'
    case 1:
      return 'rank-silver'
    case 2:
      return 'rank-bronze'
    default:
      return 'rank-default'
  }
}

/**
 * 获取状态样式类
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

// 页面加载时获取热门方向
onMounted(() => {
  fetchHotDirections()
})
</script>