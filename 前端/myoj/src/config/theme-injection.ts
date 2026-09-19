import type { InjectionKey, Ref } from 'vue'

export type LayoutThemeMode = 'light' | 'dark'

/** AppLayout provide，子页面 inject，避免仅靠 data-theme + MutationObserver 的时序问题 */
export const LAYOUT_THEME_KEY: InjectionKey<Ref<LayoutThemeMode>> = Symbol('layoutTheme')
