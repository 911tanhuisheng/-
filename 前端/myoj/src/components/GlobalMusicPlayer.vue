<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  shallowRef,
  watch,
} from 'vue'
import gsap from 'gsap'
import { useMusicPlayerStore } from '@/stores/musicPlayer'

const store = useMusicPlayerStore()

const rootRef = ref<HTMLElement | null>(null)
const panelRef = ref<HTMLElement | null>(null)
const openBurstRef = ref<HTMLElement | null>(null)
const coreRef = ref<HTMLElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const audioRef = ref<HTMLAudioElement | null>(null)
const playBtnRef = ref<HTMLElement | null>(null)

const reducedMotion = ref(
  typeof window !== 'undefined' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches,
)

/** 与视口边缘留白：避免展开卡片贴边裁切 */
const VIEW_PAD = 20
const COLLAPSED_SIZE = 64

/** 相对视口的左上角坐标（px），用于 position:fixed；默认锚定在左下角 */
const pos = reactive({ x: VIEW_PAD, y: 100 })

const dragging = ref(false)
let dragGrabX = 0
let dragGrabY = 0
/** 收起态：在胶囊上按下，先不拖；移动超过阈值再视为拖动，否则松手视为点击展开 */
let pendingCoreDrag = false
let coreStartX = 0
let coreStartY = 0
const CORE_DRAG_THRESHOLD_PX = 6

const seeking = ref(false)
const volumePopover = ref(false)

const audioCtx = shallowRef<AudioContext | null>(null)
const analyserNode = shallowRef<AnalyserNode | null>(null)
let mediaConnected = false

let rafId = 0
let breatheT = 0

/** 展开前收起态的左上角，用于收起时还原位置（避免面板高度变化导致锚点重算跳动） */
let collapsedPosBeforeExpand: { x: number; y: number } | null = null

const fftSize = 256
const barCount = 64
const barW = 4
const gap = 2

const beatPulse = ref(0.35)

const rootStyle = computed(() => ({
  left: `${pos.x}px`,
  top: `${pos.y}px`,
  right: 'auto',
  bottom: 'auto',
}))

const progressPct = computed(() =>
  store.duration > 0 ? Math.min(100, (store.currentTime / store.duration) * 100) : 0,
)

const sortedLyrics = computed(() => {
  const L = store.currentTrack?.lyrics
  if (!L?.length) return []
  return [...L].sort((a, b) => a.t - b.t)
})

/** 当前歌词行索引（用于 Transition key） */
const lyricLineKey = computed(() => {
  const lines = sortedLyrics.value
  const t = store.currentTime
  let idx = -1
  for (let i = 0; i < lines.length; i++) {
    if (lines[i]!.t <= t + 0.06) idx = i
    else break
  }
  return idx
})

const lyricCurrentText = computed(() => {
  const lines = sortedLyrics.value
  const i = lyricLineKey.value
  if (i >= 0) return lines[i]!.text
  return lines[0]?.text ?? ''
})

const lyricNextText = computed(() => {
  const lines = sortedLyrics.value
  const t = store.currentTime
  return lines.find((l) => l.t > t + 0.1)?.text ?? ''
})

function isTypingTarget(el: EventTarget | null) {
  const t = el as HTMLElement | null
  if (!t?.closest) return false
  return !!t.closest(
    'input, textarea, select, [contenteditable="true"], [role="textbox"], .monaco-editor',
  )
}

function primeAudioGraph() {
  const audio = audioRef.value
  if (!audio || mediaConnected || reducedMotion.value) return
  try {
    const ctx = new AudioContext()
    audioCtx.value = ctx
    const an = ctx.createAnalyser()
    an.fftSize = fftSize
    an.smoothingTimeConstant = 0.78
    analyserNode.value = an
    const src = ctx.createMediaElementSource(audio)
    src.connect(an)
    an.connect(ctx.destination)
    mediaConnected = true
  } catch {
    analyserNode.value = null
  }
}

async function resumeCtx() {
  const ctx = audioCtx.value
  if (ctx?.state === 'suspended') await ctx.resume().catch(() => {})
}

function togglePlay() {
  const el = audioRef.value
  if (!el) return
  primeAudioGraph()
  void resumeCtx()
  if (el.paused) {
    void el.play().then(() => {
      store.isPlaying = true
    })
  } else {
    el.pause()
    store.isPlaying = false
  }
}

function onTimeUpdate() {
  const el = audioRef.value
  if (!el || seeking.value) return
  store.currentTime = el.currentTime
}

function onLoadedMeta() {
  const el = audioRef.value
  if (!el) return
  store.duration = el.duration || 0
}

function onEnded() {
  store.isPlaying = false
  store.next()
}

function loadCurrentTrack() {
  const tr = store.currentTrack
  const el = audioRef.value
  if (!tr || !el) return
  el.crossOrigin = 'anonymous'
  el.src = tr.src
  el.load()
}

watch(
  () => store.currentIndex,
  () => {
    loadCurrentTrack()
    void nextTick(() => {
      if (store.isPlaying) void audioRef.value?.play()
    })
  },
)

watch(
  () => store.volume,
  (v) => {
    if (audioRef.value) audioRef.value.volume = v
  },
)

function seekClientX(clientX: number, bar: HTMLElement) {
  const el = audioRef.value
  if (!el || !store.duration) return
  const r = bar.getBoundingClientRect()
  const ratio = Math.max(0, Math.min(1, (clientX - r.left) / r.width))
  el.currentTime = ratio * store.duration
  store.currentTime = el.currentTime
}

function onSeekDown(e: MouseEvent) {
  seeking.value = true
  seekClientX(e.clientX, e.currentTarget as HTMLElement)
}
function onSeekMove(e: MouseEvent) {
  if (!seeking.value) return
  const bar = (e.target as HTMLElement).closest?.('.gmp-progress') as HTMLElement | null
  if (bar) seekClientX(e.clientX, bar)
}
function onSeekUp() {
  seeking.value = false
}

function selectTrack(i: number) {
  store.setTrackIndex(i)
  store.playlistOpen = false
  void nextTick(() => {
    primeAudioGraph()
    void resumeCtx()
    void audioRef.value?.play()
    store.isPlaying = true
  })
}

function skipPrev() {
  store.prev()
  void nextTick(() => {
    primeAudioGraph()
    void resumeCtx()
    void audioRef.value?.play()
    store.isPlaying = true
  })
}

