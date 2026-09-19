<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { ContestControllerService, Service } from '@generated'
import type { BlogPostListItemVO, ContestListItemVO, QuestionAdminListItemVO } from '@generated'
import { isResultSuccess } from '@/api/result'
import { mapApiError } from '@/api/mapApiError'
import { getBackendErrorMessage } from '@/api/httpError'

const route = useRoute()
const router = useRouter()

const keyword = ref('')
const submitted = ref('')
const loading = ref(false)

const problems = ref<QuestionAdminListItemVO[]>([])
const blogs = ref<BlogPostListItemVO[]>([])
const contests = ref<ContestListItemVO[]>([])

const hasQuery = computed(() => submitted.value.trim().length > 0)
const isEmpty = computed(
  () =>
    hasQuery.value &&
    !loading.value &&
    !problems.value.length &&
    !blogs.value.length &&
    !contests.value.length,
)

function contestPhaseLabel(phase?: string): string {
  const p = (phase ?? '').toLowerCase()
  if (p === 'running' || p === 'ongoing') return '进行中'
  if (p === 'upcoming' || p === 'pending') return '未开始'
  if (p === 'ended' || p === 'finished') return '已结束'
  return phase?.trim() || ''
}

async function runSearch() {
  const q = keyword.value.trim()
  submitted.value = q
  if (!q) {
    problems.value = []
    blogs.value = []
    contests.value = []
    return
  }

  loading.value = true
  try {
    const [probRes, blogRes, contestRes] = await Promise.all([
      Service.pageQuestions({ current: 1, pageSize: 8, title: q }),
      Service.pagePublic({ current: 1, pageSize: 8, titleKeyword: q }),
      ContestControllerService.listContests(),
    ])

    if (isResultSuccess(probRes.code)) {
      problems.value = probRes.data?.records ?? []
    } else {
      problems.value = []
    }

    if (isResultSuccess(blogRes.code)) {
      blogs.value = blogRes.data?.records ?? []
    } else {
      blogs.value = []
    }

    if (isResultSuccess(contestRes.code)) {
      const lower = q.toLowerCase()
      contests.value = (contestRes.data ?? []).filter((c) =>
        (c.title ?? '').toLowerCase().includes(lower),
      ).slice(0, 8)
    } else {
      contests.value = []
    }
  } catch (e) {
    Message.error(mapApiError(e, getBackendErrorMessage(e, '搜索失败')))
  } finally {
    loading.value = false
  }
}

function onSubmit() {
  const q = keyword.value.trim()
  void router.replace({ name: 'search', query: q ? { q } : {} })
  void runSearch()
}

function syncFromRoute() {
  const q = typeof route.query.q === 'string' ? route.query.q.trim() : ''
  keyword.value = q
  submitted.value = q
  if (q) void runSearch()
  else {
    problems.value = []
    blogs.value = []
    contests.value = []
  }
}

watch(
  () => route.query.q,
  () => {
    syncFromRoute()
  },
)

onMounted(() => {
  syncFromRoute()
})
</script>

