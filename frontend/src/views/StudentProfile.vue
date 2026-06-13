<template>
  <div>
    <div class="page-inner">
      <div class="page-tabs">
        <el-tabs v-model="activeTab">
          <el-tab-pane label="个人信息" name="profile" />
          <el-tab-pane label="报名记录" name="signups" />
          <el-tab-pane label="获奖记录" name="awards" />
        </el-tabs>
      </div>

      <!-- 个人信息 -->
      <div v-if="activeTab === 'profile'" class="tab-content">
        <div v-if="!userInfo" class="center-tip">请先登录</div>
        <div v-else>
          <el-form :model="editForm" label-width="80px" class="profile-form">
            <el-form-item label="用户名">
              <span class="readonly-val">{{ userInfo.username }}</span>
            </el-form-item>
            <el-form-item label="真实姓名">
              <el-input v-model="editForm.realName" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="editForm.phone" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" />
            </el-form-item>
            <el-form-item label="院系">
              <el-input v-model="editForm.department" />
            </el-form-item>
            <el-form-item label="学号" v-if="userInfo.role === 'STUDENT'">
              <span class="readonly-val">{{ userInfo.studentNo }}</span>
            </el-form-item>
            <el-form-item label="职称" v-if="userInfo.role === 'TEACHER'">
              <el-input v-model="editForm.title" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="saveProfile" :loading="saving">保存</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 报名记录 -->
      <div v-if="activeTab === 'signups'" class="tab-content">
        <div v-if="signups.length === 0" class="center-tip">暂无报名记录</div>
        <el-table v-else :data="signups" class="data-table">
          <el-table-column label="竞赛 ID" prop="competitionId" width="90" />
          <el-table-column label="状态" prop="status" width="110">
            <template #default="{ row }: { row: any }">
              <el-tag size="small" :type="signupTagType(row.status)">{{ signupStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" prop="submittedAt" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }: { row: any }">
              <!-- DRAFT：老师已同意，等待学生提交管理员审核 -->
              <el-button
                v-if="row.status === 'DRAFT'"
                size="small"
                type="primary"
                :loading="row._submitting"
                @click="handleSubmitAudit(row)"
              >
                提交审核
              </el-button>
              <!-- REJECTED：被驳回，可修改后重新提交 -->
              <el-button
                v-else-if="row.status === 'REJECTED'"
                size="small"
                :loading="row._submitting"
                @click="handleResubmit(row)"
              >
                重新提交
              </el-button>
              <span v-else class="status-done">—</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 获奖记录 -->
      <div v-if="activeTab === 'awards'" class="tab-content">
        <div v-if="awards.length === 0" class="center-tip">暂无获奖记录</div>
        <el-table v-else :data="awards" class="data-table">
          <el-table-column label="奖项名称" prop="awardName" />
          <el-table-column label="奖项等级" prop="awardLevel" width="130">
            <template #default="{ row }: { row: any }">
              {{ awardLevelText(row.awardLevel) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" prop="status" width="90">
            <template #default="{ row }: { row: any }">
              <el-tag size="small" :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'">
                {{ row.status === 'APPROVED' ? '已通过' : row.status === 'REJECTED' ? '已驳回' : '待审核' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import request from '@/utils/request'
import { signupApi } from '@/api/signup'

const activeTab = ref('profile')
const userInfo = ref<any>(null)
const editForm = reactive({ realName: '', phone: '', email: '', department: '', title: '' })
const signups = ref<any[]>([])
const awards = ref<any[]>([])
const saving = ref(false)

const signupTagType = (s: string) => (({ APPROVED: 'success', REJECTED: 'danger', PENDING: 'warning', RESUBMITTED: 'warning', DRAFT: 'info' } as Record<string, any>)[s] ?? 'info')
const signupStatusText = (s: string) => (({ DRAFT: '待提交', PENDING: '审核中', APPROVED: '已通过', REJECTED: '已驳回', RESUBMITTED: '重新提交中' } as Record<string, string>)[s] ?? s)
const awardLevelText = (s: string) => (({
  NATIONAL_FIRST: '国家级一等奖', NATIONAL_SECOND: '国家级二等奖', NATIONAL_THIRD: '国家级三等奖',
  PROVINCIAL_FIRST: '省级一等奖', PROVINCIAL_SECOND: '省级二等奖', PROVINCIAL_THIRD: '省级三等奖', OTHER: '其他'
} as Record<string, string>)[s] ?? s)

const loadProfile = async () => {
  try {
    const res: any = await request({ url: '/v1/user/info', method: 'GET' })
    if (res.code === 0) {
      userInfo.value = res.data
      editForm.realName = res.data.realName ?? ''
      editForm.phone = res.data.phone ?? ''
      editForm.email = res.data.email ?? ''
      editForm.department = res.data.department ?? ''
      editForm.title = res.data.title ?? ''
    }
  } catch { /* 未登录 */ }
}

const loadSignups = async () => {
  try {
    const res: any = await request({ url: '/v1/signups/individual/my', method: 'GET', params: { page: 1, size: 50 } })
    if (res.code === 0) signups.value = (res.data.list ?? []).map((s: any) => ({ ...s, _submitting: false }))
  } catch { /* ignore */ }
}

const loadAwards = async () => {
  try {
    const res: any = await request({ url: '/v1/award/my', method: 'GET' })
    if (res.code === 0) awards.value = res.data.content ?? res.data.list ?? []
  } catch { /* ignore */ }
}

const saveProfile = async () => {
  saving.value = true
  try {
    const res: any = await request({ url: '/v1/user/info', method: 'PUT', data: editForm })
    if (res.code === 0) ElMessage.success('保存成功')
    else ElMessage.error(res.message || '保存失败')
  } catch {
    ElMessage.error('网络错误')
  } finally {
    saving.value = false
  }
}

// DRAFT 状态：提交给管理员审核
const handleSubmitAudit = async (row: any) => {
  row._submitting = true
  try {
    const res: any = await signupApi.submitIndividualAudit(row.id)
    if (res.code === 0) {
      ElMessage.success('已提交审核，等待管理员审核')
      await loadSignups()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    row._submitting = false
  }
}

// REJECTED 状态：重新提交（状态变为 RESUBMITTED）
const handleResubmit = async (row: any) => {
  row._submitting = true
  try {
    const res: any = await request({ url: `/v1/signups/individual/${row.id}/submit`, method: 'POST' })
    if (res.code === 0) {
      ElMessage.success('已重新提交，等待管理员审核')
      await loadSignups()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    row._submitting = false
  }
}

onMounted(() => {
  loadProfile()
  loadSignups()
  loadAwards()
})
</script>

<style scoped>
.tab-content { padding-top: 16px; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.profile-form { max-width: 480px; }
.readonly-val { font-size: 14px; color: #555; }
.data-table { width: 100%; }
.status-done { font-size: 13px; color: #ccc; }
</style>
