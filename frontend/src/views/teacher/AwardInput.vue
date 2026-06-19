<template>
  <div>
    <div class="page-title">录入获奖</div>

    <el-form :model="form" label-width="120px" class="form-wrap">

      <el-form-item label="选择竞赛" required>
        <el-select
          v-model="form.competitionId"
          placeholder="请选择竞赛"
          style="width:100%;"
          filterable
          :loading="compLoading"
          @change="onCompetitionChange"
        >
          <el-option
            v-for="c in competitions"
            :key="c.id"
            :label="c.title + '（' + typeLabel(c.type) + '）'"
            :value="c.id"
          />
        </el-select>
        <div v-if="!compLoading && competitions.length === 0" class="tip-text">
          暂无可录入竞赛，需有已审核通过的报名记录
        </div>
      </el-form-item>

      <el-form-item v-if="form.competitionId" label="报名类型">
        <el-tag>{{ typeLabel(form.bizType) }}</el-tag>
      </el-form-item>

      <el-form-item label="选择学生/队伍" required>
        <el-select
          v-model="form.bizId"
          :placeholder="candidateLoading ? '加载中...' : '请先选择竞赛'"
          style="width:100%;"
          filterable
          :loading="candidateLoading"
          :disabled="!form.competitionId || candidateList.length === 0"
          @change="onCandidateChange"
        >
          <el-option
            v-for="c in candidateList"
            :key="c.bizType + '-' + c.bizId"
            :label="c.displayName"
            :value="c.bizId"
          >
            <span>{{ c.displayName }}</span>
            <el-tag v-if="c.bizType === 'TEAM'" size="small" style="margin-left:8px;">团队</el-tag>
            <el-tag v-else size="small" type="info" style="margin-left:8px;">个人</el-tag>
          </el-option>
        </el-select>
        <div v-if="form.competitionId && !candidateLoading && candidateList.length === 0" class="tip-text">
          该竞赛下无已审核通过的学生/队伍记录
        </div>
      </el-form-item>

      <el-form-item label="奖项等级" required>
        <el-select v-model="form.awardLevel" placeholder="请选择奖项等级" style="width:100%;">
          <el-option label="国家级一等奖" value="NATIONAL_FIRST" />
          <el-option label="国家级二等奖" value="NATIONAL_SECOND" />
          <el-option label="国家级三等奖" value="NATIONAL_THIRD" />
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
        <el-date-picker
          v-model="form.awardDate"
          type="date"
          placeholder="选择获奖日期"
          style="width:100%;"
          value-format="YYYY-MM-DD"
        />
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
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const submitting = ref(false)
const compLoading = ref(false)
const candidateLoading = ref(false)
const competitions = ref<any[]>([])
const candidateList = ref<any[]>([])

const form = reactive({
  competitionId: null as number | null,
  bizType: 'INDIVIDUAL',
  bizId: null as number | null,
  awardLevel: '',
  awardName: '',
  certificateUrl: '',
  awardDate: ''
})

const typeLabel = (type: string) => type === 'INDIVIDUAL' ? '个人赛' : '团队赛'

const loadCompetitions = async () => {
  compLoading.value = true
  try {
    const res: any = await request({ url: '/v1/award/teacher/competitions', method: 'GET' })
    if (res.code === 0) competitions.value = res.data ?? []
  } finally {
    compLoading.value = false
  }
}

const onCompetitionChange = async (compId: number) => {
  form.bizId = null
  candidateList.value = []
  if (!compId) return
  const comp = competitions.value.find((c: any) => c.id === compId)
  form.bizType = comp?.type || 'INDIVIDUAL'
  candidateLoading.value = true
  try {
    const res: any = await request({ url: '/v1/award/teacher/candidates', method: 'GET', params: { competitionId: compId } })
    if (res.code === 0) candidateList.value = res.data ?? []
  } finally {
    candidateLoading.value = false
  }
}

const onCandidateChange = (bizId: number) => {
  // 自动记忆 bizType（已在下拉时从 API 获取）
}

const handleUpload = async (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res: any = await request({
      url: '/v1/award/upload',
      method: 'POST',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 0) {
      form.certificateUrl = res.data.url
      ElMessage.success('上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch {
    ElMessage.error('上传失败')
  }
  return false
}

const handleSubmit = async () => {
  if (!form.competitionId) return ElMessage.warning('请选择竞赛')
  if (!form.bizId) return ElMessage.warning('请选择学生或队伍')
  if (!form.awardLevel) return ElMessage.warning('请选择奖项等级')
  if (!form.awardName.trim()) return ElMessage.warning('请输入奖项名称')
  if (!form.certificateUrl) return ElMessage.warning('请上传获奖证书图片')
  if (!form.awardDate) return ElMessage.warning('请选择获奖日期')

  const selected = candidateList.value.find((c: any) => c.bizId === form.bizId)
  if (!selected) return ElMessage.warning('请重新选择学生或队伍')

  submitting.value = true
  try {
    const res: any = await request({
      url: '/v1/award',
      method: 'POST',
      data: {
        competitionId: form.competitionId,
        bizType: selected.bizType,
        bizId: form.bizId,
        awardLevel: form.awardLevel,
        awardName: form.awardName.trim(),
        certificateUrl: form.certificateUrl,
        awardDate: form.awardDate
      }
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
  form.competitionId = null
  form.bizType = 'INDIVIDUAL'
  form.bizId = null
  form.awardLevel = ''
  form.awardName = ''
  form.certificateUrl = ''
  form.awardDate = ''
  candidateList.value = []
}

onMounted(loadCompetitions)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 24px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.form-wrap { max-width: 540px; background: #fff; padding: 24px; border: 1px solid #e0e0e0; border-radius: 6px; }
.upload-area { display: flex; align-items: center; gap: 12px; }
.upload-tip { font-size: 12px; color: #111; }
.upload-tip.muted { color: #aaa; }
.tip-text { font-size: 12px; color: #999; margin-top: 4px; }
</style>