<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { ContestControllerService } from '@generated'
import { isResultSuccess } from '@/api/result'
import { getBackendErrorMessage } from '@/api/httpError'
import { mapApiError } from '@/api/mapApiError'

type ContestItem = {
  id: string
  title: string
  startTime?: string
  endTime?: string
  phase: string
}

type ArenaTab = 'all' | 'live' | 'soon' | 'past'

const loading = ref(false)
const list = ref<ContestItem[]>([])
const activeTab = ref<ArenaTab>('all')

/** 列表卡片开赛倒计时（与详情页一致：仅「未开始」且 ≤1 小时） */
const nowTick = ref(Date.now())
let listTickTimer: ReturnType<typeof setInterval> | null = null
const LIST_START_GATE_MS = 60 * 60 * 1000

function parseContestInstant(v: unknown): number | null {
  if (v == null) return null
  if (typeof v === 'number' && Number.isFinite(v)) return v
  if (typeof v === 'string') {
    const trimmed = v.trim()
    if (!trimmed) return null
    const parsed = Date.parse(trimmed)
    if (!Number.isNaN(parsed)) return parsed
    const n = Number(trimmed)
    if (Number.isFinite(n)) return n
  }
  return null
}

function msUntilContestStart(iso?: string): number {
  const t = parseContestInstant(iso)
  if (t == null) return Number.POSITIVE_INFINITY
  return Math.max(0, t - nowTick.value)
}

function showListPremiere(c: ContestItem): boolean {
  if (c.phase !== '未开始') return false
  const ms = msUntilContestStart(c.startTime)
  return ms > 0 && ms <= LIST_START_GATE_MS
}

function listPremiereMmSs(iso?: string): { mm: number; ss: number } {
  const ms = msUntilContestStart(iso)
  const totalSec = Math.max(0, Math.ceil(ms / 1000))
  return { mm: Math.floor(totalSec / 60), ss: totalSec % 60 }
}

const liveContests = computed(() => list.value.filter((c) => c.phase === '进行中'))
const soonContests = computed(() => list.value.filter((c) => c.phase === '未开始'))
const pastContests = computed(() => list.value.filter((c) => c.phase === '已结束'))

const arenaStats = computed(() => ({
  total: list.value.length,
  live: liveContests.value.length,
  soon: soonContests.value.length,
  past: pastContests.value.length,
}))

function contestsForTab(tab: ArenaTab): ContestItem[] {
  if (tab === 'live') return liveContests.value
  if (tab === 'soon') return soonContests.value
  if (tab === 'past') return pastContests.value
  return list.value
}

function phaseRailClass(phase: string): string {
  if (phase === '进行中') return 'arena-card--live'
  if (phase === '未开始') return 'arena-card--soon'
  return 'arena-card--past'
}

function phaseLabelShort(phase: string): string {
  if (phase === '进行中') return 'LIVE'
  if (phase === '未开始') return 'SOON'
  return 'ARCHIVE'
}

