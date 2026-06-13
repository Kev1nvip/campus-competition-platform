<template>
  <div>
    <div class="page-title">获奖审核</div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="list.length === 0" class="center-tip">暂无待审核获奖记录</div>
    <el-table v-else :data="list" class="data-table">
      <el-table-column label="竞赛名称" prop="competitionTitle" min-width="160" />
      <el-table-column label="奖项名称" prop="awardName" min-width="160" />
      <el-table-column label="奖项等级" prop="awardLevel" width="130">
        <template #default="{ row }: { row: any }">{{ levelText(row.awardLevel) }}</template>
      </el-table-column>
      <el-table-column label="提交人" prop="submitterName" width="90" />
      <el-table-column label="类型" prop="bizType" width="90">
        <template #default="{ row }: { row: any }">
          <el-tag size="small" effect="plain">{{ row.bizType === 'INDIVIDUAL' ? '个人赛' : '团队赛' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="获奖日期" prop="awardDate" width="110" />
      <el-table-column label="证书" width="80">
        <template #default="{ row }: { row: any }">
          <el-button v-if="row.certificateUrl" size="small" text @click="openCert(row.certificateUrl)">查看</el-button>
          <span v-else class="none-text">无</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }: { row: any }">
          <el-button size="small" type="primary" :loading="row._loading" @click="auditAward(row, 'APPROVED')">通过</el-button>
          <el-button size="small" :loading="row._loading" @click="openReject(row)" style="margin-left:4px;">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="填写驳回原因" width="400px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请填写驳回原因（必填）" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="operating" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const isLoading = ref(false)
const operating = ref(false)
const list = ref<any[]>([])
const rejectVisible = ref(false)
const rejectReason = ref('')
const currentRow = ref<any>(null)

const levelText = (l: string) => (({
  NATIONAL_FIRST: '国家级一等奖', NATIONAL_SECOND: '国家级二等奖', NATIONAL_THIRD: '国家级三等奖',
  PROVINCIAL_FIRST: '省级一等奖', PROVINCIAL_SECOND: '省级二等奖', PROVINCIAL_THIRD: '省级三等奖', OTHER: '其他'
} as Record<string, string>)[l] ?? l)

const loadList = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: '/v1/award/pending', method: 'GET', params: { page: 1, size: 50 } })
    if (res.code === 0) list.value = (res.data.list ?? []).map((r: any) => ({ ...r, _loading: false }))
    else ElMessage.error(res.message || '加载失败')
  } catch { ElMessage.error('网络错误') }
  finally { isLoading.value = false }
}

const auditAward = async (row: any, result: string, reason?: string) => {
  row._loading = true
  try {
    const res: any = await request({
      url: '/v1/award/audit', method: 'POST',
      data: { awardRecordId: row.id, result, rejectReason: reason || null }
    })
    if (res.code === 0) {
      ElMessage.success(result === 'APPROVED' ? '已通过' : '已驳回')
      loadList()
    } else ElMessage.error(res.message || '操作失败')
  } catch { ElMessage.error('网络错误') }
  finally { row._loading = false }
}

const openReject = (row: any) => {
  currentRow.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) return ElMessage.warning('请填写驳回原因')
  operating.value = true
  rejectVisible.value = false
  await auditAward(currentRow.value, 'REJECTED', rejectReason.value)
  operating.value = false
}

const openCert = (url: string) => window.open(url, '_blank')

onMounted(loadList)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
.none-text { font-size: 13px; color: #ccc; }
</style>
