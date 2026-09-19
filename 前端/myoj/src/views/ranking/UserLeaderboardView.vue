<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { ContestControllerService, Service } from '@generated'
import type { UserLeaderboardRowVO } from '@generated'
import { isResultSuccess } from '@/api/result'
import { getBackendErrorMessage } from '@/api/httpError'
import { mapApiError } from '@/api/mapApiError'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'

type Row = UserLeaderboardRowVO

type ContestSnippet = {
  id: string
  title: string
  startTime?: string
  endTime?: string
  phase: string
}

type PodiumSlot = { row: Row; place: 1 | 2 | 3 }

const loading = ref(false)
const rows = ref<Row[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(20)

const contestsLoading = ref(false)
const contests = ref<ContestSnippet[]>([])
const sideTab = ref<'contests' | 'help'>('contests')

/** 与后端 `/user/leaderboard/page?dimension=` 一致：composite | points | submissions */
const rankDimension = ref<'composite' | 'points' | 'submissions'>('composite')

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const podiumMetricLabel = computed(() => {
  if (rankDimension.value === 'points') return '积分'
  if (rankDimension.value === 'submissions') return '提交'
  return '积分'
})

function podiumMetricValue(row: Row): number {
  if (rankDimension.value === 'points') return row.points ?? 0
  if (rankDimension.value === 'submissions') return row.submitCount ?? 0
  return row.points ?? 0
}

function rowListKey(rec: Row): string {
  return `${rankDimension.value}-${String(rec.userId ?? '')}-${String(rec.rankOrder ?? '')}`
}

/** 第一页：领奖台三人（顺序展示为 2、1、3） */
const podiumOrder = computed((): PodiumSlot[] => {
  const r = rows.value
  if (current.value !== 1 || r.length < 3) return []
  const a = r.find((x) => x.rankOrder === 1)
  const b = r.find((x) => x.rankOrder === 2)
  const c = r.find((x) => x.rankOrder === 3)
  if (!a || !b || !c) return []
  return [
    { row: b, place: 2 as const },
    { row: a, place: 1 as const },
    { row: c, place: 3 as const },
  ]
})

const listAfterPodium = computed(() => {
  if (current.value !== 1 || !podiumOrder.value.length) return rows.value
  return rows.value.filter((x) => (x.rankOrder ?? 0) > 3)
})

function avatarSrc(url: string | null | undefined) {
  return resolveMediaUrl(url?.trim() || null)
}

async function load() {
  loading.value = true
  try {
    const data = await Service.userLeaderboardPage(current.value, pageSize.value, rankDimension.value)
    if (!isResultSuccess(data.code)) {
      Message.error(data.message || '加载失败')
      return
    }
    const page = data.data
    rows.value = page?.records ?? []
    total.value = page?.total ?? 0
    current.value = page?.current ?? current.value
    pageSize.value = page?.size ?? pageSize.value
  } catch (e) {
    Message.error(mapApiError(e, getBackendErrorMessage(e, '加载失败')))
  } finally {
    loading.value = false
  }
}

async function loadContests() {
  contestsLoading.value = true
  try {
    const data = await ContestControllerService.listContests()
    if (!isResultSuccess(data.code)) {
      contests.value = []
      return
    }
    const list = Array.isArray(data.data) ? data.data : []
    contests.value = list.slice(0, 12).map((c) => ({
      id: String(c.id ?? ''),
      title: c.title ?? '',
      startTime: c.startTime,
      endTime: c.endTime,
      phase: c.phase ?? '',
    }))
  } catch {
    contests.value = []
  } finally {
    contestsLoading.value = false
  }
}

onMounted(() => {
  void loadContests()
})

watch(
  [current, pageSize, rankDimension],
  () => {
    void load()
  },
  { immediate: true },
)

function setDimension(d: 'composite' | 'points' | 'submissions') {
  if (rankDimension.value === d) return
  rankDimension.value = d
  current.value = 1
}

function onPrev() {
  if (current.value <= 1) return
  current.value -= 1
}

function onNext() {
  if (current.value >= totalPages.value) return
  current.value += 1
}

function formatContestTime(iso?: string): string {
  if (!iso) return '—'
  const t = Date.parse(iso)
  if (Number.isNaN(t)) return iso
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    weekday: 'short',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(t))
}

