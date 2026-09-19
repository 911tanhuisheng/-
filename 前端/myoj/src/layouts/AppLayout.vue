<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, provide, ref, watch } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { removeAdminRoutes } from '@/router/admin-dynamic'
import { Message } from '@arco-design/web-vue'
import { OpenAPI, Service } from '@generated'
import BrandLogo from '@/components/BrandLogo.vue'
import AppFooter from '@/components/AppFooter.vue'
import ModerationBanNotice from '@/components/moderation/ModerationBanNotice.vue'
import CheckInCalendarModal from '@/components/checkin/CheckInCalendarModal.vue'
import AuthModal from '@/components/auth/AuthModal.vue'
import { useAuthStore } from '@/stores/auth'
import { isResultSuccess } from '@/api/result'
import { getBackendErrorMessage } from '@/api/httpError'
import { mapApiError } from '@/api/mapApiError'
import { fetchNotificationSummary } from '@/api/inAppNotifications'
import { LAYOUT_THEME_KEY } from '@/config/theme-injection'
import { useBanCountdownTick } from '@/composables/useBanCountdownTick'
import { useSessionStatusPush } from '@/composables/useSessionStatusPush'
import { formatBanCountdown, parseBanUntilMs } from '@/utils/formatBanCountdown'

const route = useRoute()
const routerInstance = useRouter()
const auth = useAuthStore()
const adminOpen = ref(false)
const adminMenuRef = ref<HTMLElement | null>(null)
const userOpen = ref(false)
const userPinnedOpen = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)
let userCloseTimer: ReturnType<typeof setTimeout> | null = null
const authVisible = ref(false)

/** 站内消息 + 未读公告角标 */
const notifUnreadTotal = ref(0)

const checkInPoints = ref(0)
const checkInTotal = ref(0)
const checkInCheckedToday = ref(false)
const checkInLoading = ref(false)
const checkInModalOpen = ref(false)

const THEME_STORAGE_KEY = 'myoj:theme'
type ThemeMode = 'light' | 'dark'

function readStoredTheme(): ThemeMode {
  if (typeof localStorage === 'undefined') return 'light'
  const saved = localStorage.getItem(THEME_STORAGE_KEY)
  if (saved === 'dark' || saved === 'light') return saved
  return 'light'
}

const themeMode = ref<ThemeMode>(readStoredTheme())
if (typeof document !== 'undefined') {
  document.documentElement.setAttribute('data-theme', themeMode.value)
}
provide(LAYOUT_THEME_KEY, themeMode)
const isDarkTheme = computed(() => themeMode.value === 'dark')
const themeButtonTitle = computed(() => (isDarkTheme.value ? '切换到浅色模式' : '切换到深色模式'))
const themeButtonIcon = computed(() => (isDarkTheme.value ? '☀' : '☾'))
const userAvatarUrl = computed(() => auth.avatarUrl)
const showFooter = computed(() => route.meta.hideFooter !== true)

/** 顶栏角色展示：英文/空值统一成可读中文 */
const roleLabel = computed(() => {
  const r = auth.userRole?.trim()
  if (!r) return '普通用户'
  if (/^(user|USER)$/i.test(r)) return '普通用户'
  if (/^(admin|ADMIN|管理员)$/i.test(r)) return '管理员'
  return r
})

function applyTheme(mode: ThemeMode) {
  themeMode.value = mode
  document.documentElement.setAttribute('data-theme', mode)
  localStorage.setItem(THEME_STORAGE_KEY, mode)
}

function toggleTheme() {
  applyTheme(themeMode.value === 'dark' ? 'light' : 'dark')
}

onMounted(() => {
  const saved = localStorage.getItem(THEME_STORAGE_KEY)
  if (saved === 'dark' || saved === 'light') {
    applyTheme(saved)
    return
  }
  const preferDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
  applyTheme(preferDark ? 'dark' : 'light')
})

watch(
  () => route.query.openAuth,
  (v) => {
    if (v === '1') {
      authVisible.value = true
      const q = { ...route.query } as Record<string, string | string[] | undefined>
      delete q.openAuth
      routerInstance.replace({ path: route.path, query: q })
    }
  },
  { immediate: true },
)

