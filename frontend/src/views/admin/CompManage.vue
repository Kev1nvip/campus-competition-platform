<template>
  <div>
    <div class="page-title">竞赛管理</div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="list.length === 0" class="center-tip">暂无竞赛数据</div>
    <el-table v-else :data="list" class="data-table">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="竞赛名称" prop="title" />
      <el-table-column label="类型" prop="type" width="90">
        <template #default="{ row }: { row: any }">
          <el-tag size="small" effect="plain">{{ row.type === 'INDIVIDUAL' ? '个人赛' : '团队赛' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="主办方" prop="organizer" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }: { row: any }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="报名截止" prop="signupEnd" width="120">
        <template #default="{ row }: { row: any }">{{ formatDate(row.signupEnd) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }: { row: any }">
          <el-button size="small" text @click="handleOffline(row)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="total > pageSize"
      v-model:current-page="page"
      :page-size="pageSize"
      :total="total"
      layout="prev, pager, next, total"
      background
      small
      class="pagination"
      @current-change="loadData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCompAllList } from '@/api/admin'
import request from '@/utils/request'

const list = ref<any[]>([])
const isLoading = ref(false)
const page = ref(1)
const pageSize = 15
const total = ref(0)

const statusText = (s: string) => (({ UPCOMING: '未开始', SIGNING: '报名中', CLOSED: '已截止', ONGOING: '进行中', FINISHED: '已结束', OFFLINE: '已下架' } as Record<string, string>)[s] ?? s)
const statusTag = (s: string) => (({ SIGNING: 'success', UPCOMING: 'info', CLOSED: 'warning', FINISHED: 'danger', OFFLINE: 'info' } as Record<string, any>)[s] ?? 'info')
const formatDate = (s: string) => s ? new Date(s).toLocaleDateString('zh-CN') : '-'

const loadData = async () => {
  isLoading.value = true
  try {
    const res: any = await getCompAllList()
    if (res.code === 0) {
      list.value = res.data?.list ?? res.data ?? []
      total.value = res.data?.total ?? list.value.length
    }
  } finally {
    isLoading.value = false
  }
}

const handleOffline = async (row: any) => {
  try {
    const res: any = await request({ url: `/v1/competitions/${row.id}/status`, method: 'PATCH', data: { action: 'OFFLINE' } })
    if (res.code === 0) { ElMessage.success('已下架'); loadData() }
    else ElMessage.error(res.message || '操作失败')
  } catch { ElMessage.error('网络错误') }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
.pagination { margin-top: 16px; justify-content: center; }
</style>
