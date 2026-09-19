<script setup lang="ts">
import { computed } from 'vue'

export type ModerationBanVariant = 'account' | 'comment' | 'ai'

const props = withDefaults(
  defineProps<{
    variant: ModerationBanVariant
    title: string
    /** 副标题 / 说明 */
    description?: string
    /** 剩余时间（已格式化） */
    countdown?: string
    reason?: string
    /** 底部补充说明 */
    footer?: string
    /** 嵌入评论区 / AI 面板时使用紧凑布局 */
    compact?: boolean
  }>(),
  {
    description: '',
    countdown: '',
    reason: '',
    footer: '',
    compact: false,
  },
)

const variantLabel = computed(() => {
  if (props.variant === 'ai') return 'AI GUARD'
  if (props.variant === 'comment') return 'MODERATION'
  return 'ACCOUNT LOCK'
})

const hasCountdown = computed(() => !!props.countdown?.trim())
const hasReason = computed(() => !!props.reason?.trim())
</script>

<template>
  <div
    class="mod-ban"
    :class="[`mod-ban--${variant}`, { 'mod-ban--compact': compact }]"
    role="status"
    aria-live="polite"
  >
    <div class="mod-ban__glow" aria-hidden="true" />
    <div class="mod-ban__grid" aria-hidden="true" />
    <div class="mod-ban__scan" aria-hidden="true" />

    <div class="mod-ban__body">
      <div class="mod-ban__icon-wrap" aria-hidden="true">
        <svg v-if="variant === 'ai'" class="mod-ban__icon" viewBox="0 0 24 24" fill="none">
          <path
            d="M12 2L3 7v6c0 5 3.8 9.7 9 11 5.2-1.3 9-6 9-11V7l-9-5z"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linejoin="round"
          />
          <path d="M9 12h6M12 9v6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        </svg>
        <svg v-else-if="variant === 'comment'" class="mod-ban__icon" viewBox="0 0 24 24" fill="none">
          <path
            d="M4 5h16v11H8l-4 4V5z"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linejoin="round"
          />
          <path d="M8 10h8M8 13h5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        </svg>
        <svg v-else class="mod-ban__icon" viewBox="0 0 24 24" fill="none">
          <rect x="5" y="11" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.5" />
          <path
            d="M8 11V8a4 4 0 0 1 8 0v3"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
          />
          <circle cx="12" cy="16" r="1.25" fill="currentColor" />
        </svg>
        <span class="mod-ban__icon-ring" />
      </div>

      <div class="mod-ban__content">
        <div class="mod-ban__head">
          <span class="mod-ban__tag">{{ variantLabel }}</span>
          <h3 class="mod-ban__title">{{ title }}</h3>
        </div>
        <p v-if="description" class="mod-ban__desc">{{ description }}</p>
        <p v-if="hasReason" class="mod-ban__reason">
          <span class="mod-ban__reason-label">原因</span>
          <span class="mod-ban__reason-text">{{ reason }}</span>
        </p>
        <p v-if="footer" class="mod-ban__footer">{{ footer }}</p>
      </div>

      <div v-if="hasCountdown" class="mod-ban__countdown" aria-label="剩余限制时间">
        <span class="mod-ban__countdown-label">剩余</span>
        <span class="mod-ban__countdown-value">{{ countdown }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mod-ban {
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid transparent;
}

.mod-ban--compact {
  border-radius: 14px;
  border: 1px solid transparent;
  margin: 0;
}

.mod-ban__glow {
  position: absolute;
  inset: -40% -10%;
  pointer-events: none;
  opacity: 0.55;
  filter: blur(40px);
  animation: mod-ban-glow 6s ease-in-out infinite alternate;
}

.mod-ban__grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.35;
  background-image:
    linear-gradient(color-mix(in srgb, var(--mod-accent) 12%, transparent) 1px, transparent 1px),
    linear-gradient(90deg, color-mix(in srgb, var(--mod-accent) 12%, transparent) 1px, transparent 1px);
  background-size: 24px 24px;
  mask-image: linear-gradient(180deg, black 0%, transparent 100%);
}

