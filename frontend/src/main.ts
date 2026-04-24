import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
<<<<<<< HEAD

createApp(App).mount('#app')
=======
import router from './router'
import pinia from './store'

const app = createApp(App)

// 注册路由和状态管理
app.use(router)
app.use(pinia)

app.mount('#app')
>>>>>>> feature/frontend-init
