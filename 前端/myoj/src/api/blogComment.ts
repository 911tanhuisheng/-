import { OpenAPI, Service } from '@generated'

/** 评论展示模型（id 必填；replies 为前端树形结构） */
export type BlogCommentVO = {
  id: string
  postId?: string
  parentId?: string | null
  userId?: string
  userName?: string
  userAvatar?: string | null
  content?: string
  likeCount?: number
  liked?: boolean
  createTime?: string
  hot?: boolean
  replies?: BlogCommentVO[]
}

export type BlogCommentPageVO = {
  hotTop: BlogCommentVO[]
  records: BlogCommentVO[]
  total?: number
  totalAll?: number
  current?: number
  size?: number
  pages?: number
}

export type SpringResult<T> = {
  code?: number
  message?: string
  data?: T
}

async function bearerAuthorization(): Promise<string | undefined> {
  const tokenResolver = OpenAPI.TOKEN
  const token = typeof tokenResolver === 'function' ? await tokenResolver({} as never) : tokenResolver
  const t = token?.trim()
  return t ? `Bearer ${t}` : undefined
}

export async function blogCommentPage(body: {
  postId: string
  current?: number
  pageSize?: number
  sort?: 'hot' | 'latest'
}) {
  const authorization = await bearerAuthorization()
  return Service.page1(
    {
      postId: body.postId,
      current: body.current,
      pageSize: body.pageSize,
      sort: body.sort,
    },
    authorization,
  ) as Promise<SpringResult<BlogCommentPageVO>>
}

export function blogCommentAdd(body: { postId: string; content: string; parentId?: string }) {
  return Service.add2(body) as Promise<SpringResult<BlogCommentVO>>
}

export function blogCommentLikeToggle(body: { commentId: string }) {
  return Service.toggleLike1(body) as Promise<
    SpringResult<{ commentId?: string; liked?: boolean; likeCount?: number }>
  >
}

export function blogCommentDelete(body: { id: string }) {
  return Service.delete1({ id: body.id }) as Promise<SpringResult<boolean>>
}
