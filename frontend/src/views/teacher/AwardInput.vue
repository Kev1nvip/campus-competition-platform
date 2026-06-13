<template>
  <div>
    <div class="page-title">录入获奖</div>

    <el-form :model="form" label-width="100px" class="form-wrap">
      <el-form-item label="竞赛 ID" required>
        <el-input v-model="form.competitionId" placeholder="请输入竞赛 ID" />
      </el-form-item>
      <el-form-item label="报名类型" required>
        <el-radio-group v-model="form.bizType">
          <el-radio value="INDIVIDUAL">个人赛</el-radio>
          <el-radio value="TEAM">团队赛</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="报名 ID" required>
        <el-input v-model="form.bizId" placeholder="对应 individual_signup 或 team_signup 的 ID" />
      </el-form-item>
      <el-form-item label="奖项等级" required>
        <el-select v-model="form.awardLevel" style="width:100%;">
          <el-option label="国家一等奖" value="NATIONAL_FIRST" />
          <el-option label="国家二等奖" value="NATIONAL_SECOND" />
          <el-option label="国家三等奖" value="NATIONAL_THIRD" />
          <el-option label="省级一等奖" value="PROVINCIAL_FIRST" />
          <el-option label="省级二等奖" value="PROVINCIAL_SECOND" />
          <el-option label="省级三等奖" value="PROVINCIAL_THIRD" />
          <el-option label="其他" value="OTHER" />
        </el-select>
      </el-form-item>
      <el-form-item label="奖项名称" required>
        <el-input v-model="form.awardName" placeholder="如：全国大学生数学建模竞赛一等奖" />
      </el-form-item>
      <el-form-item label="证书图片URL" required>
        <el-input v-model="form.certificateUrl" placeholder="/uploads/certificates/..." />
      </el-form-item>
      <el-form-item label="获奖日期" required>
        <el-date-picker v-model="form.awardDate" type="date" placeholder="选择日期" style="width:100%;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交获奖记录</el-button>
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
  competitionId: '', bizType: 'INDIVIDUAL', bizId: '',
  awardLevel: '', awardName: '', certificateUrl: '', awardDate: ''
})

const handleSubmit = async () => {
  if (!form.competitionId || !form.bizId || !form.awardLevel || !form.awardName || !form.certificateUrl || !form.awardDate) {
    return ElMessage.warning('请填写所有必填项')
  }
  submitting.value = true
  try {
    const res: any = await request({
      url: '/v1/award', method: 'POST',
      data: { ...form, competitionId: Number(form.competitionId), bizId: Number(form.bizId) }
    })
    if (res.code === 0) {
      ElMessage.success('提交成功')
      resetForm()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.competitionId = ''; form.bizId = ''; form.awardLevel = ''
  form.awardName = ''; form.certificateUrl = ''; form.awardDate = ''
}
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.form-wrap { max-width: 520px; background: #fff; padding: 24px; border: 1px solid #e0e0e0; border-radius: 6px; }
</style>
