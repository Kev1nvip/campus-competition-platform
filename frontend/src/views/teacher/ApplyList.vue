<template>
  <div class="page-box">
    <h2>学生报名审核列表</h2>
    <table border="1" cellpadding="8">
      <thead>
        <tr>
          <th>学生姓名</th>
          <th>学号</th>
          <th>参赛队伍</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in tableList" :key="item.id">
          <td>{{ item.stuName }}</td>
          <td>{{ item.stuId }}</td>
          <td>{{ item.teamName }}</td>
          <td>{{ item.status }}</td>
          <td>
            <button @click="handleAudit(item,1)">通过</button>
            <button @click="handleAudit(item,2)">驳回</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getApplyList, auditApply } from '@/api/teacher'
const tableList = ref([])

onMounted(async () => {
  try {
    const res = await getApplyList()
    tableList.value = res.data.data
  } catch {}
})

const handleAudit = async (row: any, status: number) => {
  await auditApply({ id: row.id, status })
  alert(status === 1 ? '已通过' : '已驳回')
  onMounted()
}
</script>

<style scoped>
.page-box { padding:30px; }
table { width: 100%; border-collapse: collapse; }
</style>