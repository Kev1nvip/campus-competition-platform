<template>
  <div>
    <div class="page-title">个人中心</div>

    <div class="profile-grid">
      <!-- 基本信息 -->
      <div class="card">
        <div class="card-title">基本信息</div>
        <el-form :model="form" label-width="80px">
          <el-form-item label="用户名">
            <span class="readonly">{{ userInfo.username }}</span>
          </el-form-item>
          <el-form-item label="真实姓名">
            <el-input v-model="form.realName" />
          </el-form-item>
          <el-form-item label="职称">
            <el-input v-model="form.title" placeholder="如：讲师/副教授/教授" />
          </el-form-item>
          <el-form-item label="院系">
            <el-input v-model="form.department" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="small" :loading="saving" @click="saveProfile">保存</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 带队统计 -->
      <div class="card">
        <div class="card-title">带队统计</div>
        <div v-if="statsLoading" class="center-tip">加载中...</div>
        <div v-else>
          <div class="stat-row">
            <span class="stat-label">历史带队竞赛</span>
            <span class="stat-val">{{ stats.totalCompetitions }} 场</span>
          </div>
          <div class="stat-row">
            <span class="stat-label">获奖记录（已确认）</span>
            <span class="stat-val">{{ stats.approvedAwards }} 条</span>
          </div>

          <el-divider />

          <div v-if="stats.awardList.length === 0" class="center-tip">暂无获奖记录</div>
          <div v-else class="award-list">
            <div v-for="item in stats.awardList" :key="item.id" class="award-row">
              <div class="award-name">{{ item.awardName }}</div>
              <div class="award-meta">
                <el-tag size="small">{{ item.competitionTitle || '-' }}</el-tag>
                <el-tag size="small" :type="levelTag(item.awardLevel)">{{ levelText(item.awardLevel) }}</el-tag>
                <span class="award-date">{{ item.awardDate }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const saving = ref(false)
const statsLoading = ref(false)
const userInfo = ref<any>({ username: '' })
const form = reactive({ realName: '', title: '', department: '', email: '', phone: '' })
const stats = ref({ totalCompetitions: 0, approvedAwards: 0, awardList: [] as any[] })

const levelText = (l: string) => (({
  NATIONAL_FIRST: '国家一等奖', NATIONAL_SECOND: '国家二等奖', NATIONAL_THIRD: '国家三等奖',
  PROVINCIAL_FIRST: '省级一等奖', PROVINCIAL_SECOND: '省级二等奖', PROVINCIAL_THIRD: '省级三等奖', OTHER: '其他'
} as Record<string, string>)[l] ?? l)

const levelTag = (l: string) => l.startsWith('NATIONAL') ? '' : 'info'

const loadProfile = async () => {
  try {
    const res: any = await request({ url: '/v1/user/info', method: 'GET' })
    if (res.code === 0) {
      userInfo.value = res.data
      form.realName = res.data.realName ?? ''
      form.title = res.data.title ?? ''
      form.department = res.data.department ?? ''
      form.email = res.data.email ?? ''
      form.phone = res.data.phone ?? ''
    }
  } catch { /* ignore */ }
}

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res: any = await request({ url: '/v1/user/teacher-stats', method: 'GET' })
    if (res.code === 0) stats.value = res.data ?? { totalCompetitions: 0, approvedAwards: 0, awardList: [] }
  } catch { /* ignore */ }
  finally { statsLoading.value = false }
}

const saveProfile = async () => {
  saving.value = true
  try {
    const res: any = await request({ url: '/v1/user/info', method: 'PUT', data: form })
    if (res.code === 0) ElMessage.success('保存成功')
    else ElMessage.error(res.message || '保存失败')
  } catch {
    ElMessage.error('网络错误')
  } finally {
    saving.value = false
  }
}

onMounted(() => { loadProfile(); loadStats() })
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.card { background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 20px 24px; }
.card-title { font-size: 14px; font-weight: 700; color: #111; margin-bottom: 16px; }
.readonly { font-size: 14px; color: #555; }
.center-tip { text-align: center; padding: 24px; color: #aaa; font-size: 13px; }
.stat-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.stat-row:last-of-type { border-bottom: none; }
.stat-label { font-size: 13px; color: #555; }
.stat-val { font-size: 16px; font-weight: 700; color: #111; }
.award-list { display: flex; flex-direction: column; gap: 10px; max-height: 300px; overflow-y: auto; }
.award-row { padding: 10px 12px; border: 1px solid #f0f0f0; border-radius: 4px; }
.award-name { font-size: 13px; font-weight: 600; color: #111; margin-bottom: 6px; }
.award-meta { display: flex; align-items: center; gap: 10px; }
.award-date { font-size: 12px; color: #aaa; }
</style>
