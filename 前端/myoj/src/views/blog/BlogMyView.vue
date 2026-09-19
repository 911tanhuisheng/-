<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import { Service } from '@generated'
import type { BlogPostListItemVO } from '@generated'
import { mapApiError } from '@/api/mapApiError'
import { isResultSuccess } from '@/api/result'
import BlogCyberShell from '@/components/blog/BlogCyberShell.vue'

const STATUS_DRAFT = 0
const STATUS_PUBLISHED = 1

const router = useRouter()

type TabKey = 'all' | 'draft' | 'live'
const tab = ref<TabKey>('all')

const loading = ref(false)
const rows = ref<BlogPostListItemVO[]>([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

/** 正在删除的文章 id（禁用按钮防重复） */
const deleteBusyId = ref<string | null>(null)

function statusFilter(): number | undefined {
  if (tab.value === 'draft') return STATUS_DRAFT
  if (tab.value === 'live') return STATUS_PUBLISHED
  return undefined
}

function statusLabel(s?: number): string {
  if (s === STATUS_DRAFT) return '草稿'
  if (s === STATUS_PUBLISHED) return '已发布'
  return '—'
}

function formatTime(raw?: string): string {
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return raw
  return d.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

async function loadPage() {
  loading.value = true
  try {
    const st = statusFilter()
    const res = await Service.pageMine({
      current: current.value,
      pageSize: pageSize.value,
      status: st,
    })
    if (!isResultSuccess(res.code)) {
      Message.error(res.message || '加载失败')
      return
    }
    const page = res.data
    rows.value = page?.records ?? []
    total.value = page?.total ?? 0
    current.value = page?.current ?? current.value
    pageSize.value = page?.size ?? pageSize.value
  } catch (e) {
    Message.error(mapApiError(e, '加载失败'))
  } finally {
    loading.value = false
  }
}

function setTab(k: TabKey) {
  tab.value = k
  current.value = 1
  void loadPage()
}

function goPrev() {
  if (current.value <= 1) return
  current.value -= 1
  void loadPage()
}

function goNext() {
  if (current.value >= totalPages.value) return
  current.value += 1
  void loadPage()
}

function confirmDelete(item: BlogPostListItemVO) {
  const id = item.id?.trim()
  if (!id || deleteBusyId.value) return
  const title = (item.title ?? '').trim() || '这篇文章'
  Modal.confirm({
    title: '删除文章',
    content: `确定删除「${title}」吗？删除后无法恢复，评论与点赞也会一并清除。`,
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      deleteBusyId.value = id
      try {
        const res = await Service.delete({ id })
        if (!isResultSuccess(res.code)) {
          Message.error(res.message || '删除失败')
          return
        }
        Message.success('已删除')
        if (rows.value.length <= 1 && current.value > 1) {
          current.value -= 1
        }
        void loadPage()
      } catch (e) {
        Message.error(mapApiError(e, '删除失败'))
      } finally {
        deleteBusyId.value = null
      }
    },
  })
}

onMounted(() => {
  void loadPage()
})
</script>

<template>
  <BlogCyberShell>
    <div class="blog-mine">
    <section class="blog-mine__hero panel">
      <div>
        <p class="blog-mine__eyebrow">My Space</p>
        <h1 class="blog-mine__title">我的博客</h1>
        <p class="blog-mine__lead">管理草稿与已发布内容，继续打磨每一篇笔记。</p>
      </div>
      <div class="blog-mine__hero-actions">
        <button type="button" class="pill pill--primary blog-magnetic" @click="router.push({ name: 'blog-write' })">
          新建文章
        </button>
        <RouterLink :to="{ name: 'blog' }" class="pill pill--ghost blog-magnetic">逛博客首页</RouterLink>
      </div>
    </section>

    <section class="blog-mine__shell panel">
      <div class="tabs">
        <button type="button" class="tab blog-magnetic" :class="{ 'tab--on': tab === 'all' }" @click="setTab('all')">全部</button>
        <button type="button" class="tab blog-magnetic" :class="{ 'tab--on': tab === 'draft' }" @click="setTab('draft')">
          草稿
        </button>
        <button type="button" class="tab blog-magnetic" :class="{ 'tab--on': tab === 'live' }" @click="setTab('live')">
          已发布
        </button>
      </div>

      <p class="blog-mine__meta">共 {{ total }} 篇 · 第 {{ current }} / {{ totalPages }} 页</p>

      <div class="table-wrap">
        <table class="tbl">
          <thead>
            <tr>
              <th>标题</th>
              <th>状态</th>
              <th>更新</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="loading">
              <td colspan="4" class="tbl__empty">加载中…</td>
            </tr>
            <tr v-else-if="!rows.length">
              <td colspan="4" class="tbl__empty">暂无文章，去写一篇吧</td>
            </tr>
            <tr v-for="item in rows" v-else :key="item.id">
              <td class="tbl__title">{{ item.title }}</td>
              <td>
                <span class="pill-status" :class="item.status === STATUS_PUBLISHED ? 'pill-status--live' : 'pill-status--draft'">
                  {{ statusLabel(item.status) }}
                </span>
              </td>
              <td class="tbl__muted">{{ formatTime(item.createTime || item.publishedAt) }}</td>
              <td class="tbl__ops">
                <RouterLink v-if="item.id" :to="{ name: 'blog-edit', params: { id: item.id } }" class="link">编辑</RouterLink>
                <RouterLink
                  v-if="item.id && item.status === STATUS_PUBLISHED"
                  :to="{ name: 'blog-detail', params: { id: item.id } }"
                  class="link"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  查看
                </RouterLink>
                <button
                  v-if="item.id"
                  type="button"
                  class="link link--danger"
                  :disabled="deleteBusyId === item.id"
                  @click="confirmDelete(item)"
                >
                  {{ deleteBusyId === item.id ? '删除中…' : '删除' }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pager">
        <a-button class="pager-btn blog-magnetic" :disabled="current <= 1 || loading" @click="goPrev">上一页</a-button>
        <span>{{ current }} / {{ totalPages }}</span>
        <a-button class="pager-btn blog-magnetic" :disabled="current >= totalPages || loading" @click="goNext">下一页</a-button>
      </div>
    </section>
    </div>
  </BlogCyberShell>
</template>

<style scoped>
.blog-mine {
  position: relative;
  isolation: isolate;
  width: min(1040px, calc(100vw - 28px));
  margin: 0 auto;
  padding: 28px 12px 72px;
}
.panel {
  border: 1px solid var(--app-border);
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--app-surface) 93%, transparent),
    var(--app-surface-strong)
  );
  box-shadow: var(--app-card-shadow);
  border-radius: var(--app-radius-xl);
}

