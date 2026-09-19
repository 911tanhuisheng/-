import { API_BASE_URL } from '@/config/api'

/**
 * 将后端返回的资源地址转为可展示的绝对 URL（头像等）。
 * 支持完整 http(s)、协议相对 //、以及相对 API 或站点根路径。
 */
export function resolveMediaUrl(raw: string | null | undefined): string {
  const s = typeof raw === 'string' ? raw.trim() : ''
  if (!s) return ''
  if (/^https?:\/\//i.test(s)) return s
  if (s.startsWith('//')) {
    if (typeof window === 'undefined') return `https:${s}`
    return `${window.location.protocol}${s}`
  }
  if (s.startsWith('data:') || s.startsWith('blob:')) return s

  const base = API_BASE_URL.replace(/\/+$/, '')
  if (s.startsWith('/')) {
    try {
      const origin = new URL(base).origin
      return `${origin}${s}`
    } catch {
      return s
    }
  }
  return `${base}/${s.replace(/^\/+/, '')}`
}
