<!--
  组件：登录页
  说明：用户输入账号密码进行登录
-->
<script setup lang="ts">
</script>

<template>
  <div class="login">
    <h1>登录</h1>
    <p>请输入账号密码登录</p>
  </div>
</template>

<style scoped>
.login {
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