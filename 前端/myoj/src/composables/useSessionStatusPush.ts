import { onBeforeUnmount, watch } from 'vue'
import { Message } from '@arco-design/web-vue'
import { connectSessionStatusEvents } from '@/api/sessionStatusEvents'
import { useAuthStore } from '@/stores/auth'

const RECONNECT_MS = 2_000

/**
 * 登录后维持 SSE，管理员禁用/启用等变更后几乎实时刷新用户端状态（辅以低频轮询兜底）。
 */
export function useSessionStatusPush() {
  const auth = useAuthStore()
  let disconnect: (() => void) | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let syncing = false

  async function pullAndReact() {
    if (!auth.isLoggedIn || syncing) return
    syncing = true
    const wasDisabled = auth.isAccountDisabled
    const wasCommentBanned = auth.isCommentBanned
    try {
      await auth.syncSessionStatus()
      if (!wasDisabled && auth.isAccountDisabled) {
        window.dispatchEvent(new CustomEvent('myoj:account-disabled'))
      } else if (wasDisabled && !auth.isAccountDisabled) {
        window.dispatchEvent(new CustomEvent('myoj:account-enabled'))
        Message.success('账号已恢复，可正常使用各项功能')
      }
      if (!wasCommentBanned && auth.isCommentBanned) {
        Message.warning('评论功能已被限制，请查看页面提示')
      } else if (wasCommentBanned && !auth.isCommentBanned) {
        Message.success('评论限制已解除，可以正常发表评论')
      }
    } finally {
      syncing = false
    }
  }

  function teardown() {
    if (reconnectTimer != null) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    disconnect?.()
    disconnect = null
  }

  function scheduleReconnect() {
    if (reconnectTimer != null || !auth.isLoggedIn) return
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null
      connect()
    }, RECONNECT_MS)
  }

  function connect() {
    teardown()
    const token = auth.accessToken ?? auth.token
    if (!token?.trim()) return

    disconnect = connectSessionStatusEvents(token, {
      onRevision: () => {
        void pullAndReact()
      },
      onDisconnect: () => {
        scheduleReconnect()
      },
    })
  }

  watch(
    () => auth.isLoggedIn,
    (loggedIn) => {
      if (loggedIn) {
        void pullAndReact()
        connect()
      } else {
        teardown()
      }
    },
    { immediate: true },
  )

  watch(
    () => auth.accessToken ?? auth.token,
    (next, prev) => {
      if (!auth.isLoggedIn || !next?.trim() || next === prev) return
      connect()
    },
  )

  onBeforeUnmount(() => {
    teardown()
  })
}
