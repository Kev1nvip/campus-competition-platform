<template>
  <div>
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
        <div class="result-text" v-html="renderedResult"></div>
      </div>

      <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import request from '@/utils/request'

const prompt = ref('')
const result = ref('')
const loading = ref(false)
const errorMsg = ref('')

const renderedResult = computed(() => renderMarkdown(result.value))

const handleRecommend = async () => {
  if (!prompt.value.trim()) return (errorMsg.value = '请输入感兴趣的方向')
  loading.value = true
  errorMsg.value = ''
  result.value = ''
  try {
    const res: any = await request({ url: '/v1/ai/recommend', method: 'POST', data: { prompt: prompt.value }, timeout: 120000 })
    if (res.code === 0) result.value = res.data
    else errorMsg.value = res.message || '推荐失败'
  } catch {
    errorMsg.value = '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}

function renderMarkdown(text: string): string {
  if (!text) return ''
  let html = text
    // 转义 HTML
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    // 代码块 ```code```
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code>$2</code></pre>')
    // 行内代码 `code`
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    // 标题 ## heading
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    // 粗体 **text**
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    // 斜体 *text*
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    // 无序列表 - item
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    // 多行 li 包裹成 ul
    .replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>')
    // 有序列表 1. item
    .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
    // 水平线 ---
    .replace(/^---$/gm, '<hr>')
    // 换行
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
  // 包装段落（避免已被标签包裹的内容重复包裹）
  if (!html.startsWith('<')) {
    html = '<p>' + html + '</p>'
  }
  return html
}
</script>

<style scoped>
.page-inner { max-width: 680px; }
h2 { font-size: 20px; font-weight: 700; color: #111; margin-bottom: 6px; }
.sub { font-size: 13px; color: #888; margin-bottom: 24px; }
.input-area { margin-bottom: 8px; }
h3 { font-size: 15px; font-weight: 700; color: #111; margin-bottom: 12px; }
.result-text { font-size: 14px; color: #333; line-height: 1.9; background: #fafafa; border: 1px solid #e0e0e0; border-radius: 6px; padding: 16px 20px; }
.result-text :deep(pre) { background: #1a1a2e; color: #e0e0e0; padding: 12px 16px; border-radius: 4px; overflow-x: auto; font-size: 13px; }
.result-text :deep(code) { background: #eee; padding: 1px 5px; border-radius: 3px; font-size: 13px; }
.result-text :deep(pre code) { background: transparent; padding: 0; }
.result-text :deep(ul) { padding-left: 20px; margin: 8px 0; }
.result-text :deep(li) { margin-bottom: 4px; }
.result-text :deep(hr) { margin: 16px 0; border: none; border-top: 1px solid #e0e0e0; }
.result-text :deep(h2), .result-text :deep(h3), .result-text :deep(h4) { margin: 16px 0 8px; color: #111; }
.result-text :deep(strong) { font-weight: 600; }
.error-tip { font-size: 13px; color: #888; background: #f8f8f8; border: 1px solid #e8e8e8; border-radius: 4px; padding: 8px 12px; margin-top: 12px; }
</style>