/** 磁吸按钮 */
function magneticMove(e: MouseEvent) {
  if (reducedMotion.value || !playBtnRef.value) return
  const btn = playBtnRef.value
  const r = btn.getBoundingClientRect()
  const mx = e.clientX - (r.left + r.width / 2)
  const my = e.clientY - (r.top + r.height / 2)
  const nx = Math.max(-8, Math.min(8, mx * 0.18))
  const ny = Math.max(-8, Math.min(8, my * 0.18))
  gsap.to(btn, { x: nx, y: ny, duration: 0.22, ease: 'power2.out' })
}

function magneticLeave() {
  if (!playBtnRef.value) return
  gsap.to(playBtnRef.value, { x: 0, y: 0, duration: 0.35, ease: 'elastic.out(1, 0.6)' })
}

function ripplePlay(e: MouseEvent) {
  const btn = playBtnRef.value
  if (!btn || reducedMotion.value) return
  const r = btn.getBoundingClientRect()
  const ring = document.createElement('span')
  ring.className = 'gmp-ripple'
  ring.style.left = `${e.clientX - r.left}px`
  ring.style.top = `${e.clientY - r.top}px`
  btn.appendChild(ring)
  gsap.fromTo(
    ring,
    { scale: 0, opacity: 0.55 },
    {
      scale: 3.2,
      opacity: 0,
      duration: 0.65,
      ease: 'power2.out',
      onComplete: () => ring.remove(),
    },
  )
}

/** 面板 3D 倾斜 */
function panelPointer(e: PointerEvent) {
  if (reducedMotion.value || !panelRef.value) return
  const el = panelRef.value
  const r = el.getBoundingClientRect()
  const nx = (e.clientX - r.left) / Math.max(r.width, 1) - 0.5
  const ny = (e.clientY - r.top) / Math.max(r.height, 1) - 0.5
  const ry = nx * 10
  const rx = -ny * 10
  gsap.to(el, {
    rotateX: rx,
    rotateY: ry,
    duration: 0.35,
    ease: 'power2.out',
    transformPerspective: 1000,
  })
}

function panelLeave() {
  if (!panelRef.value || reducedMotion.value) return
  gsap.to(panelRef.value, {
    rotateX: 0,
    rotateY: 0,
    duration: 0.5,
    ease: 'power2.out',
  })
}

function drawSpectrum() {
  const canvas = canvasRef.value
  const an = analyserNode.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  const w = canvas.clientWidth
  const h = canvas.clientHeight
  if (w < 2 || h < 2) {
    /* 收起态 canvas 不可见，仍需驱动 beatPulse，外圈光晕才随音乐跳动 */
    if (an && store.isPlaying && !reducedMotion.value) {
      const data = new Uint8Array(an.frequencyBinCount)
      an.getByteFrequencyData(data)
      const step = Math.floor(data.length / barCount)
      let bassAcc = 0
      for (let i = 0; i < 8; i++) {
        let v = 0
        for (let j = 0; j < step; j++) v += data[i * step + j] ?? 0
        v /= step * 255
        bassAcc += v
      }
      beatPulse.value = Math.min(1, (bassAcc / 8) * 2.2)
    } else {
      breatheT += 0.012 * barCount
      beatPulse.value = 0.28 + Math.sin(breatheT * 0.8) * 0.08
    }
    rafId = requestAnimationFrame(drawSpectrum)
    return
  }
  canvas.width = Math.floor(w * dpr)
  canvas.height = Math.floor(h * dpr)
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  ctx.clearRect(0, 0, w, h)

  const data = new Uint8Array(an?.frequencyBinCount ?? 128)
  let active = false
  if (an && store.isPlaying && !reducedMotion.value) {
    an.getByteFrequencyData(data)
    active = true
  }

  let bassAcc = 0
  const step = Math.floor(data.length / barCount)

  for (let i = 0; i < barCount; i++) {
    let v = 0
    if (active) {
      for (let j = 0; j < step; j++) v += data[i * step + j] ?? 0
      v /= step * 255
      if (i < 8) bassAcc += v
    } else {
      breatheT += 0.012
      v =
        0.15 +
        Math.sin(breatheT + i * 0.22) * 0.12 +
        Math.sin(breatheT * 0.7 + i * 0.08) * 0.06
    }
    const bh = Math.max(4, v * h * 0.92)
    const x = i * (barW + gap)
    const y = h - bh

    const g = ctx.createLinearGradient(x, y + bh, x, y)
    g.addColorStop(0, '#2DD4BF')
    g.addColorStop(0.55, '#34d399')
    g.addColorStop(1, '#4ADE80')
    ctx.fillStyle = g
    ctx.fillRect(x, y, barW, bh)

    if (active && bh > 10) {
      ctx.fillStyle = 'rgba(255,255,255,0.35)'
      ctx.fillRect(x, y, barW, Math.min(6, bh * 0.15))
    }
  }

  if (active) {
    beatPulse.value = Math.min(1, (bassAcc / 8) * 2.2)
  } else {
    beatPulse.value = 0.28 + Math.sin(breatheT * 0.8) * 0.08
  }
  rafId = requestAnimationFrame(drawSpectrum)
}

function clampPos() {
  const root = rootRef.value
  const rw = root?.offsetWidth ?? COLLAPSED_SIZE
  const rh = root?.offsetHeight ?? COLLAPSED_SIZE
  const W = window.innerWidth
  const H = window.innerHeight
  pos.x = Math.max(VIEW_PAD, Math.min(W - rw - VIEW_PAD, pos.x))
  pos.y = Math.max(VIEW_PAD, Math.min(H - rh - VIEW_PAD, pos.y))
}

/** 展开尺寸下将左上角限制在视口内 */
function clampExpandedTopLeft(nx: number, ny: number, rw: number, rh: number) {
  const W = window.innerWidth
  const H = window.innerHeight
  return {
    x: Math.max(VIEW_PAD, Math.min(W - rw - VIEW_PAD, nx)),
    y: Math.max(VIEW_PAD, Math.min(H - rh - VIEW_PAD, ny)),
  }
}

function snapDock() {
  const root = rootRef.value
  if (!root) return
  void root.offsetWidth
  const r = root.getBoundingClientRect()
  const H = window.innerHeight
  /** 始终吸附到视口左下角（与 onMounted 初始位置一致） */
  const best = { x: VIEW_PAD, y: H - VIEW_PAD - r.height }

  gsap.to(pos, {
    x: best.x,
    y: best.y,
    duration: reducedMotion.value ? 0.22 : 0.55,
    ease: reducedMotion.value ? 'power2.out' : 'elastic.out(1, 0.72)',
  })
}

