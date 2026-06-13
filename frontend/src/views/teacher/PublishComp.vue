<template>
  <div>
    <div class="page-title">发布竞赛</div>

    <el-form :model="form" label-width="110px" class="form-wrap">
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

      <el-form-item label="报名开始时间" required>
        <el-date-picker v-model="form.signupStart" type="datetime" placeholder="选择日期时间" style="width:100%;" />
      </el-form-item>

      <el-form-item label="报名截止时间" required>
        <el-date-picker v-model="form.signupEnd" type="datetime" placeholder="选择日期时间" style="width:100%;" />
      </el-form-item>

      <el-form-item label="是否限制名额">
        <el-switch v-model="form.hasQuota" />
      </el-form-item>

      <el-form-item v-if="form.hasQuota" label="名额上限">
        <el-input-number v-model="form.maxQuota" :min="1" />
      </el-form-item>

      <template v-if="form.type === 'TEAM'">
        <el-form-item label="最少队员数">
          <el-input-number v-model="form.minTeamSize" :min="2" />
        </el-form-item>
        <el-form-item label="最多队员数">
          <el-input-number v-model="form.maxTeamSize" :min="2" />
        </el-form-item>
      </template>

      <el-form-item label="竞赛简介">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填" />
      </el-form-item>

      <el-form-item label="参赛要求">
        <el-input v-model="form.requirement" type="textarea" :rows="2" placeholder="选填" />
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
  title: '', type: 'INDIVIDUAL', organizer: '',
  signupStart: '', signupEnd: '',
  hasQuota: false, maxQuota: 100,
  minTeamSize: 2, maxTeamSize: 5,
  description: '', requirement: ''
})

const handleSubmit = async () => {
  if (!form.title.trim()) return ElMessage.warning('请输入竞赛名称')
  if (!form.organizer.trim()) return ElMessage.warning('请输入主办方')
  if (!form.signupStart || !form.signupEnd) return ElMessage.warning('请选择报名时间')
  submitting.value = true
  try {
    const res: any = await request({ url: '/v1/competitions', method: 'POST', data: { ...form } })
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
  form.hasQuota = false; form.description = ''; form.requirement = ''
  form.type = 'INDIVIDUAL'
}
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.form-wrap { max-width: 560px; background: #fff; padding: 24px; border: 1px solid #e0e0e0; border-radius: 6px; }
</style>
