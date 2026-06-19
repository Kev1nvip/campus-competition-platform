<template>
  <div>
    <div class="back-link" @click="router.push(`/student/competition/${route.params.id}`)">← 返回竞赛详情</div>

    <div class="page-inner">
      <div v-if="compLoading" class="center-tip">加载中...</div>

      <template v-else>
        <h2>{{ competition?.type === 'TEAM' ? '团队赛报名' : '个人赛报名' }}</h2>

        <!-- 团队赛报名 -->
        <template v-if="competition?.type === 'TEAM'">
          <el-form :model="form" label-width="90px" class="signup-form" style="margin-top:20px;">
            <el-form-item label="选择队伍" required>
              <el-select
                v-model="selectedTeamId"
                placeholder="请选择要报名的队伍"
                style="width:100%;"
                filterable
                :loading="teamLoading"
              >
                <el-option
                  v-for="t in myTeams"
                  :key="t.id"
                  :label="t.teamName + '（' + t.memberCount + '人' + (t.teacherConfirmed ? '· 老师已确认' : '· 待确认') + '）'"
                  :value="t.id"
                />
              </el-select>
              <div v-if="myTeams.length === 0 && !teamLoading" class="tip" style="margin-top:6px;">
                暂无可报名的队伍。请先前往「我的队伍」创建队伍并完成指导老师确认。
              </div>
            </el-form-item>

            <div v-if="selectedTeamId" class="team-info">
              <el-alert
                type="info"
                :closable="false"
                show-icon
              >
                <template #default>
                  队伍人数要求：{{ competition.minTeamSize }} — {{ competition.maxTeamSize }} 人。
                  你的队伍已有 {{ selectedTeamInfo?.memberCount ?? '?' }} 人。
                  <span v-if="selectedTeamInfo?.teacherConfirmed">指导老师已确认 ✅</span>
                  <span v-else style="color:#e6a23c;">指导老师尚未确认 ⚠️</span>
                </template>
              </el-alert>
            </div>

            <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>

            <el-form-item>
              <el-button type="primary" size="large" :loading="submitting" @click="handleTeamSubmit">
                报名提交管理员审核
              </el-button>
            </el-form-item>
          </el-form>
        </template>

        <!-- 个人赛 -->
        <template v-else>
          <el-form :model="form" label-width="90px" class="signup-form" style="margin-top:20px;">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="11位手机号" />
            </el-form-item>
            <el-form-item label="电子邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="参赛动机">
              <el-input v-model="form.motivation" type="textarea" :rows="3" placeholder="选填" />
            </el-form-item>
            <el-form-item label="个人简介">
              <el-input v-model="form.introduction" type="textarea" :rows="3" placeholder="选填" />
            </el-form-item>

            <el-divider />

            <el-form-item label="指导老师">
              <div class="teacher-search">
                <el-input
                  v-model="teacherKeyword"
                  placeholder="搜索老师姓名或院系..."
                  clearable
                  @input="fetchTeachers"
                  size="small"
                  style="width:220px;"
                />
              </div>

              <div v-if="isTeacherLoading" class="tip">加载中...</div>
              <div v-else-if="teachers.length === 0" class="tip">暂无可选老师（确认是否已登录）</div>
              <div v-else class="teacher-list">
                <div
                  v-for="t in teachers"
                  :key="t.id"
                  :class="['teacher-item', selectedTeacher?.id === t.id ? 'selected' : '']"
                  @click="toggleTeacher(t)"
                >
                  <div class="teacher-row">
                    <div>
                      <div class="t-name">
                        {{ t.realName }}
                        <span v-if="t.title" class="t-title">{{ t.title }}</span>
                      </div>
                      <div v-if="t.department" class="t-dept">{{ t.department }}</div>
                    </div>
                    <span v-if="selectedTeacher?.id === t.id" class="check-mark">✓</span>
                  </div>
                </div>
              </div>

              <el-checkbox v-model="noTeacher" style="margin-top:10px;">不需要指导老师</el-checkbox>
            </el-form-item>

            <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>

            <el-form-item>
              <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
                提交报名
              </el-button>
            </el-form-item>
          </el-form>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { signupApi } from '@/api/signup'
import { competitionApi } from '@/api/competition'
import type { TeacherInfo } from '@/types/signup'
import type { CompetitionDetailVO } from '@/types/competition'

const route = useRoute()
const router = useRouter()

const competition = ref<CompetitionDetailVO | null>(null)
const compLoading = ref(true)

// 表单（个人赛）
const form = reactive({ phone: '', email: '', motivation: '', introduction: '' })
const teacherKeyword = ref('')
const teachers = ref<TeacherInfo[]>([])
const selectedTeacher = ref<TeacherInfo | null>(null)
const noTeacher = ref(false)
const isTeacherLoading = ref(false)

// 团队赛
const myTeams = ref<any[]>([])
const selectedTeamId = ref<number | null>(null)
const teamLoading = ref(false)

