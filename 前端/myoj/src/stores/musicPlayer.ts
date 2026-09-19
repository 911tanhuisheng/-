import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

const VOL_KEY = 'myoj:gmp:volume'

export type LyricLine = { t: number; text: string }

export type MusicTrack = {
  id: string
  title: string
  artist: string
  src: string
  cover?: string
  lyrics?: LyricLine[]
}

function readVolume(): number {
  if (typeof localStorage === 'undefined') return 0.85
  const v = parseFloat(localStorage.getItem(VOL_KEY) || '')
  if (Number.isFinite(v) && v >= 0 && v <= 1) return v
  return 0.85
}

/**
 * 默认不随源码分发任何第三方音乐，避免毕业设计归档或公开仓库产生版权风险。
 * 如需演示播放器，请仅添加本人原创或具有明确再分发许可的音频，并在 README 标注来源与许可证。
 */
const DEFAULT_PLAYLIST: MusicTrack[] = []

export const useMusicPlayerStore = defineStore('musicPlayer', () => {
  const tracks = ref<MusicTrack[]>([...DEFAULT_PLAYLIST])
  const currentIndex = ref(0)
  const volume = ref(readVolume())
  const isExpanded = ref(false)
  const playlistOpen = ref(false)
  const isPlaying = ref(false)
  const currentTime = ref(0)
  const duration = ref(0)

  const currentTrack = computed(() => tracks.value[currentIndex.value] ?? null)

  watch(volume, (v) => {
    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(VOL_KEY, String(Math.round(v * 1000) / 1000))
    }
  })

  function setTrackIndex(i: number) {
    if (i < 0 || i >= tracks.value.length) return
    currentIndex.value = i
  }

  function next() {
    if (tracks.value.length === 0) return
    currentIndex.value = (currentIndex.value + 1) % tracks.value.length
  }

  function prev() {
    if (tracks.value.length === 0) return
    currentIndex.value = (currentIndex.value - 1 + tracks.value.length) % tracks.value.length
  }

  function toggleExpanded() {
    isExpanded.value = !isExpanded.value
    if (!isExpanded.value) playlistOpen.value = false
  }

  return {
    tracks,
    currentIndex,
    volume,
    isExpanded,
    playlistOpen,
    isPlaying,
    currentTime,
    duration,
    currentTrack,
    setTrackIndex,
    next,
    prev,
    toggleExpanded,
  }
})