function logout() {
  closeUserMenu()
  const onProtectedPage =
    route.matched.some((r) => r.meta.requiresAuth) || route.path.startsWith('/admin')
  removeAdminRoutes(routerInstance)
  auth.logout()
  Message.success('已退出登录')
  if (onProtectedPage) {
    void routerInstance.replace('/')
  }
}

type NavItem = {
  to: string
  label: string
  exact?: boolean
  accent?: boolean
}

const mainNav: NavItem[] = [
  { to: '/', label: '首页', exact: true },
  { to: '/problems', label: '题库' },
  { to: '/contests', label: '比赛' },
  { to: '/rankings', label: '排行榜' },
  { to: '/submissions', label: '提交记录' },
  { to: '/notifications', label: '消息' },
]

const adminMenus = [
  { to: '/admin/problems', title: '题目管理', desc: '编辑题目、标签与判题配置', icon: '🧩' },
  { to: '/admin/contests', title: '比赛管理', desc: '创建限时赛、绑定题目与办赛配置', icon: '🏁' },
  { to: '/admin/announcements', title: '发布公告', desc: '全站公告与未读提醒', icon: '📢' },
]

function isNavActive(path: string, exact?: boolean) {
  if (exact) return route.path === '/' || route.path === ''
  return route.path === path || route.path.startsWith(path + '/')
}

function isAdminNavActive() {
  return route.path.startsWith('/admin')
}

function toggleAdminMenu() {
  adminOpen.value = !adminOpen.value
}

function closeAdminMenu() {
  adminOpen.value = false
}

function openUserMenu() {
  if (userCloseTimer) {
    clearTimeout(userCloseTimer)
    userCloseTimer = null
  }
  userOpen.value = true
}

function closeUserMenu() {
  if (userCloseTimer) {
    clearTimeout(userCloseTimer)
    userCloseTimer = null
  }
  userPinnedOpen.value = false
  userOpen.value = false
}

function goAccountSettings() {
  closeUserMenu()
  void routerInstance.push('/account/settings')
}

async function getBearerHeaders(): Promise<Record<string, string>> {
  const tokenResolver = OpenAPI.TOKEN
  const token = typeof tokenResolver === 'function' ? await tokenResolver({} as never) : tokenResolver
  return token?.trim() ? { Authorization: `Bearer ${token.trim()}` } : {}
}

async function loadCheckInStatus() {
  if (!auth.isLoggedIn) return
  checkInLoading.value = true
  try {
    const headers = await getBearerHeaders()
    if (!headers.Authorization) return
    const data = await Service.checkInStatus()
    if (!isResultSuccess(data?.code) || !data?.data) return
    const d = data.data
    checkInPoints.value = typeof d.points === 'number' ? d.points : 0
    checkInTotal.value = typeof d.checkInCount === 'number' ? d.checkInCount : 0
    checkInCheckedToday.value = !!d.checkedToday
  } catch {
    /* 静默失败：下拉仍可正常使用（如后端未跑迁移） */
  } finally {
    checkInLoading.value = false
  }
}

function openCheckInModal() {
  if (auth.isAccountDisabled) {
    Message.warning('账号已被禁用，暂不可签到')
    return
  }
  userOpen.value = false
  checkInModalOpen.value = true
}

function onCheckInStatusFromModal(payload: {
  points: number
  checkInCount: number
  checkedToday: boolean
}) {
  checkInPoints.value = payload.points
  checkInTotal.value = payload.checkInCount
  checkInCheckedToday.value = payload.checkedToday
}

function scheduleCloseUserMenu() {
  if (userPinnedOpen.value) return
  if (userCloseTimer) {
    clearTimeout(userCloseTimer)
  }
  userCloseTimer = setTimeout(() => {
    userOpen.value = false
    userCloseTimer = null
  }, 220)
}

function toggleUserMenuPin() {
  if (userPinnedOpen.value) {
    closeUserMenu()
    return
  }
  openUserMenu()
  userPinnedOpen.value = true
}

