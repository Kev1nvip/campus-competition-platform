<template>
  <div>
    <!-- 待处理邀请 -->
    <div v-if="pendingInvites.length > 0" class="invite-section">
      <div class="section-title">
        待处理邀请
        <el-badge :value="pendingInvites.length" class="badge" />
      </div>
      <div class="invite-list">
        <div v-for="inv in pendingInvites" :key="inv.applyId" class="invite-card">
          <div class="invite-info">
            <div class="invite-team">{{ inv.teamName }}</div>
            <div class="invite-meta">
              {{ inv.competitionTitle }} · 当前 {{ inv.memberCount }} 人 · 队长：{{ inv.leaderName }}
            </div>
          </div>
          <div class="invite-actions">
            <el-button size="small" type="primary" :loading="inv._loading" @click="handleInvite(inv, 'APPROVED')">
              接受
            </el-button>
            <el-button size="small" :loading="inv._loading" @click="handleInvite(inv, 'REJECTED')">
              拒绝
            </el-button>
          </div>
        </div>
      </div>
      <el-divider />
    </div>

    <div class="page-header">
      <h2>我的队伍</h2>
      <el-button type="primary" size="small" @click="openCreate">创建队伍</el-button>
    </div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="teams.length === 0" class="center-tip">
      暂无队伍，点击右上角创建或前往队伍大厅申请加入
    </div>
    <div v-else class="team-list">
      <div
        v-for="t in teams"
        :key="t.id"
        class="team-row"
        @click="router.push(`/student/team/${t.id}`)"
      >
        <div class="team-row-left">
          <span class="team-name">{{ t.teamName }}</span>
          <el-tag size="small" :type="statusTagType(t.status)" effect="plain">
            {{ statusText(t.status) }}
          </el-tag>
          <el-tag v-if="t.leaderId === currentUserId" size="small" type="info" effect="plain">我是队长</el-tag>
          <el-tag v-else size="small" effect="plain">我是队员</el-tag>
        </div>
        <div class="team-row-right">
          <span class="team-meta">{{ t.memberCount }} 人</span>
          <span class="comp-name">{{ t.competitionTitle || `竞赛 #${t.competitionId}` }}</span>
          <span class="arrow">→</span>
        </div>
      </div>
    </div>

    <!-- 创建队伍弹窗 -->
    <el-dialog v-model="showCreate" title="创建队伍" width="440px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="队伍名称" required>
          <el-input v-model="createForm.teamName" placeholder="请输入队伍名称（最长64字）" />
        </el-form-item>
        <el-form-item label="关联竞赛" required>
          <el-select
            v-model="createForm.competitionId"
            placeholder="请选择要参加的竞赛"
            style="width:100%;"
            filterable
            :loading="compLoading"
          >
            <el-option
              v-for="c in competitions"
              :key="c.id"
              :label="c.title"
              :value="c.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { competitionApi } from '@/api/competition'

const router = useRouter()
const teams = ref<any[]>([])
const pendingInvites = ref<any[]>([])
const competitions = ref<any[]>([])
const isLoading = ref(false)
const compLoading = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const createForm = reactive({ teamName: '', competitionId: null as number | null })

const currentUserId = computed(() => {
  const info = localStorage.getItem('userInfo')
  return info ? JSON.parse(info).userId : null
})

const statusText = (s: string) => (({
  FORMING: '招募中', FULL: '已满员', SUBMITTED: '已提交',
  APPROVED: '已通过', REJECTED: '已驳回', DISMISSED: '已解散'
} as Record<string, string>)[s] ?? s)

const statusTagType = (s: string) => (({
  FORMING: 'success', FULL: 'info', SUBMITTED: 'warning',
  APPROVED: '', REJECTED: 'danger', DISMISSED: 'info'
} as Record<string, any>)[s] ?? 'info')

const fetchTeams = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: '/v1/team/my', method: 'GET' })
    if (res.code === 0) teams.value = res.data ?? []
    else ElMessage.error(res.message || '加载失败')
  } catch {
    ElMessage.error('网络错误')
  } finally {
    isLoading.value = false
  }
}

const fetchCompetitions = async () => {
  compLoading.value = true
  try {
    const res = await competitionApi.getList({ page: 1, size: 200, type: 'TEAM' })
    if (res.code === 0) competitions.value = res.data.list
  } finally {
    compLoading.value = false
  }
}

const openCreate = async () => {
  if (competitions.value.length === 0) await fetchCompetitions()
  createForm.teamName = ''
  createForm.competitionId = null
  showCreate.value = true
}

const handleCreate = async () => {
  if (!createForm.teamName.trim()) return ElMessage.warning('请填写队伍名称')
  if (!createForm.competitionId) return ElMessage.warning('请选择关联竞赛')
  creating.value = true
  try {
    const res: any = await request({
      url: '/v1/team', method: 'POST',
      data: { teamName: createForm.teamName, competitionId: createForm.competitionId }
    })
    if (res.code === 0) {
      ElMessage.success('创建成功')
      showCreate.value = false
      await fetchTeams()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    creating.value = false
  }
}

const fetchPendingInvites = async () => {
  try {
    const res: any = await request({ url: '/v1/team/invites/pending', method: 'GET' })
    if (res.code === 0) {
      pendingInvites.value = (res.data ?? []).map((inv: any) => ({ ...inv, _loading: false }))
    }
  } catch { /* ignore */ }
}

const handleInvite = async (inv: any, action: 'APPROVED' | 'REJECTED') => {
  inv._loading = true
  try {
    const res: any = await request({
      url: `/v1/team/invites/${inv.applyId}/handle`,
      method: 'PUT',
      params: { action }
    })
    if (res.code === 0) {
      ElMessage.success(action === 'APPROVED' ? `已接受加入「${inv.teamName}」` : '已拒绝邀请')
      // 刷新邀请列表和队伍列表
      await fetchPendingInvites()
      if (action === 'APPROVED') await fetchTeams()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    inv._loading = false
  }
}


onMounted(async () => {
  await fetchCompetitions()
  fetchTeams()
  fetchPendingInvites()
})
</script>

<style scoped>
.invite-section { margin-bottom: 4px; }
.section-title { font-size: 14px; font-weight: 700; color: #111; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; }
.invite-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 16px; }
.invite-card { display: flex; align-items: center; justify-content: space-between; padding: 14px 18px; background: #fff; border: 1px solid #e0e0e0; border-radius: 8px; }
.invite-info { flex: 1; }
.invite-team { font-size: 14px; font-weight: 700; color: #111; margin-bottom: 4px; }
.invite-meta { font-size: 12px; color: #aaa; }
.invite-actions { display: flex; gap: 8px; flex-shrink: 0; }
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
h2 { font-size: 18px; font-weight: 700; color: #111; margin: 0; }
.center-tip { text-align: center; padding: 60px; color: #aaa; font-size: 14px; }
.team-list { border: 1px solid #e0e0e0; border-radius: 6px; overflow: hidden; }
.team-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.12s;
}
.team-row:last-child { border-bottom: none; }
.team-row:hover { background: #fafafa; }
.team-row-left { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.team-name { font-size: 14px; font-weight: 600; color: #111; }
.team-row-right { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }
.team-meta { font-size: 13px; color: #aaa; }
.comp-name { font-size: 12px; color: #bbb; max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.arrow { color: #ccc; }
</style>
