<template>
  <div>
    <div class="page-inner">
      <div class="page-header">
        <h2>队伍广场</h2>
        <el-button type="primary" size="small" @click="showCreate = true">创建队伍</el-button>
      </div>

      <div v-if="isLoading" class="center-tip">加载中...</div>
      <div v-else-if="teams.length === 0" class="center-tip">暂无队伍信息</div>
      <div v-else class="team-list">
        <div
          v-for="t in teams"
          :key="t.id"
          class="team-row"
          @click="router.push(`/student/team/${t.id}`)"
        >
          <div class="team-row-left">
            <span class="team-name">{{ t.teamName }}</span>
            <el-tag size="small" effect="plain" :type="t.status === 'FORMING' ? 'success' : 'info'">
              {{ statusText(t.status) }}
            </el-tag>
          </div>
          <div class="team-row-right">
            <span class="team-meta">{{ t.memberCount }} 人</span>
            <span class="arrow">→</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建队伍弹窗 -->
    <el-dialog v-model="showCreate" title="创建队伍" width="420px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="队伍名称">
          <el-input v-model="createForm.teamName" placeholder="请输入队伍名称" />
        </el-form-item>
        <el-form-item label="竞赛 ID">
          <el-input v-model="createForm.competitionId" placeholder="竞赛 ID" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const teams = ref<any[]>([])
const isLoading = ref(false)
const showCreate = ref(false)
const createForm = reactive({ teamName: '', competitionId: '' })

const statusText = (s: string) => (({ FORMING: '招募中', FULL: '已满员', SUBMITTED: '已提交', APPROVED: '已通过', REJECTED: '已驳回', DISMISSED: '已解散' } as Record<string, string>)[s] ?? s)

const fetchTeams = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: '/v1/teams', method: 'GET' })
    if (res.code === 0) teams.value = res.data?.list ?? []
  } finally {
    isLoading.value = false
  }
}

const handleCreate = async () => {
  if (!createForm.teamName.trim() || !createForm.competitionId) {
    ElMessage.warning('请填写队伍名称和竞赛 ID')
    return
  }
  try {
    const res: any = await request({ url: '/v1/team', method: 'POST', data: { teamName: createForm.teamName, competitionId: Number(createForm.competitionId) } })
    if (res.code === 0) {
      ElMessage.success('创建成功')
      showCreate.value = false
      createForm.teamName = ''
      createForm.competitionId = ''
      fetchTeams()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

onMounted(fetchTeams)
</script>

<style scoped>
.page-inner { max-width: 800px; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
h2 { font-size: 18px; font-weight: 700; color: #111; margin: 0; }
.center-tip { text-align: center; padding: 60px; color: #aaa; font-size: 14px; }
.team-list { border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden; }
.team-row { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background 0.12s; }
.team-row:last-child { border-bottom: none; }
.team-row:hover { background: #fafafa; }
.team-row-left { display: flex; align-items: center; gap: 12px; }
.team-name { font-size: 14px; font-weight: 600; color: #111; }
.team-row-right { display: flex; align-items: center; gap: 12px; }
.team-meta { font-size: 13px; color: #aaa; }
.arrow { color: #ccc; }
</style>