/** 用于右侧缩略装饰色（与赛事 id 稳定关联） */
function contestHue(id: string): number {
  let h = 0
  for (let i = 0; i < id.length; i++) h = (h * 31 + id.charCodeAt(i)) >>> 0
  return h % 360
}

</script>

<template>
  <div class="lb-page oj-spotlight">
    <div class="lb-bg-grid" aria-hidden="true" />

    <header class="lb-masthead">
      <div class="lb-masthead__copy">
        <p class="lb-masthead__eyebrow">Leaderboard · 全站</p>
        <span class="lb-masthead__rule" aria-hidden="true" />
        <h1 class="lb-masthead__title">排行榜</h1>
        <p v-if="rankDimension === 'composite'" class="lb-masthead__sub">
          <strong>综合排行</strong>按做题通过数、积分与提交综合排序；<strong>需至少有一次比赛报名记录</strong>才会出现在本榜。单场实时榜请在
          <RouterLink to="/contests" class="lb-masthead__link">比赛中心</RouterLink>
          查看。
        </p>
        <p v-else-if="rankDimension === 'points'" class="lb-masthead__sub">
          <strong>积分榜</strong>按账户<strong>累计积分</strong>从高到低排列；积分相同时再比较通过题数与总提交。上榜条件与综合榜相同（须至少报名过一场比赛）。单场榜请至
          <RouterLink to="/contests" class="lb-masthead__link">比赛中心</RouterLink>。
        </p>
        <p v-else class="lb-masthead__sub">
          <strong>活跃榜</strong>按<strong>总提交次数</strong>从高到低排列，鼓励多练多交；提交量相同时再比较通过题数与积分。上榜条件与综合榜相同。单场榜请至
          <RouterLink to="/contests" class="lb-masthead__link">比赛中心</RouterLink>。
        </p>
      </div>
      <div class="lb-masthead__stat">
        <span class="lb-masthead__stat-num">{{ total }}</span>
        <span class="lb-masthead__stat-lab">位选手已上榜</span>
      </div>
    </header>

    <div class="lb-split">
      <!-- 左：排行主体 -->
      <section class="lb-panel lb-panel--rank">
        <nav class="lb-tabs" aria-label="榜单类型">
          <button
            type="button"
            :class="rankDimension === 'composite' ? 'lb-tab lb-tab--active' : 'lb-tab lb-tab--ghost'"
            @click="setDimension('composite')"
          >
            综合排行
          </button>
          <button
            type="button"
            :class="rankDimension === 'points' ? 'lb-tab lb-tab--active' : 'lb-tab lb-tab--ghost'"
            @click="setDimension('points')"
          >
            积分榜
          </button>
          <button
            type="button"
            :class="rankDimension === 'submissions' ? 'lb-tab lb-tab--active' : 'lb-tab lb-tab--ghost'"
            @click="setDimension('submissions')"
          >
            活跃榜
          </button>
        </nav>

        <a-spin :loading="loading" class="lb-spin">
          <div v-if="!loading && !rows.length" class="lb-empty">
            <div class="lb-empty__icon" aria-hidden="true">◇</div>
            <p class="lb-empty__title">暂无榜单数据</p>
            <p class="lb-empty__desc">
              可能尚无满足条件的用户，或你尚未上榜：请到比赛中心在<strong>开赛后报名</strong>至少一场比赛，再通过题库做题积累 AC 与积分。
            </p>
          </div>

          <template v-else>
            <!-- 领奖台：仅第一页且前三名齐全 -->
            <div v-if="podiumOrder.length" class="lb-podium">
              <div
                v-for="{ row, place } in podiumOrder"
                :key="place"
                class="lb-podium__slot"
                :class="`lb-podium__slot--${place}`"
              >
                <div class="lb-podium__pedestal">
                  <span class="lb-podium__crown" :class="`lb-podium__crown--${place}`" aria-hidden="true">
                    {{ place === 1 ? '👑' : place === 2 ? '♔' : '♚' }}
                  </span>
                  <div class="lb-podium__avatar-wrap">
                    <a-avatar :size="place === 1 ? 72 : 56" shape="circle" class="lb-podium__avatar">
                      <img v-if="row.avatar" :alt="row.displayName || ''" :src="avatarSrc(row.avatar)" />
                      <span v-else class="lb-podium__fallback">{{ (row.displayName || '?').slice(0, 1) }}</span>
                    </a-avatar>
                  </div>
                  <p class="lb-podium__name">{{ row.displayName || `用户 ${row.userId}` }}</p>
                  <p class="lb-podium__score">
                    <span class="lb-podium__score-label">{{ podiumMetricLabel }}</span>
                    <span class="lb-podium__score-val">{{ podiumMetricValue(row) }}</span>
                  </p>
                  <span class="lb-podium__badge-rank">#{{ place }}</span>
                </div>
              </div>
            </div>

            <!-- 名次卡片列表 -->
            <ul class="lb-row-list">
              <li v-for="rec in listAfterPodium" :key="rowListKey(rec)" class="lb-row-card">
                <span class="lb-row-card__order">{{ rec.rankOrder }}</span>
                <a-avatar :size="44" shape="circle" class="lb-row-card__avatar">
                  <img v-if="rec.avatar" :alt="rec.displayName || ''" :src="avatarSrc(rec.avatar)" />
                  <span v-else class="lb-row-card__fallback">{{ (rec.displayName || '?').slice(0, 1) }}</span>
                </a-avatar>
                <div class="lb-row-card__main">
                  <span class="lb-row-card__name">{{ rec.displayName || `用户 ${rec.userId}` }}</span>
                  <span class="lb-row-card__id">ID {{ rec.userId }}</span>
                </div>
                <dl class="lb-row-card__stats">
                  <div>
                    <dt>通过</dt>
                    <dd>{{ rec.solvedCount ?? 0 }}</dd>
                  </div>
                  <div>
                    <dt>积分</dt>
                    <dd>{{ rec.points ?? 0 }}</dd>
                  </div>
                  <div>
                    <dt>提交</dt>
                    <dd>{{ rec.submitCount ?? 0 }}</dd>
                  </div>
                </dl>
              </li>
            </ul>

            <footer class="lb-pager">
              <a-button type="outline" size="medium" :disabled="current <= 1 || loading" @click="onPrev">
                上一页
              </a-button>
              <span class="lb-pager__meta">第 {{ current }} / {{ totalPages }} 页 · 共 {{ total }} 人</span>
              <a-button type="outline" size="medium" :disabled="current >= totalPages || loading" @click="onNext">
                下一页
              </a-button>
            </footer>
          </template>
        </a-spin>
      </section>

      <!-- 右：赛场与说明 -->
      <section class="lb-panel lb-panel--side">
        <nav class="lb-side-tabs" aria-label="侧栏">
          <button
            type="button"
            class="lb-side-tab"
            :class="{ 'lb-side-tab--on': sideTab === 'contests' }"
            @click="sideTab = 'contests'"
          >
            限时赛场
          </button>
          <button
            type="button"
            class="lb-side-tab"
            :class="{ 'lb-side-tab--on': sideTab === 'help' }"
            @click="sideTab = 'help'"
          >
            排行说明
          </button>
        </nav>

        <div v-show="sideTab === 'contests'" class="lb-side-body">
          <a-spin :loading="contestsLoading">
            <p v-if="!contestsLoading && !contests.length" class="lb-side-empty">暂无比赛，稍后再来看看。</p>
            <ul v-else class="lb-contest-list">
              <li v-for="c in contests" :key="c.id">
                <RouterLink class="lb-contest-card" :to="{ name: 'contest-rank-detail', params: { contestId: c.id } }">
                  <div
                    class="lb-contest-card__thumb"
                    :style="{ '--hue': String(contestHue(c.id)) }"
                    aria-hidden="true"
                  />
                  <div class="lb-contest-card__body">
                    <p class="lb-contest-card__title">{{ c.title }}</p>
                    <p class="lb-contest-card__time">{{ formatContestTime(c.startTime) }}</p>
                  </div>
                  <span class="lb-contest-card__phase">{{ c.phase }}</span>
                  <span class="lb-contest-card__go">进入</span>
                </RouterLink>
              </li>
            </ul>
          </a-spin>
          <RouterLink to="/contests" class="lb-side-more">前往比赛中心 →</RouterLink>
        </div>

        <div v-show="sideTab === 'help'" class="lb-side-body lb-help">
          <div class="lb-help__card">
            <h3 class="lb-help__h">谁会上全站榜？</h3>
            <p class="lb-help__p">
              全站榜只展示<strong>至少成功报名过一场比赛</strong>的用户（开赛后在比赛详情页点击「报名参赛」即可，与是否得分无关）。在此之后，你在<strong>题库</strong>里产生的 AC 与提交会计入本榜统计。
            </p>
          </div>
          <div class="lb-help__card">
            <h3 class="lb-help__h">通过题数怎么算？</h3>
            <p class="lb-help__p">
              统计<strong>至少有一次 Accepted（评测通过）</strong>的<strong>不同题目</strong>数量；同一题多次 AC 仍计 1 题。<strong>提交次数</strong>为未删除记录下的每次递交总和（含未通过）。
            </p>
          </div>
          <div class="lb-help__card">
            <h3 class="lb-help__h">排序规则（当前榜单）</h3>
            <p v-if="rankDimension === 'composite'" class="lb-help__p">
              <strong>综合排行：</strong>① 通过题数多者优先；② 题数相同则积分高者优先；③ 仍相同则总提交次数<strong>少</strong>者优先（同样实力下鼓励少无效提交）。
            </p>
            <p v-else-if="rankDimension === 'points'" class="lb-help__p">
              <strong>积分榜：</strong>① 积分高者优先；② 积分相同则通过题数多者优先；③ 仍相同则总提交次数少者优先。
            </p>
            <p v-else class="lb-help__p">
              <strong>活跃榜：</strong>① 总提交次数多者优先；② 相同则通过题数多者优先；③ 仍相同则积分高者优先。
            </p>
          </div>
          <div class="lb-help__card">
            <h3 class="lb-help__h">积分从哪来？</h3>
            <p class="lb-help__p">
              账户<strong>积分</strong>由签到、活动等与站点运营规则累计；与「通过题数」并列参与排序，用于题数相同时的先后。
            </p>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.lb-page {
  --lb-radius-xl: 28px;
  --lb-radius-lg: 20px;
  --lb-radius-md: 14px;
  --lb-max: min(1180px, 100%);
  position: relative;
  max-width: var(--lb-max);
  margin: 0 auto;
  padding: 0 20px 64px;
}

