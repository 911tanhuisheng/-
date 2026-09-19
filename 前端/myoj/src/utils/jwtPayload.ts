/** 解析 JWT payload 中的 exp（秒），失败返回 null */
export function getJwtExpMs(token: string | null | undefined): number | null {
  if (!token?.trim()) return null
  try {
    const parts = token.trim().split('.')
    if (parts.length < 2) return null
    const payload = parts[1]
    if (!payload) return null
    const b64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4)
    const json = JSON.parse(atob(padded)) as { exp?: unknown }
    return typeof json.exp === 'number' ? json.exp * 1000 : null
  } catch {
    return null
  }
}

/** access 是否已过期；skewMs 为提前刷新余量（默认 60 秒） */
export function isJwtExpired(
  token: string | null | undefined,
  skewMs = 60_000,
  nowMs = Date.now(),
): boolean {
  const exp = getJwtExpMs(token)
  if (exp == null) return false
  return exp <= nowMs + skewMs
}