function onGlobalPointerDown(e: Event) {
  const target = e.target as Node | null
  if (adminOpen.value && adminMenuRef.value && target && !adminMenuRef.value.contains(target)) {
    adminOpen.value = false
  }
  if (userOpen.value && userMenuRef.value && target && !userMenuRef.value.contains(target)) {
    userPinnedOpen.value = false
    userOpen.value = false
  }
}

function onGlobalKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    adminOpen.value = false
    closeUserMenu()
  }
}

watch(
  () => route.fullPath,
  () => {
    adminOpen.value = false
    closeUserMenu()
  },
)

watch(
  () => userOpen.value,
  (open) => {
    if (open && auth.isLoggedIn) {
      void loadCheckInStatus()
    }
  },
)

let notifPollTimer: number | null = null

const { nowMs: banCountdownNow } = useBanCountdownTick()
useSessionStatusPush()

const adminBanCountdown = computed(() =>
  formatBanCountdown(parseBanUntilMs(auth.banUntil), banCountdownNow.value),
)

const commentBanCountdown = computed(() =>
  formatBanCountdown(parseBanUntilMs(auth.commentBanUntil ?? auth.banUntil), banCountdownNow.value),
)

const aiAssistBanCountdown = computed(() =>
  formatBanCountdown(parseBanUntilMs(auth.aiBanUntil ?? auth.banUntil), banCountdownNow.value),
)

const accountBanNotice = computed(() => {
  if (!auth.isAccountDisabled) return null
  const timed = auth.banType === 'admin' && !!auth.banUntil?.trim()
  return {
    title: timed ? '账号限时限制中' : '账号已被管理员禁用',
    description: timed
      ? '当前无法使用做题、博客、签到等互动功能，可浏览公开内容与基本资料。'
      : '可浏览公开内容与基本资料，但无法使用做题、写博客、签到等互动功能。',
    countdown: timed ? adminBanCountdown.value || '计算中…' : '',
    reason: (auth.banReason ?? '').trim(),
    footer: timed ? '到期后将自动恢复，请遵守平台规范。' : '如需解禁请联系管理员。',
  }
})

const commentBanNotice = computed(() => {
  if (!auth.isCommentBanned || auth.isAccountDisabled) return null
  return {
    title: '评论功能已限制',
    description: '检测到不当评论内容，系统已暂停您的评论权限。',
    countdown: commentBanCountdown.value || '计算中…',
    reason: (auth.commentBanReason ?? auth.banReason ?? '').trim(),
    footer: '做题、签到等其他功能不受影响，到期后自动恢复。',
  }
})

const aiBanNotice = computed(() => {
  if (!auth.isAiAssistBanned || auth.isAccountDisabled) return null
  return {
    title: 'AI 学习助手已限制',
    description: '向智能助手发送了不当内容，学习助手入口已临时锁定。',
    countdown: aiAssistBanCountdown.value || '计算中…',
    reason: (auth.aiBanReason ?? auth.banReason ?? '').trim(),
    footer: '做题、评论等其他功能不受影响，到期后自动恢复。',
  }
})

async function refreshNotifBadge() {
  if (!auth.isLoggedIn) {
    notifUnreadTotal.value = 0
    return
  }
  try {
    const s = await fetchNotificationSummary()
    if (!s) return
    notifUnreadTotal.value = Math.min(999, (s.inAppUnread || 0) + (s.announcementUnread || 0))
  } catch {
    /* 静默 */
  }
}

function onNotifHubEvent() {
  void refreshNotifBadge()
}

function onSessionExpired() {
  Message.warning('登录已过期，请重新登录')
  authVisible.value = true
}

function onAccountRevoked() {
  Message.warning('账号已被管理员禁用，当前会话已结束，请重新登录')
  authVisible.value = true
}

function onLoginElsewhere() {
  Message.warning('您的账号已在其他设备登录，当前设备已退出，请重新登录')
  authVisible.value = true
}

function onAccountDisabled() {
  Message.warning('账号已被禁用，做题、博客等功能暂不可用')
}

function onAccountEnabled() {
  /* 详细文案由 useSessionStatusPush 提示 */
}

