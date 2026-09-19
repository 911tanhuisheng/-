/**
 * 与 openapitools.json servers[0].url 一致；末尾不要多余 /
 * 联调：直连填完整 URL；本地代理见 vite.config 与 .env.example
 */
export const API_BASE_URL =
  (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/+$/, '') ?? '/api'