function onDragStart(e: PointerEvent) {
  if ((e.target as HTMLElement).closest?.('.gmp-no-drag')) return
  const root = e.currentTarget as HTMLElement
  if ((e.target as HTMLElement).closest?.('.gmp-core')) {
    pendingCoreDrag = true
    coreStartX = e.clientX
    coreStartY = e.clientY
    dragGrabX = e.clientX - pos.x
    dragGrabY = e.clientY - pos.y
    root.setPointerCapture(e.pointerId)
    return
  }
  dragging.value = true
  root.setPointerCapture(e.pointerId)
  dragGrabX = e.clientX - pos.x
  dragGrabY = e.clientY - pos.y
}

function onDragMove(e: PointerEvent) {
  if (pendingCoreDrag) {
    const dx = e.clientX - coreStartX
    const dy = e.clientY - coreStartY
    if (dx * dx + dy * dy > CORE_DRAG_THRESHOLD_PX * CORE_DRAG_THRESHOLD_PX) {
      pendingCoreDrag = false
      dragging.value = true
    } else {
      return
    }
  }
  if (!dragging.value) return
  pos.x = e.clientX - dragGrabX
  pos.y = e.clientY - dragGrabY
  clampPos()
}

function onDragEnd(e: PointerEvent) {
  const root = e.currentTarget as HTMLElement
  if (pendingCoreDrag) {
    pendingCoreDrag = false
    try {
      root.releasePointerCapture(e.pointerId)
    } catch {
      /* already released */
    }
    if (e.type !== 'pointercancel') void expandPanel()
    return
  }
  if (!dragging.value) return
  dragging.value = false
  try {
    root.releasePointerCapture(e.pointerId)
  } catch {
    /* */
  }
  snapDock()
}

async function expandPanel() {
  collapsedPosBeforeExpand = { x: pos.x, y: pos.y }

  const rootBefore = rootRef.value
  const cw = rootBefore?.offsetWidth ?? COLLAPSED_SIZE
  const ch = rootBefore?.offsetHeight ?? COLLAPSED_SIZE
  const anchorRight = pos.x + cw
  const anchorBottom = pos.y + ch

  store.isExpanded = true
  await nextTick()
  await new Promise<void>((resolve) => {
    requestAnimationFrame(() => requestAnimationFrame(() => resolve()))
  })

  const panel = panelRef.value
  const burst = openBurstRef.value
  const root = rootRef.value
  if (!panel || !root) return

  const rw = root.offsetWidth
  const rh = root.offsetHeight
  let nx = anchorRight - rw
  let ny = anchorBottom - rh
  const fit = clampExpandedTopLeft(nx, ny, rw, rh)
  nx = fit.x
  ny = fit.y

  const layers = panel.querySelector('.gmp-panel-layers')
  const stags = panel.querySelectorAll('.gmp-stag')

  if (reducedMotion.value) {
    pos.x = nx
    pos.y = ny
    gsap.set(panel, { clearProps: 'transform,opacity,filter' })
    if (layers) gsap.set(layers, { clearProps: 'opacity,scale' })
    gsap.set(stags, { clearProps: 'opacity,y' })
    if (burst) gsap.set(burst, { clearProps: 'opacity,scale' })
    return
  }

  gsap.killTweensOf([pos, panel, layers, burst, ...Array.from(stags)].filter(Boolean))

  gsap.set(panel, {
    transformOrigin: '100% 100%',
    scale: 0.06,
    opacity: 0,
    filter: 'blur(16px) brightness(1.12)',
    rotateX: -6,
    transformPerspective: 980,
  })
  if (layers) gsap.set(layers, { opacity: 0.35, scale: 1.06 })
  if (burst) {
    gsap.set(burst, {
      scale: 0.35,
      opacity: 0.85,
      transformOrigin: '55% 55%',
    })
  }

  const tl = gsap.timeline()
  tl.to(
    pos,
    {
      x: nx,
      y: ny,
      duration: 0.58,
      ease: 'power3.out',
    },
    0,
  )
  tl.to(
    panel,
    {
      scale: 1,
      opacity: 1,
      filter: 'blur(0px) brightness(1)',
      rotateX: 0,
      duration: 0.72,
      ease: 'power4.out',
    },
    0,
  )
  if (layers) {
    tl.to(
      layers,
      { opacity: 1, scale: 1, duration: 0.58, ease: 'power2.out' },
      0.1,
    )
  }
  if (burst) {
    tl.to(
      burst,
      {
        scale: 2.35,
        opacity: 0,
        duration: 0.62,
        ease: 'power2.out',
      },
      0,
    )
  }
  tl.from(
    stags,
    {
      opacity: 0,
      y: 26,
      stagger: 0.065,
      duration: 0.52,
      ease: 'power3.out',
    },
    0.22,
  )
}

function restoreCollapsedPosition() {
  gsap.killTweensOf(pos)
  const snap = collapsedPosBeforeExpand
  collapsedPosBeforeExpand = null
  if (snap) {
    pos.x = snap.x
    pos.y = snap.y
  }
  clampPos()
}

function collapsePanel() {
  const panel = panelRef.value
  const root = rootRef.value
  if (!panel || !root) {
    store.isExpanded = false
    restoreCollapsedPosition()
    return
  }

  if (reducedMotion.value) {
    store.isExpanded = false
    store.playlistOpen = false
    void nextTick(() => {
      restoreCollapsedPosition()
    })
    return
  }

  const layers = panel.querySelector('.gmp-panel-layers')
  const stags = panel.querySelectorAll('.gmp-stag')
  const burst = openBurstRef.value

  gsap.killTweensOf([pos, panel, layers, burst, ...Array.from(stags)].filter(Boolean))

  gsap.to(stags, {
    opacity: 0,
    y: 16,
    stagger: { each: 0.04, from: 'end' },
    duration: 0.26,
    ease: 'power2.in',
  })
  if (layers) {
    gsap.to(layers, {
      opacity: 0,
      scale: 0.94,
      duration: 0.34,
      ease: 'power2.in',
    })
  }
  gsap.to(panel, {
    scale: 0.08,
    opacity: 0,
    filter: 'blur(14px)',
    rotateX: 5,
    duration: 0.52,
    ease: 'power3.in',
    delay: 0.06,
    onComplete: () => {
      store.isExpanded = false
      store.playlistOpen = false
      gsap.set(panel, { clearProps: 'scale,opacity,filter,rotateX' })
      if (layers) gsap.set(layers, { clearProps: 'opacity,scale' })
      if (burst) gsap.set(burst, { clearProps: 'opacity,scale' })
      void nextTick(() => {
        restoreCollapsedPosition()
      })
    },
  })
}

function toggleExpand() {
  if (store.isExpanded) collapsePanel()
  else void expandPanel()
}