.lb-bg-grid {
  pointer-events: none;
  position: fixed;
  inset: 0;
  z-index: 0;
  opacity: 0.45;
  background-image:
    linear-gradient(color-mix(in srgb, var(--color-text-1) 6%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--color-text-1) 6%, transparent) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse 70% 65% at 50% 42%, #000 25%, transparent 72%);
  -webkit-mask-image: radial-gradient(ellipse 70% 65% at 50% 42%, #000 25%, transparent 72%);
}

.lb-masthead {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 24px;
  padding: 8px 4px 4px;
}

.lb-masthead__eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: color-mix(in srgb, var(--app-accent-2) 75%, var(--app-accent));
}

.lb-masthead__rule {
  display: block;
  width: min(96px, 24vw);
  height: 2px;
  margin: 0 0 16px;
  border-radius: 2px;
  background: linear-gradient(
    90deg,
    var(--app-accent),
    color-mix(in srgb, var(--app-accent-2) 58%, var(--app-accent))
  );
}

.lb-masthead__title {
  margin: 0 0 10px;
  font-family: var(--app-font-display);
  font-size: clamp(1.85rem, 4.5vw, 2.35rem);
  font-weight: 600;
  letter-spacing: -0.04em;
  color: var(--color-text-1);
  line-height: 1.1;
}

