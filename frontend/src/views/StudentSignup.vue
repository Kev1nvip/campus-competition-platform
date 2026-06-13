<template>
  <div class="page-wrap">
    <header class="nav">
      <span class="nav-brand" @click="router.push('/')">◆ 校园竞赛平台</span>
      <span class="nav-item" @click="router.push(`/competition/${route.params.id}`)">← 返回竞赛详情</span>
    </header>

    <div class="page-inner">
      <h2>个人赛报名</h2>

      <el-form :model="form" label-width="90px" class="signup-form">
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="11位手机号" />
        </el-form-item>
        <el-form-item label="电子邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="参赛动机">
          <el-input v-model="form.motivation" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="form.introduction" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>

        <el-divider />

        <el-form-item label="指导老师">
          <div class="teacher-search">
            <el-input v-model="teacherKeyword" placeholder="搜索老师姓名..." clearable @input="fetchTeachers" size="small" style="width:200px;" />
          </div>
          <div v-if="isTeacherLoading" class="tip">加载中...</div>
          <div v-else-if="teachers.length === 0" class="tip">暂无可选老师</div>
          <div v-else class="teacher-list">
            <div
              v-for="t in teachers"
              :key="t.userId"
              :class="['teacher-item', selectedTeacher?.userId === t.userId ? 'selected' : '']"
              @click="selectedTeacher = selectedTeacher?.userId === t.userId ? null : t"
            >
              <div class="t-name">{{ t.realName }} <span class="t-title">{{ t.title }}</span></div>
              <div class="t-dept">{{ t.department }}</div>
            </div>
          </div>
          <el-checkbox v-model="noTeacher" style="margin-top:8px;">不需要指导老师</el-checkbox>
        </el-form-item>

        <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">提交报名</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { signupApi } from '@/api/signup'
import type { TeacherInfo } from '@/types/signup'

const route = useRoute()
const router = useRouter()
const form = reactive({ phone: '', email: '', motivation: '', introduction: '' })
const teacherKeyword = ref('')
const teachers = ref<TeacherInfo[]>([])
const selectedTeacher = ref<TeacherInfo | null>(null)
const noTeacher = ref(false)
const isTeacherLoading = ref(false)
const submitting = ref(false)
const errorMsg = ref('')

const fetchTeachers = async () => {
  isTeacherLoading.value = true
  try {
    const res = await signupApi.getAvailableTeachers({ page: 1, size: 50, keyword: teacherKeyword.value || undefined })
    if (res.code === 0) teachers.value = res.data.list
  } finally {
    isTeacherLoading.value = false
  }
}

const handleSubmit = async () => {
  if (!form.phone || !/^1[3-9]\d{9}$/.test(form.phone)) return (errorMsg.value = '请输入正确的手机号')
  if (!form.email) return (errorMsg.value = '请输入邮箱')
  if (!noTeacher.value && !selectedTeacher.value) return (errorMsg.value = '请选择指导老师或勾选"不需要指导老师"')
  errorMsg.value = ''
  submitting.value = true
  try {
    const res = await signupApi.individualSignup({
      competitionId: Number(route.params.id),
      teacherId: selectedTeacher.value?.userId,
    })
    if (res.code === 0) {
      ElMessage.success('报名成功，等待审核')
      router.push(`/competition/${route.params.id}`)
    } else {
      errorMsg.value = res.message || '报名失败'
    }
  } catch (e: any) {
    errorMsg.value = e.response?.data?.message || '网络错误'
  } finally {
    submitting.value = false
  }
}

onMounted(fetchTeachers)
</script>

<style scoped>
.page-wrap { min-height: 100vh; background: #fff; }
.nav { height: 56px; border-bottom: 1px solid #e0e0e0; display: flex; align-items: center; justify-content: space-between; padding: 0 40px; }
.nav-brand { font-size: 14px; font-weight: 800; letter-spacing: 1px; color: #111; cursor: pointer; }
.nav-item { font-size: 13px; color: #555; cursor: pointer; padding: 6px 12px; border-radius: 4px; }
.nav-item:hover { background: #f4f4f4; color: #111; }
.page-inner { max-width: 640px; margin: 0 auto; padding: 32px 20px; }
h2 { font-size: 20px; font-weight: 700; color: #111; margin-bottom: 24px; }
.signup-form { background: #fff; }
.teacher-search { margin-bottom: 10px; }
.tip { font-size: 13px; color: #aaa; padding: 8px 0; }
.teacher-list { display: flex; flex-direction: column; gap: 6px; max-height: 240px; overflow-y: auto; }
.teacher-item { padding: 10px 14px; border: 1px solid #e0e0e0; border-radius: 6px; cursor: pointer; transition: all 0.12s; }
.teacher-item:hover { border-color: #aaa; background: #fafafa; }
.teacher-item.selected { border-color: #111; background: #f4f4f4; }
.t-name { font-size: 14px; font-weight: 600; color: #111; }
.t-title { font-size: 12px; color: #888; font-weight: 400; margin-left: 6px; }
.t-dept { font-size: 12px; color: #aaa; margin-top: 2px; }
.error-tip { font-size: 13px; color: #888; background: #f8f8f8; border: 1px solid #e8e8e8; border-radius: 4px; padding: 8px 12px; margin-bottom: 12px; }
</style>
