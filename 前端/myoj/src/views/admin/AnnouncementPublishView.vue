<script setup lang="ts">
import { ref } from 'vue'
import { Message } from '@arco-design/web-vue'
import { publishAnnouncement } from '@/api/inAppNotifications'
import { isResultSuccess } from '@/api/result'
import { getBackendErrorMessage } from '@/api/httpError'

const title = ref('')
const content = ref('')
const submitting = ref(false)

async function onSubmit() {
  const t = title.value.trim()
  const c = content.value.trim()
  if (!t) {
    Message.warning('请填写标题')
    return
  }
  if (!c) {
    Message.warning('请填写正文')
    return
  }
  submitting.value = true
  try {
    const res = await publishAnnouncement(t, c)
    if (!isResultSuccess(res.code)) {
      Message.error(res.message || '发布失败')
      return
    }
    Message.success('公告已发布，全站用户将在通知中心看到未读提示')
    title.value = ''
    content.value = ''
    window.dispatchEvent(new CustomEvent('myoj:notifications-changed'))
  } catch (e) {
    Message.error(getBackendErrorMessage(e, '发布失败'))
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="admin-page ann-admin">
    <section class="panel">
      <h1>发布公告</h1>
      <p class="lead">发布后，用户通知中心的「未读公告」会增加；用户在通知页可将公告标为已读。</p>
      <a-form layout="vertical" class="ann-form">
        <a-form-item label="标题">
          <a-input v-model="title" allow-clear placeholder="简短标题" :max-length="255" show-word-limit />
        </a-form-item>
        <a-form-item label="正文">
          <a-textarea
            v-model="content"
            allow-clear
            placeholder="支持多行，将展示在通知中心"
            :auto-size="{ minRows: 8, maxRows: 24 }"
            :max-length="20000"
            show-word-limit
          />
        </a-form-item>
        <a-button type="primary" :loading="submitting" @click="onSubmit">发布</a-button>
      </a-form>
    </section>
  </div>
</template>

<style scoped>
.ann-admin .panel {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px;
}

.ann-admin h1 {
  margin: 0 0 8px;
}

.lead {
  margin: 0 0 24px;
  color: var(--color-text-3);
  font-size: 14px;
  line-height: 1.6;
}

.ann-form {
  max-width: 100%;
}
</style>
