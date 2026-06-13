<template>
  <div>
    <div class="back-link" @click="router.push('/student/my-teams')">← 返回我的队伍</div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="!team" class="center-tip">队伍不存在</div>

    <template v-else>
      <!-- 标题区 -->
      <div class="detail-head">
        <div class="head-left">
          <h2>{{ team.teamName }}</h2>
          <el-tag size="small" :type="statusTagType(team.status)">{{ statusText(team.status) }}</el-tag>
        </div>
        <div class="head-actions">
          <!-- 队长：申请老师带队（未有老师时） -->
          <el-button
            v-if="currentUserRole === 'LEADER' && !team.teacherConfirmed && team.status === 'FORMING'"
            size="small"
            @click="showSelectTeacher = true"
          >
            申请老师带队
          </el-button>
          <!-- 队长：可邀请队友（老师已确认） -->
          <el-button
            v-if="currentUserRole === 'LEADER' && team.status === 'FORMING'"
            size="small"
            type="primary"
            @click="showInvite = true"
          >
            邀请队友
          </el-button>
          <!-- 队员：可退出（FORMING/FULL 状态） -->
          <el-button
            v-if="currentUserRole === 'MEMBER' && (team.status === 'FORMING' || team.status === 'FULL')"
            size="small"
            @click="handleQuit"
          >
            退出队伍
          </el-button>
          <!-- 非成员：可申请加入（FORMING 状态） -->
          <el-button
            v-if="currentUserRole === 'NONE' && team.status === 'FORMING'"
            size="small"
            type="primary"
            :loading="applying"
            @click="showApply = true"
          >
            申请加入
          </el-button>
        </div>
      </div>

      <el-divider />

      <!-- 基本信息 -->
      <div class="info-grid">
        <div class="info-block">
          <div class="info-label">关联竞赛</div>
          <div class="info-val">{{ team.competitionTitle || `竞赛 #${team.competitionId}` }}</div>
        </div>
        <div class="info-block">
          <div class="info-label">竞赛类型</div>
          <div class="info-val">{{ team.competitionType === 'TEAM' ? '团队赛' : '个人赛' }}</div>
        </div>
        <div class="info-block">
          <div class="info-label">队伍人数</div>
          <div class="info-val">
            {{ team.memberCount }} 人
            <span v-if="team.minTeamSize" class="sub">
              （要求 {{ team.minTeamSize }}–{{ team.maxTeamSize }} 人）
            </span>
          </div>
        </div>
        <div class="info-block">
          <div class="info-label">队长</div>
          <div class="info-val">{{ team.leaderName }}</div>
        </div>
        <div class="info-block">
          <div class="info-label">指导老师</div>
          <div class="info-val">
            <span v-if="team.teacherName">
              {{ team.teacherName }}
              <span v-if="team.teacherTitle" class="sub">{{ team.teacherTitle }}</span>
              <el-tag v-if="team.teacherConfirmed" size="small" type="success" style="margin-left:6px;">已确认</el-tag>
              <el-tag v-else size="small" type="warning" style="margin-left:6px;">待确认</el-tag>
            </span>
            <span v-else class="none-text">暂未分配</span>
          </div>
        </div>
      </div>

      <el-divider />

      <!-- 成员列表 -->
      <div class="section">
        <div class="section-title">成员列表</div>
        <div v-if="!team.members || team.members.length === 0" class="center-tip" style="padding:20px;">
          暂无成员记录
        </div>
        <el-table v-else :data="team.members" class="member-table">
          <el-table-column label="姓名" prop="realName" width="120" />
          <el-table-column label="学号" prop="studentNo" width="140" />
          <el-table-column label="院系" prop="department" />
          <el-table-column label="角色" prop="role" width="90">
            <template #default="{ row }: { row: any }">
              <el-tag size="small" :type="row.role === 'LEADER' ? '' : 'info'">
                {{ row.role === 'LEADER' ? '队长' : '队员' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" prop="joinedAt" width="140">
            <template #default="{ row }: { row: any }">
              {{ row.joinedAt ? new Date(row.joinedAt).toLocaleDateString('zh-CN') : '—' }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- 邀请队友弹窗 -->
    <el-dialog v-model="showInvite" title="邀请队友" width="400px">
      <el-form @submit.prevent label-width="80px">
        <el-form-item label="学号/姓名">
          <el-input
            v-model="inviteKeyword"
            placeholder="输入学号或姓名搜索"
            clearable
            @keydown.enter.prevent="searchStudent"
          >
            <template #append>
              <el-button @click="searchStudent" :loading="searching">搜索</el-button>
            </template>
          </el-input>
        </el-form-item>
        <div v-if="searchResults.length > 0" class="search-results">
          <div
            v-for="s in searchResults"
            :key="s.id"
            class="student-card"
            :class="{ selected: selectedStudent?.id === s.id }"
            @click="selectedStudent = s"
          >
            <div>
              <div class="s-name">{{ s.realName }}</div>
              <div class="s-no">{{ s.studentNo }} · {{ s.department }}</div>
            </div>
            <span v-if="selectedStudent?.id === s.id" class="check-icon">✓</span>
          </div>
        </div>
        <div v-else-if="searched && searchResults.length === 0" class="no-result">未找到该学生</div>
      </el-form>
      <template #footer>
        <el-button @click="closeInvite">取消</el-button>
        <el-button type="primary" :disabled="!selectedStudent" :loading="inviting" @click="sendInvite">
          发送邀请
        </el-button>
      </template>
    </el-dialog>

    <!-- 申请加入弹窗 -->
    <el-dialog v-model="showApply" title="申请加入队伍" width="380px">
      <el-form label-width="80px">
        <el-form-item label="申请理由">
          <el-input v-model="applyMotivation" type="textarea" :rows="3" placeholder="选填，向队长介绍自己" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApply = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
    <!-- 申请老师带队弹窗 -->
    <el-dialog v-model="showSelectTeacher" title="申请老师带队" width="420px">
      <div class="teacher-search">
        <el-input
          v-model="teacherKeyword"
          placeholder="搜索老师姓名或院系..."
          clearable
          @input="fetchTeachersForGuide"
          size="small"
        />
      </div>
      <div v-if="teacherListLoading" class="tip">加载中...</div>
      <div v-else-if="teacherList.length === 0" class="tip">暂无可选老师</div>
      <div v-else class="teacher-list-select">
        <div
          v-for="t in teacherList"
          :key="t.id"
          :class="['teacher-opt', selectedTeacherForGuide?.id === t.id ? 'selected' : '']"
          @click="selectedTeacherForGuide = t"
        >
          <div class="t-name">{{ t.realName }} <span class="t-title">{{ t.title }}</span></div>
          <div class="t-dept">{{ t.department }}</div>
          <span v-if="selectedTeacherForGuide?.id === t.id" class="check-icon">✓</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showSelectTeacher = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="!selectedTeacherForGuide"
          :loading="applyingTeacher"
          @click="submitApplyTeacher"
        >
          发送申请
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

const team = ref<any>(null)
const isLoading = ref(false)

// 当前用户 ID
const currentUserId = computed(() => {
  const info = localStorage.getItem('userInfo')
  return info ? Number(JSON.parse(info).userId) : null
})

// 当前用户在队伍中的角色（兼容旧数据）
const currentUserRole = computed(() => {
  if (!team.value) return 'NONE'
  // 优先使用后端返回的角色
  const serverRole = team.value.currentUserRole
  if (serverRole === 'LEADER' || serverRole === 'MEMBER') return serverRole
  // 后端返回 NONE 时，再用 leaderId 二次判断（兼容 Bug1 修复前的旧数据）
  if (currentUserId.value && team.value.leaderId === currentUserId.value) return 'LEADER'
  return 'NONE'
})

// 邀请
const showInvite = ref(false)
const inviteKeyword = ref('')
const searchResults = ref<any[]>([])
const selectedStudent = ref<any>(null)
const searched = ref(false)
const searching = ref(false)
const inviting = ref(false)

// 申请加入
const showApply = ref(false)
const applyMotivation = ref('')
const applying = ref(false)

// 申请老师带队
const showSelectTeacher = ref(false)
const teacherKeyword = ref('')
const teacherList = ref<any[]>([])
const teacherListLoading = ref(false)
const selectedTeacherForGuide = ref<any>(null)
const applyingTeacher = ref(false)

const statusText = (s: string) => (({
  FORMING: '招募中', FULL: '已满员', SUBMITTED: '已提交',
  APPROVED: '已通过', REJECTED: '已驳回', DISMISSED: '已解散'
} as Record<string, string>)[s] ?? s)

const statusTagType = (s: string) => (({
  FORMING: 'success', FULL: 'info', SUBMITTED: 'warning',
  APPROVED: '', REJECTED: 'danger', DISMISSED: 'info'
} as Record<string, any>)[s] ?? 'info')

const fetchTeam = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: `/v1/team/${route.params.id}`, method: 'GET' })
    if (res.code === 0) {
      team.value = res.data
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    isLoading.value = false
  }
}

