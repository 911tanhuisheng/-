<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { Service } from '@generated'
import { fetchCheckInCalendar, type CheckInCalendarPayload } from '@/api/checkInCalendar'
import { isResultSuccess } from '@/api/result'
import { mapApiError } from '@/api/mapApiError'
import { getBackendErrorMessage } from '@/api/httpError'
import {
  buildYearHeatmap,
  monthColumnLabels,
  WEEKDAY_LABELS,
  type HeatmapCell,
} from '@/utils/checkInHeatmap'

const visible = defineModel<boolean>('visible', { default: false })

const props = defineProps<{
  accountDisabled?: boolean
}>()

const emit = defineEmits<{
  statusChange: [payload: { points: number; checkInCount: number; checkedToday: boolean }]
}>()

const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const loading = ref(false)
const submitting = ref(false)
const calendar = ref<CheckInCalendarPayload | null>(null)

const points = computed(() => calendar.value?.points ?? 0)
const totalDays = computed(() => calendar.value?.checkInCount ?? 0)
const yearDays = computed(() => calendar.value?.yearCheckInDays ?? 0)
const checkedToday = computed(() => !!calendar.value?.checkedToday)

const checkedSet = computed(() => new Set(calendar.value?.checkedDates ?? []))

const weeks = computed(() => buildYearHeatmap(year.value, checkedSet.value))

const monthLabels = computed(() => monthColumnLabels(year.value, weeks.value.length))

const canCheckIn = computed(
  () => !props.accountDisabled && !checkedToday.value && !submitting.value && year.value === currentYear,
)

async function loadCalendar() {
  loading.value = true
  try {
    const data = await fetchCheckInCalendar(year.value)
    calendar.value = data
    if (data) {
      emit('statusChange', {
        points: data.points ?? 0,
        checkInCount: data.checkInCount ?? 0,
        checkedToday: !!data.checkedToday,
      })
    }
  } finally {
    loading.value = false
  }
}

async function onCheckInToday() {
  if (props.accountDisabled) {
    Message.warning('账号已被禁用，暂不可签到')
    return
  }
  if (!canCheckIn.value) return
  submitting.value = true
  try {
    const data = await Service.checkIn()
    if (!isResultSuccess(data?.code) || !data?.data) {
      Message.warning(data?.message?.trim() || '签到失败')
      await loadCalendar()
      return
    }
    const d = data.data
    Message.success(data?.message?.trim() || '签到成功，+100 积分')
    emit('statusChange', {
      points: typeof d.points === 'number' ? d.points : 0,
      checkInCount: typeof d.checkInCount === 'number' ? d.checkInCount : 0,
      checkedToday: true,
    })
    await loadCalendar()
  } catch (e) {
    Message.error(mapApiError(e, getBackendErrorMessage(e, '签到失败')))
    await loadCalendar()
  } finally {
    submitting.value = false
  }
}

function prevYear() {
  if (year.value <= 2000) return
  year.value -= 1
}

function nextYear() {
  if (year.value >= currentYear) return
  year.value += 1
}

function cellTitle(cell: HeatmapCell): string {
  if (!cell.dateKey) return ''
  return cell.checked ? `${cell.dateKey} 已打卡` : `${cell.dateKey} 未打卡`
}

function cellLevelClass(cell: HeatmapCell): string {
  if (!cell.dateKey) return 'checkin-cell--empty'
  if (!cell.checked) return 'checkin-cell--lv0'
  return 'checkin-cell--lv3'
}

watch(visible, (open) => {
  if (open) {
    year.value = currentYear
    void loadCalendar()
  }
})

watch(year, () => {
  if (visible.value) void loadCalendar()
})

function close() {
  visible.value = false
}
</script>

