import axios from 'axios'

function isRecord(v: unknown): v is Record<string, unknown> {
  return typeof v === 'object' && v !== null && !Array.isArray(v)
}

function humanizeMessage(message: string): string {
  const cleaned = message
    .replaceAll('judgeConfig.timeLimit:', '时间限制：')
    .replaceAll('judgeConfig.memoryLimit:', '内存限制：')
    .replaceAll('judgeConfig.stackLimit:', '栈限制：')
    .replaceAll('judgeCase:', '测试用例：')
    .replaceAll('questionType:', '题目类型：')
    .replaceAll(';', '；')
    .trim()
  if (cleaned.includes('时间限制：') || cleaned.includes('内存限制：') || cleaned.includes('栈限制：')) {
    return `评测配置需要调整：${cleaned}`
  }
  return cleaned
}

/** 从 Spring / 统一 Result / OAuth 等常见字段里取后端文案 */
function pickMessageFromBody(body: Record<string, unknown>): string | null {
  const keys = ['message', 'msg', 'errorMessage', 'error_description', 'error'] as const
  for (const k of keys) {
    const v = body[k]
    if (v != null && typeof v !== 'object' && String(v).trim()) {
      return String(v).trim()
    }
  }
  const nested = body.data
  if (isRecord(nested)) {
    for (const k of keys) {
      const v = nested[k]
      if (v != null && typeof v !== 'object' && String(v).trim()) {
        return String(v).trim()
      }
    }
  }
  return null
}

/**
 * 解析 Axios 错误里的后端提示（JWT 拦截器 401、权限、业务异常等），避免只显示「Network Error」。
 */
export function getBackendErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const status = error.response?.status
    const raw = error.response?.data
    const code = error.code || ''

    // 无响应：常见于后端服务不可达、断网、跨域预检失败、请求超时
    if (!error.response) {
      if (code === 'ERR_NETWORK' || code === 'ECONNABORTED') {
        return '网络异常，请稍后再试'
      }
      const msg = (error.message || '').toLowerCase()
      if (msg.includes('network') || msg.includes('timeout')) {
        return '网络异常，请稍后再试'
      }
    }

    if (typeof raw === 'string') {
      const t = raw.trim()
      if (t) {
        try {
          const parsed = JSON.parse(t) as unknown
          if (isRecord(parsed)) {
            const m = pickMessageFromBody(parsed)
            if (m) return humanizeMessage(m)
          }
        } catch {
          if (t.length <= 500) return humanizeMessage(t)
        }
      }
    }

    if (isRecord(raw)) {
      const m = pickMessageFromBody(raw)
      if (m) return humanizeMessage(m)
    }

    if (status) {
      return `${fallback}（HTTP ${status}）`
    }
    return error.message || fallback
  }

  if (error instanceof Error) {
    const msg = error.message.trim()
    return msg || fallback
  }

  return fallback
}

/**
 * Axios 抛错时：HTTP 401/403，或响应体里带业务 code 401/403（与 Spring Result 一致）
 */
export function isAxiosUnauthorizedResponse(error: unknown): boolean {
  if (!axios.isAxiosError(error)) return false
  const s = error.response?.status
  if (s === 401 || s === 403) return true
  const raw = error.response?.data
  if (isRecord(raw)) {
    const c = raw.code
    if (typeof c === 'number' && (c === 401 || c === 403)) return true
  }
  return false
}
