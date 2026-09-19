<script setup lang="ts">
import { defineAsyncComponent } from 'vue'
import { RouterView } from 'vue-router'

/** 音乐浮层 + GSAP 较重：首屏后再异步拉取，缩短 TTI / 主线程解析时间 */
const GlobalMusicPlayer = defineAsyncComponent(() => import('@/components/GlobalMusicPlayer.vue'))
</script>

<template>
  <RouterView />
  <GlobalMusicPlayer />
</template>

<style>
* ,
*::before,
*::after {
  box-sizing: border-box;
}

:root {
  color-scheme: light;
  --app-bg: #f5efe2;
  --app-bg-secondary: #efe4cf;
  --app-surface: rgba(255, 251, 244, 0.82);
  --app-surface-strong: rgba(255, 248, 238, 0.94);
  --app-surface-soft: rgba(255, 255, 255, 0.58);
  --app-text: #201914;
  --app-text-main: #201914;
  --app-text-muted: #5d4a3c;
  --app-text-subtle: #8c7362;
  --app-border: rgba(92, 68, 46, 0.12);
  --app-border-strong: rgba(92, 68, 46, 0.18);
  --app-hover: rgba(255, 255, 255, 0.62);
  --app-accent: #0f8c7a;
  --app-accent-2: #ff8a3d;
  --app-accent-3: #ffd166;
  --app-danger: #d6604d;
  --app-success: #129a74;
  --app-header-shadow: 0 14px 40px rgba(70, 41, 14, 0.08);
  --app-dropdown-shadow: 0 18px 45px rgba(41, 26, 10, 0.12);
  --app-card-shadow: 0 20px 40px rgba(70, 41, 14, 0.08);
  --app-radius-xl: 28px;
  --app-radius-lg: 22px;
  --app-radius-md: 16px;
  --app-page-width: 1240px;
  --app-font-display: 'Palatino Linotype', 'Book Antiqua', Palatino, 'Times New Roman', serif;
  --app-font-body: 'Segoe UI Variable', 'Microsoft YaHei UI', 'PingFang SC', sans-serif;

  --color-bg-1: #fffaf3;
  --color-bg-2: #fff3e2;
  --color-bg-3: #f1e5d5;
  --color-fill-2: rgba(92, 68, 46, 0.08);
  --color-fill-3: rgba(92, 68, 46, 0.12);
  --color-text-1: #201914;
  --color-text-2: #5d4a3c;
  --color-text-3: #8c7362;
  /** 更弱的说明文字（标签角标等） */
  --color-text-4: #a89485;
  --color-fill-1: rgba(92, 68, 46, 0.05);
  --color-border-3: rgba(92, 68, 46, 0.1);
  --color-border-2: rgba(92, 68, 46, 0.14);
  --color-primary-light-1: #d8fff4;
  --color-primary-light-2: #b2f5e3;
  --color-primary-light-3: #7ee7d0;
  --color-primary-light-4: #44d1b7;
  --color-primary-light-5: #1fb79c;
  --color-primary-light-6: #0f8c7a;
 }

/* 明确浅色：避免从深色切回时第三方样式或 clip 字重绘导致整页白底白字 */
html[data-theme='light'] {
  color-scheme: light;
}

html[data-theme='light'] body {
  color: #201914;
  background-color: #f5efe2;
}

html[data-theme='dark'] {
  color-scheme: dark;
  /* 再提亮一档：减轻「一片黑」，卡片与页面更易区分 */
  --app-bg: #141d28;
  --app-bg-secondary: #1b2633;
  --app-surface: rgba(34, 46, 60, 0.9);
  --app-surface-strong: rgba(42, 56, 72, 0.94);
  --app-surface-soft: rgba(255, 255, 255, 0.04);
  --app-text: #f6efe3;
  --app-text-main: #f6efe3;
  --app-text-muted: #dccfbf;
  --app-text-subtle: #a89482;
  --app-border: rgba(230, 208, 186, 0.14);
  --app-border-strong: rgba(230, 208, 186, 0.2);
  --app-hover: rgba(255, 255, 255, 0.07);
  --app-accent: #5ae4cc;
  --app-accent-2: #ffa864;
  --app-accent-3: #ffdea3;
  --app-danger: #ff9a8a;
  --app-success: #7ae8b8;
  --app-header-shadow: 0 18px 48px rgba(0, 0, 0, 0.28);
  --app-dropdown-shadow: 0 24px 56px rgba(0, 0, 0, 0.34);
  --app-card-shadow: 0 20px 48px rgba(0, 0, 0, 0.22);

  --color-bg-1: #222f3f;
  --color-bg-2: #2a3a4c;
  --color-bg-3: #1a2430;
  --color-fill-2: rgba(255, 255, 255, 0.08);
  --color-fill-3: rgba(255, 255, 255, 0.12);
  --color-text-1: #f6efe3;
  --color-text-2: #e8ddd0;
  --color-text-3: #c9bba8;
  --color-text-4: rgba(246, 239, 227, 0.58);
  --color-fill-1: rgba(255, 255, 255, 0.055);
  --color-border-3: rgba(230, 208, 186, 0.11);
  --color-border-2: rgba(230, 208, 186, 0.17);
  --color-primary-light-1: rgba(83, 219, 194, 0.08);
  --color-primary-light-2: rgba(83, 219, 194, 0.12);
  --color-primary-light-3: rgba(83, 219, 194, 0.18);
  --color-primary-light-4: rgba(83, 219, 194, 0.24);
  --color-primary-light-5: rgba(83, 219, 194, 0.32);
  --color-primary-light-6: #5ae4cc;
}