<template>
  <Teleport to="body">
    <Transition name="checkin-modal">
      <div v-if="visible" class="checkin-overlay" role="dialog" aria-modal="true" aria-labelledby="checkin-title">
        <button type="button" class="checkin-overlay__backdrop" aria-label="关闭" @click="close" />
        <div class="checkin-panel">
          <div class="checkin-panel__aurora" aria-hidden="true" />

          <header class="checkin-panel__head">
            <div class="checkin-panel__title-row">
              <span class="checkin-panel__ico" aria-hidden="true">
                <svg viewBox="0 0 24 24" fill="none" width="22" height="22">
                  <rect x="3" y="4" width="18" height="17" rx="3" stroke="currentColor" stroke-width="1.5" />
                  <path d="M3 9h18M8 2v4M16 2v4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                  <circle cx="12" cy="15" r="2.5" fill="currentColor" />
                </svg>
              </span>
              <div>
                <h2 id="checkin-title" class="checkin-panel__title">学习打卡日历</h2>
                <p class="checkin-panel__sub">
                  <span class="checkin-panel__year-stat">
                    {{ year }} 年共打卡 <strong>{{ loading ? '…' : yearDays }}</strong> 天
                  </span>
                  <span class="checkin-panel__dot">·</span>
                  <span>累计 {{ loading ? '…' : totalDays }} 天</span>
                  <span class="checkin-panel__dot">·</span>
                  <span>积分 {{ loading ? '…' : points }}</span>
                </p>
              </div>
            </div>

            <div class="checkin-panel__head-actions">
              <div class="checkin-year-nav" role="group" aria-label="切换年份">
                <button type="button" class="checkin-year-nav__btn" :disabled="year <= 2000" @click="prevYear">
                  ‹
                </button>
                <span class="checkin-year-nav__label">{{ year }} 年</span>
                <button
                  type="button"
                  class="checkin-year-nav__btn"
                  :disabled="year >= currentYear"
                  @click="nextYear"
                >
                  ›
                </button>
              </div>
              <button type="button" class="checkin-panel__close" aria-label="关闭" @click="close">✕</button>
            </div>
          </header>

          <div class="checkin-panel__body">
            <div class="checkin-heatmap-wrap" :class="{ 'checkin-heatmap-wrap--loading': loading }">
              <div class="checkin-heatmap-scroll">
                <div class="checkin-heatmap">
                  <div class="checkin-heatmap__month-row" aria-hidden="true">
                    <span class="checkin-heatmap__weekday-spacer" />
                    <div
                      class="checkin-heatmap__months"
                      :style="{ gridTemplateColumns: `repeat(${weeks.length}, 1fr)` }"
                    >
                      <span
                        v-for="ml in monthLabels"
                        :key="ml.month"
                        class="checkin-heatmap__month"
                        :style="{ gridColumn: ml.col + 1 }"
                      >
                        {{ ml.month }}月
                      </span>
                    </div>
                  </div>

                  <div class="checkin-heatmap__grid">
                    <div class="checkin-heatmap__weekdays">
                      <span v-for="w in WEEKDAY_LABELS" :key="w" class="checkin-heatmap__weekday">{{ w }}</span>
                    </div>
                    <div
                      class="checkin-heatmap__cells"
                      :style="{
                        gridTemplateColumns: `repeat(${weeks.length}, var(--checkin-cell-size))`,
                      }"
                    >
                      <template v-for="(col, colIdx) in weeks" :key="colIdx">
                        <span
                          v-for="(cell, rowIdx) in col"
                          :key="`${colIdx}-${rowIdx}`"
                          class="checkin-cell"
                          :class="cellLevelClass(cell)"
                          :title="cellTitle(cell)"
                        />
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <aside class="checkin-side">
              <div class="checkin-side__card">
                <p class="checkin-side__label">今日签到</p>
                <p class="checkin-side__reward">+100 <span>积分</span></p>
                <p class="checkin-side__hint">
                  {{ year < currentYear ? '请切换到当前年份进行签到' : '坚持每日打卡，点亮你的学习轨迹' }}
                </p>
                <button
                  type="button"
                  class="checkin-side__btn"
                  :class="{ 'checkin-side__btn--done': checkedToday }"
                  :disabled="!canCheckIn && !checkedToday"
                  @click="onCheckInToday"
                >
                  <span v-if="checkedToday" class="checkin-side__btn-ico">✓</span>
                  {{
                    checkedToday
                      ? '今日已签到'
                      : submitting
                        ? '签到中…'
                        : props.accountDisabled
                          ? '账号已禁用'
                          : year < currentYear
                            ? '仅可在今年签到'
                            : '今日签到'
                  }}
                </button>
              </div>

              <div class="checkin-legend">
                <span class="checkin-legend__label">少</span>
                <span class="checkin-cell checkin-cell--lv0 checkin-legend__cell" />
                <span class="checkin-cell checkin-cell--lv1 checkin-legend__cell" />
                <span class="checkin-cell checkin-cell--lv2 checkin-legend__cell" />
                <span class="checkin-cell checkin-cell--lv3 checkin-legend__cell" />
                <span class="checkin-legend__label">多</span>
              </div>
            </aside>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.checkin-overlay {
  position: fixed;
  inset: 0;
  z-index: 11000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.checkin-overlay__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(2, 6, 14, 0.62);
  backdrop-filter: blur(6px);
  cursor: pointer;
}

