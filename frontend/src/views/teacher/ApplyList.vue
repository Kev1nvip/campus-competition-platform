<template>
  <div>
    <div class="page-title">报名审核</div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="list.length === 0" class="center-tip">暂无待审核记录</div>
    <el-table v-else :data="list" class="data-table">
      <el-table-column label="报名 ID" prop="id" width="80" />
      <el-table-column label="学生 ID" prop="studentId" width="100" />
      <el-table-column label="竞赛 ID" prop="competitionId" width="100" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" prop="submittedAt" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING' || row.status === 'RESUBMITTED'">
            <el-button size="small" type="primary" @click="handleAudit(row, 'APPROVED')">通过</el-button>
            <el-button size="small" @click="handleAudit(row, 'REJECTED')" style="margin-left:4px;">驳回</el-button>
          </template>
          <span v-else class="done-text">已处理</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref<any[]>([])
const isLoading = ref(false)

const tagType = (s: string) => ({ APPROVED: 'success', REJECTED: 'danger', PENDING: 'warning', RESUBMITTED: 'warning' }[s] ?? 'info') as any

const loadData = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: '/v1/signups/individual/pending', method: 'GET' })
    if (res.code === 0) list.value = res.data?.list ?? []
  } finally {
    isLoading.value = false
  }
}

const handleAudit = async (row: any, result: string) => {
  try {
    const res: any = await request({
      url: '/v1/audit/signup', method: 'POST',
      data: { bizType: 'INDIVIDUAL', bizId: row.id, result }
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

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
.done-text { font-size: 13px; color: #ccc; }
</style>