html,
body,
#app {
  margin: 0;
  min-height: 100%;
  width: 100%;
}

html {
  scroll-behavior: smooth;
}

@media (prefers-reduced-motion: reduce) {
  html {
    scroll-behavior: auto;
  }
}

body {
  font-family: var(--app-font-body);
  background:
    radial-gradient(circle at 12% 18%, rgba(255, 163, 73, 0.22), transparent 28%),
    radial-gradient(circle at 88% 14%, rgba(15, 140, 122, 0.18), transparent 24%),
    radial-gradient(circle at 78% 82%, rgba(255, 209, 102, 0.16), transparent 24%),
    linear-gradient(180deg, var(--app-bg-secondary) 0%, var(--app-bg) 42%, var(--app-bg) 100%);
  color: var(--app-text);
  transition:
    background-color 0.35s ease,
    color 0.35s ease;
  position: relative;
}

body::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.05) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: radial-gradient(circle at center, black 48%, transparent 100%);
  opacity: 0.14;
}

html[data-theme='dark'] body::before {
  opacity: 0.07;
}

/** 暗黑：冷青氛围与 Arco 深色组件统一，避免页面底与表格「两套灰色」 */
html[data-theme='dark'] body {
  background:
    radial-gradient(circle at 14% 18%, color-mix(in srgb, var(--app-accent) 22%, transparent), transparent 36%),
    radial-gradient(circle at 88% 10%, color-mix(in srgb, var(--app-accent) 8%, transparent), transparent 30%),
    radial-gradient(circle at 76% 84%, color-mix(in srgb, var(--app-accent-2) 14%, transparent), transparent 28%),
    linear-gradient(185deg, var(--app-bg-secondary) 0%, var(--app-bg) 45%, var(--color-bg-3) 100%);
}

body::after {
  content: '';
  position: fixed;
  inset: auto 0 0 0;
  height: 44vh;
  pointer-events: none;
  background: linear-gradient(180deg, transparent 0%, rgba(255, 178, 102, 0.07) 100%);
}

button,
input,
textarea,
select {
  font: inherit;
}

a {
  color: inherit;
}

img {
  max-width: 100%;
}

::selection {
  background: rgba(15, 140, 122, 0.22);
  color: inherit;
}

html[data-theme='dark'] ::selection {
  background: rgba(90, 228, 204, 0.3);
}

:focus-visible {
  outline: 2px solid var(--app-accent);
  outline-offset: 3px;
}

:focus:not(:focus-visible) {
  outline: none;
}

::-webkit-scrollbar {
  width: 11px;
  height: 11px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  border-radius: 999px;
  border: 3px solid transparent;
  background: linear-gradient(180deg, rgba(15, 140, 122, 0.68), rgba(255, 138, 61, 0.62));
  background-clip: padding-box;
}

html[data-theme='dark'] ::-webkit-scrollbar-thumb {
  background: linear-gradient(
    185deg,
    rgba(83, 219, 194, 0.55),
    rgba(56, 189, 248, 0.42),
    rgba(167, 139, 250, 0.38)
  );
  background-clip: padding-box;
}

#app {
  isolation: isolate;
  writing-mode: horizontal-tb;
  text-orientation: mixed;
  width: 100%;
  min-height: 100%;
}

/**
 * 应用壳层曾使用泛用类名 `.layout`，易与浏览器插件 / 其它全局 CSS 中的 `.layout { flex-direction: row }`
 * 等规则冲突，表现为顶栏与主区被压成左缘几条竖列、中文一字一行。
 * 壳层已改为 `.mayue-app-shell`；此处再用 !important 兜底一次，确保纵向全宽。
 */
#app .mayue-app-shell {
  display: flex !important;
  flex-direction: column !important;
  flex-wrap: nowrap !important;
  align-items: stretch !important;
  width: 100% !important;
  min-width: 0 !important;
  max-width: none !important;
  writing-mode: horizontal-tb !important;
}

#app .mayue-app-shell > .header,
#app .mayue-app-shell > main.main {
  width: 100% !important;
  min-width: 0 !important;
  max-width: none !important;
}

#app .mayue-app-shell > footer {
  flex: 0 0 auto;
  width: 100% !important;
  min-width: 0 !important;
}

#app .mayue-app-shell .main-surface,
#app .mayue-app-shell .home-landing {
  width: 100% !important;
  min-width: 0 !important;
  max-width: none !important;
}
</style>
