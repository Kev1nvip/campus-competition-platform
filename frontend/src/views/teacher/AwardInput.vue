<template>
  <div class="page-box">
    <h2>竞赛获奖信息录入</h2>
    <div class="form-item">
      <label>竞赛名称</label>
      <select v-model="form.compId">
        <option value="">选择竞赛</option>
      </select>
    </div>
    <div class="form-item">
      <label>获奖队伍</label>
      <input v-model="form.teamName" />
    </div>
    <div class="form-item">
      <label>奖项等级</label>
      <select v-model="form.awardLevel">
        <option>一等奖</option>
        <option>二等奖</option>
        <option>三等奖</option>
      </select>
    </div>
    <button @click="submitAward">保存获奖记录</button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { addAward } from '@/api/teacher'
const form = ref({
  compId: '',
  teamName: '',
  awardLevel: ''
})

const submitAward = async () => {
  if (!form.value.compId) {
    alert('请选择竞赛')
    return
  }
  try {
    await addAward(form.value)
    alert('保存成功')
    form.value = { compId: '', teamName: '', awardLevel: '' }
  } catch (err) {
    alert('提交失败')
  }
}
</script>

<style scoped>
.page-box { padding:30px; }
.form-item { margin:15px 0; }
</style>