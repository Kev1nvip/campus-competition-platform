<template>
  <div>
    <div class="page-title">报名审核</div>

    <div class="tabs">
      <span :class="['tab', activeTab === 'individual' ? 'active' : '']" @click="activeTab = 'individual'; loadIndividual()">
        个人赛 <el-badge v-if="individualTotal > 0" :value="individualTotal" class="tab-badge" />
      </span>
      <span :class="['tab', activeTab === 'team' ? 'active' : '']" @click="activeTab = 'team'; loadTeam()">
        团队赛 <el-badge v-if="teamTotal > 0" :value="teamTotal" class="tab-badge" />
      </span>
    </div>

    <!-- 个人赛待审核 -->
    <template v-if="activeTab === 'individual'">
      <div v-if="isLoading" class="center-tip">加载中...</div>
      <div v-else-if="individualList.length === 0" class="center-tip">暂无待审核个人赛报名</div>
      <el-table v-else :data="individualList" class="data-table">
        <el-table-column label="竞赛名称" prop="competitionTitle" min-width="160" />
        <el-table-column label="学生" width="100">
          <template #default="{ row }: { row: any }">
            <div>{{ row.studentName }}</div>
            <div class="sub-text">{{ row.studentNo }}</div>
          </template>
        </el-table-column>
        <el-table-column label="院系" prop="department" width="120" />
        <el-table-column label="指导老师" prop="teacherName" width="100" />
        <el-table-column label="状态" prop="status" width="100">
          <template #default="{ row }: { row: any }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="140">
          <template #default="{ row }: { row: any }">
            {{ row.submittedAt ? new Date(row.submittedAt).toLocaleString('zh-CN', {month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button size="small" type="primary" @click="approve('INDIVIDUAL', row.id)">通过</el-button>
            <el-button size="small" @click="openReject('INDIVIDUAL', row)" style="margin-left:4px;">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 团队赛待审核 -->
    <template v-if="activeTab === 'team'">
      <div v-if="isLoading" class="center-tip">加载中...</div>
      <div v-else-if="teamList.length === 0" class="center-tip">暂无待审核团队赛报名</div>
      <el-table v-else :data="teamList" class="data-table">
        <el-table-column label="竞赛名称" prop="competitionTitle" min-width="160" />
        <el-table-column label="队伍" width="120">
          <template #default="{ row }: { row: any }">
            <div>{{ row.teamName }}</div>
            <div class="sub-text">{{ row.memberCount }} 人</div>
          </template>
        </el-table-column>
        <el-table-column label="队长" prop="leaderName" width="90" />
        <el-table-column label="指导老师" prop="teacherName" width="100" />
        <el-table-column label="状态" prop="status" width="100">
          <template #default="{ row }: { row: any }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="140">
          <template #default="{ row }: { row: any }">
            {{ row.submittedAt ? new Date(row.submittedAt).toLocaleString('zh-CN', {month:'2-digit',day:'2-digit',hour:'2-digit',minute:'2-digit'}) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }: { row: any }">
            <el-button size="small" type="primary" @click="approve('TEAM', row.id)">通过</el-button>
            <el-button size="small" @click="openReject('TEAM', row)" style="margin-left:4px;">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- 驳回原因弹窗 -->
    <el-dialog v-model="rejectVisible" title="填写驳回原因" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写驳回原因（必填）" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="operating" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const activeTab = ref('individual')
const isLoading = ref(false)
const operating = ref(false)
const individualList = ref<any[]>([])
const teamList = ref<any[]>([])
const individualTotal = ref(0)
const teamTotal = ref(0)
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentBizType = ref('')
const currentRow = ref<any>(null)

const statusTag = (s: string) => (({ PENDING: 'warning', RESUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger' } as Record<string, any>)[s] ?? 'info')
const statusText = (s: string) => (({ PENDING: '待审核', RESUBMITTED: '重新提交', APPROVED: '已通过', REJECTED: '已驳回' } as Record<string, string>)[s] ?? s)

const loadIndividual = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: '/v1/signups/individual/pending', method: 'GET', params: { page: 1, size: 50 } })
    if (res.code === 0) {
      individualList.value = res.data.list ?? []
      individualTotal.value = res.data.total ?? 0
    } else ElMessage.error(res.message || '加载失败')
  } catch { ElMessage.error('网络错误') }
  finally { isLoading.value = false }
}

const loadTeam = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: '/v1/signups/team/pending', method: 'GET', params: { page: 1, size: 50 } })
    if (res.code === 0) {
      teamList.value = res.data.list ?? []
      teamTotal.value = res.data.total ?? 0
    } else ElMessage.error(res.message || '加载失败')
  } catch { ElMessage.error('网络错误') }
  finally { isLoading.value = false }
}

const approve = async (bizType: string, bizId: number) => {
  operating.value = true
  try {
    const res: any = await request({ url: '/v1/audit/signup', method: 'POST', data: { bizType, bizId, result: 'APPROVED' } })
    if (res.code === 0) {
      ElMessage.success('审核通过')
      bizType === 'INDIVIDUAL' ? loadIndividual() : loadTeam()
    } else ElMessage.error(res.message || '操作失败')
  } catch { ElMessage.error('网络错误') }
  finally { operating.value = false }
}

const openReject = (bizType: string, row: any) => {
  currentBizType.value = bizType
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) return ElMessage.warning('请填写驳回原因')
  operating.value = true
  rejectVisible.value = false
  try {
    const res: any = await request({
      url: '/v1/audit/signup', method: 'POST',
      data: { bizType: currentBizType.value, bizId: currentRow.value.id, result: 'REJECTED', rejectReason: rejectReason.value }
    })
    if (res.code === 0) {
      ElMessage.success('已驳回')
      currentBizType.value === 'INDIVIDUAL' ? loadIndividual() : loadTeam()
    } else ElMessage.error(res.message || '操作失败')
  } catch { ElMessage.error('网络错误') }
  finally { operating.value = false }
}

onMounted(() => { loadIndividual(); loadTeam() })
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.tabs { display: flex; gap: 0; margin-bottom: 16px; border-bottom: 1px solid #e0e0e0; }
.tab { padding: 8px 20px; font-size: 13px; color: #888; cursor: pointer; border-bottom: 2px solid transparent; margin-bottom: -1px; display: flex; align-items: center; gap: 6px; }
.tab.active { color: #111; font-weight: 600; border-bottom-color: #111; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
.sub-text { font-size: 11px; color: #aaa; margin-top: 2px; }
</style>
