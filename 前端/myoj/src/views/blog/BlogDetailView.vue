<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { Message } from '@arco-design/web-vue'
import { Service } from '@generated'
import type { BlogPostDetailVO } from '@generated'
import { mapApiError } from '@/api/mapApiError'
import { isResultSuccess } from '@/api/result'
import { useAuthStore } from '@/stores/auth'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import BlogCyberShell from '@/components/blog/BlogCyberShell.vue'
import BlogMarkdown from '@/components/blog/BlogMarkdown.vue'
import BlogCommentSection from '@/components/blog/BlogCommentSection.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const post = ref<BlogPostDetailVO | null>(null)
const likeBusy = ref(false)

const postId = computed(() => String(route.params.id || '').trim())

function formatDateTime(raw?: string): string {
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return raw
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

async function loadDetail() {
  const id = postId.value
  if (!id) {
    Message.warning('缺少文章 id')
    loading.value = false
    return
  }
  loading.value = true
  post.value = null
  try {
    const res = await Service.getPublic(id)
    if (!isResultSuccess(res.code) || !res.data) {
      Message.error(res.message || '文章不存在或未发布')
      post.value = null
      return
    }
    post.value = res.data
  } catch (e) {
    Message.error(mapApiError(e, '加载失败'))
    post.value = null
  } finally {
    loading.value = false
  }
}

async function onToggleLike() {
  if (!auth.isLoggedIn) {
    void router.push({ path: '/', query: { openAuth: '1' } })
    return
  }
  const id = postId.value
  if (!id || likeBusy.value || !post.value) return
  likeBusy.value = true
  try {
    const res = await Service.toggleLike({ postId: id })
    if (!isResultSuccess(res.code) || !res.data) {
      Message.error(res.message || '操作失败')
      return
    }
    post.value = {
      ...post.value,
      liked: res.data.liked,
      likeCount: res.data.likeCount ?? post.value.likeCount,
    }
  } catch (e) {
    Message.error(mapApiError(e, '操作失败'))
  } finally {
    likeBusy.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    void loadDetail()
  },
)

onMounted(() => {
  void loadDetail()
})
</script>

<template>
  <BlogCyberShell>
    <div class="blog-read">
    <div class="blog-read__wrap">
      <nav class="blog-read__crumb">
        <RouterLink :to="{ name: 'blog' }" class="blog-read__crumb-link blog-magnetic">博客</RouterLink>
        <span class="blog-read__crumb-sep">/</span>
        <span class="blog-read__crumb-current">{{ post?.title || '文章' }}</span>
      </nav>

      <div v-if="loading" class="blog-read__panel panel blog-read__loading">
        <p>载入叙事中…</p>
      </div>

      <article v-else-if="post" class="blog-read__panel panel">
        <header class="blog-read__head">
          <h1 class="blog-read__title">{{ post.title }}</h1>
          <div class="blog-read__meta">
            <div class="blog-read__author">
              <img
                v-if="post.authorAvatar && resolveMediaUrl(post.authorAvatar)"
                :src="resolveMediaUrl(post.authorAvatar)"
                alt=""
                class="blog-read__avatar"
              />
              <span v-else class="blog-read__avatar blog-read__avatar--letter">{{
                (post.authorName || 'U').slice(0, 1)
              }}</span>
              <div>
                <p class="blog-read__name">{{ post.authorName || '作者' }}</p>
                <p class="blog-read__sub">
                  <time :datetime="post.publishedAt">{{ formatDateTime(post.publishedAt) }}</time>
                  <span class="blog-read__dot">·</span>
                  <span>阅读 {{ post.viewCount ?? 0 }}</span>
                </p>
              </div>
            </div>
            <div class="blog-read__actions">
              <button
                type="button"
                class="like-btn blog-magnetic"
                :class="{ 'like-btn--on': post.liked }"
                :disabled="likeBusy"
                @click="onToggleLike"
              >
                <span aria-hidden="true">{{ post.liked ? '♥' : '♡' }}</span>
                {{ post.liked ? '已赞' : '点赞' }}
                <span class="like-btn__cnt">{{ post.likeCount ?? 0 }}</span>
              </button>
            </div>
          </div>
          <div v-if="post.tags?.length" class="blog-read__tags">
            <span v-for="t in post.tags" :key="t" class="blog-read__tag">{{ t }}</span>
          </div>
        </header>

        <div v-if="post.coverUrl && resolveMediaUrl(post.coverUrl)" class="blog-read__cover-wrap">
          <img :src="resolveMediaUrl(post.coverUrl)" alt="" class="blog-read__cover" />
        </div>

        <p v-if="post.summary" class="blog-read__summary">{{ post.summary }}</p>

        <div class="blog-read__content">
          <BlogMarkdown :source="post.content ?? ''" />
        </div>

        <BlogCommentSection v-if="post.id" :post-id="String(post.id)" />
      </article>

      <div v-else class="blog-read__panel panel blog-read__empty">
        <p>未能加载这篇文章。</p>
        <RouterLink :to="{ name: 'blog' }" class="blog-read__back blog-magnetic">返回博客首页</RouterLink>
      </div>
    </div>
    </div>
  </BlogCyberShell>
</template>

<style scoped>
.blog-read {
  position: relative;
  isolation: isolate;
  min-height: calc(100vh - 120px);
  padding: 28px 16px 80px;
}
.blog-read__wrap {
  width: min(820px, 100%);
  margin: 0 auto;
}

.blog-read__crumb {
  font-size: 13px;
  color: var(--app-text-subtle);
  margin-bottom: 18px;
}
.blog-read__crumb-link {
  color: var(--app-accent);
  text-decoration: none;
  font-weight: 600;
}
.blog-read__crumb-link:hover {
  text-decoration: underline;
}
.blog-read__crumb-sep {
  margin: 0 8px;
  opacity: 0.45;
}
.blog-read__crumb-current {
  color: var(--app-text-muted);
}

.panel {
  border: 1px solid var(--app-border);
  background: linear-gradient(
    160deg,
    color-mix(in srgb, var(--app-surface) 94%, transparent),
    var(--app-surface-strong)
  );
  box-shadow: var(--app-card-shadow);
  border-radius: var(--app-radius-xl);
}

.blog-read__panel {
  padding: clamp(28px, 3vw, 40px) clamp(22px, 3vw, 42px) clamp(36px, 4vw, 52px);
}
.blog-read__loading,
.blog-read__empty {
  text-align: center;
  color: var(--app-text-muted);
}

.blog-read__title {
  margin: 0 0 clamp(18px, 2vw, 22px);
  font-size: clamp(26px, 4vw, 34px);
  font-weight: 900;
  letter-spacing: -0.025em;
  line-height: 1.22;
  color: var(--app-text);
}

.blog-read__meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.blog-read__author {
  display: flex;
  gap: 14px;
  align-items: center;
}
.blog-read__avatar {
  width: 48px;
  height: 48px;
  border-radius: 999px;
  object-fit: cover;
  border: 2px solid color-mix(in srgb, var(--app-accent) 35%, transparent);
  box-shadow: 0 6px 18px rgba(15, 140, 122, 0.18);
}
.blog-read__avatar--letter {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #0f766e, #14b8a6);
  color: #fff;
  font-weight: 800;
  font-size: 18px;
}
.blog-read__name {
  margin: 0;
  font-weight: 700;
  color: var(--app-text);
}
.blog-read__sub {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--app-text-subtle);
}
.blog-read__dot {
  margin: 0 6px;
  opacity: 0.45;
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 999px;
  border: 1px solid var(--app-border-strong);
  background: color-mix(in srgb, var(--app-accent) 8%, transparent);
  color: var(--app-text);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}
