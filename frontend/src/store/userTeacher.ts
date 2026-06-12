import { defineStore } from 'pinia'

interface TeacherInfo {
  id: string
  name: string
  tid: string
}

export const useTeacherStore = defineStore('teacher', {
  state: (): TeacherInfo => ({
    id: '',
    name: '',
    tid: ''
  }),
  actions: {
    setUser(info: TeacherInfo) {
      this.id = info.id
      this.name = info.name
      this.tid = info.tid
    },
    clear() {
      this.$reset()
    }
  },
  persist: true
})