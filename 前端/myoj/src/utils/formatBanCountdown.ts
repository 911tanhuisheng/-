/** 解析后端 banUntil（yyyy-MM-dd HH:mm:ss，东八区） */
export function parseBanUntilMs(raw: string | null | undefined): number | null {
  if (!raw?.trim()) return null
  const s = raw.trim().replace(' ', 'T')
  const ms = Date.parse(s.includes('+') || s.endsWith('Z') ? s : `${s}+08:00`)
  return Number.isNaN(ms) ? null : ms
}

/** 剩余封禁时间文案（始终含「秒」，每秒可观察到变化）；已到期返回「即将解除」 */
export function formatBanCountdown(banUntilMs: number | null, nowMs: number = Date.now()): string {
  if (banUntilMs == null) return ''
  const diff = banUntilMs - nowMs
  if (diff <= 0) return '即将解除'
  const sec = Math.floor(diff / 1000)
  const days = Math.floor(sec / 86400)
  const hours = Math.floor((sec % 86400) / 3600)
  const minutes = Math.floor((sec % 3600) / 60)
  const seconds = sec % 60
  const parts: string[] = []
  if (days > 0) parts.push(`${days} 天`)
  if (days > 0 || hours > 0) parts.push(`${hours} 小时`)
  if (days > 0 || hours > 0 || minutes > 0) parts.push(`${minutes} 分`)
  parts.push(`${seconds} 秒`)
  return parts.join(' ')
}