async function loadList() {
  loading.value = true
  try {
    const data = await ContestControllerService.listContests()
    if (!isResultSuccess(data.code)) {
      Message.error(data.message || '加载失败')
      return
    }
    list.value = (Array.isArray(data.data) ? data.data : []).map((c) => ({
      id: String(c.id ?? ''),
      title: c.title ?? '',
      startTime: c.startTime,
      endTime: c.endTime,
      phase: c.phase ?? '',
    }))
  } catch (e) {
    Message.error(mapApiError(e, getBackendErrorMessage(e, '加载失败')))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadList()
  listTickTimer = setInterval(() => {
    nowTick.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  if (listTickTimer != null) {
    clearInterval(listTickTimer)
    listTickTimer = null
  }
})

function formatDateTime(iso?: string): string {
  if (!iso) return '—'
  const t = Date.parse(iso)
  if (Number.isNaN(t)) return iso
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(t))
}
</script>

<template>
  <div class="arena-hub oj-spotlight">
    <!-- 沉浸式顶区：自有「赛场枢纽」语言，非第三方站点复刻 -->
    <header class="arena-hero">
      <div class="arena-hero__mesh" aria-hidden="true" />
      <div class="arena-hero__orb arena-hero__orb--a" aria-hidden="true" />
      <div class="arena-hero__orb arena-hero__orb--b" aria-hidden="true" />

      <div class="arena-hero__grid">
        <div class="arena-hero__copy">
          <p class="arena-hero__eyebrow">Arena Hub · 限时赛场</p>
          <span class="arena-hero__rule" aria-hidden="true" />
          <h1 class="arena-hero__title">比赛中心</h1>
          <p class="arena-hero__sub">
            限时赛制、实时榜单与 ICPC 风格罚时——在同一处完成报名、进入题目与追踪排名。
            <RouterLink class="arena-hero__inline-link" to="/rankings">全站排行榜</RouterLink>
            面向日常练习与积分；赛场成绩仅在本页关联的比赛中统计。
          </p>
        </div>

        <aside class="arena-hero__stats" aria-label="赛况概览">
          <div class="arena-stat arena-stat--total">
            <span class="arena-stat__num">{{ arenaStats.total }}</span>
            <span class="arena-stat__lab">赛事总数</span>
          </div>
          <div class="arena-stat arena-stat--live">
            <span class="arena-stat__num">{{ arenaStats.live }}</span>
            <span class="arena-stat__lab">进行中</span>
          </div>
          <div class="arena-stat arena-stat--soon">
            <span class="arena-stat__num">{{ arenaStats.soon }}</span>
            <span class="arena-stat__lab">未开始</span>
          </div>
          <div class="arena-stat arena-stat--past">
            <span class="arena-stat__num">{{ arenaStats.past }}</span>
            <span class="arena-stat__lab">已归档</span>
          </div>
        </aside>
      </div>
    </header>

    <div class="arena-shell">
      <section class="arena-guide" aria-label="参赛指南">
        <div class="arena-guide__card">
          <span class="arena-guide__glyph">①</span>
          <div>
            <h3 class="arena-guide__h">参赛路径</h3>
            <p class="arena-guide__p">进入场次 → <strong>开赛后再报名</strong> → 从本题卡片进做题页，顶部需显示「计入本场比赛」。</p>
          </div>
        </div>
        <div class="arena-guide__card">
          <span class="arena-guide__glyph">②</span>
          <div>
            <h3 class="arena-guide__h">榜单刷新</h3>
            <p class="arena-guide__p">开赛后可看榜；进行中约每 12 秒自动刷新，也可手动刷新。</p>
          </div>
        </div>
        <div class="arena-guide__card">
          <span class="arena-guide__glyph">③</span>
          <div>
            <h3 class="arena-guide__h">排名规则</h3>
            <p class="arena-guide__p">通过题数优先，相同则总罚时（分钟）更少更靠前。</p>
          </div>
        </div>
      </section>

      <nav class="arena-tabs" aria-label="筛选赛事">
        <button
          type="button"
          class="arena-tab"
          :class="{ 'arena-tab--on': activeTab === 'all' }"
          @click="activeTab = 'all'"
        >
          全部
          <span class="arena-tab__dot" />
        </button>
        <button
          type="button"
          class="arena-tab"
          :class="{ 'arena-tab--on': activeTab === 'live' }"
          @click="activeTab = 'live'"
        >
          进行中
          <span v-if="arenaStats.live" class="arena-tab__badge">{{ arenaStats.live }}</span>
        </button>
        <button
          type="button"
          class="arena-tab"
          :class="{ 'arena-tab--on': activeTab === 'soon' }"
          @click="activeTab = 'soon'"
        >
          即将开始
          <span v-if="arenaStats.soon" class="arena-tab__badge">{{ arenaStats.soon }}</span>
        </button>
        <button
          type="button"
          class="arena-tab"
          :class="{ 'arena-tab--on': activeTab === 'past' }"
          @click="activeTab = 'past'"
        >
          已结束
          <span v-if="arenaStats.past" class="arena-tab__badge arena-tab__badge--muted">{{ arenaStats.past }}</span>
        </button>
      </nav>

      <a-spin :loading="loading" class="arena-spin">
        <a-empty v-if="!loading && !list.length" class="arena-empty">
          <template #image>
            <div class="arena-empty__glyph" aria-hidden="true">◇</div>
          </template>
          <template #description>
            <p class="arena-empty__title">当前没有开放赛事</p>
            <p class="arena-empty__desc">新赛程上架后将出现在这里；你也可以先去题库热身。</p>
          </template>
        </a-empty>

        <template v-else-if="!loading && list.length">
          <!-- 分区视图：仅在「全部」下分三段，层次更清晰 -->
          <template v-if="activeTab === 'all'">
            <section v-if="liveContests.length" class="arena-block">
              <div class="arena-block__head">
                <span class="arena-block__rail arena-block__rail--live" />
                <h2 class="arena-block__title">进行中</h2>
                <span class="arena-block__hint">正在进行的场次优先展示</span>
              </div>
              <div class="arena-card-list">
                <RouterLink
                  v-for="c in liveContests"
                  :key="c.id"
                  class="arena-card-link"
                  :to="{ name: 'contest-rank-detail', params: { contestId: c.id } }"
                >
                  <article class="arena-card" :class="phaseRailClass(c.phase)">
                    <div class="arena-card__rail-meta">
                      <span class="arena-card__phase-en">{{ phaseLabelShort(c.phase) }}</span>
                      <span class="arena-card__phase-zh">{{ c.phase }}</span>
                    </div>
                    <div class="arena-card__body">
                      <h3 class="arena-card__title">{{ c.title }}</h3>
                      <p class="arena-card__window">
                        <time>{{ formatDateTime(c.startTime) }}</time>
                        <span class="arena-card__arrow-icon">→</span>
                        <time>{{ formatDateTime(c.endTime) }}</time>
                      </p>
                    </div>
                    <div class="arena-card__action">
                      <span>进入赛场</span>
                      <span class="arena-card__chev">›</span>
                    </div>
                  </article>
                </RouterLink>
              </div>
            </section>

            <section v-if="soonContests.length" class="arena-block">
              <div class="arena-block__head">
                <span class="arena-block__rail arena-block__rail--soon" />
                <h2 class="arena-block__title">即将开始</h2>
                <span class="arena-block__hint">开赛后方可报名；最后一小时卡片内显示倒计时</span>
              </div>
              <div class="arena-card-list">
                <RouterLink
                  v-for="c in soonContests"
                  :key="c.id"
                  class="arena-card-link"
                  :to="{ name: 'contest-rank-detail', params: { contestId: c.id } }"
                >
                  <article class="arena-card" :class="phaseRailClass(c.phase)">
                    <div class="arena-card__rail-meta">
                      <span class="arena-card__phase-en">{{ phaseLabelShort(c.phase) }}</span>
                      <span class="arena-card__phase-zh">{{ c.phase }}</span>
                    </div>
                    <div class="arena-card__body">
                      <h3 class="arena-card__title">{{ c.title }}</h3>
                      <p class="arena-card__window">
                        <time>{{ formatDateTime(c.startTime) }}</time>
                        <span class="arena-card__arrow-icon">→</span>
                        <time>{{ formatDateTime(c.endTime) }}</time>
                      </p>
                      <div v-if="showListPremiere(c)" class="arena-card__premiere">
                        <span class="arena-card__premiere-label">距开赛</span>
                        <span class="arena-card__premiere-time" aria-live="polite">
                          {{ String(listPremiereMmSs(c.startTime).mm).padStart(2, '0') }}
                          <span class="arena-card__premiere-colon">:</span>
                          {{ String(listPremiereMmSs(c.startTime).ss).padStart(2, '0') }}
                        </span>
                      </div>
                    </div>
                    <div class="arena-card__action">
                      <span>预告 / 报名</span>
                      <span class="arena-card__chev">›</span>
                    </div>
                  </article>
                </RouterLink>
              </div>
            </section>

            <section v-if="pastContests.length" class="arena-block arena-block--past">
              <div class="arena-block__head">
                <span class="arena-block__rail arena-block__rail--past" />
                <h2 class="arena-block__title">已归档</h2>
                <span class="arena-block__hint">可回看榜单与题目说明</span>
              </div>
              <div class="arena-card-list">
                <RouterLink
                  v-for="c in pastContests"
                  :key="c.id"
                  class="arena-card-link"
                  :to="{ name: 'contest-rank-detail', params: { contestId: c.id } }"
                >
                  <article class="arena-card arena-card--past" :class="phaseRailClass(c.phase)">
                    <div class="arena-card__rail-meta">
                      <span class="arena-card__phase-en">{{ phaseLabelShort(c.phase) }}</span>
                      <span class="arena-card__phase-zh">{{ c.phase }}</span>
                    </div>
                    <div class="arena-card__body">
                      <h3 class="arena-card__title">{{ c.title }}</h3>
                      <p class="arena-card__window">
                        <time>{{ formatDateTime(c.startTime) }}</time>
                        <span class="arena-card__arrow-icon">→</span>
                        <time>{{ formatDateTime(c.endTime) }}</time>
                      </p>
                    </div>
                    <div class="arena-card__action arena-card__action--muted">
                      <span>回顾</span>
                      <span class="arena-card__chev">›</span>
                    </div>
                  </article>
                </RouterLink>
              </div>
            </section>
          </template>

          <!-- 单筛选：扁平列表 -->
          <div v-else class="arena-card-list arena-card-list--flat">
            <template v-if="contestsForTab(activeTab).length">
              <RouterLink
                v-for="c in contestsForTab(activeTab)"
                :key="c.id"
                class="arena-card-link"
                :to="{ name: 'contest-rank-detail', params: { contestId: c.id } }"
              >
                <article class="arena-card" :class="phaseRailClass(c.phase)">
                  <div class="arena-card__rail-meta">
                    <span class="arena-card__phase-en">{{ phaseLabelShort(c.phase) }}</span>
                    <span class="arena-card__phase-zh">{{ c.phase }}</span>
                  </div>
                  <div class="arena-card__body">
                    <h3 class="arena-card__title">{{ c.title }}</h3>
                    <p class="arena-card__window">
                      <time>{{ formatDateTime(c.startTime) }}</time>
                      <span class="arena-card__arrow-icon">→</span>
                      <time>{{ formatDateTime(c.endTime) }}</time>
                    </p>
                    <div v-if="showListPremiere(c)" class="arena-card__premiere">
                      <span class="arena-card__premiere-label">距开赛</span>
                      <span class="arena-card__premiere-time" aria-live="polite">
                        {{ String(listPremiereMmSs(c.startTime).mm).padStart(2, '0') }}
                        <span class="arena-card__premiere-colon">:</span>
                        {{ String(listPremiereMmSs(c.startTime).ss).padStart(2, '0') }}
                      </span>
                    </div>
                  </div>
                  <div
                    class="arena-card__action"
                    :class="{ 'arena-card__action--muted': c.phase === '已结束' }"
                  >
                    <span>{{ c.phase === '已结束' ? '回顾' : c.phase === '进行中' ? '进入赛场' : '预告 / 报名' }}</span>
                    <span class="arena-card__chev">›</span>
                  </div>
                </article>
              </RouterLink>
            </template>
            <p v-else class="arena-filter-empty">该分类下暂无赛事，试试其它筛选。</p>
          </div>
        </template>
      </a-spin>
    </div>
  </div>
</template>

<style scoped>
.arena-hub {
  --arena-max: min(1180px, 100%);
  margin: 0 auto;
  padding-bottom: 64px;
}

/* —— Hero：大面积网格 + 双子星云，区别于常见竞赛站横幅 —— */
.arena-hero {
  position: relative;
  margin: 0 0 28px;
  padding: clamp(28px, 5vw, 48px) clamp(20px, 4vw, 40px) clamp(36px, 6vw, 52px);
  border-radius: 0 0 clamp(20px, 4vw, 32px) clamp(20px, 4vw, 32px);
  overflow: hidden;
  border-bottom: 1px solid color-mix(in srgb, var(--app-accent) 22%, var(--color-border-2));
  background:
    linear-gradient(
      165deg,
      color-mix(in srgb, var(--app-accent) 12%, var(--color-bg-1)) 0%,
      var(--color-bg-1) 42%,
      color-mix(in srgb, var(--app-accent-2) 8%, var(--color-bg-2)) 100%
    );
}

.arena-hero__mesh {
  pointer-events: none;
  position: absolute;
  inset: 0;
  opacity: 0.45;
  background-image:
    linear-gradient(color-mix(in srgb, var(--app-accent) 18%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--app-accent) 18%, transparent) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: radial-gradient(ellipse 95% 80% at 70% 20%, #000 15%, transparent 65%);
  -webkit-mask-image: radial-gradient(ellipse 95% 80% at 70% 20%, #000 15%, transparent 65%);
}

.arena-hero__orb {
  pointer-events: none;
  position: absolute;
  border-radius: 50%;
  filter: blur(56px);
}

.arena-hero__orb--a {
  width: min(72vw, 420px);
  height: min(72vw, 420px);
  top: -40%;
  right: -8%;
  background: radial-gradient(closest-side, color-mix(in srgb, var(--app-accent) 38%, transparent), transparent 72%);
  opacity: 0.55;
}

.arena-hero__orb--b {
  width: min(48vw, 280px);
  height: min(48vw, 280px);
  bottom: -35%;
  left: -5%;
  background: radial-gradient(closest-side, color-mix(in srgb, var(--app-accent-2) 28%, transparent), transparent 70%);
  opacity: 0.42;
}

.arena-hero__grid {
  position: relative;
  z-index: 1;
  max-width: var(--arena-max);
  margin: 0 auto;
  display: grid;
  gap: 28px;
  align-items: start;
}

@media (min-width: 900px) {
  .arena-hero__grid {
    grid-template-columns: 1fr minmax(260px, 320px);
    gap: 40px;
  }
}

.arena-hero__eyebrow {
  margin: 0 0 10px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: var(--app-accent);
}

.arena-hero__rule {
  display: block;
  width: min(104px, 26vw);
  height: 2px;
  margin: 0 0 20px;
  border-radius: 2px;
  background: linear-gradient(
    90deg,
    var(--app-accent),
    color-mix(in srgb, var(--app-accent-2) 62%, var(--app-accent))
  );
}

.arena-hero__title {
  margin: 0 0 14px;
  font-family: var(--app-font-display);
  font-size: clamp(2rem, 5vw, 2.65rem);
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.12;
  color: var(--color-text-1);
}

.arena-hero__sub {
  margin: 0;
  max-width: 52ch;
  font-size: 15px;
  line-height: 1.75;
  color: var(--color-text-2);
}

.arena-hero__inline-link {
  color: var(--app-accent);
  font-weight: 700;
  text-decoration: none;
  border-bottom: 1px solid color-mix(in srgb, var(--app-accent) 45%, transparent);
}

.arena-hero__inline-link:hover {
  border-bottom-color: var(--app-accent);
}

.arena-hero__stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.arena-stat {
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid var(--color-border-2);
  background: color-mix(in srgb, var(--color-bg-2) 88%, #ffffff 12%);
  display: flex;
  flex-direction: column;
  gap: 4px;
  box-shadow: 0 8px 24px color-mix(in srgb, var(--color-text-1) 5%, transparent);
}

.arena-stat__num {
  font-family: ui-monospace, 'Cascadia Code', Menlo, sans-serif;
  font-size: 1.75rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-1);
  line-height: 1;
}

.arena-stat__lab {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-3);
}

.arena-stat--live .arena-stat__num {
  color: var(--app-accent);
}

.arena-stat--soon .arena-stat__num {
  color: color-mix(in srgb, var(--app-accent-2) 85%, var(--color-text-1));
}

.arena-shell {
  max-width: var(--arena-max);
  margin: 0 auto;
  padding: 0 20px;
}

.arena-guide {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
  margin-bottom: 22px;
}

.arena-guide__card {
  display: flex;
  gap: 14px;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid var(--color-border-2);
  background: var(--color-bg-2);
  box-shadow: 0 4px 16px color-mix(in srgb, var(--color-text-1) 4%, transparent);
}

.arena-guide__glyph {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 800;
  color: var(--app-accent);
  background: color-mix(in srgb, var(--app-accent) 14%, transparent);
}

.arena-guide__h {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 700;
  color: var(--color-text-1);
}

.arena-guide__p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--color-text-3);
}

