import {
  AuthExpiredError,
  expireSession,
  fetchWithAuth,
  refreshAccessToken
} from '@/utils/authSession'

/**
 * 会员端 - AI 健康助手
 * 后端接口：
 *  POST   /api/member/chat/session              - 创建会话，返回 sessionId
 *  POST   /api/member/chat/stream               - SSE 流式聊天，body: { sessionId, message }
 *  GET    /api/member/chat/history/{sid}        - 获取某会话的历史消息
 *  GET    /api/member/chat/sessions             - 获取当前用户的会话列表
 *  DELETE /api/member/chat/session/{sid}        - 删除某个会话
 *  DELETE /api/member/chat/sessions/batch       - 批量删除会话，body: [sid, ...]
 *  DELETE /api/member/chat/sessions             - 清空全部会话
 *  PUT    /api/member/chat/session/{sid}/title  - 修改会话标题
 */

function authHeaders() {
  return {
    'Content-Type': 'application/json'
  }
}

export async function createSession() {
  const resp = await fetchWithAuth('/api/member/chat/session', {
    method: 'POST',
    headers: authHeaders()
  })
  const json = await resp.json()
  if (json.code !== 200) {
    throw new Error(json.message || '创建会话失败')
  }
  return json.data
}

export async function getChatHistory(sessionId) {
  const resp = await fetchWithAuth(`/api/member/chat/history/${sessionId}`, {
    method: 'GET'
  })
  const json = await resp.json()
  if (json.code !== 200) {
    throw new Error(json.message || '获取历史失败')
  }
  return (json.data || []).map((item) => ({
    role: item.role === 'user' ? 'user' : 'ai',
    content: item.message || '',
    time: item.createTime ? formatBackendTime(item.createTime) : ''
  }))
}

function formatBackendTime(t) {
  if (!t) return ''
  // 后端返回的是 LocalDateTime（无时区），按其字面时间解析，不要当成 UTC 再换算
  const s = typeof t === 'string' ? t.replace(' ', 'T') : ''
  const d = new Date(s || t)
  if (isNaN(d.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function parseBackendTime(t) {
  if (!t) return 0
  const s = typeof t === 'string' ? t.replace(' ', 'T') : ''
  const d = new Date(s || t)
  return isNaN(d.getTime()) ? 0 : d.getTime()
}

export async function fetchSessions() {
  const resp = await fetchWithAuth('/api/member/chat/sessions', {
    method: 'GET'
  })
  const json = await resp.json()
  if (json.code !== 200) {
    throw new Error(json.message || '获取会话列表失败')
  }
  return (json.data || []).map((item) => ({
    id: item.sessionId,
    title: item.title || '新会话',
    preview: item.preview || '',
    firstMessage: '',
    createdAt: parseBackendTime(item.createTime),
    updatedAt: parseBackendTime(item.updateTime) || parseBackendTime(item.createTime)
  }))
}

export async function removeSession(sessionId) {
  const resp = await fetchWithAuth(`/api/member/chat/session/${encodeURIComponent(sessionId)}`, {
    method: 'DELETE'
  })
  const json = await resp.json()
  if (json.code !== 200) {
    throw new Error(json.message || '删除会话失败')
  }
  return json.data
}

export async function removeSessions(sessionIds) {
  const resp = await fetchWithAuth('/api/member/chat/sessions/batch', {
    method: 'DELETE',
    headers: authHeaders(),
    body: JSON.stringify(sessionIds || [])
  })
  const json = await resp.json()
  if (json.code !== 200) {
    throw new Error(json.message || '批量删除会话失败')
  }
  return json.data
}

export async function removeAllSessions() {
  const resp = await fetchWithAuth('/api/member/chat/sessions', {
    method: 'DELETE'
  })
  const json = await resp.json()
  if (json.code !== 200) {
    throw new Error(json.message || '清空会话失败')
  }
  return json.data
}

export async function renameSessionApi(sessionId, title) {
  const resp = await fetchWithAuth(`/api/member/chat/session/${encodeURIComponent(sessionId)}/title`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify({ title })
  })
  const json = await resp.json()
  if (json.code !== 200) {
    throw new Error(json.message || '重命名失败')
  }
  return json.data
}

export async function chatStream(sessionId, message, onChunk, allowRetry = true) {
  const resp = await fetchWithAuth('/api/member/chat/stream', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ sessionId, message })
  })

  if (!resp.ok) {
    throw new Error(`请求失败 (${resp.status})`)
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  let fullText = ''

  function processDataPayload(payload) {
    if (!payload || payload === '[DONE]') return
    let parsed = null
    try {
      parsed = JSON.parse(payload)
    } catch {
      // 不是 JSON，当作纯文本 chunk 处理（兼容旧格式）
      fullText += payload
      if (onChunk) onChunk(payload)
      return
    }

    if (parsed?.code === 401) {
      throw new AuthExpiredError(parsed.message || '登录状态已失效，请重新登录')
    } else if (parsed.type === 'text' && typeof parsed.content === 'string') {
      fullText += parsed.content
      if (onChunk) onChunk(parsed.content)
    } else if (parsed.type === 'error') {
      const errMsg = parsed.content || 'AI服务异常'
      throw new Error(errMsg)
    } else if (parsed.type === 'done') {
      // 流结束
    } else if (typeof parsed.content === 'string') {
      // 兜底：有 content 字段就追加
      fullText += parsed.content
      if (onChunk) onChunk(parsed.content)
    } else if (typeof parsed === 'string') {
      // 兜底：整个 payload 是字符串
      fullText += parsed
      if (onChunk) onChunk(parsed)
    }
  }

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      const lines = buffer.split('\n')
      buffer = lines.pop()

      let currentData = ''
      let hasCurrentData = false

      for (const line of lines) {
        const trimmed = line.replace(/\r$/, '')
        if (trimmed === '') {
          if (hasCurrentData) {
            processDataPayload(currentData)
            currentData = ''
            hasCurrentData = false
          }
          continue
        }

        let dataPart
        if (trimmed.startsWith('data:')) {
          dataPart = trimmed.slice(5)
          if (dataPart.startsWith(' ')) dataPart = dataPart.slice(1)
        } else {
          dataPart = trimmed
        }

        if (hasCurrentData) {
          currentData += '\n' + dataPart
        } else {
          currentData = dataPart
          hasCurrentData = true
        }
      }
    }

    // 处理 buffer 中剩余的数据
    let currentData = ''
    let hasCurrentData = false
    for (const raw of buffer.split('\n')) {
      const trimmed = raw.replace(/\r$/, '')
      if (trimmed === '') {
        if (hasCurrentData) {
          processDataPayload(currentData)
          currentData = ''
          hasCurrentData = false
        }
        continue
      }
      let dataPart
      if (trimmed.startsWith('data:')) {
        dataPart = trimmed.slice(5)
        if (dataPart.startsWith(' ')) dataPart = dataPart.slice(1)
      } else {
        dataPart = trimmed
      }
      if (hasCurrentData) {
        currentData += '\n' + dataPart
      } else {
        currentData = dataPart
        hasCurrentData = true
      }
    }
    if (hasCurrentData) {
      processDataPayload(currentData)
    }
  } catch (error) {
    if (error instanceof AuthExpiredError && allowRetry && !fullText) {
      try {
        await refreshAccessToken()
        return chatStream(sessionId, message, onChunk, false)
      } catch (refreshError) {
        expireSession('登录已过期，请重新登录')
        throw new AuthExpiredError(refreshError.message)
      }
    }
    if (error instanceof AuthExpiredError) {
      expireSession('登录已过期，请重新登录')
    }
    throw error
  }

  return fullText
}