let sessionPollTimer: number | null = null

async function pollSessionStatus() {
  if (!auth.isLoggedIn) return
  await auth.syncSessionStatus()
}

watch(
  () => auth.isAccountDisabled,
  (disabled) => {
    if (!disabled) return
    if (route.matched.some((r) => r.meta.requiresActiveAccount)) {
      Message.warning('账号已被禁用，已退出当前功能页')
      routerInstance.replace('/')
    }
  },
)

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    void refreshNotifBadge()
    if (loggedIn) {
      void pollSessionStatus()
    }
  },
)

onMounted(() => {
  document.addEventListener('pointerdown', onGlobalPointerDown)
  document.addEventListener('keydown', onGlobalKeydown)
  window.addEventListener('myoj:notifications-changed', onNotifHubEvent)
  window.addEventListener('myoj:session-expired', onSessionExpired)
  window.addEventListener('myoj:account-revoked', onAccountRevoked)
  window.addEventListener('myoj:login-elsewhere', onLoginElsewhere)
  window.addEventListener('myoj:account-disabled', onAccountDisabled)
  window.addEventListener('myoj:account-enabled', onAccountEnabled)
  void refreshNotifBadge()
  void pollSessionStatus()
  notifPollTimer = window.setInterval(() => void refreshNotifBadge(), 60_000)
  sessionPollTimer = window.setInterval(() => void pollSessionStatus(), 90_000)
})

onBeforeUnmount(() => {
  if (userCloseTimer) {
    clearTimeout(userCloseTimer)
    userCloseTimer = null
  }
  if (notifPollTimer != null) {
    clearInterval(notifPollTimer)
    notifPollTimer = null
  }
  if (sessionPollTimer != null) {
    clearInterval(sessionPollTimer)
    sessionPollTimer = null
  }
  window.removeEventListener('myoj:notifications-changed', onNotifHubEvent)
  window.removeEventListener('myoj:session-expired', onSessionExpired)
  window.removeEventListener('myoj:account-revoked', onAccountRevoked)
  window.removeEventListener('myoj:login-elsewhere', onLoginElsewhere)
  window.removeEventListener('myoj:account-disabled', onAccountDisabled)
  window.removeEventListener('myoj:account-enabled', onAccountEnabled)
  document.removeEventListener('pointerdown', onGlobalPointerDown)
  document.removeEventListener('keydown', onGlobalKeydown)
})
</script>

