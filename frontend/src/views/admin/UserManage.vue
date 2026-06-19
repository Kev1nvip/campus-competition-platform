<template>
  <div>
    <div class="page-header">
      <div class="page-title">用户管理</div>
      <el-button type="primary" size="small" @click="showAdd = true">新增用户</el-button>
    </div>

    <div class="toolbar">
      <el-input v-model="keyword" placeholder="姓名 / 账号搜索" clearable size="small" style="width:220px;" @keyup.enter="loadData" @clear="loadData" />
      <el-button size="small" @click="loadData">搜索</el-button>
    </div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="list.length === 0" class="center-tip">暂无用户</div>
    <el-table v-else :data="list" class="data-table">
      <el-table-column label="ID" prop="id" width="70" />
      <el-table-column label="账号" prop="username" />
      <el-table-column label="姓名" prop="realName" />
      <el-table-column label="角色" prop="role" width="90">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ row.role }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
            {{ row.status === 'ACTIVE' ? '正常' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
<el-table-column label="操作" width="160">
        <template #default="{ row }: { row: any }">
          <el-button
            size="small"
            :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
            text
            :disabled="row.id === currentUserId"
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="showAdd" title="新增用户" width="440px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="addForm.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="addForm.password" type="password" show-password /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="addForm.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="addForm.role" style="width:100%;">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="handleAdd">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, addUser, toggleUserStatus } from '@/api/admin'

const list = ref<any[]>([])
const isLoading = ref(false)
const keyword = ref('')
const showAdd = ref(false)
const addForm = reactive({ username: '', password: '', realName: '', role: 'STUDENT' })
const currentUserId = Number(JSON.parse(localStorage.getItem('userInfo') || '{}').userId) || 0

const loadData = async () => {
  isLoading.value = true
  try {
    const res: any = await getUserList({ keyword: keyword.value || undefined })
    if (res.code === 0) {
      list.value = res.data?.list ?? res.data ?? []
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '请求失败，请检查后端服务')
  } finally {
    isLoading.value = false
  }
}

const handleAdd = async () => {
  if (!addForm.username || !addForm.password || !addForm.realName) return ElMessage.warning('请填写完整信息')
  try {
    const res: any = await addUser(addForm)
    if (res.code === 0) {
      ElMessage.success('新增成功')
      showAdd.value = false
      addForm.username = ''; addForm.password = ''; addForm.realName = ''
      loadData()
    } else {
      ElMessage.error(res.message || '新增失败')
    }
  } catch {
    ElMessage.error('网络错误')
  }
}

const handleToggleStatus = async (row: any) => {
  const newStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const label = newStatus === 'DISABLED' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${label}用户「${row.realName}」？`, '操作确认', { type: 'warning' })
    const res: any = await toggleUserStatus(row.id, newStatus)
    if (res.code === 0) {
      ElMessage.success(`已${label}`)
      loadData()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error('操作失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.page-title { font-size: 16px; font-weight: 700; color: #111; }
.toolbar { display: flex; gap: 8px; margin-bottom: 14px; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
</style>
