<template>
  <div>
    <div class="page-title">院系管理</div>

    <div v-if="isLoading" class="center-tip">加载中...</div>
    <div v-else-if="list.length === 0" class="center-tip">暂无院系数据（接口待完善）</div>
    <el-table v-else :data="list" class="data-table">
      <el-table-column label="院系名称" prop="name" />
      <el-table-column label="简介" prop="description" />
      <el-table-column label="操作" width="100">
        <template #default>
          <el-button size="small" text>编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="add-section">
      <el-button type="primary" size="small" @click="showAdd = true">新增院系</el-button>
    </div>

    <el-dialog v-model="showAdd" title="新增院系" width="380px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="院系名称">
          <el-input v-model="addForm.name" placeholder="请输入院系名称" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="addForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd = false">取消</el-button>
        <el-button type="primary" @click="ElMessage.info('接口待完善')">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getDeptList } from '@/api/admin'

const list = ref<any[]>([])
const isLoading = ref(false)
const showAdd = ref(false)
const addForm = reactive({ name: '', description: '' })

const loadData = async () => {
  isLoading.value = true
  try {
    const res: any = await getDeptList()
    if (res.code === 0) list.value = res.data ?? []
  } finally {
    isLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-title { font-size: 16px; font-weight: 700; color: #111; margin-bottom: 20px; padding-bottom: 12px; border-bottom: 1px solid #e0e0e0; }
.center-tip { text-align: center; padding: 40px; color: #aaa; font-size: 14px; }
.data-table { width: 100%; background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; }
.add-section { margin-top: 16px; }
</style>
