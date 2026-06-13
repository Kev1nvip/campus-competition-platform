<template>
  <div>
    <div class="page-title">发布竞赛</div>

    <el-form :model="form" label-width="120px" class="form-wrap">
      <el-form-item label="竞赛名称" required>
        <el-input v-model="form.title" placeholder="请输入竞赛名称" />
      </el-form-item>

      <el-form-item label="竞赛类型" required>
        <el-radio-group v-model="form.type">
          <el-radio value="INDIVIDUAL">个人赛</el-radio>
          <el-radio value="TEAM">团队赛</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="主办方" required>
        <el-input v-model="form.organizer" placeholder="主办单位名称" />
      </el-form-item>

      <el-divider content-position="left">报名时间</el-divider>

      <el-form-item label="报名开始时间" required>
        <el-date-picker v-model="form.signupStart" type="datetime" placeholder="选择日期时间" style="width:100%;" value-format="YYYY-MM-DDTHH:mm:ssZ" />
      </el-form-item>

      <el-form-item label="报名截止时间" required>
        <el-date-picker v-model="form.signupEnd" type="datetime" placeholder="选择日期时间" style="width:100%;" value-format="YYYY-MM-DDTHH:mm:ssZ" />
      </el-form-item>

      <el-divider content-position="left">比赛时间（选填）</el-divider>

      <el-form-item label="比赛开始时间">
        <el-date-picker v-model="form.competitionStart" type="datetime" placeholder="选择日期时间" style="width:100%;" value-format="YYYY-MM-DDTHH:mm:ssZ" />
      </el-form-item>

      <el-form-item label="比赛结束时间">
        <el-date-picker v-model="form.competitionEnd" type="datetime" placeholder="选择日期时间" style="width:100%;" value-format="YYYY-MM-DDTHH:mm:ssZ" />
      </el-form-item>

      <el-divider content-position="left">名额与队伍</el-divider>

      <el-form-item label="是否限制名额">
        <el-switch v-model="form.hasQuota" />
      </el-form-item>

      <el-form-item v-if="form.hasQuota" label="名额上限">
        <el-input-number v-model="form.maxQuota" :min="1" />
      </el-form-item>

      <template v-if="form.type === 'TEAM'">
        <el-form-item label="最少队员数" required>
          <el-input-number v-model="form.minTeamSize" :min="2" />
        </el-form-item>
        <el-form-item label="最多队员数" required>
          <el-input-number v-model="form.maxTeamSize" :min="2" />
        </el-form-item>
        <el-form-item label="老师最多带队数">
          <el-input-number v-model="form.maxTeachQuota" :min="1" placeholder="不限" />
          <span class="field-hint">不填表示不限制</span>
        </el-form-item>
      </template>

      <el-divider content-position="left">详细信息</el-divider>

      <el-form-item label="竞赛简介">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填" />
      </el-form-item>

      <el-form-item label="参赛要求">
        <el-input v-model="form.requirement" type="textarea" :rows="2" placeholder="专业/年级限制等，选填" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">发布竞赛</el-button>
        <el-button @click="resetForm" style="margin-left:8px;">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import request from '@/utils/request'

const submitting = ref(false)
const form = reactive({
  title: '',
  type: 'INDIVIDUAL',
  organizer: '',
  signupStart: '',
  signupEnd: '',
  competitionStart: '',
  competitionEnd: '',
  hasQuota: false,
  maxQuota: 100,
  minTeamSize: 2,
  maxTeamSize: 5,
  maxTeachQuota: null as number | null,
  description: '',
  requirement: ''
})

const handleSubmit = async () => {
  if (!form.title.trim()) return ElMessage.warning('请输入竞赛名称')
  if (!form.organizer.trim()) return ElMessage.warning('请输入主办方')
  if (!form.signupStart || !form.signupEnd) return ElMessage.warning('请选择报名开始和截止时间')
  if (new Date(form.signupEnd) <= new Date(form.signupStart)) return ElMessage.warning('报名截止时间必须晚于开始时间')
  if (form.type === 'TEAM') {
    if (form.minTeamSize < 2) return ElMessage.warning('团队最少人数不能小于2')
    if (form.maxTeamSize < form.minTeamSize) return ElMessage.warning('最多队员数不能小于最少队员数')
  }

  submitting.value = true
  try {
    // 构建提交数据，空字符串的时间字段不提交
    const data: Record<string, any> = {
      title: form.title,
      type: form.type,
      organizer: form.organizer,
      signupStart: form.signupStart,
      signupEnd: form.signupEnd,
      hasQuota: form.hasQuota,
      maxQuota: form.hasQuota ? form.maxQuota : null,
      description: form.description || null,
      requirement: form.requirement || null,
    }
    if (form.competitionStart) data.competitionStart = form.competitionStart
    if (form.competitionEnd) data.competitionEnd = form.competitionEnd
    if (form.type === 'TEAM') {
      data.minTeamSize = form.minTeamSize
      data.maxTeamSize = form.maxTeamSize
      if (form.maxTeachQuota) data.maxTeachQuota = form.maxTeachQuota
    }

    const res: any = await request({ url: '/v1/competitions', method: 'POST', data })
    if (res.code === 0) {
      ElMessage.success('发布成功')
      resetForm()
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.title = ''; form.organizer = ''; form.signupStart = ''; form.signupEnd = ''
  form.competitionStart = ''; form.competitionEnd = ''
  form.hasQuota = false; form.maxQuota = 100; form.maxTeachQuota = null
  form.description = ''; form.requirement = ''
  form.type = 'INDIVIDUAL'; form.minTeamSize = 2; form.maxTeamSize = 5
}
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.form-wrap { max-width: 580px; background: #fff; padding: 24px; border: 1px solid #e0e0e0; border-radius: 6px; }
.field-hint { font-size: 12px; color: #aaa; margin-left: 8px; }
</style>
