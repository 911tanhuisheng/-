import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { resolveMediaUrl } from '@/utils/resolveMediaUrl'

const IMAGE_EXT_IN_URL = /\.(?:png|jpe?g|gif|webp|svg|bmp|ico)(?:\?|#|$)/i

/** 判断裸 URL 是否应按图片渲染（含 Bing 缩略图等无后缀地址） */
function looksLikeImageUrl(url: string): boolean {
  const t = (url ?? '').trim()
  if (!t) return false
  if (t.startsWith('/')) {
    return IMAGE_EXT_IN_URL.test(t)
  }
  if (!/^https?:\/\//i.test(t)) return false
  if (IMAGE_EXT_IN_URL.test(t)) return true
  try {
    const u = new URL(t)
    const h = u.hostname.toLowerCase()
    const pathAndQuery = `${u.pathname}${u.search}`
    if (/\.(?:bing|live)\.(net|com)$/i.test(h) || /bing\.(net|com)$/i.test(h)) {
      return true
    }
    if (/\/th\/|\/th\?|OIP[-_.]/i.test(pathAndQuery)) {
      return true
    }
    if (/[?&]pid=imgdetmain\b/i.test(t)) {
      return true
    }
    if (/unsplash\.com|imgur\.com|ibb\.co|gyazo\.com|cloudinary\.com|qpic\.cn/i.test(h)) {
      return true
    }
  } catch {
    return false
  }
  return false
}

/** 从段落开头截取第一个 http(s) URL（避免吃掉 markdown 链接末尾的 ) ） */
function takeLeadingHttpUrl(segment: string): { url: string; rest: string } | null {
  const m = segment.match(/^(https?:\/\/[^\s)\]<>"]+)/i)
  if (!m || m[1] == null) return null
  const url = m[1]
  return { url, rest: segment.slice(url.length) }
}

/**
 * 将正文里直接粘贴的 http(s) 图片地址（含多条粘在同一行、无空格）转为 ![](url)。
 * 以 https:// 为界拆分，避免 `...rm=3https://...` 粘成一条无法识别。
 */
function preprocessBareImageUrls(text: string): string {
  const raw = text ?? ''
  const segments = raw.split(/(?=https?:\/\/)/i)
  const out: string[] = []
  for (const seg of segments) {
    if (!/^https?:\/\//i.test(seg)) {
      out.push(seg)
      continue
    }
    const taken = takeLeadingHttpUrl(seg)
    if (!taken) {
      out.push(seg)
      continue
    }
    const { url, rest } = taken
    if (looksLikeImageUrl(url)) {
      out.push(`\n\n![](${url})\n\n`)
      out.push(preprocessBareImageUrls(rest))
    } else {
      out.push(url)
      out.push(preprocessBareImageUrls(rest))
    }
  }
  return out.join('')
}

function lineLooksLikeImageUrl(trimmed: string): boolean {
  if (!trimmed || /\s/.test(trimmed)) return false
  if (trimmed.startsWith('![') || trimmed.startsWith('<')) return false
  if (trimmed.startsWith('/')) {
    return IMAGE_EXT_IN_URL.test(trimmed)
  }
  return looksLikeImageUrl(trimmed)
}

/**
 * 将「单独一行」的裸图片链接转为 Markdown 图片语法。
 */
function preprocessStandaloneImageUrls(markdown: string): string {
  const raw = markdown ?? ''
  return raw
    .split('\n')
    .map((line) => {
      const t = line.trim()
      if (!t) return line
      if (/^!\[[\s\S]*\]\([^)]+\)\s*$/.test(t)) return line

      const angle = t.match(/^<((?:https?:\/\/|\/)[^>\s]+)>\s*$/i)
      const angleHref = angle?.[1]
      if (angleHref && lineLooksLikeImageUrl(angleHref)) {
        return `![](${angleHref})`
      }

      if (lineLooksLikeImageUrl(t)) {
        return `![](${t})`
      }
      return line
    })
    .join('\n')
}

/** 不处理 ``` 代码块内部，避免示例 URL 被转成图片 */
function protectFencedCodeBlocks(markdown: string, inner: (s: string) => string): string {
  const re = /```[\s\S]*?```/g
  const blocks: string[] = []
  const stripped = markdown.replace(re, (m) => {
    blocks.push(m)
    return `\uE000${blocks.length - 1}\uE001`
  })
  const processed = inner(stripped)
  return processed.replace(/\uE000(\d+)\uE001/g, (_, i) => blocks[Number(i)] ?? '')
}

let hooksInstalled = false

function ensureSanitizeHooks() {
  if (hooksInstalled || typeof window === 'undefined') return
  hooksInstalled = true

  DOMPurify.addHook('afterSanitizeAttributes', (node) => {
    if (node.tagName !== 'A') return
    const href = node.getAttribute('href')
    if (href && /^https?:\/\//i.test(href)) {
      node.setAttribute('target', '_blank')
      node.setAttribute('rel', 'noopener noreferrer')
    }
  })

  DOMPurify.addHook('afterSanitizeAttributes', (node) => {
    if (node.tagName !== 'IMG') return
    const src = node.getAttribute('src')
    if (!src?.trim()) return
    const resolved = resolveMediaUrl(src.trim())
    if (resolved) {
      node.setAttribute('src', resolved)
    }
    if (!node.getAttribute('loading')) {
      node.setAttribute('loading', 'lazy')
    }
    if (!node.getAttribute('decoding')) {
      node.setAttribute('decoding', 'async')
    }
    const alt = node.getAttribute('alt')
    if (alt == null || alt === '') {
      node.setAttribute('alt', '')
    }
  })
}

function preprocessMarkdown(markdown: string): string {
  return protectFencedCodeBlocks(markdown, (body) =>
    preprocessStandaloneImageUrls(preprocessBareImageUrls(body)),
  )
}

marked.use({
  gfm: true,
  hooks: {
    preprocess(markdown) {
      return preprocessMarkdown(markdown)
    },
  },
  renderer: {
    image({ href, title, text, tokens }) {
      const rawHref = (href ?? '').trim()
      const url = resolveMediaUrl(rawHref) || rawHref
      let altText = text ?? ''
      if (tokens?.length) {
        altText = this.parser.parseInline(tokens, this.parser.textRenderer) as string
      }
      const esc = (s: string) =>
        String(s)
          .replace(/&/g, '&amp;')
          .replace(/"/g, '&quot;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
      let html = `<img src="${esc(url)}" alt="${esc(altText)}" loading="lazy" decoding="async"`
      if (title) {
        html += ` title="${esc(title)}"`
      }
      html += ' />'
      return html
    },
  },
})

export function renderMarkdownToHtml(src: string): string {
  ensureSanitizeHooks()
  const raw = src ?? ''
  const html = marked.parse(raw, { async: false }) as string
  return DOMPurify.sanitize(html)
}
