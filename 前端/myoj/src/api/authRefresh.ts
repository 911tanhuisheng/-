import { Service } from '@generated'
import type { TokenRefreshVO } from '@generated'
import { isResultSuccess } from '@/api/result'

export type { TokenRefreshVO }

let refreshInFlight: Promise<TokenRefreshVO | null> | null = null

/**
 * 调用 POST /user/refresh；并发 401 时共用一个 Promise，避免重复换票。
 */
export function refreshTokensWithApi(refreshToken: string): Promise<TokenRefreshVO | null> {
  if (!refreshToken.trim()) return Promise.resolve(null)
  if (refreshInFlight) return refreshInFlight

  refreshInFlight = Service.refresh({ refreshToken: refreshToken.trim() })
    .then((data) => {
      if (!isResultSuccess(data?.code) || !data?.data) return null
      const d = data.data
      const access = (d.accessToken ?? d.token ?? '').trim()
      const refresh = (d.refreshToken ?? '').trim()
      if (!access || !refresh) return null
      return { accessToken: access, refreshToken: refresh, token: access }
    })
    .catch(() => null)
    .finally(() => {
      refreshInFlight = null
    })

  return refreshInFlight
}
