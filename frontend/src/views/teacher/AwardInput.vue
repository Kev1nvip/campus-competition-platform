<template>
  <div>
    <div class="page-title">录入获奖</div>

    <el-form :model="form" label-width="110px" class="form-wrap">
      <el-form-item label="选择竞赛" required>
        <el-select
          v-model="form.competitionId"
          placeholder="请选择竞赛"
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

      <el-form-item label="报名类型" required>
        <el-radio-group v-model="form.bizType">
          <el-radio value="INDIVIDUAL">个人赛</el-radio>
          <el-radio value="TEAM">团队赛</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="报名记录 ID" required>
        <el-input v-model="form.bizId" placeholder="对应的报名记录 ID（APPROVED 状态）" />
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

      <el-form-item label="获奖证书" required>
        <div class="upload-area">
          <el-upload
            :show-file-list="false"
            :before-upload="handleUpload"
            accept=".jpg,.jpeg,.png"
          >
            <el-button size="small">上传证书图片</el-button>
          </el-upload>
          <span v-if="form.certificateUrl" class="upload-tip">已上传 ✓</span>
          <span v-else class="upload-tip muted">支持 jpg/jpeg/png，最大 5MB</span>
        </div>
      </el-form-item>

      <el-form-item label="获奖日期" required>
        <el-date-picker v-model="form.awardDate" type="date" placeholder="选择日期" style="width:100%;" value-format="YYYY-MM-DD" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交获奖记录</el-button>
        <el-button @click="resetForm" style="margin-left:8px;">重置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import request from '@/utils/request'

const submitting = ref(false)
const compLoading = ref(false)
const competitions = ref<any[]>([])
const form = reactive({
  competitionId: null as number | null,
  bizType: 'INDIVIDUAL',
  bizId: '',
  awardLevel: '',
  awardName: '',
  certificateUrl: '',
  awardDate: ''
})

const loadCompetitions = async () => {
  compLoading.value = true
  try {
    const res: any = await request({ url: '/v1/competitions', method: 'GET', params: { page: 1, size: 200 } })
    if (res.code === 0) competitions.value = res.data?.list ?? []
  } finally {
    compLoading.value = false
  }
}

const handleUpload = async (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res: any = await request({ url: '/v1/award/upload', method: 'POST', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
    if (res.code === 0) {
      form.certificateUrl = res.data.url
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch {
    ElMessage.error('上传失败')
  }
  return false // 阻止 el-upload 自动上传
}

const handleSubmit = async () => {
  if (!form.competitionId || !form.bizId || !form.awardLevel || !form.awardName || !form.certificateUrl || !form.awardDate) {
    return ElMessage.warning('请填写所有必填项')
  }
  submitting.value = true
  try {
    const res: any = await request({
      url: '/v1/award', method: 'POST',
      data: { ...form, bizId: Number(form.bizId) }
    })
    if (res.code === 0) {
      ElMessage.success('提交成功，等待管理员审核')
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
  form.competitionId = null; form.bizId = ''; form.awardLevel = ''
  form.awardName = ''; form.certificateUrl = ''; form.awardDate = ''
}

onMounted(loadCompetitions)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.form-wrap { max-width: 540px; background: #fff; padding: 24px; border: 1px solid #e0e0e0; border-radius: 6px; }
.upload-area { display: flex; align-items: center; gap: 12px; }
.upload-tip { font-size: 12px; color: #111; }
.upload-tip.muted { color: #aaa; }
</style>
