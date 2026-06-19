<template>
  <div>
    <div class="page-title">院系管理</div>

    <div class="toolbar">
      <el-button type="primary" size="small" @click="openAdd">新增院系</el-button>
    </div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="list.length === 0" class="center-tip">暂无院系数据</div>
    <el-table v-else :data="list" class="data-table">
      <el-table-column label="院系名称" prop="name" min-width="160" />
      <el-table-column label="用户数" prop="userCount" width="80" />
      <el-table-column label="操作" width="180">
        <template #default="{ row, $index }">
          <el-button size="small" text @click="openRename(row, $index)">重命名</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增院系 -->
    <el-dialog v-model="showAdd" title="新增院系" width="380px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="院系名称">
          <el-input v-model="addForm.name" placeholder="请输入院系名称" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="handleAdd">确认</el-button>
      </template>
    </el-dialog>

    <!-- 重命名院系 -->
    <el-dialog v-model="showRename" title="重命名院系" width="380px">
      <el-form label-width="80px">
        <el-form-item label="原名称">
          <el-input :model-value="renameOld" disabled />
        </el-form-item>
        <el-form-item label="新名称">
          <el-input v-model="renameForm.name" placeholder="请输入新名称" maxlength="64" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRename = false">取消</el-button>
        <el-button type="primary" :loading="renameLoading" @click="handleRename">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDeptList, addDept, renameDept, deleteDept } from '@/api/admin'

const list = ref<any[]>([])
const isLoading = ref(false)

const showAdd = ref(false)
const addLoading = ref(false)
const addForm = reactive({ name: '' })

const showRename = ref(false)
const renameLoading = ref(false)
const renameOld = ref('')
const renameIndex = ref(-1)
const renameForm = reactive({ name: '' })

const loadData = async () => {
  isLoading.value = true
  try {
    const res: any = await getDeptList()
    if (res.code === 0) list.value = res.data ?? []
  } finally {
    isLoading.value = false
  }
}

const openAdd = () => {
  addForm.name = ''
  showAdd.value = true
}

const handleAdd = async () => {
  if (!addForm.name.trim()) return ElMessage.warning('请输入院系名称')
  addLoading.value = true
  try {
    const res: any = await addDept(addForm.name.trim())
    if (res.code === 0) {
      ElMessage.success('新增成功')
      showAdd.value = false
      await loadData()
    } else {
      ElMessage.error(res.message || '新增失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    addLoading.value = false
  }
}

const openRename = (row: any, index: number) => {
  renameOld.value = row.name
  renameIndex.value = index
  renameForm.name = ''
  showRename.value = true
}

const handleRename = async () => {
  if (!renameForm.name.trim()) return ElMessage.warning('请输入新名称')
  if (renameForm.name.trim() === renameOld.value) {
    showRename.value = false
    return
  }
  renameLoading.value = true
  try {
    const res: any = await renameDept(renameOld.value, renameForm.name.trim())
    if (res.code === 0) {
      ElMessage.success('重命名成功')
      showRename.value = false
      await loadData()
    } else {
      ElMessage.error(res.message || '重命名失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    renameLoading.value = false
  }
}

const handleDelete = async (row: any) => {
  try {
    await ElMessageBox.confirm(
      `确认删除院系「${row.name}」？该操作会清空所有用户所属院系信息。`,
      '操作确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch { return }

  try {
    const res: any = await deleteDept(row.name)
    if (res.code === 0) {
      ElMessage.success('已删除')
      await loadData()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.toolbar { margin-bottom: 16px; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
</style>