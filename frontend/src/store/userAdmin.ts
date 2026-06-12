// 1. 导入pinia核心方法
import { defineStore } from 'pinia'

// 2. 定义管理员信息类型
interface AdminInfo {
  id: string
  name: string
}

export const useAdminStore = defineStore('admin', {
  state: (): AdminInfo => ({
    id: '',
    name: ''
  }),
  actions: {
    setUser(info: AdminInfo) {
      this.id = info.id
      this.name = info.name
    },
    clear() {
      this.$reset()
    }
  },
  // 持久化登录状态，刷新页面不会退出登录
  persist: true
})