/* —— 分段 Tab（胶囊 + 微交互） —— */
.arena-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 22px;
  padding: 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--color-fill-2) 100%, transparent);
  border: 1px solid var(--color-border-2);
  width: fit-content;
  max-width: 100%;
}

.arena-tab {
  appearance: none;
  border: none;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-2);
  background: transparent;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;
}

.arena-tab:hover {
  color: var(--color-text-1);
  background: color-mix(in srgb, var(--color-bg-2) 90%, transparent);
}

.arena-tab--on {
  color: var(--color-text-1);
  background: var(--color-bg-2);
  box-shadow: 0 4px 14px color-mix(in srgb, var(--color-text-1) 8%, transparent);
}

.arena-tab__badge {
  min-width: 1.25rem;
  padding: 2px 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  background: color-mix(in srgb, var(--app-accent) 18%, transparent);
  color: var(--app-accent);
}

.arena-tab__badge--muted {
  background: var(--color-fill-3);
  color: var(--color-text-3);
}

.arena-tab__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-text-4);
  opacity: 0.5;
}

.arena-tab--on .arena-tab__dot {
  background: var(--app-accent);
  opacity: 1;
  box-shadow: 0 0 10px color-mix(in srgb, var(--app-accent) 55%, transparent);
}

.arena-spin {
  width: 100%;
  min-height: 220px;
}