<template>
  <div class="layout">
    <ModerationBanNotice
      v-if="auth.isLoggedIn && accountBanNotice"
      variant="account"
      :title="accountBanNotice.title"
      :description="accountBanNotice.description"
      :countdown="accountBanNotice.countdown"
      :reason="accountBanNotice.reason"
      :footer="accountBanNotice.footer"
    />
    <ModerationBanNotice
      v-if="auth.isLoggedIn && commentBanNotice"
      variant="comment"
      :title="commentBanNotice.title"
      :description="commentBanNotice.description"
      :countdown="commentBanNotice.countdown"
      :reason="commentBanNotice.reason"
      :footer="commentBanNotice.footer"
    />
    <ModerationBanNotice
      v-if="auth.isLoggedIn && aiBanNotice"
      variant="ai"
      :title="aiBanNotice.title"
      :description="aiBanNotice.description"
      :countdown="aiBanNotice.countdown"
      :reason="aiBanNotice.reason"
      :footer="aiBanNotice.footer"
    />
    <header class="header">
      <div class="header-inner">
        <RouterLink to="/" class="brand">
          <BrandLogo :size="32" />
          <span class="brand-text">码跃OJ</span>
        </RouterLink>

        <nav class="nav">
          <template v-for="item in mainNav" :key="item.to">
            <RouterLink
              :to="item.to"
              class="nav-link nav-item-wrap"
              :class="{
                active: isNavActive(item.to, item.exact),
                accent: item.accent,
              }"
            >
              {{ item.label }}
            </RouterLink>
          </template>

          <div v-if="auth.isAdmin" ref="adminMenuRef" class="nav-item-wrap admin-wrap">
            <button
              type="button"
              class="nav-link admin-nav"
              :class="{ active: isAdminNavActive() || adminOpen }"
              :aria-expanded="adminOpen"
              aria-haspopup="menu"
              @click="toggleAdminMenu"
            >
              管理
              <span class="caret" aria-hidden="true">▾</span>
            </button>
            <div v-show="adminOpen" class="dropdown admin-dropdown" role="menu">
              <RouterLink
                v-for="item in adminMenus"
                :key="item.to"
                :to="item.to"
                class="dropdown-item admin-item"
                :class="{ active: route.path === item.to || route.path.startsWith(item.to + '/') }"
                @click="closeAdminMenu"
              >
                <span class="admin-item-icon" aria-hidden="true">{{ item.icon }}</span>
                <span class="admin-item-text">
                  <span class="admin-item-title">{{ item.title }}</span>
                  <span class="admin-item-desc">{{ item.desc }}</span>
                </span>
              </RouterLink>
            </div>
          </div>
        </nav>

        <div class="actions">
          <RouterLink to="/publish" class="btn-publish">
            <span class="icon-plane" aria-hidden="true">✈</span>
            博客
          </RouterLink>
          <button type="button" class="icon-btn" aria-label="切换主题" :title="themeButtonTitle" @click="toggleTheme">
            {{ themeButtonIcon }}
          </button>
          <RouterLink to="/notifications" class="icon-btn notif-bell" aria-label="通知">
            🔔
            <span v-if="notifUnreadTotal > 0" class="notif-badge" aria-hidden="true">{{
              notifUnreadTotal > 99 ? '99+' : notifUnreadTotal
            }}</span>
          </RouterLink>
          <template v-if="!auth.isLoggedIn">
            <button type="button" class="btn-auth" @click="authVisible = true">登录/注册</button>
          </template>
          <template v-else>
            <div
              ref="userMenuRef"
              class="user-menu"
              @mouseenter="openUserMenu"
              @mouseleave="scheduleCloseUserMenu"
            >
              <button
                type="button"
                class="user-avatar-btn"
                :class="{ 'user-avatar-btn--open': userOpen }"
                :aria-expanded="userOpen"
                aria-haspopup="menu"
                :title="auth.displayName"
                @click="toggleUserMenuPin"
              >
                <span class="user-avatar-ring" aria-hidden="true">
                  <span class="user-avatar-inner">
                    <img v-if="userAvatarUrl" :src="userAvatarUrl" alt="" class="user-avatar-img" />
                    <span v-else class="user-avatar-letter">{{ (auth.displayName || 'U').slice(0, 1).toUpperCase() }}</span>
                  </span>
                </span>
              </button>
              <Transition name="user-pop">
                <div
                  v-show="userOpen"
                  class="user-popover"
                  role="menu"
                  @mouseenter="openUserMenu"
                  @mouseleave="scheduleCloseUserMenu"
                >
                  <div class="user-popover-head">
                    <div class="user-popover-head-bg" aria-hidden="true" />
                    <div class="user-popover-head-inner">
                      <div class="user-popover-avatar" aria-hidden="true">
                        <img v-if="userAvatarUrl" :src="userAvatarUrl" alt="" class="user-popover-avatar-img" />
                        <span v-else>{{ (auth.displayName || 'U').slice(0, 1).toUpperCase() }}</span>
                      </div>
                      <div class="user-popover-meta">
                        <p class="user-popover-name">{{ auth.displayName }}</p>
                        <p class="user-popover-id">{{ roleLabel }}</p>
                        <p class="user-popover-stats" aria-live="polite">
                          <span class="user-popover-stat">积分 {{ checkInLoading ? '…' : checkInPoints }}</span>
                          <span class="user-popover-stat-sep">·</span>
                          <span class="user-popover-stat">累计签到 {{ checkInLoading ? '…' : checkInTotal }} 天</span>
                        </p>
                      </div>
                    </div>
                  </div>
                  <div class="user-popover-actions">
                    <button
                      type="button"
                      class="user-popover-btn user-popover-btn--checkin"
                      :disabled="checkInLoading || auth.isAccountDisabled"
                      @click="openCheckInModal"
                    >
                      <span class="user-popover-btn-ico" aria-hidden="true">📅</span>
                      {{ checkInCheckedToday ? '学习打卡 · 今日已签' : '学习打卡' }}
                    </button>
                    <button
                      type="button"
                      class="user-popover-btn user-popover-btn--primary"
                      @click="goAccountSettings"
                    >
                      <span class="user-popover-btn-ico" aria-hidden="true">⚙</span>
                      账户设置
                    </button>
                    <button type="button" class="user-popover-btn user-popover-btn--ghost" @click="logout">
                      <span class="user-popover-btn-ico" aria-hidden="true">↪</span>
                      退出登录
                    </button>
                  </div>
                </div>
              </Transition>
            </div>
          </template>
        </div>
      </div>
    </header>

    <AuthModal v-model:visible="authVisible" />
    <CheckInCalendarModal
      v-model:visible="checkInModalOpen"
      :account-disabled="auth.isAccountDisabled"
      @status-change="onCheckInStatusFromModal"
    />

    <main class="main">
      <RouterView />
    </main>

    <AppFooter v-if="showFooter" />
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--app-bg);
  color: var(--app-text);
  transition:
    background-color 0.2s ease,
    color 0.2s ease;
}