const selectedTeamInfo = computed(() => myTeams.value.find(t => t.id === selectedTeamId.value))

const submitting = ref(false)
const errorMsg = ref('')

const toggleTeacher = (t: TeacherInfo) => {
  selectedTeacher.value = selectedTeacher.value?.id === t.id ? null : t
}

const fetchTeachers = async () => {
  isTeacherLoading.value = true
  try {
    const res = await signupApi.getAvailableTeachers({
      page: 1,
      size: 50,
      keyword: teacherKeyword.value || undefined
    })
    if (res.code === 0) {
      teachers.value = res.data.list
    } else {
      if ((res as any).code === 40100) {
        errorMsg.value = '请先登录后再选择老师'
      }
    }
  } catch {
  } finally {
    isTeacherLoading.value = false
  }
}

const fetchMyTeams = async () => {
  teamLoading.value = true
  try {
    const res: any = await request({ url: '/v1/team/my', method: 'GET' })
    if (res.code === 0) {
      const compId = Number(route.params.id)
      myTeams.value = (res.data ?? []).filter(
        (t: any) => t.competitionId === compId && (t.status === 'FORMING' || t.status === 'FULL')
      )
    }
  } catch {
  } finally {
    teamLoading.value = false
  }
}

const handleTeamSubmit = async () => {
  if (!selectedTeamId.value) return (errorMsg.value = '请选择队伍')
  const team = myTeams.value.find(t => t.id === selectedTeamId.value)
  if (!team) return (errorMsg.value = '队伍不存在')
  if (!team.teacherConfirmed) return (errorMsg.value = '指导老师尚未确认，不可提交报名')
  errorMsg.value = ''
  submitting.value = true
  try {
    // 创建报名草稿
    const createRes: any = await request({ url: '/v1/signups/team', method: 'POST', data: { teamId: selectedTeamId.value } })
    if (createRes.code !== 0) {
      errorMsg.value = createRes.message || '创建报名失败'
      return
    }
    const signupId = createRes.data?.signupId
    if (!signupId) {
      errorMsg.value = '获取报名ID失败'
      return
    }
    // 提交管理员审核
    const submitRes: any = await request({ url: `/v1/signups/team/${signupId}/submit`, method: 'POST' })
    if (submitRes.code === 0) {
      ElMessage.success('报名已提交，等待管理员审核')
      router.push(`/student/competition/${route.params.id}`)
    } else {
      errorMsg.value = submitRes.message || '提交失败'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '网络错误'
  } finally {
    submitting.value = false
  }
}

const handleSubmit = async () => {
  if (!form.phone || !/^1[3-9]\d{9}$/.test(form.phone)) return (errorMsg.value = '请输入正确的手机号')
  if (!form.email) return (errorMsg.value = '请输入邮箱')
  if (!noTeacher.value && !selectedTeacher.value) return (errorMsg.value = '请选择指导老师或勾选"不需要指导老师"')
  errorMsg.value = ''
  submitting.value = true
  try {
    const res = await signupApi.individualSignup({
      competitionId: Number(route.params.id),
      teacherId: selectedTeacher.value?.id,
    })
    if (res.code === 0) {
      ElMessage.success('报名成功，等待审核')
      router.push(`/student/competition/${route.params.id}`)
    } else {
      errorMsg.value = res.message || '报名失败'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '网络错误'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await competitionApi.getById(Number(route.params.id))
    if (res.code === 0) competition.value = res.data
  } catch { /* ignore */ }
  finally { compLoading.value = false }

  if (competition.value?.type === 'TEAM') {
    fetchMyTeams()
  } else {
    fetchTeachers()
  }
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

.center-tip { text-align: center; padding: 60px; color: #aaa; }

.page-inner { max-width: 660px; }

h2 { font-size: 20px; font-weight: 700; color: #111; margin-bottom: 16px; }

.team-hint { margin-bottom: 4px; }

.signup-form { background: #fff; }

.teacher-search { margin-bottom: 10px; }

.tip { font-size: 13px; color: #aaa; padding: 8px 0; }

.teacher-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 280px;
  overflow-y: auto;
  width: 100%;
}

.teacher-item {
  padding: 12px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.12s;
  user-select: none;
}
.teacher-item:hover { border-color: #aaa; background: #fafafa; }
.teacher-item.selected { border-color: #111; background: #f4f4f4; }

.teacher-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.t-name { font-size: 14px; font-weight: 600; color: #111; }
.t-title {
  font-size: 12px;
  color: #888;
  font-weight: 400;
  margin-left: 6px;
  background: #f4f4f4;
  padding: 1px 6px;
  border-radius: 3px;
}
.t-dept { font-size: 12px; color: #aaa; margin-top: 3px; }

.check-mark {
  color: #111;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.error-tip {
  font-size: 13px;
  color: #888;
  background: #f8f8f8;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px 12px;
  margin-bottom: 12px;
}
</style>