const handleQuit = async () => {
  try {
    await ElMessageBox.confirm('确认退出该队伍？', '提示', { type: 'warning' })
    const res: any = await request({ url: `/v1/team/${route.params.id}/quit`, method: 'DELETE' })
    if (res.code === 0) {
      ElMessage.success('已退出队伍')
      router.push('/student/my-teams')
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e: any) {
    if (e !== 'cancel' && e?.message !== 'cancel') ElMessage.error('操作失败')
  }
}

const searchStudent = async () => {
  if (!inviteKeyword.value.trim()) return
  searching.value = true
  searched.value = false
  try {
    const res: any = await request({
      url: '/v1/team/search-student',
      method: 'GET',
      params: { keyword: inviteKeyword.value.trim() }
    })
    if (res.code === 0) {
      searchResults.value = res.data ?? []
    } else {
      searchResults.value = []
      ElMessage.error(res.message || '搜索失败')
    }
  } catch {
    searchResults.value = []
    ElMessage.error('搜索失败')
  } finally {
    searching.value = false
    searched.value = true
  }
}

const closeInvite = () => {
  showInvite.value = false
  inviteKeyword.value = ''
  searchResults.value = []
  selectedStudent.value = null
  searched.value = false
}

const sendInvite = async () => {
  if (!selectedStudent.value) return
  inviting.value = true
  try {
    const res: any = await request({
      url: `/v1/team/${route.params.id}/invite`,
      method: 'POST',
      params: { targetUserId: selectedStudent.value.id }
    })
    if (res.code === 0) {
      ElMessage.success(`已向 ${selectedStudent.value.realName} 发送邀请`)
      closeInvite()
    } else {
      ElMessage.error(res.message || '邀请失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    inviting.value = false
  }
}

const submitApply = async () => {
  applying.value = true
  try {
    const res: any = await request({
      url: `/v1/team/${route.params.id}/apply`,
      method: 'POST',
      data: { motivation: applyMotivation.value || null }
    })
    if (res.code === 0) {
      ElMessage.success('申请已提交，等待队长审核')
      showApply.value = false
      applyMotivation.value = ''
    } else {
      ElMessage.error(res.message || '申请失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    applying.value = false
  }
}

const fetchTeachersForGuide = async () => {
  teacherListLoading.value = true
  try {
    const res: any = await request({ url: '/v1/user/teachers', method: 'GET', params: { keyword: teacherKeyword.value || undefined, page: 1, size: 50 } })
    if (res.code === 0) teacherList.value = res.data?.list ?? []
  } finally {
    teacherListLoading.value = false
  }
}

const submitApplyTeacher = async () => {
  if (!selectedTeacherForGuide.value) return
  applyingTeacher.value = true
  try {
    const res: any = await request({
      url: `/v1/team/${route.params.id}/apply-teacher`,
      method: 'POST',
      params: { teacherId: selectedTeacherForGuide.value.id }
    })
    if (res.code === 0) {
      ElMessage.success(`已向 ${selectedTeacherForGuide.value.realName} 发送带队申请`)
      showSelectTeacher.value = false
      selectedTeacherForGuide.value = null
      teacherKeyword.value = ''
    } else {
      ElMessage.error(res.message || '申请失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    applyingTeacher.value = false
  }
}

onMounted(async () => {
  fetchTeam()
  fetchTeachersForGuide()
})
</script>

<style scoped>
.back-link {
  font-size: 13px;
  color: #888;
  cursor: pointer;
  margin-bottom: 20px;
  display: inline-block;
}
.back-link:hover { color: #111; }

.center-tip { text-align: center; padding: 80px; color: #aaa; }

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

h2 { font-size: 20px; font-weight: 700; color: #111; margin: 0; }

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px 32px;
  padding: 4px 0;
}

.info-label { font-size: 12px; color: #999; margin-bottom: 4px; }
.info-val { font-size: 14px; font-weight: 600; color: #111; display: flex; align-items: center; flex-wrap: wrap; gap: 4px; }
.sub { font-size: 12px; color: #aaa; font-weight: 400; }
.none-text { font-size: 14px; color: #bbb; font-weight: 400; }

.section { margin-top: 4px; }
.section-title { font-size: 14px; font-weight: 700; color: #111; margin-bottom: 12px; }
.member-table { width: 100%; border: 1px solid #e0e0e0; border-radius: 6px; }

.search-results {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
  max-height: 220px;
  overflow-y: auto;
}

.student-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.12s;
  user-select: none;
}
.student-card:hover { border-color: #aaa; background: #fafafa; }
.student-card.selected { border-color: #111; background: #f4f4f4; }

.check-icon { font-weight: 700; color: #111; font-size: 16px; }

.no-result { text-align: center; padding: 16px; color: #aaa; font-size: 13px; }

.s-name { font-size: 14px; font-weight: 600; color: #111; }
.s-no { font-size: 12px; color: #aaa; margin-top: 2px; }

.teacher-search { margin-bottom: 10px; }
.tip { text-align: center; padding: 16px; color: #aaa; font-size: 13px; }
.teacher-list-select { display: flex; flex-direction: column; gap: 8px; max-height: 260px; overflow-y: auto; }
.teacher-opt {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.12s;
  user-select: none;
}
.teacher-opt:hover { border-color: #aaa; background: #fafafa; }
.teacher-opt.selected { border-color: #111; background: #f4f4f4; }
.t-name { font-size: 14px; font-weight: 600; color: #111; }
.t-title { font-size: 12px; color: #888; font-weight: 400; margin-left: 6px; background: #f4f4f4; padding: 1px 6px; border-radius: 3px; }
.t-dept { font-size: 12px; color: #aaa; margin-top: 3px; }
</style>