.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: var(--app-surface);
  border-bottom: 1px solid var(--app-border);
  box-shadow: var(--app-header-shadow);
  transition:
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease;
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 20px;
  height: 56px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: inherit;
  flex-shrink: 0;
}

.brand-text {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.nav-item-wrap {
  position: relative;
}

.nav-link {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  font-size: 14px;
  color: var(--app-text-muted);
  text-decoration: none;
  border-radius: 6px;
  transition: color 0.15s, background 0.15s;
}

.nav-link:hover {
  color: #0d9488;
  background: rgba(13, 148, 136, 0.06);
}

.nav-link.active {
  color: #0d9488;
  font-weight: 500;
}

.nav-link.accent {
  color: #ea580c;
}

.caret {
  font-size: 10px;
  opacity: 0.7;
}

.dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  min-width: 120px;
  padding: 6px;
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: 8px;
  box-shadow: var(--app-dropdown-shadow);
}

.admin-dropdown {
  left: auto;
  right: 0;
  min-width: 260px;
  padding: 8px;
  border-radius: 12px;
}

.dropdown-item {
  display: block;
  padding: 8px 12px;
  font-size: 14px;
  color: var(--app-text-muted);
  text-decoration: none;
  border-radius: 6px;
}

.admin-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
}

.admin-item-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: rgba(13, 148, 136, 0.12);
  font-size: 14px;
  flex-shrink: 0;
}

.admin-item-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-item-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--app-text);
}

.admin-item-desc {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--app-text-subtle);
}

.admin-item.active {
  background: rgba(13, 148, 136, 0.12);
}

.dropdown-item:hover {
  background: var(--app-hover);
  color: #0d9488;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.btn-publish {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  font-size: 14px;
  color: #fff;
  background: #0d9488;
  border-radius: 6px;
  text-decoration: none;
  transition: filter 0.15s;
}

.btn-publish:hover {
  filter: brightness(1.05);
}

.icon-plane {
  font-size: 12px;
  transform: rotate(-45deg);
  display: inline-block;
}

.icon-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  transition: background-color 0.15s ease;
}

.icon-btn:hover:not(:disabled) {
  background: var(--app-hover);
}

.icon-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.notif-bell {
  position: relative;
}

.notif-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 800;
  line-height: 16px;
  text-align: center;
  color: #fff;
  background: #ef4444;
  box-shadow: 0 0 0 2px var(--color-bg-1);
}

.btn-auth {
  padding: 6px 14px;
  font-size: 14px;
  color: #fff;
  background: #0d9488;
  border-radius: 6px;
  text-decoration: none;
  margin-left: 4px;
}

.btn-auth:hover {
  filter: brightness(1.05);
}

.user-menu {
  position: relative;
}

.user-menu::after {
  content: '';
  position: absolute;
  top: 100%;
  right: 0;
  width: 100%;
  height: 12px;
}

