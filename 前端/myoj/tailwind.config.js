/** @type {import('tailwindcss').Config} */
export default {
  /** 与全站 Arco 隔离，仅用 tw- 前缀工具类 */
  prefix: 'tw-',
  corePlugins: {
    preflight: false,
  },
  darkMode: ['selector', '[data-theme="dark"]'],
  content: ['./index.html', './src/**/*.{vue,js,ts,tsx}'],
  theme: {
    extend: {
      keyframes: {
        'ai-shimmer': {
          '0%': { backgroundPosition: '200% 0' },
          '100%': { backgroundPosition: '-200% 0' },
        },
        'ai-type-caret': {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0' },
        },
      },
      animation: {
        'ai-shimmer': 'ai-shimmer 1.6s ease-in-out infinite',
        'ai-type-caret': 'ai-type-caret 1s step-end infinite',
      },
    },
  },
  plugins: [],
}
