<!--
  组件：首页
  说明：展示平台欢迎信息，提供导航入口
-->
<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="home-container">
    <!-- 头部导航 -->
    <div class="home-header">
      <div class="logo">🏆 校园竞赛平台</div>
      <nav class="nav-links">
        <button class="nav-link" @click="router.push('/competitions')">竞赛列表</button>
        <button class="nav-link" @click="router.push('/ai-recommend')">AI推荐</button>
        <button class="nav-link" @click="router.push('/teams')">团队管理</button>
        <button class="nav-link" @click="router.push('/login')">登录</button>
      </nav>
    </div>

    <!-- 主内容区 -->
    <div class="home-main">
      <div class="hero-section">
        <h1 class="hero-title">发现精彩学术竞赛</h1>
        <p class="hero-subtitle">探索校园竞赛，成就学术梦想</p>
        
        <div class="action-buttons">
          <button class="btn-primary" @click="router.push('/competitions')">
            📋 浏览竞赛
          </button>
          <button class="btn-secondary" @click="router.push('/ai-recommend')">
            🤖 AI智能推荐
          </button>
        </div>
      </div>

      <!-- 功能卡片 -->
      <div class="features-section">
        <div class="feature-card" @click="router.push('/competitions')">
          <div class="feature-icon">📊</div>
          <h3>竞赛浏览</h3>
          <p>浏览各类学术竞赛，了解详细信息</p>
        </div>
        
        <div class="feature-card" @click="router.push('/ai-recommend')">
          <div class="feature-icon">🤖</div>
          <h3>AI推荐</h3>
          <p>智能推荐适合您的竞赛方向</p>
        </div>
        
        <div class="feature-card" @click="router.push('/teams')">
          <div class="feature-icon">👥</div>
          <h3>团队管理</h3>
          <p>创建团队，携手参赛</p>
        </div>
      </div>
    </div>

    <!-- 页脚 -->
    <div class="home-footer">
      <p>&copy; 2026 校园竞赛平台 - 学术竞赛管理系统</p>
    </div>
  </div>
</template>

<style scoped>
.home-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.logo {
  font-size: 20px;
  font-weight: bold;
}

.nav-links {
  display: flex;
  gap: 16px;
}

.nav-link {
  background: none;
  border: none;
  color: white;
  font-size: 14px;
  cursor: pointer;
  padding: 8px 16px;
  border-radius: 20px;
  transition: background 0.3s;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.2);
}

.home-main {
  flex: 1;
  padding: 40px 24px;
}

.hero-section {
  text-align: center;
  margin-bottom: 48px;
}

.hero-title {
  font-size: 48px;
  color: #333;
  margin-bottom: 16px;
}

.hero-subtitle {
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
  margin-bottom: 32px;
}

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.btn-primary, .btn-secondary {
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 16px;
  cursor: pointer;
  transition: transform 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
}

.btn-secondary {
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
}

.btn-primary:hover, .btn-secondary:hover {
  transform: translateY(-2px);
}

.features-section {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.feature-card {
  width: 280px;
  padding: 24px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  text-align: center;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.feature-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 18px;
  color: #333;
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 14px;
  color: #666;
}

.home-footer {
  text-align: center;
  padding: 24px;
  background: #f5f5f5;
  color: #999;
  font-size: 14px;
}
</style>