function onKeyDown(e: KeyboardEvent) {
  if (isTypingTarget(e.target)) return
  if (e.code === 'KeyM') {
    e.preventDefault()
    toggleExpand()
    return
  }
  if (e.code === 'Space') {
    e.preventDefault()
    togglePlay()
    return
  }
  if (e.code === 'ArrowRight') {
    e.preventDefault()
    const el = audioRef.value
    if (el) el.currentTime = Math.min(store.duration, el.currentTime + 5)
    return
  }
  if (e.code === 'ArrowLeft') {
    e.preventDefault()
    const el = audioRef.value
    if (el) el.currentTime = Math.max(0, el.currentTime - 5)
    return
  }
}

function onResize() {
  clampPos()
}

watch(
  [() => store.isExpanded, () => store.playlistOpen],
  () => {
    if (!store.isExpanded) return
    void nextTick(() => {
      requestAnimationFrame(() => clampPos())
    })
  },
)

watch(
  () => store.isExpanded,
  async (expanded, prev) => {
    if (expanded || prev === undefined) return
    const core = coreRef.value
    if (!core || reducedMotion.value) return
    await nextTick()
    const defaultShadow =
      '0 16px 40px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(45, 212, 191, 0.12), 0 0 28px rgba(45, 212, 191, 0.18) inset'
    gsap
      .timeline()
      .fromTo(
        core,
        { scale: 0.35, opacity: 0 },
        { scale: 1, opacity: 1, duration: 0.42, ease: 'back.out(1.7)' },
      )
      .fromTo(
        core,
        { boxShadow: '0 0 0 14px rgba(45,212,191,0.48)' },
        { boxShadow: defaultShadow, duration: 0.48 },
        '-=0.12',
      )
  },
)

onMounted(() => {
  const h = COLLAPSED_SIZE
  pos.x = VIEW_PAD
  pos.y = window.innerHeight - VIEW_PAD - h
  loadCurrentTrack()
  if (audioRef.value) audioRef.value.volume = store.volume
  drawSpectrum()
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('mousemove', onSeekMove)
  window.addEventListener('mouseup', onSeekUp)
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('mousemove', onSeekMove)
  window.removeEventListener('mouseup', onSeekUp)
  window.removeEventListener('resize', onResize)
  audioCtx.value?.close().catch(() => {})
})
</script>