<template>
  <div class="global-search">
    <header class="global-search__hero">
      <p class="global-search__eyebrow">Search</p>
      <h1>全站搜索</h1>
      <p class="global-search__lead">在题库、博客与比赛中快速定位内容</p>
      <div class="global-search__bar">
        <a-input-search
          v-model="keyword"
          allow-clear
          size="large"
          placeholder="输入标题关键词，如：二分、动态规划、新手赛"
          search-button
          :loading="loading"
          @search="onSubmit"
          @press-enter="onSubmit"
        />
      </div>
    </header>

    <p v-if="!hasQuery" class="global-search__hint">输入关键词后回车或点击搜索</p>

    <p v-else-if="loading" class="global-search__hint">正在搜索「{{ submitted }}」…</p>

    <p v-else-if="isEmpty" class="global-search__hint">未找到与「{{ submitted }}」相关的内容</p>

    <div v-else class="global-search__sections">
      <section v-if="problems.length" class="search-section">
        <div class="search-section__head">
          <h2>题库</h2>
          <RouterLink :to="{ name: 'problems' }" class="search-section__more">进入题库</RouterLink>
        </div>
        <ul class="search-list">
          <li v-for="p in problems" :key="p.id">
            <RouterLink :to="{ name: 'problem-solve', params: { id: String(p.id) } }" class="search-item">
              <strong>{{ p.title || '未命名题目' }}</strong>
              <span v-if="p.tags?.length" class="search-item__meta">{{ p.tags.slice(0, 4).join(' · ') }}</span>
            </RouterLink>
          </li>
        </ul>
      </section>

      <section v-if="contests.length" class="search-section">
        <div class="search-section__head">
          <h2>比赛</h2>
          <RouterLink :to="{ name: 'contests' }" class="search-section__more">进入比赛</RouterLink>
        </div>
        <ul class="search-list">
          <li v-for="c in contests" :key="c.id">
            <RouterLink
              :to="{ name: 'contest-rank-detail', params: { contestId: String(c.id) } }"
              class="search-item"
            >
              <strong>{{ c.title || '未命名比赛' }}</strong>
              <span v-if="contestPhaseLabel(c.phase)" class="search-item__meta">{{
                contestPhaseLabel(c.phase)
              }}</span>
            </RouterLink>
          </li>
        </ul>
      </section>

      <section v-if="blogs.length" class="search-section">
        <div class="search-section__head">
          <h2>博客</h2>
          <RouterLink :to="{ name: 'blog' }" class="search-section__more">进入博客</RouterLink>
        </div>
        <ul class="search-list">
          <li v-for="b in blogs" :key="b.id">
            <RouterLink :to="{ name: 'blog-detail', params: { id: String(b.id) } }" class="search-item">
              <strong>{{ b.title || '未命名文章' }}</strong>
              <span v-if="b.tags?.length" class="search-item__meta">{{ b.tags.slice(0, 3).join(' · ') }}</span>
            </RouterLink>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>

<style scoped>
.global-search {
  max-width: var(--app-page-width);
  margin: 0 auto;
  padding: 28px 20px 56px;
}

.global-search__hero {
  margin-bottom: 28px;
}

.global-search__eyebrow {
  color: var(--app-accent);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  font-size: 12px;
  font-weight: 700;
  margin: 0 0 10px;
}

.global-search h1 {
  font-family: var(--app-font-display);
  margin: 0;
  font-size: clamp(28px, 4vw, 42px);
  color: var(--app-text);
}

.global-search__lead {
  margin: 10px 0 0;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.global-search__bar {
  margin-top: 22px;
  max-width: 640px;
}

.global-search__hint {
  color: var(--app-text-muted);
  margin: 0;
  padding: 24px 0;
}

.global-search__sections {
  display: grid;
  gap: 28px;
}

.search-section {
  border: 1px solid var(--app-border);
  border-radius: 20px;
  background: var(--app-surface);
  padding: 20px 22px;
  box-shadow: var(--app-card-shadow);
}

.search-section__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.search-section__head h2 {
  margin: 0;
  font-size: 18px;
  color: var(--app-text);
}

.search-section__more {
  font-size: 13px;
  color: var(--app-accent);
  text-decoration: none;
}

.search-section__more:hover {
  text-decoration: underline;
}

.search-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 8px;
}

.search-item {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 12px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid transparent;
  text-decoration: none;
  color: var(--app-text);
  transition: background 0.2s, border-color 0.2s;
}

.search-item:hover {
  background: var(--app-surface-soft);
  border-color: var(--app-border);
}

.search-item strong {
  font-weight: 600;
}

.search-item__meta {
  font-size: 13px;
  color: var(--app-text-muted);
}

@media (max-width: 640px) {
  .global-search {
    padding: 20px 14px 44px;
  }
}
</style>
