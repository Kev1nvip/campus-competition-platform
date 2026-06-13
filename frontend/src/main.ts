import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import pinia from './store'
// 新增持久化插件
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const app = createApp(App)
// 注册持久化
pinia.use(piniaPluginPersistedstate)

app.use(router)
app.use(pinia)

app.mount('#app')