import { Service } from '@generated'
import type {
  InAppNotificationItemVO,
  NotificationSummaryVO,
  SiteAnnouncementItemVO,
} from '@generated'
import { isResultSuccess } from '@/api/result'

export type NotificationSummary = NotificationSummaryVO
export type InAppNotificationItem = InAppNotificationItemVO
export type SiteAnnouncementItem = SiteAnnouncementItemVO

type ApiResult<T> = { code?: number; message?: string; data?: T }

export async function fetchNotificationSummary(): Promise<NotificationSummary | null> {
  const res = await Service.summary()
  if (!isResultSuccess(res.code) || !res.data) return null
  return res.data
}

export async function fetchNotificationPage(current: number, pageSize: number) {
  return Service.page2(current, pageSize) as Promise<
    ApiResult<{
      records?: InAppNotificationItem[]
      total?: number
      current?: number
      size?: number
    }>
  >
}

export async function markNotificationsRead(ids: string[]) {
  return Service.markRead({ ids }) as Promise<ApiResult<unknown>>
}

export async function markAllNotificationsRead() {
  return Service.markAllRead() as Promise<ApiResult<unknown>>
}

export async function fetchRecentAnnouncements(limit = 10) {
  return Service.recent(limit) as Promise<ApiResult<SiteAnnouncementItem[]>>
}

export async function readAnnouncementsUpTo(id: string) {
  return Service.readAnnouncementUpTo({ id }) as Promise<ApiResult<unknown>>
}

export async function publishAnnouncement(title: string, content: string) {
  return Service.adminPublish({ title, content }) as Promise<ApiResult<string | number>>
}
