/** 解析评论纯文本中的 @昵称，用于蓝色高亮展示 */
export type CommentSegment = { type: 'text' | 'mention'; value: string }

export function parseCommentSegments(text: string): CommentSegment[] {
  const s = text ?? ''
  if (!s) return []
  const re = /@([\w\u4e00-\u9fa5·\-]{1,30})/gu
  const out: CommentSegment[] = []
  let last = 0
  let m: RegExpExecArray | null
  while ((m = re.exec(s))) {
    if (m.index > last) {
      out.push({ type: 'text', value: s.slice(last, m.index) })
    }
    out.push({ type: 'mention', value: m[0] })
    last = m.index + m[0].length
  }
  if (last < s.length) {
    out.push({ type: 'text', value: s.slice(last) })
  }
  return out
}