/* 右上角头像：青色渐变环 + 外发光（与深色顶栏/账户页视觉一致） */
.user-avatar-btn {
  position: relative;
  padding: 0;
  border: none;
  background: transparent;
  border-radius: 999px;
  cursor: pointer;
  line-height: 0;
  filter: drop-shadow(0 0 6px rgba(0, 194, 160, 0.45)) drop-shadow(0 0 16px rgba(0, 194, 160, 0.28));
  transition:
    transform 0.2s cubic-bezier(0.34, 1.2, 0.64, 1),
    filter 0.22s ease;
}

.user-avatar-ring {
  display: block;
  width: 40px;
  height: 40px;
  border-radius: 999px;
  padding: 2px;
  background: linear-gradient(145deg, #5eead4 0%, #00c2a0 42%, #00a896 100%);
  box-shadow:
    0 0 0 1px rgba(0, 194, 160, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.22);
}

.user-avatar-inner {
  width: 100%;
  height: 100%;
  border-radius: 999px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(155deg, #0f3d3a 0%, #0f766e 48%, #14b8a6 100%);
  color: #fff;
}

.user-avatar-letter {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: -0.02em;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}

.user-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.user-avatar-btn:hover {
  transform: translateY(-1px) scale(1.04);
  filter: drop-shadow(0 0 10px rgba(0, 194, 160, 0.55)) drop-shadow(0 0 24px rgba(0, 194, 160, 0.4));
}

.user-avatar-btn:focus-visible {
  outline: 2px solid #14b8a6;
  outline-offset: 3px;
}

.user-avatar-btn--open .user-avatar-ring {
  background: linear-gradient(145deg, #99f6e4 0%, #5eead4 35%, #00c2a0 100%);
  box-shadow:
    0 0 0 2px rgba(0, 194, 160, 0.35),
    0 0 20px rgba(0, 194, 160, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.28);
}

.user-avatar-btn--open {
  filter: drop-shadow(0 0 8px rgba(0, 194, 160, 0.55)) drop-shadow(0 0 28px rgba(0, 194, 160, 0.42));
}

.user-pop-enter-active,
.user-pop-leave-active {
  transition:
    opacity 0.2s cubic-bezier(0.34, 1.2, 0.64, 1),
    transform 0.22s cubic-bezier(0.34, 1.2, 0.64, 1);
}

.user-pop-enter-from,
.user-pop-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}

.user-popover {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 268px;
  border: 1px solid rgba(13, 148, 136, 0.14);
  border-radius: 14px;
  background: var(--app-surface);
  box-shadow:
    0 20px 40px rgba(15, 23, 42, 0.12),
    0 8px 16px rgba(15, 23, 42, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.06) inset;
  overflow: hidden;
  z-index: 120;
}

.user-popover-head {
  position: relative;
  padding: 0;
  border-bottom: 1px solid var(--app-border);
}

.user-popover-head-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(20, 184, 166, 0.12) 0%, rgba(15, 118, 110, 0.06) 50%, transparent 100%);
  pointer-events: none;
}

.user-popover-head-inner {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 16px 14px;
}

