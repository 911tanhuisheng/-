<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import {
  blogCommentAdd,
  blogCommentDelete,
  blogCommentLikeToggle,
  blogCommentPage,
  type BlogCommentVO,
} from '@/api/blogComment'
import { COMMENT_PROFANITY_BANNED_CODE, isResultSuccess } from '@/api/result'
import { mapApiError } from '@/api/mapApiError'
import { useAuthStore } from '@/stores/auth'
import { useBanCountdownTick } from '@/composables/useBanCountdownTick'
import { formatBanCountdown, parseBanUntilMs } from '@/utils/formatBanCountdown'
import ModerationBanNotice from '@/components/moderation/ModerationBanNotice.vue'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'
import { parseCommentSegments } from '@/utils/parseCommentSegments'

const props = defineProps<{
  postId: string
}>()

const router = useRouter()
const auth = useAuthStore()

const sort = ref<'hot' | 'latest'>('latest')
const loading = ref(false)
const loadingMore = ref(false)
const submitting = ref(false)
const hotTop = ref<BlogCommentVO[]>([])
const records = ref<BlogCommentVO[]>([])
const totalAll = ref(0)
const pageCurrent = ref(1)
const pagePages = ref(0)

const replyParent = ref<BlogCommentVO | null>(null)
const draft = ref('')
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const emojiOpen = ref(false)

const toastVisible = ref(false)
const toastText = ref('评论已发布')

const likeBusy = ref<Set<string>>(new Set())
const likePulse = ref<Set<string>>(new Set())
const { nowMs: banCountdownNow } = useBanCountdownTick()

const commentBanCountdown = computed(() => {
  if (!auth.isCommentBanned) return ''
  return formatBanCountdown(
    parseBanUntilMs(auth.commentBanUntil ?? auth.banUntil),
    banCountdownNow.value,
  )
})

const cannotPostComment = computed(() => auth.isAccountDisabled || auth.isCommentBanned)
/** 当前悬停评论行 id（操作按钮显示） */
const hoverRowId = ref<string | null>(null)

const QUICK_EMOJIS = ['😀', '😂', '🥰', '😍', '🤔', '👍', '👏', '🔥', '💯', '✨', '🎉', '❤️', '🙏', '😭', '🤣', '🥳', '💪', '🌟', '☕', '📝', '👀', '💬']

const empty = computed(
  () => !loading.value && !hotTop.value.length && !records.value.length,
)

const hasMore = computed(() => pagePages.value > 0 && pageCurrent.value < pagePages.value)

const composerAvatar = computed(() => {
  if (!auth.isLoggedIn || !auth.avatar) return ''
  return resolveMediaUrl(auth.avatar)
})

function segmentsOf(c: BlogCommentVO) {
  return parseCommentSegments(c.content ?? '')
}

function isMyComment(c: BlogCommentVO): boolean {
  if (!auth.isLoggedIn || c.userId == null || String(c.userId).trim() === '') return false
  return String(c.userId).trim() === String(auth.userId ?? '').trim()
}

/** 从列表移除评论（含一级下的回复条数，同步减少 totalAll） */
function removeCommentFromState(c: BlogCommentVO) {
  const idStr = String(c.id)
  for (const list of [hotTop.value, records.value]) {
    const topIdx = list.findIndex((x) => x.id === idStr)
    if (topIdx >= 0) {
      const removed = list[topIdx]!
      const dec = 1 + (removed.replies?.length ?? 0)
      list.splice(topIdx, 1)
      totalAll.value = Math.max(0, totalAll.value - dec)
      return
    }
    for (const row of list) {
      const reps = row.replies
      if (!reps?.length) continue
      const ri = reps.findIndex((r) => r.id === idStr)
      if (ri >= 0) {
        reps.splice(ri, 1)
        totalAll.value = Math.max(0, totalAll.value - 1)
        return
      }
    }
  }
}

function confirmDelete(c: BlogCommentVO) {
  Modal.confirm({
    title: '删除评论',
    content: '删除后不可恢复，确定要删除这条评论吗？',
    okText: '删除',
    cancelText: '取消',
    okButtonProps: { status: 'danger' },
    onOk: async () => {
      try {
        const res = await blogCommentDelete({ id: c.id })
        if (!isResultSuccess(res.code)) {
          Message.error(res.message || '删除失败')
          return
        }
        removeCommentFromState(c)
        Message.success('已删除')
      } catch (e) {
        Message.error(mapApiError(e, '删除失败'))
      }
    },
  })
}

