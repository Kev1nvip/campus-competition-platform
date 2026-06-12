<template>
  <div class="page-box">
    <h2>发布竞赛</h2>
    <div class="form-item">
      <label>竞赛名称</label>
      <input v-model="form.name" placeholder="请输入竞赛名称" />
    </div>
    <div class="form-item">
      <label>竞赛简介</label>
      <textarea v-model="form.desc"></textarea>
    </div>
    <div class="form-item">
      <label>报名截止时间</label>
      <input v-model="form.endTime" type="date" />
    </div>
    <button @click="submit">提交发布</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { addCompetition } from '@/api/teacher'
const form = ref({
  name: '',
  desc: '',
  endTime: ''
})

// 提交发布
const submit = async () => {
  if (!form.value.name) {
    alert('请输入竞赛名称')
    return
  }
  try {
    const res = await addCompetition(form.value)
    if (res.data.code === 200) {
      alert('发布成功')
      form.value = { name: '', desc: '', endTime: '' }
    }
  } catch (err) {
    alert('提交失败，后端未启动')
  }
}
</script>

<style scoped>
.page-box {
  padding: 30px;
}
.form-item {
  margin: 15px 0;
}
input, textarea {
  width: 400px;
  padding: 6px;
  margin-left: 10px;
}
</style>