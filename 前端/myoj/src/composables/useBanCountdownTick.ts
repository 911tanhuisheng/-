import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

/** 全局共享的「当前时间」毫秒，每秒更新，供封禁倒计时文案使用 */
const nowMs = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null
let subscriberCount = 0

function startTick() {
  if (timer != null) return
  nowMs.value = Date.now()
  timer = setInterval(() => {
    nowMs.value = Date.now()
  }, 1000)
}

function stopTick() {
  subscriberCount = Math.max(0, subscriberCount - 1)
  if (subscriberCount === 0 && timer != null) {
    clearInterval(timer)
    timer = null
  }
}

/**
 * 订阅封禁倒计时刷新（多组件共用一个 1s 定时器）。
 */
export function useBanCountdownTick(): { nowMs: Ref<number> } {
  onMounted(() => {
    subscriberCount += 1
    startTick()
  })
  onBeforeUnmount(() => {
    stopTick()
  })
  return { nowMs }
}
