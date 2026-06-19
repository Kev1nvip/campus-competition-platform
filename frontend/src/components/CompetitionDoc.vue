<template>
  <div class="doc-section">
    <div class="doc-header">
      <span class="doc-title">竞赛文档</span>
      <el-button v-if="canManage" size="small" type="primary" :loading="uploading" @click="triggerUpload">
        上传文档
      </el-button>
    </div>

    <input ref="fileInput" type="file" accept=".pdf" style="display:none" @change="handleFileChange" />

    <el-dialog v-model="showUpload" title="上传竞赛文档" width="420px">
      <el-form label-width="100px">
        <el-form-item label="文档类型">
          <el-select v-model="uploadDocType" style="width:100%;">
            <el-option v-for="t in docTypes" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload ref="uploadRef" :auto-upload="false" :show-file-list="true" :limit="1" accept=".pdf" :on-change="onFileChange">
            <el-button size="small">选择 PDF 文件</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="confirmUpload">确认上传</el-button>
      </template>
    </el-dialog>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="list.length === 0" class="center-tip">暂无文档</div>
    <div v-else class="doc-list">
      <div v-for="item in list" :key="item.id" class="doc-item">
        <div class="doc-info">
          <div class="doc-name">{{ item.fileName }}</div>
          <div class="doc-meta">
            <el-tag size="small">{{ item.docTypeLabel }}</el-tag>
            <span class="doc-size">{{ formatSize(item.fileSize) }}</span>
            <span class="doc-time">{{ formatTime(item.createdAt) }}</span>
          </div>
        </div>
        <div class="doc-actions">
          <el-button size="small" text @click="downloadDoc(item)">下载</el-button>
          <el-button v-if="canManage" size="small" text type="danger" :loading="item._deleting" @click="deleteDoc(item)">删除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const props = defineProps<{ competitionId: number }>()

const canManage = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return info.role === 'ADMIN' || info.role === 'TEACHER'
  } catch { return false }
})

const list = ref<any[]>([])
const isLoading = ref(false)
const uploading = ref(false)
const showUpload = ref(false)
const uploadDocType = ref('SIGNUP_GUIDE')
const selectedFile = ref<File | null>(null)
const fileInput = ref<HTMLInputElement>()

const docTypes = [
  { value: 'SIGNUP_GUIDE', label: '报名须知' },
  { value: 'PRELIMINARY', label: '初赛说明' },
  { value: 'FINAL', label: '复赛说明' },
  { value: 'SUPPLEMENTARY', label: '补充材料' },
]

const loadData = async () => {
  isLoading.value = true
  try {
    const res: any = await request({ url: `/v1/competitions/${props.competitionId}/documents`, method: 'GET' })
    if (res.code === 0) list.value = res.data ?? []
  } finally {
    isLoading.value = false
  }
}

const triggerUpload = () => {
  showUpload.value = true
  selectedFile.value = null
  uploadDocType.value = 'SIGNUP_GUIDE'
}

const onFileChange = (file: any) => {
  selectedFile.value = file.raw
}

const confirmUpload = async () => {
  if (!selectedFile.value) return ElMessage.warning('请选择文件')
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('docType', uploadDocType.value)
    const res: any = await request({
      url: `/v1/competitions/${props.competitionId}/documents`,
      method: 'POST',
      data: formData,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.code === 0) {
      ElMessage.success('上传成功')
      showUpload.value = false
      await loadData()
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    uploading.value = false
  }
}

const downloadDoc = (item: any) => {
  window.open(`/api/v1/competitions/documents/${item.id}/download`, '_blank')
}

const deleteDoc = async (item: any) => {
  try {
    await ElMessageBox.confirm('确认删除该文档？', '操作确认', { type: 'warning' })
  } catch { return }
  item._deleting = true
  try {
    const res: any = await request({ url: `/v1/competitions/documents/${item.id}`, method: 'DELETE' })
    if (res.code === 0) {
      ElMessage.success('已删除')
      await loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    item._deleting = false
  }
}

const formatSize = (bytes: number) => {
  if (!bytes) return ''
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / (1024 * 1024)).toFixed(1) + 'MB'
}

const formatTime = (s: string) => {
  if (!s) return ''
  return new Date(s).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(loadData)
</script>

<style scoped>
.doc-section { margin-top: 20px; }
.doc-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.doc-title { font-size: 14px; font-weight: 700; color: #111; }
.center-tip { text-align: center; padding: 24px; color: #aaa; font-size: 13px; }
.doc-list { display: flex; flex-direction: column; gap: 8px; }
.doc-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border: 1px solid #e0e0e0; border-radius: 6px; background: #fafafa; }
.doc-info { flex: 1; }
.doc-name { font-size: 13px; font-weight: 600; color: #111; margin-bottom: 4px; }
.doc-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #888; }
.doc-size, .doc-time { color: #aaa; }
.doc-actions { display: flex; gap: 4px; flex-shrink: 0; }
</style>
