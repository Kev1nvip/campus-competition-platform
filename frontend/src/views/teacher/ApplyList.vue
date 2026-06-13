<template>
  <div>
    <div class="page-title">报名审核</div>

    <div class="tabs">
      <span :class="['tab', bizType === 'INDIVIDUAL' ? 'active' : '']" @click="bizType = 'INDIVIDUAL'; loadData()">个人赛</span>
      <span :class="['tab', bizType === 'TEAM' ? 'active' : '']" @click="bizType = 'TEAM'; loadData()">团队赛</span>
    </div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="list.length === 0" class="center-tip">暂无待审核记录</div>
    <el-table v-else :data="list" class="data-table">
      <el-table-column label="报名 ID" prop="id" width="80" />
      <el-table-column label="竞赛名称" prop="competitionTitle" min-width="140">
        <template #default="{ row }: { row: any }">
          {{ row.competitionTitle || `竞赛 #${row.competitionId}` }}
        </template>
      </el-table-column>
      <el-table-column label="学生姓名" prop="studentName" width="100" />
      <el-table-column label="学号" prop="studentNo" width="120" />
      <el-table-column label="院系" prop="department" width="120" />
      <el-table-column label="指导老师" prop="teacherName" width="100" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }: { row: any }">
          <el-tag :type="tagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submittedAt" width="140">
        <template #default="{ row }: { row: any }">
          {{ row.submittedAt ? new Date(row.submittedAt).toLocaleString('zh-CN') : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }: { row: any }">
          <template v-if="row.status === 'PENDING' || row.status === 'RESUBMITTED'">
            <el-button size="small" type="primary" @click="handleAudit(row, 'APPROVED')">通过</el-button>
            <el-button size="small" @click="showRejectDialog(row)" style="margin-left:4px;">驳回</el-button>
          </template>
          <span v-else class="done-text">已处理</span>
        </template>
      </el-table-column>
    </el-table>

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
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref<any[]>([])
const isLoading = ref(false)
const bizType = ref('INDIVIDUAL')
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref<any>(null)

const tagType = (s: string) => (({ APPROVED: 'success', REJECTED: 'danger', PENDING: 'warning', RESUBMITTED: 'warning' } as Record<string, any>)[s] ?? 'info')
const statusText = (s: string) => (({ PENDING: '待审核', RESUBMITTED: '重新提交', APPROVED: '已通过', REJECTED: '已驳回' } as Record<string, string>)[s] ?? s)

const loadData = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: '/v1/signups/individual/pending', method: 'GET', params: { page: 1, size: 50 } })
    if (res.code === 0) list.value = res.data?.list ?? []
    else ElMessage.error(res.message || '加载失败')
  } catch {
    ElMessage.error('网络错误')
  } finally {
    isLoading.value = false
  }
}

const handleAudit = async (row: any, result: string, reason?: string) => {
  try {
    const res: any = await request({
      url: '/v1/audit/signup', method: 'POST',
      data: { bizType: 'INDIVIDUAL', bizId: row.id, result, rejectReason: reason || null }
    })
    if (res.code === 0) {
      ElMessage.success(result === 'APPROVED' ? '已通过' : '已驳回')
      loadData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误')
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

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.tabs { display: flex; gap: 0; margin-bottom: 16px; border-bottom: 1px solid #e0e0e0; }
.tab { padding: 8px 20px; font-size: 13px; color: #888; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; }
.tab.active { color: #111; font-weight: 600; border-bottom-color: #111; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
.done-text { font-size: 13px; color: #ccc; }
</style>
