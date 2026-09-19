<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { Message, Modal } from '@arco-design/web-vue'
import { Service } from '@generated'
import type { BlogPostDetailVO } from '@generated'
import { mapApiError } from '@/api/mapApiError'
import { isResultSuccess } from '@/api/result'
import BlogCyberShell from '@/components/blog/BlogCyberShell.vue'

const STATUS_DRAFT = 0
const STATUS_PUBLISHED = 1

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.name === 'blog-edit')
const editId = computed(() => String(route.params.id || '').trim())

const loading = ref(false)
const saving = ref(false)

const title = ref('')
const summary = ref('')
const coverUrl = ref('')
const tagsStr = ref('')
const content = ref('')
const status = ref<number>(STATUS_DRAFT)

function parseTags(raw: string): string[] {
  return raw
    .split(/[,，]/)
    .map((t) => t.trim())
    .filter(Boolean)
}

async function loadMine() {
  const id = editId.value
  if (!id) return
  loading.value = true
  try {
    const res = await Service.getMine(id)
    if (!isResultSuccess(res.code) || !res.data) {
      Message.error(res.message || '无法加载文章')
      return
    }
    applyDetail(res.data)
  } catch (e) {
    Message.error(mapApiError(e, '无法加载文章'))
  } finally {
    loading.value = false
  }
}

function applyDetail(d: BlogPostDetailVO) {
  title.value = d.title ?? ''
  summary.value = d.summary ?? ''
  coverUrl.value = d.coverUrl ?? ''
  tagsStr.value = (d.tags ?? []).join(', ')
  content.value = d.content ?? ''
  status.value = typeof d.status === 'number' ? d.status : STATUS_DRAFT
}

async function submit() {
  const t = title.value.trim()
  const c = content.value.trim()
  if (!t) {
    Message.warning('请填写标题')
    return
  }
  if (c.length < 10) {
    Message.warning('正文至少 10 个字符')
    return
  }
  saving.value = true
  try {
    if (!isEdit.value) {
      const res = await Service.add1({
        title: t,
        summary: summary.value.trim() || undefined,
        content: c,
        coverUrl: coverUrl.value.trim() || undefined,
        tags: parseTags(tagsStr.value),
        status: status.value,
      })
      if (!isResultSuccess(res.code)) {
        Message.error(res.message || '保存失败')
        return
      }
      const newId = res.data?.trim()
      Message.success(status.value === STATUS_PUBLISHED ? '已发布' : '草稿已保存')
      if (newId) {
        void router.replace({ name: 'blog-detail', params: { id: newId } })
      } else {
        void router.push({ name: 'blog-my' })
      }
      return
    }

    const id = editId.value
    const res = await Service.update1({
      id,
      title: t,
      summary: summary.value.trim() || undefined,
      content: c,
      coverUrl: coverUrl.value.trim() || undefined,
      tags: parseTags(tagsStr.value),
      status: status.value,
    })
    if (!isResultSuccess(res.code)) {
      Message.error(res.message || '保存失败')
      return
    }
    Message.success('已更新')
    void router.push({ name: 'blog-detail', params: { id } })
  } catch (e) {
    Message.error(mapApiError(e, '保存失败'))
  } finally {
    saving.value = false
  }
}

function confirmDelete() {
  if (!isEdit.value) return
  const id = editId.value
  Modal.warning({
    title: '删除文章',
    content: '删除后不可恢复，确定继续吗？',
    hideCancel: false,
    onOk: async () => {
      try {
        const res = await Service.delete({ id })
        if (!isResultSuccess(res.code)) {
          Message.error(res.message || '删除失败')
          return
        }
        Message.success('已删除')
        void router.replace({ name: 'blog-my' })
      } catch (e) {
        Message.error(mapApiError(e, '删除失败'))
      }
    },
  })
}

watch(
  () => route.params.id,
  () => {
    if (isEdit.value) void loadMine()
  },
)

onMounted(() => {
  if (isEdit.value) void loadMine()
})
</script>