<template>
  <Teleport to="body">
    <div
      v-if="store.tracks.length > 0"
      ref="rootRef"
      class="gmp"
      :class="{ 'gmp--expanded': store.isExpanded, 'gmp--drag': dragging }"
      :style="rootStyle"
      @pointerdown="onDragStart"
      @pointermove="onDragMove"
      @pointerup="onDragEnd"
      @pointercancel="onDragEnd"
    >
      <!-- 声纳背景（展开时） -->
      <svg
        v-if="store.isExpanded && !reducedMotion"
        class="gmp-sonar"
        aria-hidden="true"
      >
        <circle class="gmp-sonar-ring gmp-sonar-ring--a" cx="50%" cy="50%" r="28%" />
        <circle class="gmp-sonar-ring gmp-sonar-ring--b" cx="50%" cy="50%" r="28%" />
        <circle class="gmp-sonar-ring gmp-sonar-ring--c" cx="50%" cy="50%" r="28%" />
      </svg>

      <!-- 收起：音频核心（科技感动态：播放随节拍脉动 + 能量环） -->
      <button
        ref="coreRef"
        type="button"
        class="gmp-core"
        :class="{
          'gmp-core--hidden': store.isExpanded,
          'gmp-core--live': store.isPlaying && !reducedMotion,
          'gmp-core--idle-pulse': !store.isPlaying && !reducedMotion,
        }"
        :style="{ '--core-beat': beatPulse }"
        aria-label="展开音乐播放器"
        @keydown.enter.prevent="expandPanel"
        @keydown.space.prevent="expandPanel"
      >
        <span class="gmp-core-layers" aria-hidden="true">
          <span class="gmp-core-sweep" />
          <span class="gmp-core-orbit" />
          <span class="gmp-core-halo" />
        </span>
        <svg
          class="gmp-core-note"
          viewBox="0 0 24 24"
          width="26"
          height="26"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M9 18V5l12-2v13" />
          <circle cx="6" cy="18" r="3" fill="currentColor" stroke="none" />
        </svg>
      </button>

      <!-- 展开卡片 -->
      <div
        v-show="store.isExpanded"
        ref="panelRef"
        class="gmp-panel"
        @pointermove="panelPointer"
        @pointerleave="panelLeave"
      >
        <!-- 深空动态背景（磨砂下层：光球 / 星野 / 网格） -->
        <div class="gmp-panel-layers" aria-hidden="true">
          <div
            class="gmp-dyn-bg"
            :class="{ 'gmp-dyn-bg--still': reducedMotion }"
          >
            <div class="gmp-dyn-bg-deep" />
            <div class="gmp-orb gmp-orb--a" />
            <div class="gmp-orb gmp-orb--b" />
            <div class="gmp-orb gmp-orb--c" />
            <div class="gmp-orb gmp-orb--d" />
            <div class="gmp-grid-flo" />
            <svg class="gmp-starfield" viewBox="0 0 420 320" preserveAspectRatio="xMidYMid slice">
              <circle class="gmp-s s0" cx="12%" cy="18%" r="0.9" fill="#e2e8f0" />
              <circle class="gmp-s s1" cx="88%" cy="12%" r="0.7" fill="#94a3b8" />
              <circle class="gmp-s s2" cx="72%" cy="38%" r="1.1" fill="#e2e8f0" />
              <circle class="gmp-s s3" cx="22%" cy="55%" r="0.6" fill="#cbd5e1" />
              <circle class="gmp-s s4" cx="55%" cy="22%" r="0.8" fill="#f1f5f9" />
              <circle class="gmp-s s5" cx="38%" cy="72%" r="1" fill="#94a3b8" />
              <circle class="gmp-s s6" cx="92%" cy="68%" r="0.65" fill="#e2e8f0" />
              <circle class="gmp-s s7" cx="8%" cy="82%" r="0.75" fill="#cbd5e1" />
              <circle class="gmp-s s8" cx="48%" cy="88%" r="0.55" fill="#94a3b8" />
              <circle class="gmp-s s9" cx="65%" cy="8%" r="0.5" fill="#f8fafc" />
              <circle class="gmp-s s10" cx="18%" cy="35%" r="0.45" fill="#94a3b8" />
              <circle class="gmp-s s11" cx="82%" cy="45%" r="0.9" fill="#e2e8f0" />
              <circle class="gmp-s s12" cx="30%" cy="12%" r="0.4" fill="#64748b" />
              <circle class="gmp-s s13" cx="95%" cy="28%" r="0.5" fill="#cbd5e1" />
            </svg>
            <div class="gmp-dyn-shimmer" />
          </div>
          <div class="gmp-panel-frost" />
          <div class="gmp-panel-edge-glow" />
        </div>

        <!-- 开舱径向光晕：展开首帧由 GSAP 驱动 -->
        <div
          ref="openBurstRef"
          class="gmp-open-burst gmp-no-drag"
          aria-hidden="true"
        />

        <div class="gmp-drag-hint gmp-stag" aria-hidden="true">拖拽此处或封面移动 · 收起时可拖左下角胶囊 · M 收起</div>

        <canvas ref="canvasRef" class="gmp-spectrum gmp-stag gmp-no-drag" height="72" />

        <div class="gmp-row gmp-stag">
          <div
            class="gmp-cover-wrap"
            :style="{ '--beat': beatPulse }"
          >
            <div
              class="gmp-cover-ring"
              :class="{ 'gmp-cover-ring--spin': store.isPlaying && !reducedMotion }"
            />
            <div
              class="gmp-cover"
              :class="{ 'gmp-cover--tilt': store.isPlaying && !reducedMotion }"
            >
              <img
                v-if="store.currentTrack?.cover"
                :src="store.currentTrack.cover"
                alt=""
              >
              <div v-else class="gmp-cover-fallback" aria-hidden="true" />
            </div>
          </div>

          <div class="gmp-meta gmp-no-drag">
            <h3 class="gmp-title">{{ store.currentTrack?.title ?? '—' }}</h3>
            <p class="gmp-artist">{{ store.currentTrack?.artist ?? '' }}</p>

            <div v-if="sortedLyrics.length" class="gmp-lyrics">
              <Transition name="gmp-lyric" mode="out-in">
                <p
                  :key="`${store.currentTrack?.id}-${lyricLineKey}`"
                  class="gmp-lyric-current"
                >
                  {{ lyricCurrentText }}
                </p>
              </Transition>
              <p class="gmp-lyric-next">{{ lyricNextText }}</p>
            </div>
          </div>
        </div>

        <div
          class="gmp-progress gmp-stag gmp-no-drag"
          :class="{ 'gmp-progress--drag': seeking }"
          @mousedown="onSeekDown"
        >
          <div class="gmp-progress-track">
            <div class="gmp-progress-fill" :style="{ width: `${progressPct}%` }" />
          </div>
        </div>

        <div class="gmp-toolbar gmp-stag gmp-no-drag">
          <button
            type="button"
            class="gmp-icon-btn"
            aria-label="上一曲"
            @click="skipPrev"
          >
            ⏮
          </button>

          <button
            ref="playBtnRef"
            type="button"
            class="gmp-play gmp-no-drag"
            :class="{ 'gmp-play--live': store.isPlaying && !reducedMotion }"
            aria-label="播放或暂停"
            @mousemove="magneticMove"
            @mouseleave="magneticLeave"
            @click="
              (e) => {
                ripplePlay(e)
                togglePlay()
              }
            "
          >
            <span v-if="store.isPlaying" class="gmp-play-ico">❚❚</span>
            <span v-else class="gmp-play-ico">▶</span>
          </button>

          <button type="button" class="gmp-icon-btn" aria-label="下一曲" @click="store.next">
            ⏭
          </button>

          <div
            class="gmp-vol"
            @mouseenter="volumePopover = true"
            @mouseleave="volumePopover = false"
          >
            <button type="button" class="gmp-icon-btn" aria-label="音量">🔊</button>
            <div v-show="volumePopover" class="gmp-vol-pop">
              <input
                v-model.number="store.volume"
                type="range"
                min="0"
                max="1"
                step="0.02"
                orient="vertical"
                aria-label="音量滑块"
              >
            </div>
          </div>

          <button
            type="button"
            class="gmp-icon-btn"
            aria-label="播放列表"
            @click="store.playlistOpen = !store.playlistOpen"
          >
            ☰
          </button>

          <button
            type="button"
            class="gmp-icon-btn gmp-close"
            aria-label="收起播放器"
            @click="collapsePanel"
          >
            ✕
          </button>
        </div>

        <Transition name="gmp-pl">
          <div v-if="store.playlistOpen" class="gmp-playlist gmp-no-drag">
            <button
              v-for="(t, i) in store.tracks"
              :key="t.id"
              type="button"
              class="gmp-pl-item"
              :class="{ 'gmp-pl-item--on': i === store.currentIndex }"
              @click="selectTrack(i)"
            >
              <span class="gmp-pl-title">{{ t.title }}</span>
              <span class="gmp-pl-artist">{{ t.artist }}</span>
            </button>
          </div>
        </Transition>
      </div>

      <audio
        ref="audioRef"
        crossorigin="anonymous"
        preload="metadata"
        @timeupdate="onTimeUpdate"
        @loadedmetadata="onLoadedMeta"
        @ended="onEnded"
        @play="store.isPlaying = true"
        @pause="store.isPlaying = false"
      />
    </div>
  </Teleport>
</template>

<style scoped>
.gmp {
  position: fixed;
  z-index: 9999;
  width: 64px;
  height: 64px;
  pointer-events: auto;
  touch-action: none;
}

.gmp--expanded {
  width: 420px;
  height: auto;
  min-height: 64px;
}

.gmp-sonar {
  position: absolute;
  inset: -24%;
  width: 148%;
  height: 148%;
  left: -24%;
  top: -24%;
  pointer-events: none;
  opacity: 0;
  animation: gmpSonarRootIn 0.85s ease forwards;
}

.gmp-sonar-ring {
  fill: none;
  stroke: rgba(45, 212, 191, 0.25);
  stroke-width: 1.5;
  transform-origin: center;
  animation: gmpSonar 3s ease-out infinite;
}

.gmp-sonar-ring--b {
  animation-delay: 1s;
}

.gmp-sonar-ring--c {
  animation-delay: 2s;
}

@keyframes gmpSonar {
  0% {
    transform: scale(0.35);
    opacity: 0.55;
  }
  100% {
    transform: scale(1.15);
    opacity: 0;
  }
}

@keyframes gmpSonarRootIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 0.38;
  }
}