.mod-ban__scan {
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  top: 0;
  background: linear-gradient(
    90deg,
    transparent,
    var(--mod-accent),
    color-mix(in srgb, var(--mod-accent) 60%, #fff),
    var(--mod-accent),
    transparent
  );
  opacity: 0.7;
  animation: mod-ban-scan 3.2s linear infinite;
}

.mod-ban__body {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 14px 22px;
}

.mod-ban--compact .mod-ban__body {
  flex-wrap: wrap;
  gap: 12px;
  padding: 14px 16px;
}

.mod-ban__icon-wrap {
  position: relative;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  background: color-mix(in srgb, var(--mod-accent) 14%, transparent);
  border: 1px solid color-mix(in srgb, var(--mod-accent) 35%, transparent);
  box-shadow:
    0 0 24px color-mix(in srgb, var(--mod-accent) 25%, transparent),
    inset 0 1px 0 color-mix(in srgb, #fff 12%, transparent);
}

.mod-ban--compact .mod-ban__icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 10px;
}

.mod-ban__icon {
  width: 22px;
  height: 22px;
  color: var(--mod-accent-bright);
}

.mod-ban__icon-ring {
  position: absolute;
  inset: -3px;
  border-radius: 14px;
  border: 1px dashed color-mix(in srgb, var(--mod-accent) 45%, transparent);
  animation: mod-ban-spin 12s linear infinite;
}

.mod-ban__content {
  flex: 1;
  min-width: 0;
}

.mod-ban__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
  margin-bottom: 4px;
}

.mod-ban__tag {
  font-family: var(--app-font-mono, ui-monospace, 'Cascadia Code', monospace);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.14em;
  padding: 3px 8px;
  border-radius: 4px;
  color: var(--mod-accent-bright);
  background: color-mix(in srgb, var(--mod-accent) 18%, transparent);
  border: 1px solid color-mix(in srgb, var(--mod-accent) 32%, transparent);
}

.mod-ban__title {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--mod-title);
  line-height: 1.35;
}

.mod-ban--compact .mod-ban__title {
  font-size: 14px;
}

.mod-ban__desc {
  margin: 0 0 6px;
  font-size: 13px;
  line-height: 1.55;
  color: var(--mod-text);
}

.mod-ban__reason {
  margin: 0 0 6px;
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  font-size: 12px;
  line-height: 1.5;
}

.mod-ban__reason-label {
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--mod-accent-bright);
  opacity: 0.9;
}

.mod-ban__reason-text {
  color: var(--mod-text-muted);
  word-break: break-word;
}

.mod-ban__footer {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--mod-text-muted);
  opacity: 0.92;
}

.mod-ban__countdown {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: center;
  padding: 8px 14px;
  border-radius: 10px;
  background: color-mix(in srgb, var(--mod-accent) 10%, transparent);
  border: 1px solid color-mix(in srgb, var(--mod-accent) 28%, transparent);
  min-width: 108px;
}

.mod-ban--compact .mod-ban__countdown {
  width: 100%;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  min-width: unset;
}

.mod-ban__countdown-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--mod-text-muted);
  margin-bottom: 2px;
}

.mod-ban--compact .mod-ban__countdown-label {
  margin-bottom: 0;
}

.mod-ban__countdown-value {
  font-family: var(--app-font-mono, ui-monospace, 'Cascadia Code', monospace);
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--mod-accent-bright);
  text-shadow: 0 0 20px color-mix(in srgb, var(--mod-accent) 50%, transparent);
  animation: mod-ban-pulse 2s ease-in-out infinite;
}

