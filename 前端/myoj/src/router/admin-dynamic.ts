import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { adminRouteRecords, adminRouteNames } from '@/router/admin-routes'

let adminRoutesMounted = false

export function isAdminRoutesMounted() {
  return adminRoutesMounted
}

/** 管理员：将 /admin/* 子路由挂到 name 为 layout 的父路由下（幂等） */
export function registerAdminRoutes(r: Router) {
  const auth = useAuthStore()
  if (!auth.isAdmin) return
  if (adminRoutesMounted) return
  for (const record of adminRouteRecords) {
    r.addRoute('layout', record)
  }
  adminRoutesMounted = true
}

/** 退出时移除动态路由；若当前在 /admin 下则回首页 */
export function removeAdminRoutes(r: Router) {
  if (!adminRoutesMounted) return
  const path = r.currentRoute.value.path
  for (const name of adminRouteNames) {
    if (r.hasRoute(name)) {
      r.removeRoute(name)
    }
  }
  adminRoutesMounted = false
  if (path.startsWith('/admin')) {
    void r.replace('/')
  }
}
