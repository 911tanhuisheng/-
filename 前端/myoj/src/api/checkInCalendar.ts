import { Service } from '@generated'
import type { UserCheckInCalendarVO } from '@generated'
import { isResultSuccess } from '@/api/result'

export type CheckInCalendarPayload = UserCheckInCalendarVO

export async function fetchCheckInCalendar(year: number): Promise<CheckInCalendarPayload | null> {
  try {
    const res = await Service.checkInCalendar(year)
    if (!isResultSuccess(res.code) || !res.data) return null
    return res.data
  } catch {
    return null
  }
}