/* —— 账号禁用：红橙警示 —— */
.mod-ban--account {
  --mod-accent: #f97316;
  --mod-accent-bright: #fdba74;
  --mod-title: #fff7ed;
  --mod-text: rgba(255, 247, 237, 0.92);
  --mod-text-muted: rgba(254, 215, 170, 0.78);
  background: linear-gradient(
    105deg,
    rgba(15, 10, 8, 0.94) 0%,
    rgba(40, 18, 10, 0.88) 45%,
    rgba(28, 12, 8, 0.92) 100%
  );
  border-bottom-color: color-mix(in srgb, var(--mod-accent) 40%, transparent);
}

html[data-theme='light'] .mod-ban--account {
  --mod-title: #431407;
  --mod-text: #7c2d12;
  --mod-text-muted: #9a3412;
  background: linear-gradient(
    105deg,
    rgba(255, 247, 237, 0.98) 0%,
    rgba(254, 226, 200, 0.95) 50%,
    rgba(255, 237, 213, 0.98) 100%
  );
}

/* —— 评论限制：琥珀 —— */
.mod-ban--comment {
  --mod-accent: #f59e0b;
  --mod-accent-bright: #fcd34d;
  --mod-title: #fffbeb;
  --mod-text: rgba(255, 251, 235, 0.9);
  --mod-text-muted: rgba(253, 230, 138, 0.75);
  background: linear-gradient(
    105deg,
    rgba(12, 10, 6, 0.94) 0%,
    rgba(35, 25, 8, 0.9) 50%,
    rgba(20, 16, 8, 0.93) 100%
  );
  border-bottom-color: color-mix(in srgb, var(--mod-accent) 38%, transparent);
}

html[data-theme='light'] .mod-ban--comment {
  --mod-title: #422006;
  --mod-text: #78350f;
  --mod-text-muted: #92400e;
  background: linear-gradient(
    105deg,
    rgba(255, 251, 235, 0.98) 0%,
    rgba(254, 243, 199, 0.96) 100%
  );
}

/* —— AI 限制：青蓝科技 —— */
.mod-ban--ai {
  --mod-accent: #22d3ee;
  --mod-accent-bright: #67e8f9;
  --mod-title: #ecfeff;
  --mod-text: rgba(236, 254, 255, 0.9);
  --mod-text-muted: rgba(103, 232, 249, 0.72);
  background: linear-gradient(
    105deg,
    rgba(6, 12, 20, 0.96) 0%,
    rgba(8, 28, 42, 0.92) 40%,
    rgba(10, 18, 32, 0.95) 100%
  );
  border-bottom-color: color-mix(in srgb, var(--mod-accent) 35%, transparent);
}

html[data-theme='light'] .mod-ban--ai {
  --mod-accent: #0891b2;
  --mod-accent-bright: #0e7490;
  --mod-title: #083344;
  --mod-text: #155e75;
  --mod-text-muted: #0e7490;
  background: linear-gradient(
    105deg,
    rgba(236, 254, 255, 0.98) 0%,
    rgba(207, 250, 254, 0.95) 55%,
    rgba(224, 242, 254, 0.98) 100%
  );
}

@keyframes mod-ban-glow {
  from {
    transform: translateX(-8%) scale(1);
    opacity: 0.4;
  }
  to {
    transform: translateX(8%) scale(1.05);
    opacity: 0.65;
  }
}

@keyframes mod-ban-scan {
  0% {
    top: 0;
    opacity: 0;
  }
  8% {
    opacity: 0.85;
  }
  92% {
    opacity: 0.85;
  }
  100% {
    top: 100%;
    opacity: 0;
  }
}

@keyframes mod-ban-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes mod-ban-pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.82;
  }
}

@media (max-width: 640px) {
  .mod-ban__body {
    flex-wrap: wrap;
    padding: 12px 14px;
  }

  .mod-ban__countdown {
    width: 100%;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    min-width: unset;
  }

  .mod-ban__countdown-label {
    margin-bottom: 0;
  }
}
</style>