.gmp-core {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 1px solid rgba(45, 212, 191, 0.45);
  background:
    radial-gradient(circle at 32% 28%, rgba(74, 222, 128, 0.38) 0%, transparent 55%),
    radial-gradient(circle at 72% 72%, rgba(12, 95, 70, 0.45) 0%, transparent 50%),
    radial-gradient(circle at 50% 50%, #164032 0%, #0a241c 65%, #050f0c 100%);
  backdrop-filter: blur(16px) saturate(165%);
  -webkit-backdrop-filter: blur(16px) saturate(165%);
  box-shadow:
    0 16px 42px rgba(0, 0, 0, 0.52),
    0 0 0 1px rgba(45, 212, 191, 0.16),
    0 0 calc(8px + var(--core-beat, 0.32) * 20px) rgba(45, 212, 191, 0.42),
    inset 0 0 26px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.09);
  cursor: grab;
  display: grid;
  place-items: center;
  color: #f8fafc;
  padding: 0;
  overflow: visible;
  isolation: isolate;
  transition: border-color 0.35s ease;
}

.gmp-core-layers {
  position: absolute;
  inset: -9px;
  pointer-events: none;
  z-index: 0;
}

/* 锥形扫光：仅播放时转动 */
.gmp-core-sweep {
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  background: conic-gradient(
    from 200deg,
    transparent 0deg,
    rgba(45, 212, 191, 0.08) 55deg,
    rgba(74, 222, 128, 0.38) 95deg,
    rgba(45, 212, 191, 0.12) 130deg,
    transparent 165deg
  );
  opacity: 0;
  animation: gmpCoreSweep 2.6s linear infinite;
}

.gmp-core--live .gmp-core-sweep {
  opacity: 0.95;
}

/* 外圈虚线轨道反向慢转 */
.gmp-core-orbit {
  position: absolute;
  inset: -2px;
  border-radius: 50%;
  border: 1px dashed rgba(45, 212, 191, 0.28);
  opacity: 0;
  animation: gmpCoreOrbit 14s linear infinite;
}

.gmp-core--live .gmp-core-orbit {
  opacity: 1;
}

/* 外晕随 --core-beat（频谱低音）起伏 */
.gmp-core-halo {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  box-shadow:
    0 0 calc(4px + var(--core-beat, 0.32) * 16px) rgba(45, 212, 191, 0.55),
    0 0 calc(16px + var(--core-beat, 0.32) * 28px) rgba(74, 222, 128, 0.12);
  opacity: 0.75;
}

.gmp-core--idle-pulse .gmp-core-halo {
  animation: gmpCoreHaloIdle 3.5s ease-in-out infinite;
}

.gmp-core-note {
  position: relative;
  z-index: 1;
  filter: drop-shadow(0 0 5px rgba(255, 255, 255, 0.35));
}

.gmp-core--live .gmp-core-note {
  animation: gmpCoreNotePulse 1.25s ease-in-out infinite;
}

@keyframes gmpCoreSweep {
  to {
    transform: rotate(360deg);
  }
}

@keyframes gmpCoreOrbit {
  to {
    transform: rotate(-360deg);
  }
}

@keyframes gmpCoreHaloIdle {
  0%,
  100% {
    opacity: 0.4;
    transform: scale(0.96);
  }
  50% {
    opacity: 0.85;
    transform: scale(1.03);
  }
}

@keyframes gmpCoreNotePulse {
  0%,
  100% {
    transform: translateY(0) scale(1);
    opacity: 1;
  }
  50% {
    transform: translateY(-0.5px) scale(1.05);
    opacity: 0.95;
  }
}

