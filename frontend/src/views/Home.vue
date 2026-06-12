<!--
  组件：首页
  说明：展示平台欢迎信息，提供导航入口
-->
<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const navigateTo = (path: string) => {
  router.push(path)
}
</script>

<template>
  <div class="home">
    <h1>🏆 校园竞赛平台</h1>
    <p class="subtitle">发现精彩竞赛，记录成长足迹</p>
    
    <div class="nav-grid">
      <div class="nav-card" @click="navigateTo('/competitions')">
        <div class="nav-icon">📋</div>
        <h3>竞赛列表</h3>
        <p>浏览各类学科竞赛</p>
      </div>
      
      <div class="nav-card" @click="navigateTo('/profile')">
        <div class="nav-icon">👤</div>
        <h3>个人中心</h3>
        <p>管理参赛和获奖记录</p>
      </div>
      
      <div class="nav-card" @click="navigateTo('/teams')">
        <div class="nav-icon">👥</div>
        <h3>组队大厅</h3>
        <p>寻找志同道合的队友</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

h1 {
  font-size: 42px;
  color: white;
  margin-bottom: 10px;
  font-weight: 700;
}

.subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin-bottom: 50px;
}

.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  width: 100%;
  max-width: 900px;
}

.nav-card {
  background: white;
  border-radius: 16px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.nav-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
}

.nav-icon {
  font-size: 48px;
  margin-bottom: 15px;
}

.nav-card h3 {
  font-size: 20px;
  color: #333;
  margin-bottom: 10px;
}

.nav-card p {
  font-size: 14px;
  color: #666;
  margin: 0;
}
</style>