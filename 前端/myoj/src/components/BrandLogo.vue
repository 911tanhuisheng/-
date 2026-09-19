<script setup lang="ts">
/**
 * 原创品牌图形（码跃 OJ），渐变 + 代码符号 </>
 * 灵感来自主流开发者产品常见的极简几何风格，非任何第三方商标。
 */
withDefaults(
  defineProps<{
    size?: number
  }>(),
  { size: 32 },
)
</script>

<template>
  <svg
    class="brand-logo"
    :width="size"
    :height="size"
    viewBox="0 0 32 32"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <defs>
      <linearGradient id="brandLogoBg" x1="4" y1="4" x2="30" y2="28" gradientUnits="userSpaceOnUse">
        <stop stop-color="#0f766e" />
        <stop offset="0.45" stop-color="#14b8a6" />
        <stop offset="1" stop-color="#5eead4" />
      </linearGradient>
      <linearGradient id="brandLogoShine" x1="0" y1="0" x2="0" y2="32" gradientUnits="userSpaceOnUse">
        <stop stop-color="#ffffff" stop-opacity="0.35" />
        <stop offset="0.35" stop-color="#ffffff" stop-opacity="0.06" />
        <stop offset="1" stop-color="#ffffff" stop-opacity="0" />
      </linearGradient>
    </defs>
    <rect width="32" height="32" rx="8" fill="url(#brandLogoBg)" />
    <rect width="32" height="32" rx="8" fill="url(#brandLogoShine)" />
    <path
      d="M11.5 10.2L8.2 15.4c-.25.4-.25.9 0 1.3l3.3 5.2"
      stroke="white"
      stroke-width="2.1"
      stroke-linecap="round"
      stroke-linejoin="round"
      opacity="0.96"
    />
    <path
      d="M20.5 10.2l3.3 5.2c.25.4.25.9 0 1.3l-3.3 5.2"
      stroke="white"
      stroke-width="2.1"
      stroke-linecap="round"
      stroke-linejoin="round"
      opacity="0.96"
    />
    <path
      d="M17.2 9.5l-3.4 13"
      stroke="white"
      stroke-width="2.15"
      stroke-linecap="round"
      opacity="0.94"
    />
  </svg>
</template>

<style scoped>
.brand-logo {
  display: block;
  flex-shrink: 0;
}
</style>
