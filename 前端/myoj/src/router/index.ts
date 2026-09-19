import { createRouter, createWebHistory } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/layouts/AppLayout.vue'
import { registerAdminRoutes, isAdminRoutesMounted } from '@/router/admin-dynamic'

const SITE_TITLE = '码跃OJ'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth', top: 80 }
    }
    return { left: 0, top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'layout',
      component: AppLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/home/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'problems',
          name: 'problems',
          component: () => import('@/views/problem/ProblemBankView.vue'),
          meta: { title: '题库' },
        },
        {
          path: 'problems/:id/solve',
          name: 'problem-solve',
          component: () => import('@/views/problem/ProblemSolveView.vue'),
          meta: { title: '做题', hideFooter: true, requiresActiveAccount: true },
        },
        {
          path: 'contests',
          name: 'contests',
          component: () => import('@/views/contest/ContestRankListView.vue'),
          meta: { title: '比赛' },
        },
        {
          path: 'rankings/:contestId',
          name: 'contest-rank-detail',
          component: () => import('@/views/contest/ContestRankDetailView.vue'),
          meta: { title: '竞赛排行榜' },
        },
        {
          path: 'rankings',
          name: 'rankings',
          component: () => import('@/views/ranking/UserLeaderboardView.vue'),
          meta: { title: '排行榜' },
        },
        {
          path: 'submissions',
          name: 'submissions',
          component: () => import('@/views/submission/SubmissionsListView.vue'),
          meta: { title: '提交记录', requiresAuth: true, requiresActiveAccount: true },
        },
        {
          path: 'blog',
          name: 'blog',
          component: () => import('@/views/blog/BlogHomeView.vue'),
          meta: { title: '博客' },
        },
        {
          path: 'blog/my',
          name: 'blog-my',
          component: () => import('@/views/blog/BlogMyView.vue'),
          meta: { title: '我的博客', requiresAuth: true, requiresActiveAccount: true },
        },
        {
          path: 'blog/write',
          name: 'blog-write',
          component: () => import('@/views/blog/BlogComposeView.vue'),
          meta: { title: '写文章', requiresAuth: true, requiresActiveAccount: true },
        },
        {
          path: 'blog/edit/:id',
          name: 'blog-edit',
          component: () => import('@/views/blog/BlogComposeView.vue'),
          meta: { title: '编辑文章', requiresAuth: true, requiresActiveAccount: true },
        },
        {
          path: 'blog/post/:id',
          name: 'blog-detail',
          component: () => import('@/views/blog/BlogDetailView.vue'),
          meta: { title: '文章' },
        },
        {
          path: 'publish',
          redirect: '/blog',
        },
        {
          path: 'discuss',
          redirect: '/blog',
        },
        {
          path: 'notifications',
          name: 'notifications',
          component: () => import('@/views/notifications/NotificationsView.vue'),
          meta: { title: '通知', requiresActiveAccount: true },
        },
        {
          path: 'account/settings',
          name: 'account-settings',
          component: () => import('@/views/account/AccountSettingsView.vue'),
          meta: { title: '账户设置', requiresAuth: true },
        },
        {
          path: 'legal/terms',
          name: 'legal-terms',
          component: () => import('@/views/legal/TermsView.vue'),
          meta: { title: '服务协议' },
        },
        {
          path: 'legal/privacy',
          name: 'legal-privacy',
          component: () => import('@/views/legal/PrivacyView.vue'),
          meta: { title: '隐私政策' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      redirect: '/',
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/', query: { ...to.query, openAuth: '1' }, replace: true }
  }

  if (to.meta.requiresActiveAccount && auth.isLoggedIn && auth.isAccountDisabled) {
    Message.warning('账号已被禁用，暂不可使用此功能。请联系管理员解禁。')
    return { path: '/', replace: true }
  }

  if (auth.isAdmin && !isAdminRoutesMounted()) {
    registerAdminRoutes(router)
    if (to.path.startsWith('/admin')) {
      return { path: to.fullPath, query: to.query, hash: to.hash, replace: true }
    }
  }

  if (to.meta.requiresAdmin) {
    if (!auth.isLoggedIn) {
      return { path: '/', query: { ...to.query, openAuth: '1' } }
    }
    if (!auth.isAdmin) {
      return { path: '/' }
    }
  }
})

router.afterEach((to) => {
  const raw = to.meta.title
  const page =
    typeof raw === 'string' && raw.trim()
      ? raw.trim()
      : to.name === 'home'
        ? '首页'
        : ''
  document.title = page ? `${page} · ${SITE_TITLE}` : SITE_TITLE
})

export { registerAdminRoutes, removeAdminRoutes } from './admin-dynamic'
export default router