.blog-mine__hero {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-end;
  padding: 28px 30px;
  margin-bottom: 22px;
}
.blog-mine__eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--app-text-subtle);
  font-weight: 800;
}
.blog-mine__title {
  margin: 0 0 8px;
  font-size: clamp(24px, 3vw, 30px);
  font-weight: 900;
  letter-spacing: -0.02em;
  color: var(--app-text);
}
.blog-mine__lead {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 15px;
  line-height: 1.55;
  max-width: 520px;
}
.blog-mine__hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  text-decoration: none;
  transition:
    transform 0.15s ease,
    filter 0.15s ease;
}
.pill--primary {
  color: #fff;
  background: linear-gradient(135deg, #0f8c7a, #14b8a6);
  box-shadow: 0 12px 28px rgba(15, 140, 122, 0.22);
}
.pill--primary:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
}
.pill--ghost {
  color: var(--app-accent);
  border: 1px solid color-mix(in srgb, var(--app-accent) 35%, var(--app-border));
  background: color-mix(in srgb, var(--app-accent) 6%, transparent);
}
.pill--ghost:hover {
  transform: translateY(-1px);
}

.blog-mine__shell {
  padding: 22px 24px 28px;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;
}
.tab {
  padding: 8px 18px;
  border-radius: 999px;
  border: 1px solid var(--app-border);
  background: transparent;
  color: var(--app-text-muted);
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.15s ease,
    color 0.15s ease,
    border-color 0.15s ease;
}
.tab--on {
  border-color: color-mix(in srgb, var(--app-accent) 45%, transparent);
  background: color-mix(in srgb, var(--app-accent) 12%, transparent);
  color: var(--app-accent);
}

.blog-mine__meta {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--app-text-subtle);
}

.table-wrap {
  overflow-x: auto;
  border-radius: 16px;
  border: 1px solid var(--color-border-2);
}
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.tbl th,
.tbl td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid var(--color-border-2);
}
.tbl thead th {
  font-weight: 800;
  color: var(--color-text-2);
  background: color-mix(in srgb, var(--color-fill-2) 75%, transparent);
}
.tbl__title {
  font-weight: 700;
  color: var(--app-text);
  max-width: 360px;
}
.tbl__muted {
  color: var(--app-text-subtle);
  font-size: 13px;
  white-space: nowrap;
}
.tbl__empty {
  text-align: center;
  padding: 36px !important;
  color: var(--app-text-subtle);
}
.tbl__ops {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.link {
  color: var(--app-accent);
  font-weight: 700;
  text-decoration: none;
  font-size: 13px;
}
.link:hover {
  text-decoration: underline;
}

button.link {
  border: none;
  background: none;
  padding: 0;
  cursor: pointer;
  font: inherit;
}
.link:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  text-decoration: none;
}
.link--danger {
  color: rgb(var(--red-6));
}
.link--danger:hover:not(:disabled) {
  color: rgb(var(--red-7));
  text-decoration: underline;
}
html[data-theme='dark'] .link--danger {
  color: #f87171;
}
html[data-theme='dark'] .link--danger:hover:not(:disabled) {
  color: #fca5a5;
}

.pill-status {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}
.pill-status--draft {
  background: color-mix(in srgb, rgb(var(--orange-6)) 14%, transparent);
  color: rgb(var(--orange-6));
}
.pill-status--live {
  background: color-mix(in srgb, rgb(var(--arcoblue-6)) 14%, transparent);
  color: rgb(var(--arcoblue-6));
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 22px;
  color: var(--app-text-muted);
}
.pager-btn {
  border-radius: 12px;
}

html[data-theme='dark'] .tbl thead th {
  background: rgba(34, 55, 84, 0.82);
}
</style>
