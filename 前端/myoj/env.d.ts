/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

declare module 'prismjs/components/prism-clike'
declare module 'prismjs/components/prism-c'
declare module 'prismjs/components/prism-cpp'
declare module 'prismjs/components/prism-java'
declare module 'prismjs/components/prism-python'
declare module 'prismjs/components/prism-javascript'
