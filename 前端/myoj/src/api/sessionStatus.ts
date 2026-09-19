import { Service } from '@generated'
import type { UserSessionStatusVO } from '@generated'
import { isResultSuccess } from '@/api/result'
import { useAuthStore } from '@/stores/auth'

export type SessionStatusPayload = UserSessionStatusVO & {
  userId?: string | number
  statusRevision?: number | string
}

/** 拉取当前登录用户的账号状态（0 禁用 / 1 正常） */
export async function fetchSessionStatus(): Promise<SessionStatusPayload | null> {
  const auth = useAuthStore()
  const token = auth.accessToken ?? auth.token
  if (!token?.trim()) return null
  try {
    const res = await Service.sessionStatus()
    if (!isResultSuccess(res.code) || !res.data) return null
    return res.data as SessionStatusPayload
  } catch {
    return null
  }
}