.arena-empty__glyph {
  font-size: 56px;
  line-height: 1;
  color: color-mix(in srgb, var(--app-accent) 55%, var(--color-text-3));
  opacity: 0.85;
}

.arena-empty__title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-1);
}

.arena-empty__desc {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-3);
}

/* —— 分区标题 —— */
.arena-block {
  margin-bottom: 28px;
}

.arena-block--past {
  opacity: 0.96;
}

.arena-block__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
  margin-bottom: 14px;
}

.arena-block__rail {
  width: 4px;
  height: 22px;
  border-radius: 4px;
}

.arena-block__rail--live {
  background: linear-gradient(180deg, var(--app-accent), color-mix(in srgb, var(--app-accent) 60%, #38bdf8));
  box-shadow: 0 0 14px color-mix(in srgb, var(--app-accent) 45%, transparent);
}

.arena-block__rail--soon {
  background: linear-gradient(180deg, var(--app-accent-2), color-mix(in srgb, var(--app-accent-3) 70%, var(--app-accent-2)));
}

.arena-block__rail--past {
  background: var(--color-fill-3);
}

.arena-block__title {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: var(--color-text-1);
}

.arena-block__hint {
  font-size: 12px;
  color: var(--color-text-3);
  flex: 1 1 100%;
}

@media (min-width: 640px) {
  .arena-block__hint {
    flex: 0 1 auto;
    margin-left: auto;
  }
}

/* —— 赛事卡片：左侧竖轨 + 横向信息流 —— */
.arena-card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.arena-card-link {
  text-decoration: none;
  color: inherit;
  display: block;
}

.arena-card {
  position: relative;
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 16px 20px;
  align-items: center;
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid var(--color-border-2);
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--color-bg-2) 96%, var(--app-accent) 4%) 0%,
    var(--color-bg-2) 100%
  );
  box-shadow: 0 6px 22px color-mix(in srgb, var(--color-text-1) 5%, transparent);
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease,
    border-color 0.22s ease;
  overflow: hidden;
}

