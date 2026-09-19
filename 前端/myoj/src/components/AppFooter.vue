<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <footer class="app-footer">
    <div class="app-footer__inner">
      <RouterLink to="/legal/terms" class="app-footer__link">服务协议</RouterLink>
      <span class="app-footer__sep" aria-hidden="true">·</span>
      <RouterLink to="/legal/privacy" class="app-footer__link">隐私政策</RouterLink>
      <span class="app-footer__sep" aria-hidden="true">·</span>
      <RouterLink to="/blog" class="app-footer__link">博客</RouterLink>
    </div>
  </footer>
</template>

<style scoped>
.app-footer {
  margin-top: auto;
  padding: 20px 16px 28px;
  border-top: 1px solid var(--color-border-2);
  background: var(--color-bg-2);
}

.app-footer__inner {
  max-width: var(--app-page-width, 1240px);
  margin: 0 auto;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px 12px;
  font-size: 13px;
  color: var(--color-text-3);
}

.app-footer__link {
  color: var(--color-text-2);
  text-decoration: none;
}

.app-footer__link:hover {
  color: var(--color-primary-light-6, #0f8c7a);
  text-decoration: underline;
}

.app-footer__sep {
  opacity: 0.5;
  user-select: none;
}
</style>
