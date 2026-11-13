import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'

let refreshPromise = null
let lastExpiredAt = 0

export class AuthExpiredError extends Error {
  constructor(message = '登录状态已失效，请重新登录') {
    super(message)
    this.name = 'AuthExpiredError'
  }
}

/**
 * 刷新令牌。Axios 和 fetch 共用同一个 Promise，避免并发刷新。
 * failedAccessToken 用于识别“请求失败期间令牌已被其他请求刷新”的情况。
 */
export function refreshAccessToken(failedAccessToken = '') {
  const userStore = useUserStore()
  const currentAccessToken = userStore.accessToken

  if (failedAccessToken && currentAccessToken && failedAccessToken !== currentAccessToken) {
    return Promise.resolve({
      accessToken: currentAccessToken,
      refreshToken: userStore.refreshToken
    })
  }
  if (!userStore.refreshToken) {
    return Promise.reject(new AuthExpiredError('没有可用的刷新令牌'))
  }
  if (refreshPromise) {
    return refreshPromise
  }

  const oldAccessToken = currentAccessToken
  const oldRefreshToken = userStore.refreshToken
  refreshPromise = axios.post(
    '/api/auth/refresh',
    {},
    {
      headers: {
        Authorization: oldAccessToken,
        refreshtoken: oldRefreshToken
      },
      timeout: 15000
    }
  ).then((response) => {
    const payload = response.data
    if (payload?.code !== 200 || !payload.data) {
      throw new AuthExpiredError(payload?.message || '令牌刷新失败')
    }
    userStore.setTokens(payload.data)
    return payload.data
  }).catch((error) => {
    if (error instanceof AuthExpiredError) throw error
    throw new AuthExpiredError(error.response?.data?.message || '登录已过期，请重新登录')
  }).finally(() => {
    refreshPromise = null
  })

  return refreshPromise
}

export function expireSession(message = '登录状态已失效，请重新登录') {
  const userStore = useUserStore()
  userStore.logout()

  const now = Date.now()
  if (now - lastExpiredAt > 1000) {
    lastExpiredAt = now
    ElMessage.error(message)
  }

  const current = router.currentRoute.value.fullPath
  if (!current.startsWith('/login')) {
    router.replace(`/login?redirect=${encodeURIComponent(current)}`)
  }
}

/**
 * 带自动鉴权刷新能力的 fetch。
 * 支持 HTTP 401，以及 HTTP 200 响应体中的业务 code=401。
 */
export async function fetchWithAuth(input, init = {}, retry = true) {
  const userStore = useUserStore()
  const requestAccessToken = userStore.accessToken
  const headers = new Headers(init.headers || {})
  if (requestAccessToken) {
    headers.set('Authorization', requestAccessToken)
  }

  const response = await fetch(input, { ...init, headers })
  if (!(await isAuthFailure(response))) {
    return response
  }
  if (!retry) {
    const message = await readAuthFailureMessage(response)
    expireSession(message)
    throw new AuthExpiredError(message)
  }

  try {
    await refreshAccessToken(requestAccessToken)
    return fetchWithAuth(input, init, false)
  } catch (error) {
    expireSession('登录已过期，请重新登录')
    throw new AuthExpiredError(error.message)
  }
}

async function isAuthFailure(response) {
  if (response.status === 401) {
    return true
  }
  const contentType = (response.headers.get('content-type') || '').toLowerCase()
  if (!contentType.includes('application/json')) {
    return false
  }
  try {
    const payload = await response.clone().json()
    return payload?.code === 401
  } catch {
    return false
  }
}

async function readAuthFailureMessage(response) {
  try {
    const payload = await response.clone().json()
    return payload?.message || '登录状态已失效，请重新登录'
  } catch {
    return '登录状态已失效，请重新登录'
  }
}