.arena-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 4px;
  background: var(--color-fill-3);
}

.arena-card--live::before {
  background: linear-gradient(180deg, var(--app-accent), color-mix(in srgb, var(--app-accent) 50%, #22d3ee));
  box-shadow: 0 0 16px color-mix(in srgb, var(--app-accent) 35%, transparent);
}

.arena-card--soon::before {
  background: linear-gradient(180deg, var(--app-accent-2), var(--app-accent-3));
}

.arena-card--past::before {
  background: var(--color-border-2);
}

.arena-card-link:hover .arena-card {
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--app-accent) 35%, var(--color-border-2));
  box-shadow:
    0 16px 40px color-mix(in srgb, var(--app-accent) 12%, transparent),
    0 8px 24px color-mix(in srgb, var(--color-text-1) 6%, transparent);
}

.arena-card__rail-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 52px;
}

.arena-card__phase-en {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.12em;
  color: var(--color-text-3);
}

.arena-card__phase-zh {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-text-2);
}

.arena-card--live .arena-card__phase-en {
  color: var(--app-accent);
}

.arena-card__title {
  margin: 0 0 8px;
  font-size: 1.08rem;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.35;
  color: var(--color-text-1);
}

.arena-card__window {
  margin: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-3);
}

.arena-card__arrow-icon {
  opacity: 0.45;
  font-weight: 400;
}

