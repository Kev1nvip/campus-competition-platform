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
          <el-table-column label="竞赛 ID" prop="competitionId" width="100" />
          <el-table-column label="状态" prop="status">
            <template #default="{ row }">
              <el-tag size="small" :type="signupTagType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="提交时间" prop="submittedAt" />
        </el-table>
      </div>

      <!-- 获奖记录 -->
      <div v-if="activeTab === 'awards'" class="tab-content">
        <div v-if="awards.length === 0" class="center-tip">暂无获奖记录</div>
        <el-table v-else :data="awards" class="data-table">
          <el-table-column label="奖项名称" prop="awardName" />
          <el-table-column label="奖项等级" prop="awardLevel" />
          <el-table-column label="状态" prop="status">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 'APPROVED' ? 'success' : 'info'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const activeTab = ref('profile')
const userInfo = ref<any>(null)
const editForm = reactive({ realName: '', phone: '', email: '', department: '', title: '' })
const signups = ref<any[]>([])
const awards = ref<any[]>([])
const saving = ref(false)

const signupTagType = (s: string) => ({ APPROVED: 'success', REJECTED: 'danger', PENDING: 'warning' }[s] ?? 'info') as any

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
    const res: any = await request({ url: '/v1/signups/individual/my', method: 'GET', params: { page: 1, size: 20 } })
    if (res.code === 0) signups.value = res.data.list ?? []
  } catch { /* ignore */ }
}

const loadAwards = async () => {
  try {
    const res: any = await request({ url: '/v1/award/my', method: 'GET' })
    if (res.code === 0) awards.value = res.data.content ?? []
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
</style>