.user-popover-avatar {
  width: 48px;
  height: 48px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(145deg, #0f766e 0%, #14b8a6 60%, #2dd4bf 120%);
  box-shadow:
    0 4px 12px rgba(13, 148, 136, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  flex-shrink: 0;
  overflow: hidden;
}

.user-popover-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.user-popover-meta {
  min-width: 0;
}

.user-popover-name {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--app-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-popover-id {
  margin: 5px 0 0;
  font-size: 12px;
  color: var(--app-text-subtle);
  line-height: 1.35;
}

.user-popover-stats {
  margin: 8px 0 0;
  font-size: 11px;
  color: var(--app-text-muted);
  line-height: 1.45;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.user-popover-stat-sep {
  opacity: 0.55;
}

.user-popover-actions {
  padding: 12px 14px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.5) 0%, transparent 100%);
}

.user-popover-btn {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease,
    color 0.2s ease;
}

.user-popover-btn:focus-visible {
  outline: 2px solid #14b8a6;
  outline-offset: 2px;
}

.user-popover-btn-ico {
  font-size: 14px;
  opacity: 0.9;
}

.user-popover-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.user-popover-btn--checkin {
  border: 1px solid rgba(52, 211, 153, 0.4);
  background: linear-gradient(180deg, rgba(236, 253, 245, 0.98) 0%, rgba(209, 250, 229, 0.7) 100%);
  color: #047857;
  box-shadow: 0 2px 10px rgba(16, 185, 129, 0.14);
}

.user-popover-btn--checkin:hover:not(:disabled) {
  border-color: #34d399;
  color: #065f46;
  background: linear-gradient(180deg, #ecfdf5 0%, #a7f3d0 100%);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.24);
  transform: translateY(-1px);
}

.user-popover-btn--checkin:active:not(:disabled) {
  transform: translateY(0);
}

.user-popover-btn--primary {
  border: 1px solid rgba(13, 148, 136, 0.45);
  background: linear-gradient(180deg, rgba(236, 253, 245, 0.95) 0%, rgba(240, 253, 250, 0.75) 100%);
  color: #0f766e;
  box-shadow: 0 2px 8px rgba(13, 148, 136, 0.12);
}

.user-popover-btn--primary:hover {
  border-color: #14b8a6;
  color: #0d9488;
  background: linear-gradient(180deg, #ecfdf5 0%, #d1fae5 100%);
  box-shadow: 0 4px 14px rgba(13, 148, 136, 0.2);
  transform: translateY(-1px);
}

.user-popover-btn--primary:active {
  transform: translateY(0);
}

.user-popover-btn--ghost {
  border: 1px solid var(--app-border);
  background: var(--app-surface);
  color: var(--app-text-muted);
}

.user-popover-btn--ghost:hover {
  border-color: #cbd5e1;
  color: var(--app-text);
  background: var(--app-hover);
}

.user-popover-btn--ghost:active {
  transform: scale(0.99);
}

.admin-nav {
  border: none;
  cursor: pointer;
  background: transparent;
}

.admin-nav .caret {
  transition: transform 0.16s ease;
}

.admin-nav[aria-expanded='true'] .caret {
  transform: rotate(180deg);
}

.main {
  flex: 1;
}

:global(html[data-theme='dark']) .nav-link:hover {
  color: #2dd4bf;
  background: rgba(45, 212, 191, 0.12);
}
:global(html[data-theme='dark']) .dropdown-item:hover {
  background: #334155;
  color: #2dd4bf;
}

:global(html[data-theme='dark']) .user-popover {
  border-color: rgba(51, 65, 85, 0.9);
  box-shadow:
    0 24px 48px rgba(0, 0, 0, 0.45),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset;
}

:global(html[data-theme='dark']) .user-popover-actions {
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.6) 0%, transparent 100%);
}

:global(html[data-theme='dark']) .user-popover-btn--primary {
  border-color: rgba(45, 212, 191, 0.35);
  background: linear-gradient(180deg, rgba(15, 118, 110, 0.35) 0%, rgba(15, 118, 110, 0.15) 100%);
  color: #5eead4;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.25);
}

:global(html[data-theme='dark']) .user-popover-btn--primary:hover {
  border-color: #2dd4bf;
  color: #99f6e4;
  background: linear-gradient(180deg, rgba(15, 118, 110, 0.5) 0%, rgba(15, 118, 110, 0.25) 100%);
}

:global(html[data-theme='dark']) .user-popover-btn--checkin {
  border-color: rgba(251, 191, 36, 0.4);
  background: linear-gradient(180deg, rgba(120, 53, 15, 0.45) 0%, rgba(69, 26, 3, 0.35) 100%);
  color: #fde68a;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.3);
}

:global(html[data-theme='dark']) .user-popover-btn--checkin:hover:not(:disabled) {
  border-color: #fbbf24;
  color: #fffbeb;
  background: linear-gradient(180deg, rgba(146, 64, 14, 0.55) 0%, rgba(120, 53, 15, 0.4) 100%);
}
</style>
