import type { RouteRecordRaw } from 'vue-router'

/** 仅管理员：运行时通过 addRoute('layout', …) 挂载，并配合懒加载分包 */
export const adminRouteRecords: RouteRecordRaw[] = [
  {
    path: 'admin/problems/new',
    name: 'admin-problem-new',
    component: () => import('@/views/admin/ProblemEditorView.vue'),
    meta: { title: '新增题目', requiresAdmin: true },
  },
  {
    path: 'admin/problems/:id/edit',
    name: 'admin-problem-edit',
    component: () => import('@/views/admin/ProblemEditorView.vue'),
    meta: { title: '编辑题目', requiresAdmin: true },
  },
  {
    path: 'admin/problems',
    name: 'admin-problems',
    component: () => import('@/views/admin/ProblemManageView.vue'),
    meta: { title: '题目管理', requiresAdmin: true },
  },
  {
    path: 'admin/contests',
    name: 'admin-contests',
    component: () => import('@/views/admin/ContestManageView.vue'),
    meta: { title: '赛事管理', requiresAdmin: true },
  },
  {
    path: 'admin/announcements',
    name: 'admin-announcements',
    component: () => import('@/views/admin/AnnouncementPublishView.vue'),
    meta: { title: '发布公告', requiresAdmin: true },
  },
]

export const adminRouteNames = adminRouteRecords.map((r) => r.name as string)
