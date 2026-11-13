import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { expireSession, refreshAccessToken } from '@/utils/authSession'

/**
 * Axios 统一封装
 * --------------------------------------------------------------
 * 后端约定（严格对齐）：
 *  1. 统一返回 { code, message, data }，code === 200 表示成功；
 *     业务异常（401/403/400/404/500）HTTP 状态码仍为 200，在响应体 code 中体现。
 *  2. 鉴权：请求头 Authorization 放原始 JWT（无 Bearer 前缀）。
 *  3. 刷新令牌：POST /api/auth/refresh，请求头带 Authorization(旧accessToken)
 *     与 refreshtoken(旧refreshToken)，返回 data = { accessToken, refreshToken }。
 */
const service = axios.create({
  baseURL: '/api',
  timeout: 120000
})

// ---------- 请求拦截器：统一注入 Token ----------
service.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.accessToken) {
      config.headers['Authorization'] = userStore.accessToken
    }
    return config
  },
  (error) => Promise.reject(error)
)

function getFailedAccessToken(config) {
  const headers = config?.headers
  if (!headers) return ''
  return headers.Authorization || headers.authorization || headers.get?.('Authorization') || ''
}

// ---------- 响应拦截器 ----------
service.interceptors.response.use(
  async (response) => {
    const res = response.data

    // 成功
    if (res.code === 200) {
      return res
    }

    // 401：尝试用 refreshToken 刷新一次后重放原请求
    if (res.code === 401) {
      const originalConfig = response.config
      if (originalConfig._retried) {
        expireSession('登录状态已失效，请重新登录')
        return Promise.reject(res)
      }

      originalConfig._retried = true
      try {
        await refreshAccessToken(getFailedAccessToken(originalConfig))
        // 重放原请求（请求拦截器会重新注入最新 token）
        return service(originalConfig)
      } catch (e) {
        expireSession('登录已过期，请重新登录')
        return Promise.reject(res)
      }
    }

    // 其他业务错误统一提示
    ElMessage.error(res.message || '操作失败')
    return Promise.reject(res)
  },
  async (error) => {
    // 网络层 / HTTP 状态码异常
    if (error.response?.status === 401 && error.config && !error.config._retried) {
      error.config._retried = true
      try {
        await refreshAccessToken(getFailedAccessToken(error.config))
        return service(error.config)
      } catch (refreshError) {
        expireSession('登录状态已失效，请重新登录')
        return Promise.reject(error)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，AI分析可能需要更长时间，请稍后到「我的评测记录」查看结果')
    } else if (!error.response) {
      ElMessage.error('网络异常，无法连接服务器')
    } else {
      ElMessage.error(error.response.data?.message || `请求失败（${error.response.status}）`)
    }
    return Promise.reject(error)
  }
)

export default service
