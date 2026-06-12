<template>
  <div class="page-box">
    <h2>院系/专业数据维护</h2>
    <button>新增院系</button>
    <div style="display:flex; gap:40px; margin-top:20px;">
      <div>
        <h4>院系列表</h4>
        <ul>
          <li v-for="d in deptList" :key="d.id">{{ d.name }}</li>
        </ul>
      </div>
      <div>
        <h4>对应专业</h4>
        <button>添加专业</button>
        <ul>
          <li v-for="m in majorList" :key="m.id">{{ m.name }}</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDeptList } from '@/api/admin'
const deptList = ref([])
const majorList = ref([])

const load = async () => {
  try {
    const res = await getDeptList()
    deptList.value = res.data.data
  } catch {}
}
onMounted(load)
</script>

<style scoped>
.page-box { padding:30px; }
</style>