.lb-masthead__sub {
  margin: 0;
  max-width: 52ch;
  font-size: 14px;
  line-height: 1.65;
  color: var(--color-text-2);
}

.lb-masthead__link {
  color: var(--app-accent);
  font-weight: 700;
  text-decoration: none;
  border-bottom: 1px solid color-mix(in srgb, var(--app-accent) 40%, transparent);
}

.lb-masthead__link:hover {
  border-bottom-color: var(--app-accent);
}

.lb-masthead__stat {
  padding: 18px 22px;
  border-radius: var(--lb-radius-lg);
  border: 1px solid var(--color-border-2);
  background: color-mix(in srgb, var(--color-bg-2) 92%, #fff 8%);
  box-shadow: 0 12px 36px color-mix(in srgb, var(--color-text-1) 6%, transparent);
  text-align: right;
}

.lb-masthead__stat-num {
  display: block;
  font-family: ui-monospace, Menlo, sans-serif;
  font-size: 2rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--app-accent);
  line-height: 1;
}

.lb-masthead__stat-lab {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-3);
}

.lb-split {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 22px;
  align-items: start;
}

@media (min-width: 1024px) {
  .lb-split {
    grid-template-columns: minmax(0, 1fr) minmax(320px, 400px);
    gap: 26px;
  }
}