.like-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--app-accent) 45%, transparent);
  box-shadow: 0 10px 24px rgba(15, 140, 122, 0.15);
}
.like-btn:disabled {
  opacity: 0.55;
  cursor: wait;
}
.like-btn--on {
  border-color: color-mix(in srgb, #fb7185 55%, transparent);
  background: color-mix(in srgb, #fb7185 12%, transparent);
  color: #e11d48;
}
.like-btn__cnt {
  font-variant-numeric: tabular-nums;
  opacity: 0.85;
}

.blog-read__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.blog-read__tag {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--color-fill-2);
  color: var(--app-text-muted);
  font-weight: 600;
}

.blog-read__cover-wrap {
  margin: 28px 0 32px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid var(--app-border);
}
.blog-read__cover {
  width: 100%;
  display: block;
  max-height: 420px;
  object-fit: cover;
}

.blog-read__summary {
  margin: 0 0 clamp(28px, 3vw, 36px);
  padding: clamp(18px, 2vw, 22px) clamp(20px, 2.5vw, 26px);
  border-radius: 18px;
  border-left: 4px solid color-mix(in srgb, var(--app-accent) 65%, transparent 35%);
  background: color-mix(in srgb, var(--app-accent) 6%, transparent);
  color: var(--app-text-muted);
  font-size: clamp(15px, 0.4vw + 14.5px, 16.5px);
  line-height: 1.78;
  letter-spacing: 0.012em;
}

.blog-read__content {
  margin-top: 0.35rem;
}

.blog-read__back {
  display: inline-block;
  margin-top: 16px;
  color: var(--app-accent);
  font-weight: 700;
}

:global(html[data-theme='dark']) .like-btn--on {
  color: #fda4af;
  border-color: rgba(251, 113, 133, 0.45);
}
</style>