function formatTime(raw?: string): string {
  if (!raw) return ''
  const d = new Date(raw)
  if (Number.isNaN(d.getTime())) return raw
  const now = Date.now()
  const diff = now - d.getTime()
  if (diff < 60_000) return '刚刚'
  if (diff < 3600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86400_000) return `${Math.floor(diff / 3600_000)} 小时前`
  return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

async function fetchPage(reset: boolean) {
  const pid = props.postId?.trim()
  if (!pid) return
  if (reset) {
    loading.value = true
    pageCurrent.value = 1
  } else {
    loadingMore.value = true
  }
  try {
    const res = await blogCommentPage({
      postId: pid,
      current: pageCurrent.value,
      pageSize: 12,
      sort: sort.value,
    })
    if (!isResultSuccess(res.code) || !res.data) {
      Message.error(res.message || '评论加载失败')
      return
    }
    const d = res.data
    totalAll.value = Number(d.totalAll ?? 0)
    pagePages.value = Number(d.pages ?? 0)
    if (reset) {
      hotTop.value = d.hotTop ?? []
      records.value = d.records ?? []
    } else {
      records.value = [...records.value, ...(d.records ?? [])]
    }
  } catch (e) {
    Message.error(mapApiError(e, '评论加载失败'))
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  pageCurrent.value += 1
  void fetchPage(false)
}

function showToast(text: string) {
  toastText.value = text
  toastVisible.value = true
  window.setTimeout(() => {
    toastVisible.value = false
  }, 2400)
}

function mergeNewComment(vo: BlogCommentVO) {
  if (vo.parentId) {
    const pid = String(vo.parentId)
    const attach = (list: BlogCommentVO[]) => {
      for (const row of list) {
        if (String(row.id) === pid) {
          if (!row.replies) row.replies = []
          row.replies.push(vo)
          return true
        }
      }
      return false
    }
    if (!attach(hotTop.value) && !attach(records.value)) {
      void fetchPage(true)
      return
    }
    totalAll.value += 1
    return
  }
  records.value = [vo, ...records.value]
  totalAll.value += 1
}

async function submit() {
  const pid = props.postId?.trim()
  const text = draft.value.trim()
  if (!pid || !text) {
    Message.warning('写点什么再发送吧')
    return
  }
  if (!auth.isLoggedIn) {
    void router.push({ path: '/', query: { openAuth: '1' } })
    return
  }
  if (cannotPostComment.value) {
    Message.warning(
      auth.isAccountDisabled ? '账号已被禁用，暂不可发表评论' : '评论功能已限制，暂不可发表评论',
    )
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const payload: { postId: string; content: string; parentId?: string } = { postId: pid, content: text }
    if (replyParent.value?.id) {
      payload.parentId = replyParent.value.id
    }
    const res = await blogCommentAdd(payload)
    if (res.code === COMMENT_PROFANITY_BANNED_CODE) {
      Message.error(res.message || '评论含违禁内容，评论功能已被限制')
      void auth.syncSessionStatus()
      return
    }
    if (!isResultSuccess(res.code) || !res.data) {
      Message.error(res.message || '发送失败')
      return
    }
    draft.value = ''
    replyParent.value = null
    emojiOpen.value = false
    mergeNewComment(res.data)
    showToast('评论已发布')
    await nextTick()
    textareaRef.value?.focus()
  } catch (e) {
    Message.error(mapApiError(e, '发送失败'))
  } finally {
    submitting.value = false
  }
}

async function toggleLike(c: BlogCommentVO) {
  if (!auth.isLoggedIn) {
    void router.push({ path: '/', query: { openAuth: '1' } })
    return
  }
  const id = c.id
  if (!id || likeBusy.value.has(id)) return
  likeBusy.value.add(id)
  try {
    const res = await blogCommentLikeToggle({ commentId: id })
    if (!isResultSuccess(res.code)) {
      Message.error(res.message || '操作失败')
      return
    }
    const liked = res.data?.liked ?? false
    const cnt = res.data?.likeCount ?? c.likeCount ?? 0
    c.liked = liked
    c.likeCount = cnt
    likePulse.value.add(id)
    window.setTimeout(() => likePulse.value.delete(id), 520)
  } catch (e) {
    Message.error(mapApiError(e, '操作失败'))
  } finally {
    likeBusy.value.delete(id)
  }
}

/**
 * 回复始终挂在「一级评论」上（与后端约定一致）；mention 用于 @ 展示对象。
 */
function startReply(top: BlogCommentVO, mention?: BlogCommentVO) {
  replyParent.value = top
  const nick = (mention ?? top).userName ?? '用户'
  const prefix = `@${nick} `
  if (!draft.value.includes(prefix.trim())) {
    draft.value = `${prefix}${draft.value}`.trimEnd()
  }
  void nextTick(() => textareaRef.value?.focus())
}

function cancelReply() {
  replyParent.value = null
}

async function copyText(c: BlogCommentVO) {
  try {
    await navigator.clipboard.writeText(c.content ?? '')
    Message.success('已复制')
  } catch {
    Message.warning('复制失败')
  }
}

function reportComment() {
  Message.success('感谢反馈，我们已记录')
}

function toggleEmoji() {
  emojiOpen.value = !emojiOpen.value
}

function insertEmoji(e: string) {
  draft.value += e
  void nextTick(() => textareaRef.value?.focus())
}

function onPickSort(s: 'hot' | 'latest') {
  if (sort.value === s) return
  sort.value = s
  pageCurrent.value = 1
  void fetchPage(true)
}

watch(
  () => props.postId,
  (id) => {
    if (id?.trim()) void fetchPage(true)
    else {
      hotTop.value = []
      records.value = []
      totalAll.value = 0
    }
  },
)

onMounted(() => {
  if (props.postId?.trim()) void fetchPage(true)
})
</script>

<template>
  <section class="cmt" aria-label="评论区">
    <header class="cmt-head">
      <div class="cmt-head__title">
        <span class="cmt-head__count">{{ totalAll }}</span>
        <span class="cmt-head__label">条评论</span>
      </div>
      <div class="cmt-head__tabs" role="tablist">
        <button
          type="button"
          role="tab"
          class="cmt-tab"
          :class="{ 'cmt-tab--on': sort === 'hot' }"
          @click="onPickSort('hot')"
        >
          最热
        </button>
        <button
          type="button"
          role="tab"
          class="cmt-tab"
          :class="{ 'cmt-tab--on': sort === 'latest' }"
          @click="onPickSort('latest')"
        >
          最新
        </button>
      </div>
    </header>

    <Transition name="cmt-toast">
      <div v-if="toastVisible" class="cmt-new-bar" role="status">
        {{ toastText }}
      </div>
    </Transition>

    <div v-if="loading && !records.length && !hotTop.length" class="cmt-skel">
      <div v-for="n in 4" :key="n" class="cmt-skel__row" />
    </div>

    <div v-else-if="empty" class="cmt-empty">
      <svg class="cmt-empty__svg" viewBox="0 0 120 120" aria-hidden="true">
        <defs>
          <linearGradient id="cmtBlob" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" style="stop-color: rgba(45, 212, 255, 0.35)" />
            <stop offset="100%" style="stop-color: rgba(192, 132, 252, 0.35)" />
          </linearGradient>
        </defs>
        <ellipse cx="60" cy="68" rx="46" ry="38" fill="url(#cmtBlob)" opacity="0.9" />
        <circle cx="44" cy="58" r="5" fill="#1e293b" opacity="0.65" />
        <circle cx="76" cy="58" r="5" fill="#1e293b" opacity="0.65" />
        <path
          d="M46 78 Q60 88 74 78"
          fill="none"
          stroke="#1e293b"
          stroke-width="3"
          stroke-linecap="round"
          opacity="0.45"
        />
        <circle cx="95" cy="36" r="10" fill="#fef08a" opacity="0.85" />
      </svg>
      <p class="cmt-empty__title">还没有评论</p>
      <p class="cmt-empty__hint">抢沙发，来说说你的想法吧 ✨</p>
    </div>

    <div v-else class="cmt-list">
      <template v-if="hotTop.length">
        <p class="cmt-section-tag">热评</p>
        <template v-for="(c, hci) in hotTop" :key="'h-' + c.id">
          <div v-if="hci > 0" class="cmt-divider" />
          <article
            class="cmt-row"
            @mouseenter="hoverRowId = c.id"
            @mouseleave="hoverRowId = null"
          >
            <div class="cmt-row__avatar-wrap">
              <img
                v-if="c.userAvatar && resolveMediaUrl(c.userAvatar)"
                :src="resolveMediaUrl(c.userAvatar)"
                alt=""
                class="cmt-row__avatar"
              />
              <div v-else class="cmt-row__avatar cmt-row__avatar--placeholder" aria-hidden="true" />
            </div>
            <div class="cmt-row__main">
              <div class="cmt-row__head">
                <span class="cmt-row__name">{{ c.userName || '用户' }}</span>
                <span v-if="c.hot || (c.likeCount ?? 0) >= 10" class="cmt-badge">热评</span>
                <span class="cmt-row__time">{{ formatTime(c.createTime) }}</span>
              </div>
              <p class="cmt-row__text">
                <template v-for="(seg, si) in segmentsOf(c)" :key="si">
                  <span v-if="seg.type === 'mention'" class="cmt-mention">{{ seg.value }}</span>
                  <span v-else>{{ seg.value }}</span>
                </template>
              </p>
              <div class="cmt-row__actions" :class="{ 'cmt-row__actions--show': hoverRowId === c.id }">
                <button
                  type="button"
                  class="cmt-act"
                  :class="{ 'cmt-act--on': c.liked, 'cmt-act--pulse': likePulse.has(c.id) }"
                  @click="toggleLike(c)"
                >
                  <span class="cmt-act__ico" aria-hidden="true">{{ c.liked ? '♥' : '♡' }}</span>
                  {{ c.likeCount ?? 0 }}
                </button>
                <button type="button" class="cmt-act" @click="startReply(c)">回复</button>
                <button type="button" class="cmt-act" @click="reportComment">举报</button>
                <button type="button" class="cmt-act" @click="copyText(c)">复制</button>
                <button
                  v-if="isMyComment(c)"
                  type="button"
                  class="cmt-act cmt-act--danger"
                  @click="confirmDelete(c)"
                >
                  删除
                </button>
              </div>
              <div v-if="c.replies?.length" class="cmt-replies">
                <div
                  v-for="r in c.replies"
                  :key="r.id"
                  class="cmt-row cmt-row--nest"
                  @mouseenter="hoverRowId = r.id"
                  @mouseleave="hoverRowId = null"
                >
                  <div class="cmt-row__avatar-wrap">
                    <img
                      v-if="r.userAvatar && resolveMediaUrl(r.userAvatar)"
                      :src="resolveMediaUrl(r.userAvatar)"
                      alt=""
                      class="cmt-row__avatar"
                    />
                    <div v-else class="cmt-row__avatar cmt-row__avatar--placeholder" aria-hidden="true" />
                  </div>
                  <div class="cmt-row__main">
                    <div class="cmt-row__head">
                      <span class="cmt-row__name">{{ r.userName || '用户' }}</span>
                      <span class="cmt-row__time">{{ formatTime(r.createTime) }}</span>
                    </div>
                    <p class="cmt-row__text">
                      <template v-for="(seg, si) in segmentsOf(r)" :key="si">
                        <span v-if="seg.type === 'mention'" class="cmt-mention">{{ seg.value }}</span>
                        <span v-else>{{ seg.value }}</span>
                      </template>
                    </p>
                    <div class="cmt-row__actions" :class="{ 'cmt-row__actions--show': hoverRowId === r.id }">
                      <button
                        type="button"
                        class="cmt-act"
                        :class="{ 'cmt-act--on': r.liked, 'cmt-act--pulse': likePulse.has(r.id) }"
                        @click="toggleLike(r)"
                      >
                        <span class="cmt-act__ico" aria-hidden="true">{{ r.liked ? '♥' : '♡' }}</span>
                        {{ r.likeCount ?? 0 }}
                      </button>
                      <button type="button" class="cmt-act" @click="startReply(c, r)">回复</button>
                      <button type="button" class="cmt-act" @click="reportComment">举报</button>
                      <button type="button" class="cmt-act" @click="copyText(r)">复制</button>
                      <button
                        v-if="isMyComment(r)"
                        type="button"
                        class="cmt-act cmt-act--danger"
                        @click="confirmDelete(r)"
                      >
                        删除
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </article>
        </template>
        <div v-if="hotTop.length && records.length" class="cmt-divider" />
      </template>

      <template v-for="(c, ri) in records" :key="'r-' + c.id">
        <div v-if="ri > 0" class="cmt-divider" />
        <article
          class="cmt-row"
          @mouseenter="hoverRowId = c.id"
          @mouseleave="hoverRowId = null"
        >
          <div class="cmt-row__avatar-wrap">
            <img
              v-if="c.userAvatar && resolveMediaUrl(c.userAvatar)"
              :src="resolveMediaUrl(c.userAvatar)"
              alt=""
              class="cmt-row__avatar"
            />
            <div v-else class="cmt-row__avatar cmt-row__avatar--placeholder" aria-hidden="true" />
          </div>
          <div class="cmt-row__main">
            <div class="cmt-row__head">
              <span class="cmt-row__name">{{ c.userName || '用户' }}</span>
              <span v-if="(c.likeCount ?? 0) >= 10" class="cmt-badge">热评</span>
              <span class="cmt-row__time">{{ formatTime(c.createTime) }}</span>
            </div>
            <p class="cmt-row__text">
              <template v-for="(seg, si) in segmentsOf(c)" :key="si">
                <span v-if="seg.type === 'mention'" class="cmt-mention">{{ seg.value }}</span>
                <span v-else>{{ seg.value }}</span>
              </template>
            </p>
            <div class="cmt-row__actions" :class="{ 'cmt-row__actions--show': hoverRowId === c.id }">
              <button
                type="button"
                class="cmt-act"
                :class="{ 'cmt-act--on': c.liked, 'cmt-act--pulse': likePulse.has(c.id) }"
                @click="toggleLike(c)"
              >
                <span class="cmt-act__ico" aria-hidden="true">{{ c.liked ? '♥' : '♡' }}</span>
                {{ c.likeCount ?? 0 }}
              </button>
              <button type="button" class="cmt-act" @click="startReply(c)">回复</button>
              <button type="button" class="cmt-act" @click="reportComment">举报</button>
              <button type="button" class="cmt-act" @click="copyText(c)">复制</button>
              <button
                v-if="isMyComment(c)"
                type="button"
                class="cmt-act cmt-act--danger"
                @click="confirmDelete(c)"
              >
                删除
              </button>
            </div>
            <div v-if="c.replies?.length" class="cmt-replies">
              <div
                v-for="r in c.replies"
                :key="r.id"
                class="cmt-row cmt-row--nest"
                @mouseenter="hoverRowId = r.id"
                @mouseleave="hoverRowId = null"
              >
                <div class="cmt-row__avatar-wrap">
                  <img
                    v-if="r.userAvatar && resolveMediaUrl(r.userAvatar)"
                    :src="resolveMediaUrl(r.userAvatar)"
                    alt=""
                    class="cmt-row__avatar"
                  />
                  <div v-else class="cmt-row__avatar cmt-row__avatar--placeholder" aria-hidden="true" />
                </div>
                <div class="cmt-row__main">
                  <div class="cmt-row__head">
                    <span class="cmt-row__name">{{ r.userName || '用户' }}</span>
                    <span class="cmt-row__time">{{ formatTime(r.createTime) }}</span>
                  </div>
                  <p class="cmt-row__text">
                    <template v-for="(seg, si) in segmentsOf(r)" :key="si">
                      <span v-if="seg.type === 'mention'" class="cmt-mention">{{ seg.value }}</span>
                      <span v-else>{{ seg.value }}</span>
                    </template>
                  </p>
                  <div class="cmt-row__actions" :class="{ 'cmt-row__actions--show': hoverRowId === r.id }">
                    <button
                      type="button"
                      class="cmt-act"
                      :class="{ 'cmt-act--on': r.liked, 'cmt-act--pulse': likePulse.has(r.id) }"
                      @click="toggleLike(r)"
                    >
                      <span class="cmt-act__ico" aria-hidden="true">{{ r.liked ? '♥' : '♡' }}</span>
                      {{ r.likeCount ?? 0 }}
                    </button>
                    <button type="button" class="cmt-act" @click="startReply(c, r)">回复</button>
                    <button type="button" class="cmt-act" @click="reportComment">举报</button>
                    <button type="button" class="cmt-act" @click="copyText(r)">复制</button>
                    <button
                      v-if="isMyComment(r)"
                      type="button"
                      class="cmt-act cmt-act--danger"
                      @click="confirmDelete(r)"
                    >
                      删除
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </article>
      </template>

      <div v-if="hasMore" class="cmt-more">
        <button type="button" class="cmt-more__btn" :disabled="loadingMore" @click="loadMore">
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
      </div>
    </div>

    <div class="cmt-spacer" aria-hidden="true" />

    <ModerationBanNotice
      v-if="auth.isAccountDisabled"
      variant="account"
      compact
      class="cmt-composer-ban-notice"
      title="暂不可评论"
      description="账号已被管理员禁用，评论功能已关闭。"
      footer="可浏览内容；解禁后恢复评论。"
    />
    <ModerationBanNotice
      v-else-if="auth.isCommentBanned"
      variant="comment"
      compact
      class="cmt-composer-ban-notice"
      title="评论功能已限制"
      description="检测到不当评论，系统已暂停本区发言。"
      :countdown="commentBanCountdown"
      :reason="(auth.commentBanReason ?? auth.banReason ?? '').trim()"
      footer="做题与其他功能不受影响，到期后自动恢复。"
    />
    <div class="cmt-composer">
      <div v-if="replyParent" class="cmt-composer__reply-hint">
        回复 <strong>@{{ replyParent.userName }}</strong>
        <button type="button" class="cmt-composer__cancel" @click="cancelReply">取消</button>
      </div>
      <div v-if="emojiOpen" class="cmt-emoji-panel">
        <button v-for="em in QUICK_EMOJIS" :key="em" type="button" class="cmt-emoji-btn" @click="insertEmoji(em)">
          {{ em }}
        </button>
      </div>
      <div class="cmt-composer__row">
        <div class="cmt-composer__avatar-wrap">
          <img v-if="composerAvatar" :src="composerAvatar" alt="" class="cmt-composer__avatar" />
          <div v-else class="cmt-composer__avatar cmt-composer__avatar--ph" aria-hidden="true" />
        </div>
        <textarea
          ref="textareaRef"
          v-model="draft"
          class="cmt-composer__input"
          rows="1"
          maxlength="2000"
          placeholder="有爱评论，说点儿好听的～"
          @focus="emojiOpen = false"
        />
        <button type="button" class="cmt-composer__emoji" aria-label="表情" @click="toggleEmoji">😊</button>
        <button
          type="button"
          class="cmt-composer__send"
          :disabled="submitting || cannotPostComment"
          @click="submit"
        >
          发送
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.cmt {
  position: relative;
  margin-top: 28px;
  padding-bottom: calc(96px + env(safe-area-inset-bottom, 0px));
  color: var(--cb-text, var(--app-text));
}

.cmt-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.cmt-head__count {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.cmt-head__label {
  margin-left: 4px;
  font-size: 14px;
  color: var(--cb-muted, var(--app-text-muted));
}

.cmt-head__tabs {
  display: inline-flex;
  padding: 3px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

html[data-theme='light'] .cmt-head__tabs {
  background: rgba(15, 23, 42, 0.04);
  border-color: rgba(15, 23, 42, 0.08);
}

.cmt-tab {
  border: none;
  background: transparent;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: var(--cb-muted, var(--app-text-muted));
  cursor: pointer;
  transition:
    background 0.18s ease,
    color 0.18s ease;
}

.cmt-tab--on {
  background: rgba(45, 212, 255, 0.14);
  color: var(--cb-cyan, var(--app-accent));
}

html[data-theme='light'] .cmt-tab--on {
  background: rgba(14, 165, 233, 0.12);
  color: #0369a1;
}

.cmt-toast-enter-active,
.cmt-toast-leave-active {
  transition:
    opacity 0.28s ease,
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.cmt-toast-enter-from,
.cmt-toast-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

.cmt-new-bar {
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  text-align: center;
  background: linear-gradient(90deg, rgba(45, 212, 255, 0.14), rgba(192, 132, 252, 0.14));
  border: 1px solid rgba(45, 212, 255, 0.22);
  color: var(--cb-text, var(--app-text));
}

.cmt-skel__row {
  height: 72px;
  margin-bottom: 12px;
  border-radius: 14px;
  background: linear-gradient(110deg, rgba(255, 255, 255, 0.04) 8%, rgba(255, 255, 255, 0.09) 18%, rgba(255, 255, 255, 0.04) 33%);
  background-size: 200% 100%;
  animation: cmt-shimmer 1.1s ease-in-out infinite;
}

@keyframes cmt-shimmer {
  to {
    background-position: -200% 0;
  }
}

.cmt-empty {
  text-align: center;
  padding: 36px 16px 48px;
}

.cmt-empty__svg {
  width: 112px;
  height: 112px;
  margin: 0 auto 12px;
  display: block;
}

.cmt-empty__title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 700;
}

.cmt-empty__hint {
  margin: 0;
  font-size: 14px;
  color: var(--cb-muted, var(--app-text-muted));
}

.cmt-section-tag {
  margin: 8px 0 4px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(251, 146, 60, 0.95);
}

.cmt-divider {
  height: 1px;
  margin: 0;
  background: rgba(255, 255, 255, 0.08);
}

html[data-theme='light'] .cmt-divider {
  background: rgba(15, 23, 42, 0.08);
}

.cmt-row {
  display: flex;
  gap: 12px;
  padding: 14px 10px;
  margin: 0 -10px;
  border-radius: 14px;
  transition: background 0.18s ease;
}

.cmt-row:hover {
  background: rgba(255, 255, 255, 0.045);
}

html[data-theme='light'] .cmt-row:hover {
  background: rgba(15, 23, 42, 0.04);
}

.cmt-row--nest {
  margin: 8px 0 0;
  padding: 10px 8px;
}

.cmt-row__avatar-wrap {
  flex-shrink: 0;
}

.cmt-row__avatar {
  width: 40px;
  height: 40px;
  border-radius: 999px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: transform 0.22s cubic-bezier(0.34, 1.4, 0.64, 1);
}

.cmt-row:hover .cmt-row__avatar {
  transform: scale(1.06);
}

.cmt-row__avatar--placeholder {
  background: linear-gradient(145deg, rgba(148, 163, 184, 0.35), rgba(71, 85, 105, 0.45));
  border: none;
}

.cmt-row__main {
  flex: 1;
  min-width: 0;
}

.cmt-row__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.cmt-row__name {
  font-size: 14px;
  font-weight: 700;
  color: var(--cb-text, var(--app-text));
}

.cmt-row__time {
  margin-left: auto;
  font-size: 12px;
  color: var(--cb-muted, var(--app-text-subtle));
}

.cmt-badge {
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(251, 146, 60, 0.25), rgba(248, 113, 113, 0.2));
  border: 1px solid rgba(251, 146, 60, 0.35);
  color: #fdba74;
}

html[data-theme='light'] .cmt-badge {
  color: #c2410c;
}

.cmt-row__text {
  margin: 0;
  font-size: 15px;
  line-height: 1.65;
  word-break: break-word;
  white-space: pre-wrap;
  color: color-mix(in srgb, var(--cb-text, var(--app-text)) 92%, transparent);
}

.cmt-mention {
  color: #38bdf8;
  font-weight: 600;
}

html[data-theme='light'] .cmt-mention {
  color: #0284c7;
}

.cmt-row__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-top: 8px;
  opacity: 0;
  pointer-events: none;
  transition: opacity 0.15s ease;
}

.cmt-row__actions--show {
  opacity: 1;
  pointer-events: auto;
}

@media (hover: none) {
  .cmt-row__actions {
    opacity: 1;
    pointer-events: auto;
  }
}

.cmt-act {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--cb-muted, var(--app-text-muted));
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition:
    color 0.15s ease,
    transform 0.15s ease;
}

.cmt-act:hover {
  color: var(--cb-cyan, var(--app-accent));
}

.cmt-act--on {
  color: #fb7185;
}

.cmt-act--danger:hover {
  color: #f87171 !important;
}

.cmt-act--pulse .cmt-act__ico {
  animation: cmt-heart-pop 0.52s cubic-bezier(0.34, 1.6, 0.64, 1);
}

@keyframes cmt-heart-pop {
  0% {
    transform: scale(1);
  }
  35% {
    transform: scale(1.35);
  }
  100% {
    transform: scale(1);
  }
}

.cmt-replies {
  margin-top: 6px;
  padding-left: 4px;
  border-left: 2px solid rgba(255, 255, 255, 0.06);
}

html[data-theme='light'] .cmt-replies {
  border-left-color: rgba(15, 23, 42, 0.08);
}

.cmt-more {
  text-align: center;
  padding: 18px 0 8px;
}

.cmt-more__btn {
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
  color: inherit;
  padding: 8px 22px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

html[data-theme='light'] .cmt-more__btn {
  border-color: rgba(15, 23, 42, 0.12);
  background: rgba(15, 23, 42, 0.03);
}

.cmt-spacer {
  height: 12px;
}

.cmt-composer-ban-notice {
  margin: 0 16px 12px;
}

.cmt-composer {
  position: fixed;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  width: min(calc(820px + 32px), 100%);
  padding: 10px 16px calc(12px + env(safe-area-inset-bottom, 0px));
  z-index: 40;
  background: color-mix(in srgb, var(--cb-ink, #030508) 82%, transparent);
  backdrop-filter: blur(16px);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 -12px 40px rgba(0, 0, 0, 0.25);
}

html[data-theme='light'] .cmt-composer {
  background: rgba(255, 255, 255, 0.98);
  border-top-color: rgba(15, 23, 42, 0.08);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  box-shadow: 0 -4px 24px rgba(15, 23, 42, 0.06);
}

.cmt-composer__reply-hint {
  font-size: 12px;
  margin-bottom: 8px;
  color: var(--cb-muted, var(--app-text-muted));
}

.cmt-composer__cancel {
  margin-left: 10px;
  border: none;
  background: transparent;
  color: var(--cb-cyan, var(--app-accent));
  font-weight: 700;
  cursor: pointer;
}

.cmt-emoji-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.cmt-emoji-btn {
  border: none;
  background: transparent;
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
  padding: 4px;
  border-radius: 8px;
}

.cmt-emoji-btn:hover {
  background: rgba(255, 255, 255, 0.08);
}

.cmt-composer__row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.cmt-composer__avatar-wrap {
  flex-shrink: 0;
}

.cmt-composer__avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  object-fit: cover;
  border: 1px solid rgba(255, 255, 255, 0.12);
  transition: transform 0.2s ease;
}

.cmt-composer__avatar:hover {
  transform: scale(1.08);
}

.cmt-composer__avatar--ph {
  background: linear-gradient(145deg, rgba(148, 163, 184, 0.4), rgba(71, 85, 105, 0.5));
  border: none;
}

.cmt-composer__input {
  flex: 1;
  min-height: 40px;
  max-height: 120px;
  resize: none;
  border-radius: 20px;
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.45;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  color: inherit;
  outline: none;
}

html[data-theme='light'] .cmt-composer__input {
  background: rgba(15, 23, 42, 0.04);
  border-color: rgba(15, 23, 42, 0.1);
}

.cmt-composer__emoji {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;
  font-size: 20px;
}

.cmt-composer__send {
  flex-shrink: 0;
  height: 40px;
  padding: 0 18px;
  border-radius: 999px;
  border: none;
  font-weight: 700;
  font-size: 14px;
  cursor: pointer;
  color: #fff;
  background: linear-gradient(135deg, #0891b2, #2563eb);
  box-shadow: 0 0 20px rgba(45, 212, 255, 0.25);
}

.cmt-composer__send:disabled {
  opacity: 0.55;
  cursor: wait;
}
</style>