.lb-panel {
  border-radius: var(--lb-radius-xl);
  border: 1px solid var(--color-border-2);
  background: color-mix(in srgb, var(--color-bg-2) 96%, #fff 4%);
  box-shadow:
    0 20px 50px color-mix(in srgb, var(--color-text-1) 7%, transparent),
    0 0 0 1px color-mix(in srgb, #ffffff 55%, transparent) inset;
  overflow: hidden;
}

.lb-panel--rank {
  padding: 20px 20px 22px;
}

.lb-panel--side {
  padding: 18px 18px 20px;
}

.lb-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}

.lb-tab {
  border: none;
  cursor: pointer;
  padding: 10px 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  background: transparent;
  color: var(--color-text-3);
  transition: background 0.2s ease, color 0.2s ease;
}

.lb-tab--active {
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--app-accent-2) 22%, var(--color-bg-1)),
    color-mix(in srgb, var(--app-accent) 14%, var(--color-bg-1))
  );
  color: var(--color-text-1);
  box-shadow: 0 6px 18px color-mix(in srgb, var(--app-accent-2) 18%, transparent);
}

.lb-tab--ghost:hover {
  color: var(--color-text-1);
  background: var(--color-fill-2);
}

.lb-spin {
  width: 100%;
  min-height: 200px;
}

.lb-empty {
  text-align: center;
  padding: 48px 20px;
}

.lb-empty__icon {
  font-size: 48px;
  color: color-mix(in srgb, var(--app-accent) 45%, var(--color-text-3));
  opacity: 0.85;
  margin-bottom: 12px;
}

.lb-empty__title {
  margin: 0 0 8px;
  font-size: 17px;
  font-weight: 700;
  color: var(--color-text-1);
}

.lb-empty__desc {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-3);
}

/* —— 领奖台 —— */
.lb-podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: clamp(8px, 2vw, 18px);
  margin-bottom: 22px;
  padding: 8px 4px 4px;
}

