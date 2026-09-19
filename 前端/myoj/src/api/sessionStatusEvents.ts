import { OpenAPI } from '@generated'

export type SessionStatusEventHandlers = {
  /** 连接成功或收到 status 推送（revision 为毫秒时间戳） */
  onRevision: (revision: number) => void
  /** 连接断开，可重连 */
  onDisconnect?: () => void
}

/**
 * 订阅账号状态 SSE（管理员禁用/启用、评论限制等变更后服务端主动推送）。
 * EventSource 无法带 Authorization，使用 query access_token。
 * @returns 关闭连接的函数
 */
export function connectSessionStatusEvents(
  accessToken: string,
  handlers: SessionStatusEventHandlers,
): () => void {
  const token = accessToken.trim()
  if (!token) return () => {}

  const base = OpenAPI.BASE.replace(/\/+$/, '')
  const url = `${base}/user/session/events?access_token=${encodeURIComponent(token)}`
  const es = new EventSource(url, { withCredentials: OpenAPI.WITH_CREDENTIALS })

  const handleRevision = (raw: string) => {
    const rev = Number(String(raw).trim())
    if (!Number.isNaN(rev) && rev > 0) {
      handlers.onRevision(rev)
    }
  }

  es.addEventListener('connected', (ev) => {
    handleRevision((ev as MessageEvent).data)
  })
  es.addEventListener('status', (ev) => {
    handleRevision((ev as MessageEvent).data)
  })

  es.onerror = () => {
    es.close()
    handlers.onDisconnect?.()
  }

  return () => {
    es.close()
  }
}
