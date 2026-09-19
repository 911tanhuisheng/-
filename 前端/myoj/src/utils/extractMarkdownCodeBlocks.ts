export type MarkdownCodeFence = {
  lang: string
  code: string
}

/** 从 Markdown 正文提取 ``` 围栏代码块（用于「插入编辑器」等） */
export function extractMarkdownCodeBlocks(md: string): MarkdownCodeFence[] {
  const out: MarkdownCodeFence[] = []
  const raw = md ?? ''
  const re = /```([\w-+#.]*)\r?\n?([\s\S]*?)```/g
  let m: RegExpExecArray | null
  while ((m = re.exec(raw)) !== null) {
    const lang = (m[1] ?? 'text').trim() || 'text'
    const code = (m[2] ?? '').replace(/\r?\n$/, '')
    if (code.trim()) out.push({ lang, code })
  }
  return out
}