.lb-podium__slot {
  flex: 1;
  max-width: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.lb-podium__slot--1 {
  order: 2;
  z-index: 2;
}

.lb-podium__slot--2 {
  order: 1;
}

.lb-podium__slot--3 {
  order: 3;
}

.lb-podium__pedestal {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 12px 18px;
  border-radius: var(--lb-radius-lg);
  border: 1px solid var(--color-border-2);
  background: linear-gradient(180deg, color-mix(in srgb, var(--color-bg-1) 95%, #fff 5%) 0%, var(--color-bg-2) 100%);
  box-shadow: 0 14px 32px color-mix(in srgb, var(--color-text-1) 8%, transparent);
  position: relative;
}

.lb-podium__slot--1 .lb-podium__pedestal {
  padding-top: 22px;
  padding-bottom: 24px;
  min-height: 220px;
  border-color: color-mix(in srgb, rgb(var(--gold-6)) 35%, var(--color-border-2));
  box-shadow:
    0 20px 44px color-mix(in srgb, rgb(var(--gold-6)) 14%, transparent),
    0 0 0 1px color-mix(in srgb, #ffffff 70%, transparent) inset;
}

.lb-podium__slot--2 .lb-podium__pedestal {
  min-height: 188px;
  opacity: 0.96;
}

.lb-podium__slot--3 .lb-podium__pedestal {
  min-height: 172px;
  opacity: 0.94;
}

.lb-podium__crown {
  font-size: 22px;
  line-height: 1;
  margin-bottom: 8px;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.12));
}

.lb-podium__crown--1 {
  font-size: 26px;
}

.lb-podium__avatar-wrap {
  margin-bottom: 10px;
}

.lb-podium__avatar {
  border: 3px solid color-mix(in srgb, var(--app-accent) 28%, var(--color-border-2));
  box-shadow: 0 8px 22px color-mix(in srgb, var(--color-text-1) 10%, transparent);
}

.lb-podium__slot--1 .lb-podium__avatar {
  border-width: 4px;
  border-color: color-mix(in srgb, rgb(var(--gold-6)) 55%, var(--color-border-2));
}

.lb-podium__fallback {
  font-size: 22px;
  font-weight: 800;
  color: var(--app-accent);
}

.lb-podium__name {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 800;
  color: var(--color-text-1);
  text-align: center;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.lb-podium__score {
  margin: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.lb-podium__score-label {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-3);
  letter-spacing: 0.06em;
}

.lb-podium__score-val {
  font-family: ui-monospace, Menlo, sans-serif;
  font-size: 1.25rem;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-1);
}

.lb-podium__badge-rank {
  position: absolute;
  top: 10px;
  right: 10px;
  font-size: 11px;
  font-weight: 800;
  color: var(--color-text-3);
  opacity: 0.85;
}

/* —— 列表卡片 —— */
.lb-row-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.lb-row-card {
  display: grid;
  grid-template-columns: 44px 48px minmax(0, 1fr) auto;
  gap: 12px 14px;
  align-items: center;
  padding: 14px 16px;
  border-radius: var(--lb-radius-md);
  border: 1px solid var(--color-border-2);
  background: color-mix(in srgb, var(--color-bg-1) 88%, var(--color-bg-2) 12%);
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.lb-row-card:hover {
  border-color: color-mix(in srgb, var(--app-accent) 28%, var(--color-border-2));
  box-shadow: 0 10px 26px color-mix(in srgb, var(--app-accent) 8%, transparent);
  transform: translateY(-2px);
}

.lb-row-card__order {
  font-family: ui-monospace, Menlo, sans-serif;
  font-size: 15px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-2);
  text-align: center;
}

.lb-row-card__avatar {
  border: 2px solid var(--color-border-2);
}

.lb-row-card__fallback {
  font-size: 15px;
  font-weight: 700;
  color: var(--app-accent);
}

.lb-row-card__main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.lb-row-card__name {
  font-weight: 800;
  font-size: 14px;
  color: var(--color-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lb-row-card__id {
  font-size: 11px;
  color: var(--color-text-3);
  font-variant-numeric: tabular-nums;
}

.lb-row-card__stats {
  display: flex;
  gap: 14px;
  margin: 0;
}

.lb-row-card__stats dt {
  margin: 0;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--color-text-3);
  text-transform: uppercase;
}

.lb-row-card__stats dd {
  margin: 2px 0 0;
  font-family: ui-monospace, Menlo, sans-serif;
  font-size: 13px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--color-text-1);
}

@media (max-width: 560px) {
  .lb-row-card {
    grid-template-columns: 40px 44px 1fr;
  }

  .lb-row-card__stats {
    grid-column: 1 / -1;
    justify-content: flex-start;
    padding-top: 4px;
    border-top: 1px dashed var(--color-border-3);
  }
}

.lb-pager {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--color-border-2);
}

.lb-pager__meta {
  font-size: 13px;
  color: var(--color-text-3);
}

/* —— 右侧 —— */
.lb-side-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 16px;
  padding: 5px;
  border-radius: 999px;
  background: var(--color-fill-2);
}