.checkin-panel {
  --checkin-cell-size: 13px;
  --checkin-accent: #34d399;
  --checkin-accent-dim: #064e3b;
  position: relative;
  z-index: 1;
  width: min(920px, 100%);
  max-height: min(88vh, 640px);
  display: flex;
  flex-direction: column;
  border-radius: 20px;
  border: 1px solid rgba(52, 211, 153, 0.22);
  background: linear-gradient(145deg, rgba(10, 18, 28, 0.97) 0%, rgba(6, 12, 22, 0.98) 55%, rgba(8, 16, 26, 0.97) 100%);
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.45),
    0 0 0 1px rgba(255, 255, 255, 0.04) inset,
    0 0 48px rgba(16, 185, 129, 0.08);
  overflow: hidden;
  color: #e2e8f0;
}

html[data-theme='light'] .checkin-panel {
  --checkin-accent: #059669;
  --checkin-accent-dim: #d1fae5;
  border-color: rgba(5, 150, 105, 0.25);
  background: linear-gradient(145deg, #f8fffe 0%, #f0fdf9 45%, #ecfdf5 100%);
  color: #0f172a;
  box-shadow:
    0 24px 60px rgba(15, 23, 42, 0.12),
    0 0 0 1px rgba(5, 150, 105, 0.08) inset;
}

.checkin-panel__aurora {
  position: absolute;
  inset: -30% -20%;
  pointer-events: none;
  background:
    radial-gradient(ellipse 50% 40% at 20% 0%, rgba(52, 211, 153, 0.18), transparent 55%),
    radial-gradient(ellipse 40% 35% at 90% 20%, rgba(56, 189, 248, 0.1), transparent 50%);
  opacity: 0.9;
}

.checkin-panel__head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 14px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.checkin-panel__title-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
}

.checkin-panel__ico {
  flex-shrink: 0;
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  color: var(--checkin-accent);
  background: rgba(52, 211, 153, 0.12);
  border: 1px solid rgba(52, 211, 153, 0.28);
  box-shadow: 0 0 24px rgba(52, 211, 153, 0.15);
}

.checkin-panel__title {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.03em;
}

.checkin-panel__sub {
  margin: 6px 0 0;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.95);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

html[data-theme='light'] .checkin-panel__sub {
  color: #64748b;
}

.checkin-panel__year-stat strong {
  color: var(--checkin-accent);
  font-variant-numeric: tabular-nums;
}

.checkin-panel__dot {
  opacity: 0.45;
}

.checkin-panel__head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.checkin-year-nav {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.45);
  border: 1px solid rgba(148, 163, 184, 0.15);
}

html[data-theme='light'] .checkin-year-nav {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(15, 23, 42, 0.08);
}

.checkin-year-nav__btn {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: inherit;
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s ease;
}

.checkin-year-nav__btn:hover:not(:disabled) {
  background: rgba(52, 211, 153, 0.15);
}

.checkin-year-nav__btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.checkin-year-nav__label {
  min-width: 64px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.checkin-panel__close {
  width: 34px;
  height: 34px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.35);
  color: inherit;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s ease;
}

html[data-theme='light'] .checkin-panel__close {
  background: rgba(255, 255, 255, 0.9);
}

.checkin-panel__close:hover {
  background: rgba(52, 211, 153, 0.12);
}

