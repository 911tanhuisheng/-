import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  /**
   * 部署在子路径时必须与 nginx location 一致，例如站点为 https://域名/oj/ 则设为 base: '/oj/'
   * （末尾必须有 /）。根路径部署保持默认 '/' 即可。
   */
  // base: '/your-sub-path/',
  plugins: [vue()],
  build: {
    /** 现代浏览器：略减 polyfill 体积 */
    target: 'es2020',
    /** Monaco / Arco 体积大，提高阈值避免无意义告警 */
    /** Monaco 主包 + worker 合并后常 >1MB，属预期；其余 chunk 仍宜控制体积 */
    chunkSizeWarningLimit: 4500,
    /**
     * 与原先 rollupOptions 合并为 rolldownOptions，避免两项并存时后者覆盖导致 manualChunks 失效。
     * checks.pluginTimings：关闭构建末尾「插件耗时占比」提示（非错误，Tailwind + Vue SFC 常见占比较高）。
     */
    rolldownOptions: {
      checks: {
        pluginTimings: false,
      },
      output: {
        /**
         * 稳定 vendor 分包：并行下载 + 长期缓存；避免单 chunk 过大导致首包/解析过久。
         */
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('monaco-editor') || id.includes('@monaco-editor')) return 'vendor-monaco'
          if (id.includes('@arco-design')) return 'vendor-arco'
          if (id.includes('marked') || id.includes('dompurify')) return 'vendor-markdown'
          if (id.includes('prismjs')) return 'vendor-prism'
          if (id.includes('gsap')) return 'vendor-gsap'
          if (id.includes('@vueuse')) return 'vendor-vueuse'
          if (id.includes('axios')) return 'vendor-axios'
          if (id.includes('pinia') || id.includes('vue-router')) return 'vendor-vue'
          if (id.includes('/vue/') || id.includes('\\vue\\')) return 'vendor-vue'
        },
      },
    },
  },
  /** 开发联调：与后端端口一致。后端若用 $env:SERVER_PORT=8889 启动，此处同终端 $env:SERVER_PORT=8889 再 npm run dev */
  server: {
    port: 5173,
     // 内网穿透域名示例：allowedHosts: ['your-tunnel.example.com'],
     host: '0.0.0.0',
    proxy: {
      '/api': {
        target: `http://localhost:${process.env.SERVER_PORT ?? '8888'}`,
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@generated': fileURLToPath(new URL('./generated', import.meta.url)),
    },
  },
})
