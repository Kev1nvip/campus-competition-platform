<template>
  <div class="page-box">
    <h2>全部竞赛管理</h2>
    <button>新增竞赛</button>
    <table border="1" cellpadding="8">
      <thead>
        <tr>
          <th>竞赛ID</th>
          <th>竞赛名称</th>
          <th>指导教师</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in compList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.compName }}</td>
          <td>{{ item.teacherName }}</td>
          <td>{{ item.status }}</td>
          <td>
            <button>编辑</button>
            <button>下架</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getCompAllList } from '@/api/admin'
const compList = ref([])

const loadData = async () => {
  try {
    const res = await getCompAllList()
    compList.value = res.data.data
  } catch {}
}
onMounted(loadData)
</script>

<style scoped>
.page-box { padding:30px; }
table { width:100%; border-collapse: collapse; margin-top:20px; }
</style>