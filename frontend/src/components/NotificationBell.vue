<template>
  <el-popover placement="bottom-end" :width="380" trigger="click" @show="loadNotifications">
    <template #reference>
      <el-badge :value="unreadCount || ''" :hidden="unreadCount === 0" class="notif-badge">
        <span class="notif-icon" title="消息通知">🔔</span>
      </el-badge>
    </template>

    <div class="notif-panel">
      <div class="notif-header">
        <span>消息通知</span>
        <el-button v-if="unreadCount > 0" size="small" text @click="markAllRead">全部已读</el-button>
      </div>

      <div v-if="notifLoading" class="notif-empty">加载中...</div>
      <div v-else-if="notifications.length === 0" class="notif-empty">暂无消息</div>
      <div v-else class="notif-list">
        <div
          v-for="n in notifications"
          :key="n.id"
          class="notif-item"
          :class="{ unread: !n.isRead }"
          @click="markRead(n)"
        >
          <div class="notif-title">{{ n.title }}</div>
          <div class="notif-content">{{ n.content }}</div>
          <div class="notif-time">{{ formatTime(n.createdAt) }}</div>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const unreadCount = ref(0)
const notifications = ref<any[]>([])
const notifLoading = ref(false)
let pollTimer: ReturnType<typeof setInterval> | null = null

const fetchUnreadCount = async () => {
  try {
    const res: any = await request({ url: '/v1/notifications/unread/count', method: 'GET' })
    if (res.code === 0) unreadCount.value = Number(res.data) || 0
  } catch { /* ignore */ }
}

const loadNotifications = async () => {
  notifLoading.value = true
  try {
    const res: any = await request({ url: '/v1/notifications', method: 'GET', params: { page: 1, size: 30 } })
    if (res.code === 0) {
      notifications.value = (res.data.list ?? []).map((n: any) => ({
        ...n,
        _loading: false
      }))
    }
  } finally {
    notifLoading.value = false
  }
}

const markRead = async (n: any) => {
  if (n.isRead) return
  try {
    await request({ url: `/v1/notifications/${n.id}/read`, method: 'PUT' })
    n.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch { /* ignore */ }
}

const markAllRead = async () => {
  try {
    await request({ url: '/v1/notifications/read/all', method: 'PUT' })
    notifications.value.forEach((n: any) => { n.isRead = true })
    unreadCount.value = 0
    ElMessage.success('已全部标记已读')
  } catch { /* ignore */ }
}

const formatTime = (s: string) => {
  if (!s) return ''
  return new Date(s).toLocaleString('zh-CN', {
    month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}

onMounted(() => {
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 30_000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.notif-badge { cursor: pointer; }
.notif-icon { font-size: 16px; padding: 4px; cursor: pointer; user-select: none; }
.notif-panel { max-height: 480px; display: flex; flex-direction: column; }
.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  font-weight: 700;
  color: #111;
  margin-bottom: 2px;
}
.notif-empty { text-align: center; padding: 32px; color: #aaa; font-size: 13px; }
.notif-list { overflow-y: auto; max-height: 420px; }
.notif-item {
  padding: 12px 4px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.1s;
}
.notif-item:last-child { border-bottom: none; }
.notif-item:hover { background: #fafafa; }
.notif-item.unread { background: #fffbf0; }
.notif-item.unread:hover { background: #fff8e6; }
.notif-title { font-size: 13px; font-weight: 600; color: #111; margin-bottom: 3px; }
.notif-content { font-size: 12px; color: #555; line-height: 1.5; margin-bottom: 4px; }
.notif-time { font-size: 11px; color: #bbb; }
</style>