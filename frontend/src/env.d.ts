// 识别vue文件，消除import导入红波浪
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// Element Plus 全局 API 类型声明（通过 app.use(ElementPlus) 注册后全局可用）
declare const ElMessage: typeof import('element-plus')['ElMessage']
declare const ElMessageBox: typeof import('element-plus')['ElMessageBox']
