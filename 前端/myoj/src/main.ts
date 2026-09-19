import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { OpenAPI } from '@generated'
import App from './App.vue'
import { API_BASE_URL } from '@/config/api'
import { setupAuthInterceptor } from '@/api/setupAuthInterceptor'
import { resolveAccessToken, useAuthStore } from '@/stores/auth'
import { AUTH_STORAGE_KEY } from '@/config/auth-storage'
import router, { registerAdminRoutes } from './router'
import ArcoVue from '@arco-design/web-vue'
import '@arco-design/web-vue/dist/arco.css'
import '@/styles/tailwind.css'
/** 暗黑：仅比赛详情「实时榜单」表格与全站色板对齐（需在 Arco 之后加载） */
import '@/styles/dark-contest-leaderboard-hi.css'
/** 比赛 / 排行榜 / 详情：赛场高光（依赖上表，仍须在 Arco 之后） */
import '@/styles/oj-spotlight-pages.css'

/** 与 generated/core/OpenAPI 及后端 servers.url 对齐；登录后 TOKEN 供后续 Service 请求带 Bearer */
OpenAPI.BASE = API_BASE_URL
OpenAPI.WITH_CREDENTIALS = true
OpenAPI.TOKEN = async () => {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY)
    if (!raw) return ''
    const o = JSON.parse(raw) as { token?: string; accessToken?: string }
    return resolveAccessToken(o)
  } catch {
    return ''
  }
}

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
setupAuthInterceptor()
registerAdminRoutes(router)
void useAuthStore(pinia).ensureFreshAccessToken()

/** 新部署后旧缓存仍指向上一版 hashed chunk 时，动态 import 会失败；自动刷新一次常见可恢复 */
router.onError((err, to) => {
  const msg = err instanceof Error ? err.message : String(err)
  if (
    !/dynamically imported module|Loading chunk \d+ failed|ChunkLoadError|module script failed/i.test(
      msg,
    )
  ) {
    return
  }
  const k = `myoj:chunk-reload:${to.fullPath}`
  if (sessionStorage.getItem(k)) return
  sessionStorage.setItem(k, '1')
  window.location.assign(to.fullPath)
})

app.use(router)
app.use(ArcoVue)
app.mount('#app')
