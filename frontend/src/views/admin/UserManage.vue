<template>
  <div class="page-box">
    <h2>平台账号管理</h2>
    <div class="search-bar">
      <input v-model="searchVal" placeholder="姓名/账号搜索" />
      <button @click="searchUser">搜索</button>
      <button @click="openAdd">新增账号</button>
    </div>
    <table border="1" cellpadding="8">
      <thead>
        <tr>
          <th>ID</th>
          <th>账号</th>
          <th>姓名</th>
          <th>身份</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in userList" :key="item.id">
          <td>{{ item.id }}</td>
          <td>{{ item.account }}</td>
          <td>{{ item.name }}</td>
          <td>{{ item.role }}</td>
          <td>{{ item.status }}</td>
          <td>
            <button>编辑</button>
            <button>禁用</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getUserList, addUser } from '@/api/admin'
const userList = ref([])
const searchVal = ref('')

const load = async () => {
  try {
    const res = await getUserList()
    userList.value = res.data.data
  } catch {}
}
onMounted(load)

const searchUser = () => load()
const openAdd = () => alert('弹出新增账号弹窗')
</script>

<style scoped>
.page-box { padding:30px; }
.search-bar { margin-bottom:20px; }
table { width:100%; border-collapse: collapse; }
</style>