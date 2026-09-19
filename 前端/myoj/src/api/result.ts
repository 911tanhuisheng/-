/**
 * 与仓库根目录 openapitools.json 中 ResultVoid / ResultLoginVO 的 code（int32）约定一致。
 * 常见后端：Spring 统一返回 code=200 表示业务成功；部分使用 code=0。
 */
export function isResultSuccess(code: number | undefined | null): boolean {
  if (code === undefined || code === null) return false
  return code === 200 || code === 0
}

/**
 * accessToken 失效、需换票（HTTP 常为 200，body.code=401）。
 * 不含 40303/40304（账号禁用）及泛用 403（权限不足）。
 */
export function isAccessTokenExpiredCode(code: number | undefined | null): boolean {
  if (code === undefined || code === null) return false
  return code === 401 || code === 40100 || code === 40101
}

/** Spring 统一 Result：未登录 / token 失效（不含账号禁用 40303/40304） */
export function isResultUnauthorized(code: number | undefined | null): boolean {
  if (isAccessTokenExpiredCode(code)) return true
  if (isAccountDisabledCode(code) || isAccountSessionRevokedCode(code) || isLoginElsewhereCode(code)) {
    return false
  }
  return code === 403
}

/** 账号被禁用：已登录但不可使用业务功能 */
export const ACCOUNT_DISABLED_CODE = 40303

/** 禁用前签发的会话已作废，需重新登录 */
export const ACCOUNT_SESSION_REVOKED_CODE = 40304

export function isAccountDisabledCode(code: number | undefined | null): boolean {
  return code === ACCOUNT_DISABLED_CODE
}

export function isAccountSessionRevokedCode(code: number | undefined | null): boolean {
  return code === ACCOUNT_SESSION_REVOKED_CODE
}

/** 发布违禁评论触发封禁 */
export const COMMENT_PROFANITY_BANNED_CODE = 40305

/** 同账号在其他设备登录，当前会话已失效 */
export const LOGIN_ELSEWHERE_CODE = 40306

/** 向 AI 助手发送违禁内容，学习助手已被限制 */
export const AI_ASSIST_PROFANITY_BANNED_CODE = 40307

export function isLoginElsewhereCode(code: number | undefined | null): boolean {
  return code === LOGIN_ELSEWHERE_CODE
}

export function isCommentProfanityBannedCode(code: number | undefined | null): boolean {
  return code === COMMENT_PROFANITY_BANNED_CODE
}

export function isAiAssistProfanityBannedCode(code: number | undefined | null): boolean {
  return code === AI_ASSIST_PROFANITY_BANNED_CODE
}
