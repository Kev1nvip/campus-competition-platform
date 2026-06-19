<template>
  <div>
    <div class="page-title">申请处理</div>

    <div class="tabs">
      <span :class="['tab', activeTab === 'audit' ? 'active' : '']" @click="activeTab = 'audit'; loadAuditList()">指导申请</span>
      <span :class="['tab', activeTab === 'guide' ? 'active' : '']" @click="activeTab = 'guide'; loadGuideList()">带队申请</span>
    </div>

    <!-- 报名审核 tab -->
    <template v-if="activeTab === 'audit'">
      <div v-if="isLoading" class="center-tip">加载中...</div>
      <div v-else-if="auditList.length === 0" class="center-tip">暂无待处理指导申请</div>
      <el-table v-else :data="auditList" class="data-table">
        <el-table-column label="申请 ID" prop="applyId" width="80" />
        <el-table-column label="竞赛名称" prop="competitionTitle" min-width="140">
          <template #default="{ row }: { row: any }">
            {{ row.competitionTitle || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="学生姓名" prop="studentName" width="100" />
        <el-table-column label="学号" prop="studentNo" width="120" />
        <el-table-column label="院系" prop="department" />
        <el-table-column label="申请时间" prop="createdAt" width="140">
          <template #default="{ row }: { row: any }">
            {{ row.createdAt ? new Date(row.createdAt).toLocaleString('zh-CN') : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button size="small" type="primary" @click="handleAudit(row, 'APPROVED')">同意指导</el-button>
            <el-button size="small" @click="handleAudit(row, 'REJECTED')" style="margin-left:4px;">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 带队申请 tab -->
    <template v-if="activeTab === 'guide'">
      <div v-if="isLoading" class="center-tip">加载中...</div>
      <div v-else-if="guideList.length === 0" class="center-tip">暂无待处理带队申请</div>
      <div v-else class="guide-list">
        <div v-for="item in guideList" :key="item.applyId" class="guide-card">
          <div class="guide-info">
            <div class="guide-team">{{ item.teamName }}</div>
            <div class="guide-meta">
              {{ item.competitionTitle }} · {{ item.memberCount }} 人 · 队长：{{ item.leaderName }}
            </div>
          </div>
          <div class="guide-actions">
            <el-button size="small" type="primary" :loading="item._loading" @click="handleGuide(item, 'APPROVED')">
              同意带队
            </el-button>
            <el-button size="small" :loading="item._loading" @click="handleGuide(item, 'REJECTED')">
              拒绝
            </el-button>
          </div>
        </div>
      </div>
    </template>

    <!-- 驳回原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="填写驳回原因" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写驳回原因（必填）" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const activeTab = ref('guide')   // 默认打开带队申请，方便老师第一次使用
const auditList = ref<any[]>([])
const guideList = ref<any[]>([])
const isLoading = ref(false)
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref<any>(null)

const tagType = (s: string) => (({ APPROVED: 'success', REJECTED: 'danger', PENDING: 'warning', RESUBMITTED: 'warning' } as Record<string, any>)[s] ?? 'info')
const statusText = (s: string) => (({ PENDING: '待审核', RESUBMITTED: '重新提交', APPROVED: '已通过', REJECTED: '已驳回' } as Record<string, string>)[s] ?? s)

const loadAuditList = async () => {
  isLoading.value = true
  try {
    // 查询老师收到的个人赛指导申请（INDIVIDUAL_GUIDE）
    const res: any = await request({ url: '/v1/recruitment/guide/pending', method: 'GET' })
    if (res.code === 0) auditList.value = res.data ?? []
    else ElMessage.error(res.message || '加载失败')
  } catch {
    ElMessage.error('网络错误')
  } finally {
    isLoading.value = false
  }
}

const loadGuideList = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: '/v1/team/team-guide/pending', method: 'GET' })
    if (res.code === 0) guideList.value = (res.data ?? []).map((i: any) => ({ ...i, _loading: false }))
    else ElMessage.error(res.message || '加载失败')
  } catch {
    ElMessage.error('网络错误')
  } finally {
    isLoading.value = false
  }
}

const handleAudit = async (row: any, result: string, reason?: string) => {
  try {
    // 个人赛指导申请：调 /recruitment/guide/{applyId}/handle
    const res: any = await request({
      url: `/v1/recruitment/guide/${row.applyId}/handle`,
      method: 'PUT',
      params: { action: result }
    })
    if (res.code === 0) {
      ElMessage.success(result === 'APPROVED' ? '已同意指导' : '已拒绝')
      loadAuditList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

const handleGuide = async (item: any, action: string) => {
  item._loading = true
  try {
    const res: any = await request({
      url: `/v1/team/team-guide/${item.applyId}/handle`,
      method: 'PUT',
      params: { action }
    })
    if (res.code === 0) {
      ElMessage.success(action === 'APPROVED' ? `已同意带领「${item.teamName}」` : '已拒绝')
      loadGuideList()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    item._loading = false
  }
}

const showRejectDialog = (row: any) => {
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) return ElMessage.warning('请填写驳回原因')
  rejectVisible.value = false
  await handleAudit(currentRow.value, 'REJECTED', rejectReason.value)
}

onMounted(() => {
  loadGuideList()
  loadAuditList()
})

// 监听全局事件：当在通知铃铛中操作后同步刷新列表
const handleApplyEvent = () => {
  loadGuideList()
  loadAuditList()
}
onMounted(() => window.addEventListener('apply-handled', handleApplyEvent))
onUnmounted(() => window.removeEventListener('apply-handled', handleApplyEvent))
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.tabs { display: flex; gap: 0; margin-bottom: 16px; border-bottom: 1px solid #e0e0e0; }
.tab { padding: 8px 20px; font-size: 13px; color: #888; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tab.active { color: #111; font-weight: 600; border-bottom-color: #111; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
.done-text { font-size: 13px; color: #ccc; }

.guide-list { display: flex; flex-direction: column; gap: 12px; }
.guide-card { display: flex; align-items: center; justify-content: space-between; padding: 16px 20px; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; }
.guide-info { flex: 1; }
.guide-team { font-size: 14px; font-weight: 700; color: #111; margin-bottom: 4px; }
.guide-meta { font-size: 12px; color: #aaa; }
.guide-actions { display: flex; gap: 8px; flex-shrink: 0; }
</style>
