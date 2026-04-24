<!--
  组件：首页
  说明：展示平台欢迎信息，提供导航入口
-->
<script setup lang="ts">
</script>

<template>
  <div class="home">
    <h1>校园竞赛平台</h1>
    <p>欢迎使用校园竞赛平台</p>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
}

h1 {
  font-size: 32px;
  color: #333;
  margin-bottom: 20px;
}

p {
  font-size: 18px;
  color: #666;
}
</style>