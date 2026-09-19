import axios from 'axios'
import { ApiError } from '@generated'
import { getBackendErrorMessage } from '@/api/httpError'

/** 统一解析 openapi-codegen / Axios 错误文案（与登录弹层逻辑一致） */
export function mapApiError(e: unknown, fallback: string): string {
  if (e instanceof ApiError) {
    const body = e.body
    if (body && typeof body === 'object' && 'message' in body) {
      const m = (body as { message?: unknown }).message
      if (m != null && String(m).trim()) return String(m)
    }
    if (typeof body === 'string' && body.trim()) {
      try {
        const p = JSON.parse(body) as { message?: string }
        if (p.message) return p.message
      } catch {
        return body
      }
    }
    return e.message || fallback
  }
  if (axios.isAxiosError(e)) {
    return getBackendErrorMessage(e, fallback)
  }
  if (e instanceof Error && e.message.trim()) return e.message
  return fallback
}