<template>
  <BlogCyberShell>
    <div class="blog-compose">
    <div class="blog-compose__inner">
      <header class="blog-compose__head panel">
        <div>
          <p class="blog-compose__eyebrow">Editor</p>
          <h1 class="blog-compose__title">{{ isEdit ? '编辑文章' : '写文章' }}</h1>
          <p class="blog-compose__hint">正文不少于 10 字；草稿仅自己可见，发布后出现在博客首页。</p>
        </div>
        <div class="blog-compose__head-actions">
          <RouterLink :to="{ name: 'blog' }" class="ghost-link blog-magnetic">返回首页</RouterLink>
          <RouterLink
            v-if="isEdit && editId && status === STATUS_PUBLISHED"
            :to="{ name: 'blog-detail', params: { id: editId } }"
            class="ghost-link blog-magnetic"
          >
            打开公开页
          </RouterLink>
        </div>
      </header>

      <div v-if="loading && isEdit" class="panel blog-compose__loading">加载编辑器…</div>

      <div v-else class="blog-compose__form panel">
        <label class="field">
          <span class="label">标题</span>
          <a-input v-model="title" allow-clear placeholder="给你的想法起一个清晰的标题" size="large" />
        </label>

        <label class="field">
          <span class="label">摘要（可选）</span>
          <a-textarea
            v-model="summary"
            class="compose-sans"
            placeholder="列表卡片与详情顶部摘要；留空则从正文截取"
            :auto-size="{ minRows: 2, maxRows: 5 }"
          />
        </label>

        <div class="field-row">
          <label class="field field--grow">
            <span class="label">封面图 URL（可选）</span>
            <a-input v-model="coverUrl" allow-clear placeholder="https://… 或站内相对路径" />
          </label>
          <label class="field field--grow">
            <span class="label">标签</span>
            <a-input v-model="tagsStr" allow-clear placeholder="逗号分隔，最多 12 个" />
          </label>
        </div>

        <fieldset class="status-field">
          <legend class="label">发布状态</legend>
          <label class="radio-line">
            <input v-model.number="status" type="radio" name="st" :value="STATUS_DRAFT" />
            <span>草稿（仅「我的博客」可见）</span>
          </label>
          <label class="radio-line">
            <input v-model.number="status" type="radio" name="st" :value="STATUS_PUBLISHED" />
            <span>立即发布（博客首页可见）</span>
          </label>
        </fieldset>

        <label class="field">
          <span class="label">正文</span>
          <a-textarea
            v-model="content"
            class="content-area"
            placeholder="支持 Markdown：可直接粘贴图片地址（多条可紧挨着）；Bing 等无后缀链接也会显示为图；或用 ![](url)、# 标题、**加粗** 等"
            :auto-size="{ minRows: 16, maxRows: 36 }"
          />
        </label>

        <div class="action-row">
          <a-button type="primary" size="large" class="save-btn blog-magnetic" :loading="saving" @click="submit">
            {{ isEdit ? '保存修改' : '提交' }}
          </a-button>
          <a-button
            v-if="isEdit"
            size="large"
            status="danger"
            class="del-btn blog-magnetic"
            :disabled="saving"
            @click="confirmDelete"
          >
            删除
          </a-button>
        </div>
      </div>
    </div>
    </div>
  </BlogCyberShell>
</template>

<style scoped>
.blog-compose {
  position: relative;
  isolation: isolate;
  padding: 28px 14px 72px;
  font-family:
    'Plus Jakarta Sans',
    'Noto Sans SC',
    'DM Sans',
    var(--app-font-body, system-ui),
    -apple-system,
    sans-serif;
  font-optical-sizing: auto;
  -webkit-font-smoothing: antialiased;
  text-rendering: optimizeLegibility;
}
.blog-compose__inner {
  width: min(880px, 100%);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel {
  border: 1px solid var(--app-border);
  background: linear-gradient(
    155deg,
    color-mix(in srgb, var(--app-surface) 93%, transparent),
    var(--app-surface-strong)
  );
  box-shadow: var(--app-card-shadow);
  border-radius: var(--app-radius-xl);
}

.blog-compose__head {
  padding: 26px 28px;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
}
.blog-compose__eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--app-text-subtle);
  font-weight: 700;
}
.blog-compose__title {
  margin: 0 0 8px;
  font-size: clamp(22px, 3vw, 28px);
  font-weight: 900;
  letter-spacing: -0.02em;
  color: var(--app-text);
}
.blog-compose__hint {
  margin: 0;
  font-size: 14px;
  color: var(--app-text-muted);
  line-height: 1.65;
  letter-spacing: 0.01em;
  max-width: 560px;
}
.blog-compose__head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}
.ghost-link {
  font-size: 14px;
  font-weight: 700;
  color: var(--app-accent);
  text-decoration: none;
}
.ghost-link:hover {
  text-decoration: underline;
}

.blog-compose__loading {
  padding: 48px;
  text-align: center;
  color: var(--app-text-muted);
}

.blog-compose__form {
  padding: 28px 28px 32px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.field--grow {
  flex: 1 1 240px;
}
.label {
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: color-mix(in srgb, var(--app-text) 84%, var(--app-text-muted) 16%);
}

.status-field {
  margin: 0;
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px dashed var(--color-border-2);
  background: color-mix(in srgb, var(--color-fill-2) 80%, transparent);
}
.radio-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.55;
  letter-spacing: 0.01em;
  color: color-mix(in srgb, var(--app-text-muted) 88%, var(--app-text) 12%);
  cursor: pointer;
}
.radio-line:first-of-type {
  margin-top: 4px;
}

.blog-compose__form :deep(.arco-input) {
  font-size: 15px;
  line-height: 1.55;
  letter-spacing: 0.01em;
}

.compose-sans :deep(.arco-textarea) {
  font-family:
    'Plus Jakarta Sans',
    'Noto Sans SC',
    'DM Sans',
    var(--app-font-body, system-ui),
    sans-serif;
  font-size: 15px;
  line-height: 1.72;
  letter-spacing: 0.012em;
}

.content-area :deep(.arco-textarea) {
  font-family:
    'JetBrains Mono',
    'Noto Sans SC',
    'Cascadia Code',
    ui-monospace,
    monospace;
  font-size: clamp(14px, 0.35vw + 13.2px, 16px);
  line-height: 1.82;
  letter-spacing: 0.02em;
  font-feature-settings:
    'kern' 1,
    'liga' 1,
    'calt' 1;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
}
.save-btn {
  border-radius: 14px;
  min-width: 140px;
}
.del-btn {
  border-radius: 14px;
}
</style>