.checkin-panel__body {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1fr minmax(200px, 240px);
  gap: 0;
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

@media (max-width: 768px) {
  .checkin-panel__body {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }
}

.checkin-heatmap-wrap {
  padding: 12px 16px 18px 18px;
  min-width: 0;
  position: relative;
}

.checkin-heatmap-wrap--loading::after {
  content: '';
  position: absolute;
  inset: 12px 16px 18px 18px;
  border-radius: 12px;
  background: rgba(2, 6, 14, 0.35);
  pointer-events: none;
}

.checkin-heatmap-scroll {
  overflow-x: auto;
  overflow-y: hidden;
  padding-bottom: 6px;
  scrollbar-width: thin;
  scrollbar-color: rgba(52, 211, 153, 0.35) transparent;
}

.checkin-heatmap {
  min-width: max-content;
}

.checkin-heatmap__month-row {
  display: flex;
  align-items: flex-end;
  margin-bottom: 6px;
}

.checkin-heatmap__weekday-spacer {
  width: 28px;
  flex-shrink: 0;
}

.checkin-heatmap__months {
  display: grid;
  flex: 1;
  font-size: 10px;
  color: rgba(148, 163, 184, 0.85);
  min-height: 14px;
}

.checkin-heatmap__month {
  white-space: nowrap;
}

.checkin-heatmap__grid {
  display: flex;
  gap: 6px;
}

.checkin-heatmap__weekdays {
  display: grid;
  grid-template-rows: repeat(7, var(--checkin-cell-size));
  gap: 3px;
  width: 22px;
  flex-shrink: 0;
  font-size: 10px;
  color: rgba(148, 163, 184, 0.75);
  line-height: var(--checkin-cell-size);
}

.checkin-heatmap__cells {
  display: grid;
  grid-auto-flow: column;
  grid-template-rows: repeat(7, var(--checkin-cell-size));
  gap: 3px;
}

.checkin-cell {
  width: var(--checkin-cell-size);
  height: var(--checkin-cell-size);
  border-radius: 3px;
  box-sizing: border-box;
}

.checkin-cell--empty {
  background: transparent;
  pointer-events: none;
}

.checkin-cell--lv0 {
  background: rgba(30, 41, 59, 0.85);
  border: 1px solid rgba(51, 65, 85, 0.6);
}

html[data-theme='light'] .checkin-cell--lv0 {
  background: rgba(226, 232, 240, 0.9);
  border-color: rgba(203, 213, 225, 0.9);
}

.checkin-cell--lv1 {
  background: rgba(52, 211, 153, 0.28);
  border: 1px solid rgba(52, 211, 153, 0.35);
}

.checkin-cell--lv2 {
  background: rgba(52, 211, 153, 0.52);
  border: 1px solid rgba(52, 211, 153, 0.45);
}

.checkin-cell--lv3 {
  background: #34d399;
  border: 1px solid #6ee7b7;
  box-shadow: 0 0 8px rgba(52, 211, 153, 0.45);
}

html[data-theme='light'] .checkin-cell--lv3 {
  background: #10b981;
  border-color: #34d399;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.35);
}

.checkin-side {
  padding: 14px 18px 18px 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border-left: 1px solid rgba(148, 163, 184, 0.1);
}

@media (max-width: 768px) {
  .checkin-side {
    padding: 0 18px 18px;
    border-left: 0;
    border-top: 1px solid rgba(148, 163, 184, 0.1);
  }
}

.checkin-side__card {
  padding: 16px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.5);
  border: 1px solid rgba(52, 211, 153, 0.18);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

html[data-theme='light'] .checkin-side__card {
  background: rgba(255, 255, 255, 0.92);
  border-color: rgba(5, 150, 105, 0.2);
}

.checkin-side__label {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--checkin-accent);
}

.checkin-side__reward {
  margin: 8px 0 0;
  font-size: 28px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1.1;
}

.checkin-side__reward span {
  font-size: 14px;
  font-weight: 600;
  opacity: 0.75;
}

.checkin-side__hint {
  margin: 10px 0 14px;
  font-size: 12px;
  line-height: 1.5;
  color: rgba(148, 163, 184, 0.9);
}

html[data-theme='light'] .checkin-side__hint {
  color: #64748b;
}

.checkin-side__btn {
  width: 100%;
  padding: 12px 16px;
  border: 0;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  color: #042f1a;
  background: linear-gradient(135deg, #6ee7b7 0%, #34d399 45%, #10b981 100%);
  box-shadow:
    0 4px 20px rgba(52, 211, 153, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.35);
  transition:
    transform 0.15s ease,
    box-shadow 0.2s ease,
    filter 0.2s ease;
}

.checkin-side__btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 8px 28px rgba(52, 211, 153, 0.42);
  filter: brightness(1.05);
}

.checkin-side__btn:disabled {
  cursor: not-allowed;
  transform: none;
  opacity: 0.55;
  filter: grayscale(0.2);
}

.checkin-side__btn--done {
  color: #78350f;
  background: linear-gradient(135deg, #fde68a 0%, #fbbf24 100%);
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.25);
}

.checkin-side__btn-ico {
  margin-right: 6px;
}

.checkin-legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  padding: 0 4px;
}

.checkin-legend__label {
  font-size: 10px;
  color: rgba(148, 163, 184, 0.8);
  margin: 0 2px;
}

.checkin-legend__cell {
  pointer-events: none;
}

.checkin-modal-enter-active,
.checkin-modal-leave-active {
  transition: opacity 0.22s ease;
}

.checkin-modal-enter-active .checkin-panel,
.checkin-modal-leave-active .checkin-panel {
  transition:
    transform 0.26s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.22s ease;
}

.checkin-modal-enter-from,
.checkin-modal-leave-to {
  opacity: 0;
}

.checkin-modal-enter-from .checkin-panel,
.checkin-modal-leave-to .checkin-panel {
  transform: translateY(12px) scale(0.98);
  opacity: 0;
}
</style>
