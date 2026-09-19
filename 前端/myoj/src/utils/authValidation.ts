/** 与 openapitools.json → components.schemas.RegisterRequest 中 pattern 一致 */

export const USERNAME_PATTERN = /^[a-zA-Z0-9_]{4,16}$/
export const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/

export function validateRegisterUsername(username: string): string | null {
  const u = username.trim()
  if (!u) return '请输入用户名'
  if (!USERNAME_PATTERN.test(u)) return '用户名须为 4～16 位字母、数字或下划线'
  return null
}

export function validateRegisterPassword(password: string): string | null {
  if (!password) return '请输入密码'
  if (!PASSWORD_PATTERN.test(password)) return '密码须 6～20 位，且同时包含字母与数字'
  return null
}

const EMAIL_LOOSE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateOptionalEmail(email: string): string | null {
  const e = email.trim()
  if (!e) return null
  if (!EMAIL_LOOSE.test(e)) return '邮箱格式不正确'
  return null
}
