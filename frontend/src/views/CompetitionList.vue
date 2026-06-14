<template>
  <div>
    <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-select
          v-model="filters.type"
          placeholder="竞赛类型"
          clearable
          size="small"
          style="width:120px;"
          @change="handleFilterChange"
          @clear="handleFilterChange"
        >
          <el-option label="个人赛" value="INDIVIDUAL" />
          <el-option label="团队赛" value="TEAM" />
        </el-select>
        <el-select
          v-model="filters.status"
          placeholder="竞赛状态"
          clearable
          size="small"
          style="width:120px;"
          @change="handleFilterChange"
          @clear="handleFilterChange"
        >
          <el-option label="未开始" value="UPCOMING" />
          <el-option label="报名中" value="SIGNING" />
          <el-option label="报名截止" value="CLOSED" />
          <el-option label="进行中" value="ONGOING" />
          <el-option label="已结束" value="FINISHED" />
        </el-select>
        <el-input
          v-model="filters.keyword"
          placeholder="搜索竞赛名称..."
          clearable
          size="small"
          style="width:220px;"
          @keyup.enter="handleFilterChange"
          @clear="handleFilterChange"
        />
        <el-button size="small" type="primary" @click="handleFilterChange">搜索</el-button>
        <el-button size="small" @click="handleReset">重置</el-button>
      </div>

      <!-- 列表 -->
      <div v-if="isLoading" class="center-tip">加载中...</div>
      <div v-else-if="list.length === 0" class="center-tip">暂无竞赛信息</div>
      <div v-else class="comp-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="comp-row"
          @click="router.push(`/student/competition/${item.id}`)"
        >
          <div class="comp-row-left">
            <el-tag :type="item.type === 'INDIVIDUAL' ? '' : 'info'" size="small" effect="plain">
              {{ typeMap[item.type] }}
            </el-tag>
            <span class="comp-title">{{ item.title }}</span>
            <span class="comp-org">{{ item.organizer }}</span>
          </div>
          <div class="comp-row-right">
            <span class="comp-date">{{ formatDate(item.signupEnd) }} 截止</span>
            <el-tag :type="statusTagType(item.status)" size="small">{{ statusMap[item.status] }}</el-tag>
            <span class="arrow">→</span>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-if="pagination.total > pagination.size"
        v-model:current-page="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="prev, pager, next, total"
        background
        small
        class="pagination"
        @current-change="fetchList"
      />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { competitionApi } from '@/api/competition'
import type { CompetitionVO } from '@/types/competition'
import { statusMap, typeMap } from '@/types/competition'

const router = useRouter()
const list = ref<CompetitionVO[]>([])
const isLoading = ref(false)
const pagination = reactive({ page: 1, size: 12, total: 0 })
const filters = reactive({ status: '', type: '', keyword: '' })

const statusTagType = (status: string) => {
  const map: Record<string, any> = { SIGNING: 'success', UPCOMING: 'info', CLOSED: 'warning', ONGOING: '', FINISHED: 'danger' }
  return map[status] ?? 'info'
}

const formatDate = (str: string) => {
  if (!str) return '-'
  return new Date(str).toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }).replace('/', '/')
}

const fetchList = async () => {
  isLoading.value = true
  try {
    const res = await competitionApi.getList({ page: pagination.page, size: pagination.size, ...filters })
    if (res.code === 0) {
      list.value = res.data.list
      pagination.total = res.data.total
    }
  } finally {
    isLoading.value = false
  }
}

// 筛选条件变化时重置页码并重新请求
const handleFilterChange = () => {
  pagination.page = 1
  fetchList()
}

const handleReset = () => {
  filters.status = ''
  filters.type = ''
  filters.keyword = ''
  pagination.page = 1
  fetchList()
}

onMounted(fetchList)
</script>

<style scoped>
.filter-bar { display: flex; gap: 8px; align-items: center; margin-bottom: 20px; flex-wrap: wrap; }
.center-tip { text-align: center; padding: 60px; color: #aaa; font-size: 14px; }
.comp-list { border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden; }
.comp-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.12s;
  gap: 12px;
}
.comp-row:last-child { border-bottom: none; }
.comp-row:hover { background: #fafafa; }
.comp-row-left { display: flex; align-items: center; gap: 12px; flex: 1; min-width: 0; }
.comp-title { font-size: 14px; font-weight: 600; color: #111; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.comp-org { font-size: 12px; color: #aaa; white-space: nowrap; }
.comp-row-right { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.comp-date { font-size: 12px; color: #999; white-space: nowrap; }
.arrow { color: #ccc; font-size: 14px; }
.pagination { margin-top: 20px; justify-content: center; }
</style>