.arena-card__premiere {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--app-accent) 35%, var(--color-border-2));
  background: color-mix(in srgb, var(--app-accent) 10%, var(--color-bg-1));
}

.arena-card__premiere-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--app-accent);
}

.arena-card__premiere-time {
  font-family: ui-monospace, Menlo, sans-serif;
  font-size: 15px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-1);
}

.arena-card__premiere-colon {
  opacity: 0.55;
  padding: 0 2px;
}

.arena-card__action {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 700;
  color: var(--app-accent);
  white-space: nowrap;
}

.arena-card__action--muted {
  color: var(--color-text-3);
}

.arena-card__chev {
  font-size: 20px;
  font-weight: 300;
  opacity: 0.85;
}

.arena-filter-empty {
  margin: 24px 0;
  padding: 28px;
  text-align: center;
  border-radius: 16px;
  border: 1px dashed var(--color-border-2);
  color: var(--color-text-3);
  font-size: 14px;
}

@media (max-width: 640px) {
  .arena-card {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .arena-card__rail-meta {
    flex-direction: row;
    align-items: baseline;
    gap: 10px;
    min-width: 0;
  }

  .arena-card__action {
    justify-content: flex-start;
    padding-top: 4px;
    border-top: 1px dashed var(--color-border-3);
  }
}

/* 暗黑：与首页首屏 / 功能入口高亮一致，见 @/styles/oj-spotlight-pages.css */
</style>
