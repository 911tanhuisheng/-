import { nextTick, onMounted, onBeforeUnmount, type Ref } from 'vue'

const SELECTOR = '[data-scroll-reveal]'

function markIn(el: HTMLElement) {
  el.classList.add('scroll-reveal--in')
}

/** 已进入视口的节点立即补上 class（避免 IO 首帧未回调时一直 opacity:0） */
function revealAlreadyVisible(root: HTMLElement) {
  const vh = typeof window !== 'undefined' ? window.innerHeight : 0
  root.querySelectorAll<HTMLElement>(SELECTOR).forEach((el) => {
    if (el.classList.contains('scroll-reveal--in')) return
    const r = el.getBoundingClientRect()
    if (r.top < vh * 0.92 && r.bottom > -40) {
      markIn(el)
    }
  })
}

/**
 * 在 root 内观察带 data-scroll-reveal 的元素，进入视口时添加 scroll-reveal--in。
 * 配合 CSS：初始 opacity/transform，--sr-delay 控制错峰。
 */
export function useScrollReveal(rootRef: Ref<HTMLElement | null>) {
  let observer: IntersectionObserver | null = null

  onMounted(() => {
    const setup = () => {
      const root = rootRef.value
      if (!root) return

      const els = root.querySelectorAll<HTMLElement>(SELECTOR)
      if (els.length === 0) return

      if (typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
        els.forEach((el) => markIn(el))
        return
      }

      observer = new IntersectionObserver(
        (entries) => {
          entries.forEach((entry) => {
            if (!entry.isIntersecting) return
            markIn(entry.target as HTMLElement)
            observer?.unobserve(entry.target)
          })
        },
        {
          root: null,
          rootMargin: '0px 0px -4% 0px',
          threshold: [0, 0.02, 0.06, 0.12],
        },
      )

      els.forEach((el) => observer!.observe(el))

      requestAnimationFrame(() => {
        revealAlreadyVisible(root)
      })
    }

    void nextTick(() => {
      requestAnimationFrame(setup)
    })
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
  })
}
