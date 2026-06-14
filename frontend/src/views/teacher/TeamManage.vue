<template>
  <div>
    <div class="page-title">队伍管理</div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索队伍名称..." clearable size="small" style="width:220px;" @keyup.enter="loadData" />
      <el-button size="small" @click="loadData">搜索</el-button>
    </div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="list.length === 0" class="center-tip">暂无队伍记录</div>
    <el-table v-else :data="filteredList" class="data-table">
      <el-table-column label="队伍 ID" prop="id" width="80" />
      <el-table-column label="队伍名称" prop="teamName" />
      <el-table-column label="成员数" prop="memberCount" width="90" />
      <el-table-column label="竞赛 ID" prop="competitionId" width="100" />
      <el-table-column label="状态" prop="status" width="100">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref<any[]>([])
const isLoading = ref(false)
const keyword = ref('')

const statusText = (s: string) => (({ FORMING: '招募中', FULL: '已满员', SUBMITTED: '已提交', APPROVED: '已通过', REJECTED: '已驳回' } as Record<string, string>)[s] ?? s)
const tagType = (s: string) => (({ APPROVED: 'success', REJECTED: 'danger', SUBMITTED: 'warning', FORMING: 'info' } as Record<string, any>)[s] ?? 'info')

const filteredList = computed(() =>
  keyword.value ? list.value.filter((t: any) => t.teamName?.includes(keyword.value)) : list.value
)

const loadData = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: '/v1/teacher/team/list', method: 'GET' })
    if (res.code === 0) list.value = res.data ?? []
  } finally {
    isLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.toolbar { display: flex; gap: 8px; margin-bottom: 16px; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
</style>
