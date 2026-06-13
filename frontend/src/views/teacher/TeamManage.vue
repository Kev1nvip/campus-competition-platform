<template>
  <div class="page-box">
    <h2>指导队伍管理</h2>
    <div class="search-bar">
      <input v-model="searchKey" placeholder="搜索队伍名称" />
      <button @click="search">查询</button>
    </div>
    <table border="1" cellpadding="8">
      <thead>
        <tr>
          <th>队伍ID</th>
          <th>队伍名称</th>
          <th>队员</th>
          <th>所属竞赛</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in teamList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.teamName }}</td>
          <td>{{ item.member }}</td>
          <td>{{ item.compName }}</td>
          <td><button>查看详情</button></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getTeamList } from '@/api/teacher'
const teamList = ref([])
const searchKey = ref('')

const loadData = async () => {
  try {
    const res = await getTeamList()
    teamList.value = res.data.data
  } catch {}
}
onMounted(loadData)

const search = () => {
  loadData()
}
</script>

<style scoped>
.page-box { padding:30px; }
.search-bar { margin-bottom:20px; }
table { width:100%; border-collapse: collapse; }
</style>