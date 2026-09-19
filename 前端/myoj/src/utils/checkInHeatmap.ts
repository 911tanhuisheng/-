/** 上海时区自然日 yyyy-MM-dd */
export function formatLocalDateKey(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export type HeatmapCell = {
  dateKey: string | null
  checked: boolean
  /** 0 空 / 1 已签（预留多级强度） */
  level: number
}

export type HeatmapWeekColumn = HeatmapCell[]

/** GitHub 风格：7 行（周一→周日），列为周 */
export function buildYearHeatmap(year: number, checkedDates: Iterable<string>): HeatmapWeekColumn[] {
  const checked = new Set(checkedDates)
  const weeks: HeatmapWeekColumn[] = []
  const start = new Date(year, 0, 1)
  const end = new Date(year, 11, 31)
  const cur = new Date(start)

  let week: HeatmapCell[] = Array.from({ length: 7 }, () => ({
    dateKey: null,
    checked: false,
    level: 0,
  }))

  const firstDow = (cur.getDay() + 6) % 7
  for (let i = 0; i < firstDow; i++) {
    week[i] = { dateKey: null, checked: false, level: 0 }
  }

  while (cur <= end) {
    const dow = (cur.getDay() + 6) % 7
    const key = formatLocalDateKey(cur)
    const isChecked = checked.has(key)
    week[dow] = {
      dateKey: key,
      checked: isChecked,
      level: isChecked ? 1 : 0,
    }
    if (dow === 6) {
      weeks.push(week)
      week = Array.from({ length: 7 }, () => ({
        dateKey: null,
        checked: false,
        level: 0,
      }))
    }
    cur.setDate(cur.getDate() + 1)
  }

  if (week.some((c) => c.dateKey != null)) {
    weeks.push(week)
  }
  return weeks
}

/** 每月第一周列索引（用于顶部月份标签） */
export function monthColumnLabels(year: number, weekCount: number): { month: number; col: number }[] {
  const labels: { month: number; col: number }[] = []
  let lastMonth = -1
  for (let col = 0; col < weekCount; col++) {
    const approxDay = col * 7 + 1
    const d = new Date(year, 0, approxDay)
    if (d.getFullYear() !== year) continue
    const m = d.getMonth()
    if (m !== lastMonth) {
      labels.push({ month: m + 1, col })
      lastMonth = m
    }
  }
  return labels
}

export const WEEKDAY_LABELS = ['一', '二', '三', '四', '五', '六', '日'] as const