.gmp-core:hover {
  border-color: rgba(45, 212, 191, 0.65);
  box-shadow:
    0 18px 48px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(45, 212, 191, 0.22),
    0 0 calc(14px + var(--core-beat, 0.32) * 24px) rgba(45, 212, 191, 0.45),
    inset 0 0 28px rgba(0, 0, 0, 0.38),
    inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.gmp--drag .gmp-core {
  cursor: grabbing;
}

.gmp-core--hidden {
  visibility: hidden;
  pointer-events: none;
}

@media (prefers-reduced-motion: reduce) {
  .gmp-core-sweep,
  .gmp-core-orbit,
  .gmp-core--idle-pulse .gmp-core-halo,
  .gmp-core--live .gmp-core-note {
    animation: none !important;
  }

  .gmp-core--live .gmp-core-sweep {
    opacity: 0;
  }

  .gmp-core--live .gmp-core-orbit {
    opacity: 0.35;
  }
}

.gmp-panel {
  position: relative;
  width: 420px;
  max-height: min(560px, calc(100vh - 40px));
  max-height: min(560px, calc(100dvh - 40px));
  padding: 14px 16px 16px;
  border-radius: 24px;
  border: 1px solid rgba(45, 212, 191, 0.22);
  overflow-x: hidden;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(45, 212, 191, 0.35) transparent;
  box-shadow:
    0 24px 56px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(45, 212, 191, 0.12),
    0 0 50px rgba(45, 212, 191, 0.08) inset,
    0 -20px 80px rgba(45, 212, 191, 0.06);
  transform-style: preserve-3d;
  color: #e2e8f0;
  font-family:
    'JetBrains Mono',
    ui-monospace,
    SFMono-Regular,
    Menlo,
    Monaco,
    Consolas,
    'Liberation Mono',
    'Courier New',
    monospace;
}

.gmp-panel > :not(.gmp-panel-layers):not(.gmp-open-burst) {
  position: relative;
  z-index: 2;
}

/* 右下角开舱闪光：在磨砂之上、UI 之下（仅用定位，避免与 GSAP transform 冲突） */
.gmp-open-burst {
  position: absolute;
  right: -12%;
  bottom: -12%;
  width: min(280px, 90vw);
  height: min(280px, 90vw);
  pointer-events: none;
  z-index: 1;
  border-radius: 50%;
  background: radial-gradient(
    circle at 45% 45%,
    rgba(255, 255, 255, 0.22) 0%,
    rgba(45, 212, 191, 0.42) 18%,
    rgba(74, 222, 128, 0.18) 42%,
    transparent 70%
  );
  opacity: 0;
  mix-blend-mode: screen;
  filter: blur(0.5px);
}

.gmp-panel:hover {
  border-color: rgba(45, 212, 191, 0.38);
  box-shadow:
    0 22px 54px rgba(0, 0, 0, 0.52),
    0 0 24px rgba(45, 212, 191, 0.28),
    0 0 44px rgba(45, 212, 191, 0.12) inset;
}

/* —— 动态深空背景层 —— */
.gmp-panel-layers {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.gmp-dyn-bg {
  position: absolute;
  inset: -2px;
}

.gmp-dyn-bg-deep {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 120% 80% at 50% -20%, rgba(45, 212, 191, 0.18), transparent 55%),
    radial-gradient(ellipse 90% 70% at 100% 60%, rgba(56, 189, 248, 0.12), transparent 50%),
    radial-gradient(ellipse 70% 60% at 0% 100%, rgba(74, 222, 128, 0.1), transparent 45%),
    linear-gradient(165deg, #050810 0%, #0a1020 38%, #0d1528 72%, #080c18 100%);
  animation: gmpDeepShift 28s ease-in-out infinite alternate;
}

.gmp-dyn-bg--still .gmp-dyn-bg-deep,
.gmp-dyn-bg--still .gmp-orb,
.gmp-dyn-bg--still .gmp-grid-flo,
.gmp-dyn-bg--still .gmp-starfield,
.gmp-dyn-bg--still .gmp-dyn-shimmer {
  animation: none !important;
}

.gmp-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(46px);
  mix-blend-mode: screen;
  opacity: 0.55;
  will-change: transform, opacity;
}

.gmp-orb--a {
  width: 200px;
  height: 200px;
  left: -12%;
  top: -18%;
  background: radial-gradient(circle at 40% 40%, rgba(45, 212, 191, 0.95), transparent 68%);
  animation: gmpOrbA 22s ease-in-out infinite alternate;
}

.gmp-orb--b {
  width: 160px;
  height: 160px;
  right: -8%;
  top: 8%;
  background: radial-gradient(circle at 50% 50%, rgba(74, 222, 128, 0.75), transparent 65%);
  animation: gmpOrbB 26s ease-in-out infinite alternate;
}

.gmp-orb--c {
  width: 140px;
  height: 140px;
  left: 35%;
  bottom: -15%;
  background: radial-gradient(circle at 50% 50%, rgba(56, 189, 248, 0.55), transparent 62%);
  animation: gmpOrbC 19s ease-in-out infinite alternate;
}

.gmp-orb--d {
  width: 100px;
  height: 100px;
  right: 25%;
  bottom: 35%;
  background: radial-gradient(circle at 50% 50%, rgba(129, 140, 248, 0.35), transparent 60%);
  animation: gmpOrbD 24s ease-in-out infinite alternate;
}

.gmp-grid-flo {
  position: absolute;
  inset: -40%;
  opacity: 0.22;
  background-image:
    linear-gradient(rgba(45, 212, 191, 0.09) 1px, transparent 1px),
    linear-gradient(90deg, rgba(45, 212, 191, 0.09) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: radial-gradient(ellipse 85% 75% at 50% 42%, black 0%, transparent 72%);
  transform: perspective(420px) rotateX(58deg);
  transform-origin: 50% 0%;
  animation: gmpGridDrift 32s linear infinite;
}

.gmp-starfield {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.55;
}

.gmp-starfield .gmp-s {
  animation: gmpTwinkle 3.2s ease-in-out infinite;
}

.gmp-starfield .s1 {
  animation-delay: 0.3s;
}
.gmp-starfield .s2 {
  animation-delay: 0.8s;
}
.gmp-starfield .s3 {
  animation-delay: 1.1s;
}
.gmp-starfield .s4 {
  animation-delay: 0.2s;
}
.gmp-starfield .s5 {
  animation-delay: 1.6s;
}
.gmp-starfield .s6 {
  animation-delay: 0.5s;
}
.gmp-starfield .s7 {
  animation-delay: 2.1s;
}
.gmp-starfield .s8 {
  animation-delay: 1.3s;
}
.gmp-starfield .s9 {
  animation-delay: 0.7s;
}
.gmp-starfield .s10 {
  animation-delay: 2.4s;
}
.gmp-starfield .s11 {
  animation-delay: 1.9s;
}
.gmp-starfield .s12 {
  animation-delay: 0.4s;
}
.gmp-starfield .s13 {
  animation-delay: 2.8s;
}

.gmp-dyn-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    105deg,
    transparent 0%,
    rgba(45, 212, 191, 0.04) 42%,
    rgba(255, 255, 255, 0.06) 50%,
    rgba(74, 222, 128, 0.05) 58%,
    transparent 100%
  );
  background-size: 200% 100%;
  animation: gmpBgShimmer 14s ease-in-out infinite;
  mix-blend-mode: overlay;
  pointer-events: none;
}

.gmp-panel-frost {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: rgba(11, 15, 25, 0.42);
  backdrop-filter: blur(26px) saturate(185%);
  -webkit-backdrop-filter: blur(26px) saturate(185%);
}

.gmp-panel-edge-glow {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  box-shadow:
    inset 0 1px 0 rgba(45, 212, 191, 0.12),
    inset 0 0 100px rgba(5, 8, 16, 0.65);
  pointer-events: none;
}

@keyframes gmpDeepShift {
  0% {
    filter: hue-rotate(0deg) brightness(1);
  }
  100% {
    filter: hue-rotate(12deg) brightness(1.06);
  }
}

@keyframes gmpOrbA {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 0.5;
  }
  100% {
    transform: translate(28px, 36px) scale(1.12);
    opacity: 0.72;
  }
}

@keyframes gmpOrbB {
  0% {
    transform: translate(0, 0) scale(1.05);
    opacity: 0.45;
  }
  100% {
    transform: translate(-32px, 22px) scale(0.92);
    opacity: 0.68;
  }
}

@keyframes gmpOrbC {
  0% {
    transform: translate(0, 0) scale(0.95);
    opacity: 0.4;
  }
  100% {
    transform: translate(18px, -24px) scale(1.08);
    opacity: 0.62;
  }
}

@keyframes gmpOrbD {
  0% {
    transform: translate(0, 0) scale(1);
    opacity: 0.35;
  }
  100% {
    transform: translate(-14px, 16px) scale(1.15);
    opacity: 0.5;
  }
}

@keyframes gmpGridDrift {
  0% {
    transform: perspective(420px) rotateX(58deg) translate(0, 0);
  }
  100% {
    transform: perspective(420px) rotateX(58deg) translate(-28px, 28px);
  }
}

@keyframes gmpTwinkle {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 1;
  }
}

@keyframes gmpBgShimmer {
  0%,
  100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .gmp-dyn-bg-deep,
  .gmp-orb,
  .gmp-grid-flo,
  .gmp-starfield .gmp-s,
  .gmp-dyn-shimmer {
    animation: none !important;
  }
}

.gmp-drag-hint {
  font-size: 10px;
  letter-spacing: 0.06em;
  color: #64748b;
  text-transform: uppercase;
  margin-bottom: 8px;
  cursor: move;
}

.gmp-spectrum {
  display: block;
  width: 100%;
  height: 72px;
  margin-bottom: 12px;
  border-radius: 12px;
  background: rgba(2, 6, 14, 0.45);
  border: 1px solid rgba(45, 212, 191, 0.14);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.06),
    0 8px 28px rgba(0, 0, 0, 0.25);
}