.lb-side-tab {
  flex: 1;
  border: none;
  cursor: pointer;
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text-3);
  background: transparent;
  transition: background 0.2s ease, color 0.2s ease;
}

.lb-side-tab--on {
  background: var(--color-bg-2);
  color: var(--color-text-1);
  box-shadow: 0 4px 14px color-mix(in srgb, var(--color-text-1) 8%, transparent);
}

.lb-side-body {
  min-height: 200px;
}

.lb-side-empty {
  margin: 24px 0;
  text-align: center;
  font-size: 14px;
  color: var(--color-text-3);
}

.lb-contest-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lb-contest-card {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto auto;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  border-radius: var(--lb-radius-md);
  border: 1px solid var(--color-border-2);
  background: color-mix(in srgb, var(--color-bg-1) 94%, transparent);
  text-decoration: none;
  color: inherit;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.lb-contest-card:hover {
  border-color: color-mix(in srgb, var(--app-accent) 35%, var(--color-border-2));
  box-shadow: 0 12px 28px color-mix(in srgb, var(--app-accent) 10%, transparent);
  transform: translateY(-2px);
}

.lb-contest-card__thumb {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  background:
    linear-gradient(
      145deg,
      hsl(calc(var(--hue) + 18), 72%, 62%) 0%,
      hsl(var(--hue), 58%, 52%) 45%,
      hsl(calc(var(--hue) + 40), 45%, 38%) 100%
    );
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.35),
    0 8px 18px rgba(0, 0, 0, 0.12);
  position: relative;
  overflow: hidden;
}

.lb-contest-card__thumb::after {
  content: '';
  position: absolute;
  inset: 18% 22%;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.55), rgba(255, 255, 255, 0.08));
  opacity: 0.65;
  transform: rotate(-12deg);
}

.lb-contest-card__title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 800;
  color: var(--color-text-1);
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.lb-contest-card__time {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-3);
}

.lb-contest-card__phase {
  font-size: 11px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--color-fill-3);
  color: var(--color-text-3);
  white-space: nowrap;
}

.lb-contest-card__go {
  font-size: 13px;
  font-weight: 700;
  color: color-mix(in srgb, var(--app-accent) 85%, #4338ca);
}

.lb-side-more {
  display: block;
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--app-accent);
  text-decoration: none;
}

.lb-side-more:hover {
  text-decoration: underline;
}

.lb-help {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lb-help__card {
  padding: 14px 16px;
  border-radius: var(--lb-radius-md);
  border: 1px solid var(--color-border-2);
  background: color-mix(in srgb, var(--color-bg-1) 92%, transparent);
}

.lb-help__h {
  margin: 0 0 8px;
  font-size: 13px;
  font-weight: 800;
  color: var(--color-text-1);
}

.lb-help__p {
  margin: 0;
  font-size: 13px;
  line-height: 1.65;
  color: var(--color-text-2);
}

/* 暗黑：与首页首屏 / 功能入口高亮一致，见 @/styles/oj-spotlight-pages.css */
</style>
