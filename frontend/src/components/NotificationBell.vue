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

          <!-- 个人赛指导申请（老师收到）：同意/拒绝 -->
          <div
            v-if="n.type === 'APPLY_RECEIVED' && n.title === '收到指导申请' && !n._handled"
            class="notif-actions"
            @click.stop
          >
            <el-button size="small" type="primary" :loading="n._loading" @click="handleApply(n, 'APPROVED', 'individual-guide')">
              同意指导
            </el-button>
            <el-button size="small" :loading="n._loading" @click="handleApply(n, 'REJECTED', 'individual-guide')">
              拒绝
            </el-button>
          </div>

          <!-- 入队申请（队长收到）：同意/拒绝 -->
          <div
            v-if="n.type === 'APPLY_RECEIVED' && n.title === '收到入队申请' && !n._handled"
            class="notif-actions"
            @click.stop
          >
            <el-button size="small" type="primary" :loading="n._loading" @click="handleApply(n, 'APPROVED', 'join')">
              同意入队
            </el-button>
            <el-button size="small" :loading="n._loading" @click="handleApply(n, 'REJECTED', 'join')">
              拒绝
            </el-button>
          </div>

          <!-- 带队申请（老师收到）：同意/拒绝 -->
          <div
            v-if="n.type === 'APPLY_RECEIVED' && n.title === '收到带队申请' && !n._handled"
            class="notif-actions"
            @click.stop
          >
            <el-button size="small" type="primary" :loading="n._loading" @click="handleApply(n, 'APPROVED', 'guide')">
              同意带队
            </el-button>
            <el-button size="small" :loading="n._loading" @click="handleApply(n, 'REJECTED', 'guide')">
              拒绝
            </el-button>
          </div>

          <!-- 队伍邀请（被邀请人收到）：接受/拒绝 -->
          <div
            v-if="n.type === 'TEAM_INVITE' && !n._handled"
            class="notif-actions"
            @click.stop
          >
            <el-button size="small" type="primary" :loading="n._loading" @click="handleApply(n, 'APPROVED', 'invite')">
              接受邀请
            </el-button>
            <el-button size="small" :loading="n._loading" @click="handleApply(n, 'REJECTED', 'invite')">
              拒绝
            </el-button>
          </div>

          <div v-if="n._handled" class="notif-handled">{{ n._handleResult }}</div>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// 跨会话持久化已处理的申请ID，关闭弹窗再打开、页面刷新后依然生效
const HANDLED_KEY = '_handledApplyIds'
const getHandledIds = (): Set<number> => {
  try {
    const raw = localStorage.getItem(HANDLED_KEY)
    return new Set<number>(raw ? JSON.parse(raw) : [])
  } catch { return new Set<number>() }
}
const saveHandledIds = (ids: Set<number>) => {
  localStorage.setItem(HANDLED_KEY, JSON.stringify([...ids]))
}
const handledApplyIds = getHandledIds()

// 暴露未读数给父组件用（可选）
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
      notifications.value = (res.data.list ?? []).map((n: any) => {
        const isHandled = handledApplyIds.has(n.relatedId)
        return {
          ...n,
          _loading: false,
          _handled: isHandled,
          _handleResult: isHandled ? '已处理' : ''
        }
      })
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

// 处理入队申请或邀请（APPLY_RECEIVED / TEAM_INVITE）
// relatedId 存的是 applyId，applyKind 区分处理接口
const handleApply = async (n: any, action: 'APPROVED' | 'REJECTED', applyKind: 'join' | 'guide' | 'invite' | 'individual-guide') => {
  if (!n.relatedId) {
    ElMessage.error('申请记录ID缺失，请刷新后重试')
    return
  }
  n._loading = true
  try {
    const urlMap: Record<string, string> = {
      'join': `/v1/team/invites/${n.relatedId}/handle`,
      'guide': `/v1/team/team-guide/${n.relatedId}/handle`,
      'invite': `/v1/team/invites/${n.relatedId}/handle`,
      'individual-guide': `/v1/recruitment/guide/${n.relatedId}/handle`,
    }
    const res: any = await request({ url: urlMap[applyKind], method: 'PUT', params: { action } })
if (res.code === 0) {
      handledApplyIds.add(n.relatedId)
      saveHandledIds(handledApplyIds)
      n._handled = true
      const labels: Record<string, string> = {
        join: action === 'APPROVED' ? '✓ 已同意入队' : '✗ 已拒绝',
        guide: action === 'APPROVED' ? '✓ 已同意带队' : '✗ 已拒绝',
        invite: action === 'APPROVED' ? '✓ 已接受邀请' : '✗ 已拒绝',
        'individual-guide': action === 'APPROVED' ? '✓ 已同意指导' : '✗ 已拒绝',
      }
      n._handleResult = labels[applyKind]
      markRead(n)
      ElMessage.success(n._handleResult)
      // 通知 ApplyList 页面同步刷新
      window.dispatchEvent(new CustomEvent('apply-handled'))
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch {
    ElMessage.error('网络错误')
  } finally {
    n._loading = false
  }
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
.notif-time { font-size: 11px; color: #bbb; margin-bottom: 6px; }

.notif-actions { display: flex; gap: 8px; margin-top: 6px; }

.notif-handled {
  font-size: 12px;
  color: #888;
  margin-top: 6px;
  font-style: italic;
}
</style>
