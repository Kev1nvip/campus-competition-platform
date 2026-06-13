<template>
  <div class="dashboard">
    <!-- 欢迎语 -->
    <div class="welcome">
      <h2>你好，{{ userName }} 👋</h2>
      <p>查看最新竞赛动态，管理你的报名与队伍</p>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-cards">
      <div class="quick-card" @click="router.push('/student/competitions')">
        <div class="qc-icon">▤</div>
        <div class="qc-info">
          <div class="qc-title">竞赛列表</div>
          <div class="qc-sub">浏览全部可报名竞赛</div>
        </div>
        <span class="qc-arrow">→</span>
      </div>
      <div class="quick-card" @click="router.push('/student/signups')">
        <div class="qc-icon">◎</div>
        <div class="qc-info">
          <div class="qc-title">我的报名</div>
          <div class="qc-sub">查看报名状态与审核进度</div>
        </div>
        <span class="qc-arrow">→</span>
      </div>
      <div class="quick-card" @click="router.push('/student/teams')">
        <div class="qc-icon">◈</div>
        <div class="qc-info">
          <div class="qc-title">我的队伍</div>
          <div class="qc-sub">创建或加入参赛队伍</div>
        </div>
        <span class="qc-arrow">→</span>
      </div>
      <div class="quick-card" @click="router.push('/student/ai')">
        <div class="qc-icon">◉</div>
        <div class="qc-info">
          <div class="qc-title">AI 推荐</div>
          <div class="qc-sub">智能匹配适合你的竞赛</div>
        </div>
        <span class="qc-arrow">→</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const userName = computed(() => {
  const info = localStorage.getItem('userInfo')
  return info ? (JSON.parse(info).realName || '同学') : '同学'
})
</script>

<style scoped>
.dashboard { padding-top: 8px; }

.welcome {
  margin-bottom: 32px;
}
.welcome h2 {
  font-size: 22px;
  font-weight: 700;
  color: #111;
  margin: 0 0 6px;
}
.welcome p {
  font-size: 14px;
  color: #888;
  margin: 0;
}

.quick-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.quick-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.quick-card:hover {
  border-color: #aaa;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}

.qc-icon {
  font-size: 20px;
  color: #333;
  width: 36px;
  text-align: center;
  flex-shrink: 0;
}

.qc-info { flex: 1; }
.qc-title {
  font-size: 14px;
  font-weight: 700;
  color: #111;
  margin-bottom: 4px;
}
.qc-sub {
  font-size: 12px;
  color: #888;
}

.qc-arrow { color: #ccc; font-size: 16px; }
</style>