.gmp-row {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.gmp-cover-wrap {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.gmp-cover-ring {
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  border: 2px solid rgba(45, 212, 191, 0.35);
  box-shadow: 0 0 calc(12px + var(--beat, 0.3) * 28px) rgba(45, 212, 191, calc(0.15 + var(--beat, 0.3) * 0.35));
  pointer-events: none;
}

.gmp-cover-ring--spin {
  animation: gmpRingPulse 2s ease-in-out infinite;
}

@keyframes gmpRingPulse {
  50% {
    opacity: 0.85;
    filter: brightness(1.25);
  }
}

.gmp-cover {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  background: #0f172a;
  transition: transform 0.5s cubic-bezier(0.22, 1, 0.36, 1);
}

.gmp-cover--tilt {
  transform: perspective(800px) rotateY(15deg);
  animation: gmpCoverSpin 60s linear infinite;
}

@keyframes gmpCoverSpin {
  to {
    transform: perspective(800px) rotateY(15deg) rotateZ(360deg);
  }
}

.gmp-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gmp-cover-fallback {
  width: 100%;
  height: 100%;
  background: conic-gradient(from 210deg, #2dd4bf, #4ade80, #818cf8, #2dd4bf);
  opacity: 0.85;
}

.gmp-meta {
  flex: 1;
  min-width: 0;
  font-family:
    var(--app-font-body, 'Segoe UI Variable', 'Microsoft YaHei UI', 'PingFang SC', sans-serif),
    sans-serif;
}

.gmp-title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.03em;
  color: #f1f5f9;
  line-height: 1.35;
  text-shadow:
    0 1px 0 rgba(0, 0, 0, 0.35),
    0 0 28px rgba(45, 212, 191, 0.18);
}

.gmp-artist {
  margin: 5px 0 0;
  font-size: 12px;
  letter-spacing: 0.04em;
  color: #94a3b8;
  opacity: 0.92;
}

.gmp-lyrics {
  margin-top: 10px;
  min-height: 52px;
}

.gmp-lyric-current {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2dd4bf;
  text-shadow: 0 0 18px rgba(45, 212, 191, 0.35);
}

.gmp-lyric-enter-active,
.gmp-lyric-leave-active {
  transition:
    opacity 0.38s ease,
    transform 0.38s ease,
    filter 0.38s ease;
}

.gmp-lyric-enter-from {
  opacity: 0;
  filter: blur(5px);
  transform: translateY(12px);
}

.gmp-lyric-leave-to {
  opacity: 0;
  filter: blur(7px);
  transform: translateY(-10px);
}

.gmp-lyric-next {
  margin: 6px 0 0;
  font-size: 14px;
  color: #64748b;
}

.gmp-progress {
  margin-bottom: 14px;
  padding: 8px 0;
  cursor: pointer;
}

.gmp-progress-track {
  height: 4px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  overflow: hidden;
  position: relative;
}

.gmp-progress--drag .gmp-progress-track {
  height: 6px;
  box-shadow: 0 0 16px rgba(45, 212, 191, 0.35);
}

.gmp-progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #2dd4bf, #4ade80);
  position: relative;
  overflow: hidden;
}

.gmp-progress-fill::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    100deg,
    transparent 0%,
    rgba(255, 255, 255, 0.45) 45%,
    transparent 70%
  );
  animation: gmpShimmer 2s ease-in-out infinite;
}

@keyframes gmpShimmer {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(200%);
  }
}

.gmp-toolbar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.gmp-icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.04);
  color: #e2e8f0;
  cursor: pointer;
  font-size: 15px;
  padding: 0;
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

.gmp-icon-btn:hover {
  background: rgba(45, 212, 191, 0.08);
  border-color: rgba(45, 212, 191, 0.25);
}

.gmp-play {
  position: relative;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: #2dd4bf;
  color: #0f172a;
  cursor: pointer;
  display: grid;
  place-items: center;
  overflow: hidden;
  font-size: 15px;
}

.gmp-play--live {
  animation: gmpPlayHalo 2s ease-in-out infinite;
}

@keyframes gmpPlayHalo {
  0%,
  100% {
    box-shadow:
      0 0 0 0 rgba(74, 222, 128, 0.45),
      0 8px 24px rgba(45, 212, 191, 0.35);
  }
  50% {
    box-shadow:
      0 0 0 12px rgba(74, 222, 128, 0),
      0 10px 28px rgba(45, 212, 191, 0.45);
  }
}

.gmp-ripple {
  position: absolute;
  width: 16px;
  height: 16px;
  margin: -8px 0 0 -8px;
  border-radius: 50%;
  border: 2px solid rgba(74, 222, 128, 0.85);
  pointer-events: none;
}

.gmp-vol {
  position: relative;
}

.gmp-vol-pop {
  position: absolute;
  bottom: 48px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 8px;
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.95);
  border: 1px solid rgba(45, 212, 191, 0.2);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.4);
}

.gmp-vol-pop input[type='range'] {
  writing-mode: vertical-lr;
  direction: rtl;
  width: 28px;
  height: 110px;
  accent-color: #4ade80;
}

.gmp-close {
  margin-left: auto;
}

.gmp-playlist {
  margin-top: 12px;
  max-height: 200px;
  overflow-y: auto;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding-top: 10px;
}

.gmp-pl-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  width: 100%;
  padding: 10px 12px 10px 14px;
  margin: 0;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: #94a3b8;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
  transition:
    background 0.2s ease,
    color 0.2s ease,
    transform 0.2s ease,
    box-shadow 0.2s ease;
  border-left: 2px solid transparent;
}

.gmp-pl-item:hover {
  background: rgba(45, 212, 191, 0.05);
  border-left-color: #2dd4bf;
  color: #e2e8f0;
  transform: translateX(4px);
}

.gmp-pl-item--on {
  box-shadow: 0 0 0 1px rgba(45, 212, 191, 0.25);
  background: rgba(45, 212, 191, 0.06);
  color: #e2e8f0;
}

.gmp-pl-title {
  font-size: 13px;
  font-weight: 600;
}

.gmp-pl-artist {
  font-size: 11px;
  opacity: 0.85;
}

.gmp-pl-enter-active,
.gmp-pl-leave-active {
  transition:
    opacity 0.28s ease,
    transform 0.28s ease;
}

.gmp-pl-enter-from,
.gmp-pl-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (prefers-reduced-motion: reduce) {
  .gmp-sonar,
  .gmp-sonar-ring,
  .gmp-cover--tilt,
  .gmp-cover-ring--spin,
  .gmp-play--live,
  .gmp-progress-fill::after {
    animation: none !important;
  }

  .gmp-cover--tilt {
    transform: none;
  }

  .gmp-panel {
    transform: none !important;
  }
}
</style>
