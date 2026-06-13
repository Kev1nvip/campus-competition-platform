<template>
  <div class="page-wrap">
    <header class="nav">
      <span class="nav-brand" @click="router.push('/')">◆ 校园竞赛平台</span>
      <span class="nav-item" @click="router.push('/')">← 返回首页</span>
    </header>

    <div class="page-inner">
      <h2>AI 竞赛推荐</h2>
      <p class="sub">描述你感兴趣的方向，AI 为你推荐合适的竞赛</p>

      <div class="input-area">
        <el-input
          v-model="prompt"
          type="textarea"
          :rows="3"
          placeholder="例如：我对人工智能、数学建模感兴趣，想参加编程类比赛..."
          :disabled="loading"
        />
        <el-button
          type="primary"
          :loading="loading"
          style="margin-top:12px;"
          @click="handleRecommend"
        >
          {{ loading ? '推荐中...' : '获取推荐' }}
        </el-button>
      </div>

      <el-divider v-if="result" />

      <div v-if="result" class="result-area">
        <h3>推荐结果</h3>
        <div class="result-text">{{ result }}</div>
      </div>

      <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const prompt = ref('')
const result = ref('')
const loading = ref(false)
const errorMsg = ref('')

const handleRecommend = async () => {
  if (!prompt.value.trim()) return (errorMsg.value = '请输入感兴趣的方向')
  loading.value = true
  errorMsg.value = ''
  result.value = ''
  try {
    const res: any = await request({ url: '/v1/ai/recommend', method: 'POST', data: { prompt: prompt.value } })
    if (res.code === 0) result.value = res.data
    else errorMsg.value = res.message || '推荐失败'
  } catch {
    errorMsg.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page-wrap { min-height: 100vh; background: #fff; }
.nav { height: 56px; border-bottom: 1px solid #e0e0e0; display: flex; align-items: center; justify-content: space-between; padding: 0 40px; }
.nav-brand { font-size: 14px; font-weight: 800; letter-spacing: 1px; color: #111; cursor: pointer; }
.nav-item { font-size: 13px; color: #555; cursor: pointer; padding: 6px 12px; border-radius: 4px; }
.nav-item:hover { background: #f4f4f4; color: #111; }
.page-inner { max-width: 680px; margin: 0 auto; padding: 40px 20px; }
h2 { font-size: 20px; font-weight: 700; color: #111; margin-bottom: 6px; }
.sub { font-size: 13px; color: #888; margin-bottom: 24px; }
.input-area { margin-bottom: 8px; }
h3 { font-size: 15px; font-weight: 700; color: #111; margin-bottom: 12px; }
.result-text { font-size: 14px; color: #333; line-height: 1.9; white-space: pre-wrap; background: #fafafa; border: 1px solid #e0e0e0; border-radius: 6px; padding: 16px 20px; }
.error-tip { font-size: 13px; color: #888; background: #f8f8f8; border: 1px solid #e8e8e8; border-radius: 4px; padding: 8px 12px; margin-top: 12px; }
</style>
