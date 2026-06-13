<template>
  <div class="home-wrap">
    <!-- 顶部导航 -->
    <header class="nav">
      <div class="nav-brand">◆ 校园竞赛平台</div>
      <nav class="nav-links">
        <span class="nav-item" @click="router.push('/competitions')">竞赛列表</span>
        <span class="nav-item" @click="router.push('/teams')">队伍</span>
        <span class="nav-item" @click="router.push('/ai-recommend')">AI 推荐</span>
        <el-button size="small" @click="router.push('/login')" style="margin-left:8px;">登录</el-button>
        <el-button size="small" type="primary" @click="router.push('/register')">注册</el-button>
      </nav>
    </header>

    <!-- Hero -->
    <section class="hero">
      <div class="hero-content">
        <h1>校园学术竞赛管理平台</h1>
        <p class="hero-sub">浏览竞赛、组建团队、智能推荐，一站式竞赛管理体验</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/competitions')">浏览竞赛</el-button>
          <el-button size="large" @click="router.push('/register')">立即注册</el-button>
        </div>
      </div>
    </section>

    <!-- 功能入口 -->
    <section class="features">
      <div class="feature-card" @click="router.push('/competitions')">
        <div class="feat-icon">▤</div>
        <h3>竞赛列表</h3>
        <p>浏览全部校园学术竞赛，按状态和类型筛选</p>
      </div>
      <div class="feature-card" @click="router.push('/teams')">
        <div class="feat-icon">◈</div>
        <h3>队伍管理</h3>
        <p>创建或加入队伍，协作参赛</p>
      </div>
      <div class="feature-card" @click="router.push('/ai-recommend')">
        <div class="feat-icon">◉</div>
        <h3>AI 智能推荐</h3>
        <p>根据你的方向智能匹配合适竞赛</p>
      </div>
      <div class="feature-card" @click="router.push('/profile')">
        <div class="feat-icon">◎</div>
        <h3>个人中心</h3>
        <p>查看报名记录、获奖情况</p>
      </div>
    </section>

    <footer class="foot">© 2026 校园竞赛平台</footer>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.home-wrap {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
}

.nav {
  height: 56px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
}

.nav-brand {
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 1px;
  color: #111;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-item {
  padding: 6px 12px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.15s;
}
.nav-item:hover { background: #f4f4f4; color: #111; }

.hero {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 40px 60px;
  border-bottom: 1px solid #e0e0e0;
}

.hero-content {
  text-align: center;
  max-width: 560px;
}

h1 {
  font-size: 36px;
  font-weight: 800;
  color: #111;
  line-height: 1.2;
  margin-bottom: 16px;
}

.hero-sub {
  font-size: 15px;
  color: #666;
  margin-bottom: 32px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.features {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-bottom: 1px solid #e0e0e0;
}

.feature-card {
  padding: 36px 28px;
  border-right: 1px solid #e0e0e0;
  cursor: pointer;
  transition: background 0.15s;
}
.feature-card:last-child { border-right: none; }
.feature-card:hover { background: #fafafa; }

.feat-icon {
  font-size: 24px;
  color: #333;
  margin-bottom: 12px;
}

.feature-card h3 {
  font-size: 15px;
  font-weight: 700;
  color: #111;
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 13px;
  color: #888;
  line-height: 1.6;
}

.foot {
  padding: 20px 40px;
  text-align: center;
  font-size: 12px;
  color: #bbb;
}
</style>
