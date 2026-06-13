<template>
  <div>
    <div class="page-header">
      <h2>队伍大厅</h2>
      <div class="filter-bar">
        <el-select
          v-model="filterCompId"
          placeholder="筛选竞赛"
          clearable
          size="small"
          style="width:200px;"
          @change="fetchHall"
        >
          <el-option
            v-for="c in competitions"
            :key="c.id"
            :label="c.title"
            :value="c.id"
          />
        </el-select>
      </div>
    </div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="teams.length === 0" class="center-tip">
      暂无招募中的队伍
    </div>
    <div v-else class="team-grid">
      <div
        v-for="t in teams"
        :key="t.id"
        class="team-card"
        @click="router.push(`/student/team/${t.id}`)"
      >
        <div class="card-head">
          <span class="team-name">{{ t.teamName }}</span>
          <el-tag size="small" type="success">招募中</el-tag>
        </div>
        <div class="card-info">
          <div class="info-item">
            <span class="label">竞赛</span>
            <span class="val">{{ t.competitionTitle || `#${t.competitionId}` }}</span>
          </div>
          <div class="info-item">
            <span class="label">队长</span>
            <span class="val">{{ t.leaderName }}</span>
          </div>
          <div class="info-item">
            <span class="label">人数</span>
            <span class="val">
              {{ t.memberCount }} 人
              <span v-if="t.minTeamSize" class="sub">（需 {{ t.minTeamSize }}–{{ t.maxTeamSize }} 人）</span>
            </span>
          </div>
          <div class="info-item">
            <span class="label">类型</span>
            <span class="val">{{ t.competitionType === 'TEAM' ? '团队赛' : '个人赛' }}</span>
          </div>
        </div>
        <div class="card-footer">
          点击查看详情并申请加入 →
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { competitionApi } from '@/api/competition'

const router = useRouter()
const teams = ref<any[]>([])
const competitions = ref<any[]>([])
const isLoading = ref(false)
const filterCompId = ref<number | null>(null)

const fetchHall = async () => {
  isLoading.value = true
  try {
    const params: any = {}
    if (filterCompId.value) params.competitionId = filterCompId.value
    const res: any = await request({ url: '/v1/team/hall', method: 'GET', params })
    if (res.code === 0) teams.value = res.data ?? []
    else ElMessage.error(res.message || '加载失败')
  } catch {
    ElMessage.error('网络错误')
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  // 加载竞赛列表用于筛选
  const res = await competitionApi.getList({ page: 1, size: 200 })
  if (res.code === 0) competitions.value = res.data.list
  fetchHall()
})
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
h2 { font-size: 18px; font-weight: 700; color: #111; margin: 0; }
.center-tip { text-align: center; padding: 60px; color: #aaa; font-size: 14px; }

.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.team-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 18px 20px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.team-card:hover {
  border-color: #aaa;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.team-name { font-size: 15px; font-weight: 700; color: #111; }

.card-info { display: flex; flex-direction: column; gap: 6px; }
.info-item { display: flex; gap: 8px; font-size: 13px; }
.label { color: #999; width: 32px; flex-shrink: 0; }
.val { color: #333; }
.sub { color: #aaa; font-size: 12px; }

.card-footer {
  margin-top: 14px;
  font-size: 12px;
  color: #bbb;
  text-align: right;
}
</style>
