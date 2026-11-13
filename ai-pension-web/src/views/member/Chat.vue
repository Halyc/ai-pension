<template>
  <div
    class="chat-root"
    :class="{
      'is-mobile': isMobile,
      'sidebar-collapsed': !isMobile && sidebarCollapsed,
      'mobile-sidebar-open': isMobile && mobileSidebarOpen
    }"
  >
    <!-- ============ 侧边栏 ============ -->
    <aside class="chat-sidebar" :class="{ 'sidebar-hidden': sidebarCollapsed && !isMobile }">
      <div class="sidebar-header">
        <div class="sidebar-brand">
          <span class="brand-icon">💬</span>
          <span>历史会话</span>
        </div>
        <button
          v-if="!isMobile"
          class="sidebar-close-btn"
          @click="toggleSidebar"
          title="收起侧边栏"
        >
          <el-icon :size="18"><DArrowLeft /></el-icon>
        </button>
        <button
          v-else
          class="sidebar-close-btn"
          @click="mobileSidebarOpen = false"
          title="关闭"
        >
          <el-icon :size="18"><Close /></el-icon>
        </button>
      </div>

      <div class="sidebar-actions">
        <el-button
          type="primary"
          class="new-chat-btn"
          :icon="Plus"
          @click="createNewSession"
        >
          新建会话
        </el-button>
        <el-tooltip :content="selectMode ? '退出批量管理' : '批量管理'" placement="bottom">
          <button class="manage-btn" :class="{ active: selectMode }" @click="toggleSelectMode">
            <el-icon :size="18"><Select /></el-icon>
          </button>
        </el-tooltip>
      </div>

      <div class="sidebar-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索会话..."
          :prefix-icon="Search"
          clearable
          size="default"
        />
      </div>

      <div class="sidebar-list-wrapper" ref="sessionListRef">
        <div v-if="!groupedSessions.length" class="sidebar-empty">
          <el-icon :size="36" color="#c0c4cc"><ChatLineSquare /></el-icon>
          <p>暂无历史会话</p>
        </div>

        <div v-for="group in groupedSessions" :key="group.label" class="session-group">
          <div class="session-group-label">{{ group.label }}</div>
          <div class="sidebar-list">
            <div
              v-for="s in group.items"
              :key="s.id"
              class="session-item"
              :class="{ active: s.id === currentSessionId, selected: isSelected(s.id) }"
              @click="handleSessionClick(s.id)"
              @contextmenu.prevent="openContextMenu($event, s)"
            >
              <div
                v-if="selectMode"
                class="session-check"
                :class="{ checked: isSelected(s.id) }"
                @click.stop="toggleSelect(s.id)"
              >
                <el-icon v-if="isSelected(s.id)" :size="12"><Check /></el-icon>
              </div>
              <div v-else class="session-item-icon">💬</div>
              <div class="session-item-body">
                <div class="session-title">{{ s.title || '新会话' }}</div>
                <div class="session-preview">{{ getSessionPreview(s) }}</div>
              </div>
              <div v-if="!selectMode" class="session-item-actions">
                <el-tooltip content="重命名" placement="top">
                  <button class="action-icon" @click.stop="renameSession(s.id)">
                    <el-icon><EditPen /></el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <button class="action-icon danger" @click.stop="deleteSession(s.id)">
                    <el-icon><Delete /></el-icon>
                  </button>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="sidebar-footer">
        <div v-if="selectMode" class="batch-bar">
          <span class="batch-count">已选 {{ selectedIds.length }} 项</span>
          <div class="batch-actions">
            <el-button link class="batch-cancel-btn" @click="toggleSelectMode">取消</el-button>
            <el-button
              type="danger"
              size="small"
              :disabled="!selectedIds.length"
              @click="deleteSelectedSessions"
            >
              删除选中
            </el-button>
          </div>
        </div>
        <el-button v-else link class="clear-all-btn" @click="clearAllSessions" v-if="sessions.length">
          清空全部会话
        </el-button>
      </div>
    </aside>

    <!-- 移动端遮罩 -->
    <div
      v-if="isMobile && mobileSidebarOpen"
      class="sidebar-mask"
      @click="mobileSidebarOpen = false"
    ></div>

    <main class="chat-main">
      <div class="chat-window">
        <div class="chat-header">
          <div class="chat-header-left">
            <button
              v-if="isMobile || sidebarCollapsed"
              class="sidebar-toggle-btn"
              @click="toggleSidebar"
              title="历史会话"
            >
              <el-icon :size="18"><Menu /></el-icon>
            </button>
            <div class="chat-avatar">🤖</div>
            <div>
              <div class="chat-name">AI 健康助手</div>
              <div class="chat-status">
                <el-icon color="#50C878"><CircleCheckFilled /></el-icon>
                在线 · 已接入您的健康档案与预约信息
              </div>
            </div>
          </div>
          <div class="chat-header-actions">
            <el-tooltip content="新建会话" placement="bottom">
              <button class="icon-action-btn" @click="createNewSession">
                <el-icon :size="18"><Plus /></el-icon>
              </button>
            </el-tooltip>
          </div>
        </div>

        <div ref="messageBoxRef" class="chat-messages">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="getMessageRole(m)">
            <div v-if="getMessageRole(m) === 'user'" class="msg-avatar msg-avatar-img"><img :src="userAvatar" alt="" /></div>
            <div v-else class="msg-avatar">🤖</div>
            <div class="msg-bubble">
              <div class="msg-text">
                <template v-if="getMessageRole(m) === 'ai'">
                  <div class="markdown-body" v-html="renderMarkdown(getMessageContent(m))"></div>
                </template>
                <template v-else>
                  {{ getMessageContent(m) }}
                </template>
                <span v-if="m && m.streaming" class="cursor">|</span>
              </div>
              <div class="msg-time">{{ m && m.time ? m.time : '' }}</div>
            </div>
          </div>
          <div v-if="!messages.length" class="welcome">
            <el-icon :size="40" color="#2f7d6d"><ChatDotRound /></el-icon>
            <p>您好！我是您的 AI 健康助手，可以为您解答健康问题、推荐体检套餐、解读评测结果。</p>
            <div class="suggestions">
              <el-button
                v-for="s in suggestions"
                :key="s"
                round
                size="large"
                @click="quickSend(s)"
              >
                {{ s }}
              </el-button>
            </div>
          </div>
        </div>

        <div class="chat-input">
          <textarea
            ref="nativeTextareaRef"
            v-model="input"
            class="chat-textarea"
            rows="2"
            placeholder="请输入您想咨询的健康问题，Enter 发送 / Shift+Enter 换行"
            @keydown="handleKeydown"
          ></textarea>
          <el-button
            type="primary"
            circle
            :icon="Promotion"
            :loading="sending"
            class="send-btn"
            @click="send"
          />
        </div>
      </div>
    </main>

    <!-- 右键菜单 -->
    <ul
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
    >
      <li @click="renameSession(ctxMenu.sessionId)">
        <el-icon><EditPen /></el-icon>重命名
      </li>
      <li class="danger" @click="deleteSession(ctxMenu.sessionId)">
        <el-icon><Delete /></el-icon>删除会话
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, watch, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Promotion,
  ChatDotRound,
  CircleCheckFilled,
  Plus,
  Delete,
  Search,
  Menu,
  Close,
  DArrowLeft,
  EditPen,
  ChatLineSquare,
  Select,
  Check
} from '@element-plus/icons-vue'
import {
  chatStream,
  createSession,
  getChatHistory,
  fetchSessions,
  removeSession,
  removeSessions,
  removeAllSessions,
  renameSessionApi
} from '@/api/chat'
import { memberAppointmentApi } from '@/api/memberAppointment'
import { memberActivityApi } from '@/api/memberActivity'
import { healthApi } from '@/api/health'
import { memberAssessmentApi } from '@/api/memberAssessment'
import { useUserStore } from '@/store/user'
import { useResponsive } from '@/utils/useResponsive'
import { buildAvatar, getScoreInfo, parseJsonSafe, replaceSalutation as _replaceSalutation } from '@/utils/format'
import { getImage, userKey } from '@/utils/imageCache'
import { marked } from 'marked'

marked.setOptions({
  breaks: true,
  gfm: true
})

const userStore = useUserStore()
const { isMobile } = useResponsive()

const avatarRefreshTick = ref(0)
const userAvatar = computed(() => {
  avatarRefreshTick.value
  const u = userStore.user
  if (u?.id) {
    const cached = getImage(userKey(u.id))
    if (cached) return cached
  }
  return buildAvatar(u)
})

window.addEventListener('user-avatar-updated', () => { avatarRefreshTick.value++ })
window.addEventListener('image-cache-ready', () => { avatarRefreshTick.value++ })

const input = ref('')
const sending = ref(false)
const messages = ref([])
const messageBoxRef = ref(null)
const sessionListRef = ref(null)
const nativeTextareaRef = ref(null)

const sidebarCollapsed = ref(false)
const mobileSidebarOpen = ref(false)
const searchKeyword = ref('')

const STORAGE_DRAFT_PREFIX = 'ai_pension_chat_draft_'

function getUid() {
  return (userStore && userStore.user && userStore.user.id) || 'guest'
}
function getDraftKey() {
  return `${STORAGE_DRAFT_PREFIX}${getUid()}`
}

function formatDateTime(ts) {
  const d = new Date(ts)
  const now = new Date()
  const diff = now - d
  const pad = (n) => String(n).padStart(2, '0')
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (d.toDateString() === now.toDateString()) return `今天 ${pad(d.getHours())}:${pad(d.getMinutes())}`
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  if (diff < 86400000 * 7) return `${Math.floor(diff / 86400000)}天前`
  return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())}`
}

function generateSessionId() {
  return 's_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 8)
}

const sessions = ref([])
const currentSessionId = ref(null)

async function refreshSessions() {
  try {
    sessions.value = await fetchSessions()
  } catch (e) {
    console.warn('加载会话列表失败:', e)
  }
}

const selectMode = ref(false)
const selectedIds = ref([])

function toggleSelectMode() {
  selectMode.value = !selectMode.value
  selectedIds.value = []
}

function isSelected(id) {
  return selectMode.value && selectedIds.value.includes(id)
}

function toggleSelect(id) {
  const index = selectedIds.value.indexOf(id)
  if (index >= 0) selectedIds.value.splice(index, 1)
  else selectedIds.value.push(id)
}

function handleSessionClick(id) {
  if (selectMode.value) {
    toggleSelect(id)
    return
  }
  switchSession(id)
}

const sortedSessions = computed(() =>
  [...sessions.value].sort((a, b) => b.updatedAt - a.updatedAt)
)

const filteredSessions = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return sortedSessions.value
  return sortedSessions.value.filter((s) => {
    if (s.title && s.title.toLowerCase().includes(kw)) return true
    if (s.firstMessage && s.firstMessage.toLowerCase().includes(kw)) return true
    return (s.preview || '').toLowerCase().includes(kw)
  })
})

const groupedSessions = computed(() => {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const weekStart = today - 86400000 * 6
  const groups = { '今天': [], '昨天': [], '最近7天': [], '更早': [] }
  filteredSessions.value.forEach((s) => {
    const t = s.updatedAt || 0
    if (t >= today) groups['今天'].push({ ...s, timeLabel: formatDateTime(t) })
    else if (t >= today - 86400000) groups['昨天'].push({ ...s, timeLabel: formatDateTime(t) })
    else if (t >= weekStart) groups['最近7天'].push({ ...s, timeLabel: formatDateTime(t) })
    else groups['更早'].push({ ...s, timeLabel: formatDateTime(t) })
  })
  return Object.entries(groups)
    .filter(([, items]) => items.length > 0)
    .map(([label, items]) => ({ label, items }))
})

function getSessionPreview(session) {
  return session.preview || '暂无消息'
}

async function createNewSession() {
  let id
  try {
    id = await createSession()
  } catch {
    id = generateSessionId()
  }
  messages.value = []
  currentSessionId.value = id
  if (isMobile.value) mobileSidebarOpen.value = false
}

function ensureSessionSaved(sessionId, firstMessage) {
  if (!sessionId) return
  const existing = sessions.value.find((s) => s.id === sessionId)
  if (existing) return
  const now = Date.now()
  sessions.value.unshift({
    id: sessionId,
    title: firstMessage ? (firstMessage.length > 20 ? firstMessage.slice(0, 20) + '...' : firstMessage) : '新会话',
    createdAt: now,
    updatedAt: now,
    firstMessage: firstMessage || '',
    preview: firstMessage || ''
  })
}

const CONTEXT_USER_MSG_PREFIX = '[以下是本系统数据库中的最新信息'
const CONTEXT_USER_MSG_SEP = '\n\n用户问题：'

function stripContextPrefix(content) {
  if (typeof content !== 'string') return content
  const idx = content.indexOf(CONTEXT_USER_MSG_SEP)
  if (idx < 0) return content
  return content.slice(idx + CONTEXT_USER_MSG_SEP.length)
}

function cleanMessageList(list) {
  return (list || []).map((m) => {
    if (m && m.role === 'user') {
      return { ...m, content: stripContextPrefix(m.content) }
    }
    return m
  })
}

function mergeMessages(backendList, localList) {
  backendList = cleanMessageList(backendList)
  localList = cleanMessageList(localList)
  if (!backendList || !backendList.length) return localList || []
  if (!localList || !localList.length) return backendList || []

  const merged = []
  const localUsed = new Set()

  for (const bMsg of backendList) {
    let picked = bMsg
    const matchIdx = localList.findIndex(
      (lMsg, i) =>
        !localUsed.has(i) &&
        lMsg.role === bMsg.role &&
        lMsg.content &&
        bMsg.content &&
        (lMsg.content === bMsg.content ||
          bMsg.content.includes(lMsg.content) ||
          lMsg.content.includes(bMsg.content))
    )
    if (matchIdx >= 0) {
      const lMsg = localList[matchIdx]
      localUsed.add(matchIdx)
      if (bMsg.role === 'user') {
        picked = lMsg.content.length < bMsg.content.length ? lMsg : bMsg
      } else if (lMsg.content.length > bMsg.content.length) {
        picked = lMsg
      }
    }
    merged.push(picked)
  }

  for (let i = 0; i < localList.length; i++) {
    if (!localUsed.has(i)) merged.push(localList[i])
  }

  return cleanMessageList(merged)
}

async function switchSession(id) {
  currentSessionId.value = id
  try {
    const history = await getChatHistory(id)
    messages.value = cleanMessageList(history)
  } catch (e) {
    console.warn('加载聊天记录失败:', e)
    messages.value = []
  }
  scrollToBottom()
  if (isMobile.value) mobileSidebarOpen.value = false
}

async function deleteSession(id) {
  try {
    await ElMessageBox.confirm('确定要删除这个会话吗？删除后不可恢复。', '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
      type: 'warning',
      draggable: false
    })
  } catch {
    return
  }
  try {
    await removeSession(id)
    await refreshSessions()
    if (currentSessionId.value === id) {
      if (sessions.value.length > 0) {
        switchSession(sortedSessions.value[0].id)
      } else {
        createNewSession()
      }
    }
    ElMessage.success('会话已删除')
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

async function deleteSelectedSessions() {
  if (!selectedIds.value.length) return
  const ids = [...selectedIds.value]
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 个会话吗？删除后不可恢复。`, '批量删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
      type: 'warning',
      draggable: false
    })
  } catch {
    return
  }
  try {
    await removeSessions(ids)
    const currentDeleted = ids.includes(currentSessionId.value)
    await refreshSessions()
    selectMode.value = false
    selectedIds.value = []
    if (currentDeleted) {
      if (sessions.value.length > 0) {
        switchSession(sortedSessions.value[0].id)
      } else {
        createNewSession()
      }
    }
    ElMessage.success(`已删除 ${ids.length} 个会话`)
  } catch (e) {
    ElMessage.error(e.message || '批量删除失败')
  }
}

async function renameSession(id) {
  const session = sessions.value.find((s) => s.id === id)
  if (!session) return
  let value
  try {
    const res = await ElMessageBox.prompt('请输入新的会话名称', '重命名会话', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: session.title,
      inputPattern: /\S+/,
      inputErrorMessage: '名称不能为空'
    })
    value = res.value
  } catch {
    return
  }
  const title = (value || '').trim().slice(0, 50)
  if (!title) return
  try {
    await renameSessionApi(id, title)
    await refreshSessions()
    ElMessage.success('已更新')
  } catch (e) {
    ElMessage.error(e.message || '重命名失败')
  }
}

async function clearAllSessions() {
  try {
    await ElMessageBox.confirm(
      '确定要清空全部会话记录吗？此操作不可恢复。',
      '清空全部会话',
      {
        confirmButtonText: '清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  try {
    await removeAllSessions()
    await refreshSessions()
    await createNewSession()
    ElMessage.success('已清空全部会话')
  } catch (e) {
    ElMessage.error(e.message || '清空失败')
  }
}

function openContextMenu(e, s) {
  closeContextMenu()
  ctxMenu.value = { visible: true, x: e.clientX, y: e.clientY, sessionId: s.id }
}
function closeContextMenu() {
  ctxMenu.value = { visible: false, x: 0, y: 0, sessionId: null }
}
const ctxMenu = ref({ visible: false, x: 0, y: 0, sessionId: null })
function onDocClick() {
  closeContextMenu()
}
onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))

function toggleSidebar() {
  if (isMobile.value) {
    mobileSidebarOpen.value = !mobileSidebarOpen.value
  } else {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }
}

onMounted(async () => {
  await refreshSessions()

  try {
    const draft = localStorage.getItem(getDraftKey())
    if (draft) {
      input.value = draft
      if (nativeTextareaRef.value) {
        nativeTextareaRef.value.value = draft
      }
    }
  } catch {}
})

watch(
  input,
  (val) => {
    try {
      localStorage.setItem(getDraftKey(), val || '')
    } catch {}
  },
  { flush: 'debounce' }
)

watch(isMobile, (val) => {
  if (!val) mobileSidebarOpen.value = false
})

const suggestions = ['最近适合做什么体检？', '血压偏高要注意什么？', '帮我推荐一个体检套餐']

function getMessageRole(message) {
  return message && message.role ? message.role : 'ai'
}
function getMessageContent(message) {
  return message && typeof message.content === 'string' ? message.content : ''
}
function now() {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function renderMarkdown(text) {
  if (!text) return ''
  try {
    return marked.parse(preprocessMarkdown(replaceSalutation(text)))
  } catch {
    return replaceSalutation(text)
  }
}

function replaceSalutation(text) {
  return _replaceSalutation(text, userStore.user)
}

function preprocessMarkdown(text) {
  let s = text.replace(/\r\n?/g, '\n')

  // 1. 去掉加粗标记，避免 ** 裸露在界面上
  s = s.replace(/\*\*/g, '')
  s = s.replace(/^[ \t]*#{1,6}[ \t]*/gm, '')

  // 2. 统一项目符号（* • · ● ▪）为 Markdown 列表
  s = s.replace(/^([ \t]*)[*•·●▪◆][ \t]*/gm, '$1- ')
  // 3. 列表标记后补空格："-xxx" -> "- xxx"
  s = s.replace(/^([ \t]*)([-+])(?=[^\s-])/gm, '$1$2 ')
  // 4. 有序列表补空格："1.xxx" -> "1. xxx"
  s = s.replace(/^([ \t]*)(\d{1,2})\.(?=\S)/gm, '$1$2. ')

  // 5. 去掉残留的单星号（斜体/装饰符）
  s = s.replace(/\*/g, '')

  // 6. 压缩多余空行，避免列表/段落之间出现大片空白
  s = s.replace(/\n{3,}/g, '\n\n')

  return s.trim()
}

function scrollToBottom() {
  nextTick(() => {
    if (messageBoxRef.value) {
      messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight
    }
  })
}

let cachedContext = ''
let cachedContextAt = 0
let cachedPackages = []
let cachedActivities = []
const CONTEXT_TTL_MS = 10 * 60 * 1000

function parseTs(v) { return v ? new Date(v).getTime() : NaN }
function dynIsCancelled(a) { return a.status === 'CANCELLED' || a.status === 'PAUSED' }
function dynIsEnded(a) {
  const ae = parseTs(a.activityEnd)
  if (!isNaN(ae)) return Date.now() > ae
  return a.status === 'ENDED' || a.status === 'FINISHED'
}
function dynIsOngoing(a) {
  if (dynIsCancelled(a) || dynIsEnded(a)) return false
  const now = Date.now()
  const as = parseTs(a.activityStart), ae = parseTs(a.activityEnd)
  return !isNaN(as) && now >= as && (isNaN(ae) || now <= ae)
}
function dynIsRecruiting(a) {
  if (dynIsCancelled(a) || dynIsEnded(a) || dynIsOngoing(a)) return false
  const now = Date.now()
  const rs = parseTs(a.registrationStart), re = parseTs(a.registrationEnd)
  return !isNaN(rs) && !isNaN(re) && now >= rs && now <= re
}
function dynIsRegOpen(a) {
  if (dynIsCancelled(a) || dynIsEnded(a) || dynIsOngoing(a)) return false
  const now = Date.now()
  const rs = parseTs(a.registrationStart), re = parseTs(a.registrationEnd)
  return !isNaN(rs) && !isNaN(re) && now >= rs && now <= re
}
function dynStatusLabel(a) {
  if (dynIsCancelled(a)) return '已取消'
  if (dynIsEnded(a)) return '已结束'
  if (dynIsOngoing(a)) return '进行中'
  if (dynIsRecruiting(a)) return '报名中'
  const rs = parseTs(a.registrationStart)
  if (!isNaN(rs) && Date.now() < rs) return '报名未开始'
  return '报名已截止'
}

async function fetchDataContext() {
  const now = Date.now()
  if (cachedContext && now - cachedContextAt < CONTEXT_TTL_MS) {
    console.log('[AI Context] 使用缓存:', cachedContext.slice(0, 200), '...')
    return cachedContext
  }
  try {
    const [pkgRes, actRes, mineRes, healthRes, assessRes] = await Promise.all([
      memberAppointmentApi.listPackages().catch((e) => { console.warn('[AI Context] 套餐 API 失败:', e); return null }),
      memberActivityApi.listActivities().catch((e) => { console.warn('[AI Context] 活动 API 失败:', e); return null }),
      memberActivityApi.listMyActivities().catch((e) => { console.warn('[AI Context] 我的报名 API 失败:', e); return null }),
      healthApi.history(12).catch((e) => { console.warn('[AI Context] 健康档案 API 失败:', e); return null }),
      memberAssessmentApi.history().catch((e) => { console.warn('[AI Context] 评测历史 API 失败:', e); return null })
    ])
    const packages = (pkgRes?.data || pkgRes || []).slice(0, 8)
    const activities = (actRes?.data || actRes || []).slice(0, 8)
    const myIds = new Set(
      (mineRes?.data || mineRes || []).map((r) => Number(r.activityId))
    )
    const healthRecords = (healthRes?.data || healthRes || []).slice(0, 8)
    const assessments = (assessRes?.data || assessRes || []).slice(0, 6)
    cachedPackages = packages
    cachedActivities = activities

    const recentDetails = []
    for (const a of assessments.slice(0, 3)) {
      try {
        const d = await memberAssessmentApi.detail(a.id)
        if (d?.data) recentDetails.push(d.data)
      } catch { /* ignore */ }
    }

    const pkgLines = packages.map((p, i) => {
      const name = p.name || p.title || '未命名套餐'
      const price = p.price != null ? `${p.price}积分` : ''
      const suitable = p.suitablePeople || p.suitable_people || ''
      let itemStr = ''
      const rawItems = p.items
      if (Array.isArray(rawItems)) {
        itemStr = rawItems.slice(0, 4).join('/')
      } else if (typeof rawItems === 'string') {
        itemStr = rawItems.split(/[,，、;；]/).map((s) => s.trim()).filter(Boolean).slice(0, 4).join('/')
      }
      return `${i + 1}. ${name}${price ? `（${price}）` : ''}${suitable ? ` 适用:${suitable}` : ''}${itemStr ? ` 项目:${itemStr}` : ''}`
    })

    const enrichAct = (a) => {
      const reg = dynIsRecruiting(a)
      const regOpen = dynIsRegOpen(a)
      const mine = myIds.has(Number(a.id))
      return {
        ...a,
        _dynStatus: dynStatusLabel(a),
        _dynRecruiting: reg,
        _dynRegOpen: regOpen,
        _mine: mine,
        _recommendable: regOpen && !mine
      }
    }
    const enriched = activities.map(enrichAct)

    const recommendable = enriched.filter((a) => a._recommendable)
    const enrolled = enriched.filter((a) => a._mine)
    const notOpen = enriched.filter((a) => !a._recommendable && !a._mine)

    const parts = []

    if (healthRecords.length) {
      const now = Date.now()
      const threeMonthsAgo = now - 90 * 24 * 3600 * 1000

      const parseTs = (v) => {
        if (!v) return NaN
        const d = new Date(v)
        return isNaN(d.getTime()) ? NaN : d.getTime()
      }
      const healthTs = healthRecords.map((h) => parseTs(h.recorded_at || h.recordedAt || h.record_time || h.createTime))
      const isRecent3M = (_, i) => healthTs[i] >= threeMonthsAgo
      const isOlder = (_, i) => healthTs[i] < threeMonthsAgo

      const fmtHealthLine = (h) => {
        const t = h.recorded_at || h.recordedAt || h.record_time || h.createTime || ''
        const bp = h.systolic && h.diastolic ? `${h.systolic}/${h.diastolic}mmHg` : (h.systolic ? `收缩压${h.systolic}mmHg` : '') + (h.diastolic ? `舒张压${h.diastolic}mmHg` : '')
        const sugar = h.blood_sugar != null ? `血糖${h.blood_sugar}mmol/L` : (h.bloodSugar != null ? `血糖${h.bloodSugar}mmol/L` : '')
        const hr = h.heart_rate != null ? `心率${h.heart_rate}次/分` : (h.heartRate != null ? `心率${h.heartRate}次/分` : '')
        const wt = h.weight != null ? `体重${h.weight}kg` : ''
        const ht = h.height != null ? `身高${h.height}cm` : ''
        const bmi = h.bmi != null ? `BMI${h.bmi}` : ''
        const memo = h.memo ? `备注:${h.memo}` : ''
        const items = [bp, sugar, hr, wt, ht, bmi, memo].filter(Boolean)
        return `${t || '未知时间'} ${items.join(' ')}`
      }

      const recent3M = healthRecords.filter(isRecent3M)
      const older = healthRecords.filter(isOlder)

      let healthBlock = ''
      if (recent3M.length) {
        healthBlock += `【最近三个月（近90天）的健康档案，共${recent3M.length}条 — 回答"最近三个月""近三个月""近三个月的记录"时只用这组数据】\n`
        healthBlock += recent3M.map((h, i) => `${i + 1}. ${fmtHealthLine(h)}`).join('\n')
      }
      if (older.length) {
        if (healthBlock) healthBlock += '\n\n'
        healthBlock += `【更早的健康档案（90天前），共${older.length}条 — 仅在用户明确询问更早时间段时使用，回答"最近三个月"不要用这组数据】\n`
        healthBlock += older.map((h, i) => `${i + 1}. ${fmtHealthLine(h)}`).join('\n')
      }
      parts.push(`【您的全部健康档案记录（最近12个月，共${healthRecords.length}条）】\n以下数据已按时间分组，请严格按照分组回答用户问题：\n${healthBlock}`)
    } else {
      parts.push(`【您的健康档案记录】\n您的系统中目前没有找到健康档案记录（血压、血糖、心率、体重等体征数据）。如果用户询问健康记录，可以如实告知。`)
    }

    if (assessments.length) {
      const detailText = recentDetails.map((d, idx) => {
        let scoreInfo = ''
        if (d.aiScore != null) {
          const info = getScoreInfo(d.aiScore)
          if (info) scoreInfo = `${info.score}分（${info.level}）`
          else scoreInfo = `${d.aiScore}分`
        }
        const ansObj = parseJsonSafe(d.answers, {})
        const qas = (d.questions || []).map((q, qi) => {
          let ans = ansObj[q.id] ?? ansObj[String(q.id)] ?? ansObj[String(qi + 1)] ?? ''
          if (Array.isArray(ans)) ans = ans.join('、')
          else ans = String(ans)
          return `  Q${qi + 1}. ${q.content} → ${ans || '未作答'}`
        }).join('\n')
        return `---\n【第${idx + 1}次】${d.questionnaireTitle || '未命名问卷'} ${scoreInfo} ${d.createTime || ''}\n答题详情:\n${qas}\nAI建议: ${d.aiSuggestion || '（暂无）'}\n---`
      }).join('\n')

      const assessLines = assessments.map((a, i) => {
        const title = a.questionnaireTitle || '未命名问卷'
        let scoreInfo = ''
        if (a.aiScore != null) {
          const info = getScoreInfo(a.aiScore)
          if (info) scoreInfo = ` ${info.score}分·${info.level}`
          else scoreInfo = ` ${a.aiScore}分`
        }
        const t = a.createTime || ''
        return `${i + 1}. ${title}${scoreInfo}${t ? ` 提交时间:${t}` : ''}`
      })

      parts.push(
        `【您的健康评测记录（共${assessments.length}条）】\n` +
        `以下是您在系统中提交的健康评测问卷和AI评分结果。系统展示给用户的是百分制分数和等级（如46分·较差、82分·良好），请优先使用百分制分数和等级来回答。\n` +
        `汇总列表:\n${assessLines.join('\n')}\n\n` +
        `最近${recentDetails.length}次的完整答题详情和AI健康建议:\n${detailText}`
      )
    } else {
      parts.push(`【您的健康评测记录】\n您的系统中目前没有找到健康评测记录。如果用户询问健康评测，可以如实告知。`)
    }

    if (pkgLines.length) {
      parts.push(`【当前可预约体检套餐】\n${pkgLines.join('\n')}`)
    }

    if (recommendable.length) {
      const recLines = recommendable.map((a, i) => {
        const cap = a.maxParticipants ? `${a.currentParticipants || 0}/${a.maxParticipants}` : ''
        const start = a.activityStart || a.activity_start || ''
        const regEnd = a.registrationEnd || a.registration_end || ''
        return `${i + 1}. ${a.title} [${a._dynStatus}]${start ? ` 活动时间:${start}` : ''}${regEnd ? ` 报名截止:${regEnd}` : ''}${cap ? ` 报名:${cap}` : ''}`
      })
      parts.push(`【当前可以报名的社区活动】\n注意：只有以下活动处于报名开放期且您尚未报名，才能推荐用户参加。\n${recLines.join('\n')}`)
    }

    if (enrolled.length) {
      const mineLines = enrolled.map((a, i) => {
        const start = a.activityStart || a.activity_start || ''
        return `${i + 1}. ${a.title} [${a._dynStatus}]${start ? ` 活动时间:${start}` : ''}`
      })
      parts.push(`【您已经报名的活动】\n以下活动您已经报名，绝对不要再推荐用户报名这些！\n${mineLines.join('\n')}`)
    }

    if (notOpen.length) {
      const notOpenLines = notOpen.map((a, i) => {
        const reason =
          dynIsCancelled(a) ? '活动已取消' :
          dynIsEnded(a) ? '活动已结束' :
          dynIsOngoing(a) ? '活动进行中，报名已截止' :
          dynIsRecruiting(a) ? '' : '报名已截止或尚未开始'
        return `${i + 1}. ${a.title} [${a._dynStatus}] — ${reason}`
      })
      parts.push(`【当前不可报名的社区活动】\n以下活动不可报名，不要推荐用户参加：\n${notOpenLines.join('\n')}`)
    }

    cachedContext = parts.join('\n\n')
    cachedContextAt = now
    return cachedContext
  } catch (e) {
    console.warn('fetchDataContext failed:', e)
    return cachedContext || ''
  }
}

async function detectAndExecuteAction(text) {
  if (!text) return null

  const CANCEL_ACTIVITY_KEYS = ['取消报名', '取消活动', '撤销报名', '退出活动', '退出报名', '取消活动报名', '取消参加']
  const CANCEL_KEYS = ['取消预约', '取消体检', '撤销预约', '退款预约']
  const BOOK_KEYS = ['预约', '预定', '挂号', '帮我约', '帮我预约', '确认预约', '好的帮我约', '行帮我约']
  const REG_KEYS = ['报名活动', '报名参加', '参加活动', '帮我报名', '帮我参加', '活动报名']

  const isCancelActivity = CANCEL_ACTIVITY_KEYS.some((k) => text.includes(k)) || /取消.{0,6}(活动|报名)/.test(text)
  const isCancel = !isCancelActivity && (CANCEL_KEYS.some((k) => text.includes(k)) || /取消.{0,6}预约/.test(text))
  const isRegister = !isCancelActivity && !isCancel && REG_KEYS.some((k) => text.includes(k))
  const isBook = !isCancelActivity && !isCancel && !isRegister && BOOK_KEYS.some((k) => text.includes(k))

  if (!isCancelActivity && !isCancel && !isBook && !isRegister) return null

  try {
    if (isCancelActivity) {
      const myRes = await memberActivityApi.listMyActivities()
      const myList = (myRes?.data || myRes || [])
      if (!myList.length) {
        return '【系统执行-取消活动报名】失败：您当前没有报名任何活动，无法取消。'
      }
      const acts = cachedActivities.length ? cachedActivities : (await memberActivityApi.listActivities()).data || []
      let matchedReg = null
      let matchedAct = null
      let matchedActTitle = ''
      for (const reg of myList) {
        const actId = reg.activityId || reg.activity?.id
        const actTitle = reg.activity?.title || ''
        const fullMatch = acts.find((a) => Number(a.id) === Number(actId))
        const title = fullMatch?.title || actTitle
        if (title && text.includes(title)) {
          matchedReg = reg
          matchedAct = fullMatch
          matchedActTitle = title
          break
        }
      }
      if (!matchedReg) {
        matchedReg = myList[0]
        const actId = matchedReg.activityId || matchedReg.activity?.id
        matchedAct = acts.find((a) => Number(a.id) === Number(actId))
        matchedActTitle = matchedReg.activity?.title || matchedAct?.title || '活动'
      }
      if (matchedReg.checkInStatus === 'CHECKED_IN') {
        return `【系统执行-取消活动报名】失败：活动「${matchedActTitle}」您已签到，无法取消报名。`
      }
      if (matchedAct && !dynIsRegOpen(matchedAct)) {
        const reason = dynIsCancelled(matchedAct) ? '活动已取消'
          : dynIsEnded(matchedAct) ? '活动已结束'
          : dynIsOngoing(matchedAct) ? '活动正在进行中，报名已截止'
          : '报名已截止'
        return `【系统执行-取消活动报名】失败：活动「${matchedActTitle}」${reason}，仅在报名期内可以取消报名。`
      }
      const actId = matchedReg.activityId || matchedReg.activity?.id
      if (!actId) {
        return '【系统执行-取消活动报名】失败：未能找到您报名的活动信息。'
      }
      await memberActivityApi.cancelRegistration(actId)
      cachedContextAt = 0
      return `【系统执行-取消活动报名】成功！已取消您在活动「${matchedActTitle}」的报名。请用中文友好地告知用户取消成功，并建议如果改变主意报名期内可以重新报名。`
    }

    if (isCancel) {
      const myRes = await memberAppointmentApi.listMyAppointments()
      const list = (myRes?.data || myRes || [])
      const pending = list.filter((a) => {
        const s = String(a.status || '').toUpperCase()
        return s === 'CONFIRMED' || s.includes('待') || s.includes('PENDING') || s === '0'
      })
      if (!pending.length) {
        return '【系统执行-取消预约】失败：您当前没有待体检的预约记录，无法取消。'
      }
      const target = pending[0]
      const apptId = target.id
      await memberAppointmentApi.cancelAppointment(apptId)
      const pkgName = target.package?.name || target.packageName || '体检套餐'
      const refund = target.package?.price ?? target.price ?? 0
      const cur = userStore.user?.points || 0
      userStore.setUser({ ...userStore.user, points: cur + refund })
      return `【系统执行-取消预约】成功！预约ID:${apptId} 套餐:${pkgName} 已返还${refund}积分。请告知用户取消成功和积分已退回。`
    }

    if (isBook) {
      let matchedPkg = null
      const pkgs = cachedPackages.length ? cachedPackages : (await memberAppointmentApi.listPackages()).data || []
      for (const p of pkgs) {
        const n = p.name || p.title || ''
        if (n && text.includes(n)) { matchedPkg = p; break }
      }
      if (!matchedPkg) {
        const fuzzyMap = [['基础', '基础'], ['进阶', '进阶'], ['高血压', '高血压专项'], ['糖尿病', '糖尿病专项']]
        for (const [kw, label] of fuzzyMap) {
          if (text.includes(kw)) {
            matchedPkg = pkgs.find((p) => (p.name || '').includes(label))
            if (matchedPkg) break
          }
        }
      }
      if (!matchedPkg && pkgs.length === 1) matchedPkg = pkgs[0]
      if (!matchedPkg) {
        return '【系统执行-预约体检】未执行：未能识别您想预约的套餐名称，请说完整的套餐名（如"基础体检套餐"、"高血压专项检查"），或先浏览可预约套餐。'
      }

      let slotId = null
      const slotRes = await memberAppointmentApi.listSlots(matchedPkg.id)
      const dates = slotRes?.data || slotRes || []
      const allSlots = []
      for (const d of dates) {
        for (const s of d.slots || []) {
          if (s.available !== false && s.status !== 'FULL') allSlots.push(s)
        }
      }
      if (!allSlots.length) {
        return `【系统执行-预约体检】失败：套餐「${matchedPkg.name}」当前没有可预约的时段。`
      }

      const timeMatch = text.match(/(\d{1,4})[年\/\-](\d{1,2})[月\/\-](\d{1,2})[^0-9]*(\d{1,2})[:：]?(\d{0,2})/)
      let chosenSlot = null
      if (timeMatch) {
        const targetHour = parseInt(timeMatch[4], 10)
        chosenSlot = allSlots.find((s) => {
          const tr = s.timeRange || s.time_range || ''
          const m = tr.match(/(\d{1,2})[:：]/)
          return m && parseInt(m[1], 10) === targetHour
        })
      }
      if (!chosenSlot) chosenSlot = allSlots[0]
      slotId = chosenSlot.id

      await memberAppointmentApi.bookAppointment(slotId, matchedPkg.id)
      const price = matchedPkg.price || 0
      const cur = userStore.user?.points || 0
      userStore.setUser({ ...userStore.user, points: cur - price })
      return `【系统执行-预约体检】成功！套餐:${matchedPkg.name} 消耗积分:${price}分 时段:${chosenSlot.timeRange || ''} 预约ID已生成。请用中文友好地告知用户预约详情、提醒体检注意事项（空腹、按时到达、携带身份证等）。`
    }

    if (isRegister) {
      let matchedAct = null
      const acts = cachedActivities.length ? cachedActivities : (await memberActivityApi.listActivities()).data || []
      for (const a of acts) {
        const title = a.title || ''
        if (title && text.includes(title)) { matchedAct = a; break }
      }
      if (!matchedAct) {
        return '【系统执行-活动报名】未执行：未能识别您想报名的活动名称，请说完整的活动标题。'
      }
      const regCheck = await memberActivityApi.checkRegistered(matchedAct.id).catch(() => null)
      const alreadyRegistered = regCheck?.data?.registered === true
      if (alreadyRegistered) {
        return `【系统执行-活动报名】失败：活动「${matchedAct.title}」您已经报名过了，不能重复报名。`
      }
      if (!dynIsRegOpen(matchedAct)) {
        const reason = dynIsCancelled(matchedAct) ? '活动已取消'
          : dynIsEnded(matchedAct) ? '活动已结束，报名截止'
          : dynIsOngoing(matchedAct) ? '活动正在进行中，报名已截止'
          : '报名尚未开始或已截止'
        return `【系统执行-活动报名】失败：活动「${matchedAct.title}」当前${reason}，无法报名。`
      }
      await memberActivityApi.registerActivity(matchedAct.id)
      cachedContextAt = 0
      return `【系统执行-活动报名】成功！活动:${matchedAct.title} 时间:${matchedAct.activityStart || ''} 请用中文友好告知用户报名成功信息。`
    }

    return null
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || String(e)
    if (isCancelActivity) return `【系统执行-取消活动报名】失败：${msg}。`
    if (isBook) return `【系统执行-预约体检】失败：${msg}。请告知用户该情况。`
    if (isCancel) return `【系统执行-取消预约】失败：${msg}。`
    if (isRegister) return `【系统执行-活动报名】失败：${msg}。`
    return null
  }
}

async function doSend(text) {
  if (!currentSessionId.value) {
    await createNewSession()
  }
  ensureSessionSaved(currentSessionId.value, text)
  messages.value.push({ role: 'user', content: text, time: now() })
  const aiMsg = reactive({ role: 'ai', content: '', time: now(), streaming: true })
  messages.value.push(aiMsg)
  scrollToBottom()
  sending.value = true

  let finalMessage = text
  try {
    const ctx = await fetchDataContext()
    const actionResult = await detectAndExecuteAction(text)
    const parts = []

    const u = userStore.user || {}
    const fullName = u.real_name || u.nickname || ''
    const gender = u.gender || ''
    const surname = fullName ? fullName.charAt(0) : ''
    let greetingHint = ''
    if (surname && gender === '男') {
      greetingHint = `请严格用「${surname}叔叔」来称呼用户，这是用户的姓氏+性别尊称，绝对不要说错姓氏。`
    } else if (surname && gender === '女') {
      greetingHint = `请严格用「${surname}阿姨」来称呼用户，这是用户的姓氏+性别尊称，绝对不要说错姓氏。`
    } else if (surname) {
      greetingHint = `请用「${surname}先生/女士」或「亲爱的老朋友」来称呼用户。`
    } else {
      greetingHint = `请用「亲爱的老朋友」来称呼用户。`
    }
    parts.push(`[用户身份信息]\n用户姓名：${fullName || '未填写'}\n用户性别：${gender || '未填写'}\n用户姓氏：${surname || '未填写'}\n${greetingHint}`)

    if (ctx) parts.push(`[以下是本系统数据库中的最新信息，请据此准确回答用户的问题，不要编造不存在的套餐或活动]\n${ctx}`)
    if (actionResult) parts.push(`[以下是系统刚刚执行的真实操作结果，请基于这个真实结果来回复用户，不要说"我帮您预约好了"这种空话，要讲真实结果]\n${actionResult}`)
    if (parts.length) {
      finalMessage = `${parts.join('\n\n')}\n\n用户问题：${text}`
    }
  } catch (e) {
    console.warn('context build failed:', e)
  }
  console.log('[AI Context] 最终发送给 AI 的消息（前500字）:', finalMessage.slice(0, 500))

  try {
    await chatStream(currentSessionId.value, finalMessage, (chunk) => {
      aiMsg.content += chunk
      scrollToBottom()
    })
    if (!aiMsg.content) aiMsg.content = '（暂无回复）'
    try { localStorage.removeItem(getDraftKey()) } catch {}
  } catch (e) {
    console.error('chat error:', e)
    aiMsg.content = (e && (e.message || e.msg)) || '抱歉，服务暂时不可用，请稍后再试。'
  } finally {
    aiMsg.streaming = false
    sending.value = false
    scrollToBottom()
    refreshSessions()
  }
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

async function send() {
  let text = input.value
  if (!text && nativeTextareaRef.value) {
    text = nativeTextareaRef.value.value
  }
  text = (text || '').trim()
  if (!text) {
    ElMessage.warning('请输入咨询内容')
    return
  }
  input.value = ''
  if (nativeTextareaRef.value) {
    nativeTextareaRef.value.value = ''
  }
  await doSend(text)
}

function quickSend(text) {
  input.value = text
  nextTick(() => send())
}
</script>

<style scoped>
.chat-root {
  display: flex;
  height: 100%;
  width: 100%;
  overflow: hidden;
  background: transparent;
}

.chat-sidebar {
  width: 280px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(40px) saturate(1.6);
  -webkit-backdrop-filter: blur(40px) saturate(1.6);
  border-right: 1px solid var(--border-light);
  transition: width 0.25s ease, transform 0.25s ease;
  z-index: 50;
}
.chat-root.sidebar-collapsed .chat-sidebar {
  width: 0;
  overflow: hidden;
}
.chat-sidebar.sidebar-hidden {
  width: 0;
  overflow: hidden;
}


.sidebar-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 40;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 16px 12px;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  color: var(--text-main);
  font-size: 15px;
}
.brand-icon {
  font-size: 22px;
}
.sidebar-close-btn {
  background: none;
  border: none;
  color: var(--text-sub);
  cursor: pointer;
  padding: 4px;
  border-radius: 6px;
}
.sidebar-close-btn:hover {
  color: var(--primary);
  background: rgba(74, 144, 226, 0.08);
}

.sidebar-actions {
  padding: 14px 12px 8px;
  flex-shrink: 0;
  display: flex;
  gap: 8px;
}
.new-chat-btn {
  flex: 1;
  height: 40px;
  border-radius: 12px !important;
  font-weight: 600;
  letter-spacing: 0.3px;
}
.manage-btn {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border: 1px solid var(--border-light);
  border-radius: 12px;
  background: #fff;
  color: var(--text-light);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}
.manage-btn:hover {
  border-color: #4a90e2;
  color: #4a90e2;
}
.manage-btn.active {
  background: rgba(74, 144, 226, 0.1);
  border-color: #4a90e2;
  color: #4a90e2;
}

.sidebar-search {
  padding: 8px 12px 10px;
  flex-shrink: 0;
}
.sidebar-search :deep(.el-input__wrapper) {
  background: rgba(74, 144, 226, 0.04);
  border-radius: 10px;
  box-shadow: none;
  border: 1px solid transparent;
}
.sidebar-search :deep(.el-input__wrapper:hover),
.sidebar-search :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  border-color: rgba(74, 144, 226, 0.3);
  box-shadow: 0 2px 8px rgba(74, 144, 226, 0.1);
}
.sidebar-search :deep(.el-input__inner) {
  color: var(--text-main);
}
.sidebar-search :deep(.el-input__inner::placeholder) {
  color: var(--text-light);
}
.sidebar-search :deep(.el-input__prefix .el-icon) {
  color: var(--text-light);
}

.sidebar-list-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px 8px;
}
.sidebar-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.sidebar-list-wrapper::-webkit-scrollbar {
  width: 5px;
}
.sidebar-list-wrapper::-webkit-scrollbar-thumb {
  background: rgba(74, 144, 226, 0.15);
  border-radius: 3px;
}

.sidebar-empty {
  text-align: center;
  padding: 48px 20px;
  color: var(--text-light);
}
.sidebar-empty p {
  margin-top: 12px;
  font-size: 13px;
}

.session-group {
  margin-top: 10px;
}
.session-group:first-child {
  margin-top: 0;
}
.session-group-label {
  font-size: 11px;
  font-weight: 600;
  padding: 6px 10px 4px;
  color: var(--text-light);
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}
.session-item.selected {
  background: rgba(74, 144, 226, 0.1);
}
.session-check {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  border: 1.5px solid #c8d3e0;
  border-radius: 50%;
  background: #fff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s ease;
}
.session-check.checked {
  background: #4a90e2;
  border-color: #4a90e2;
}
.session-item:hover {
  background: rgba(74, 144, 226, 0.07);
}
.session-item.selected:hover {
  background: rgba(74, 144, 226, 0.12);
}
.session-item.active {
  background: linear-gradient(135deg, rgba(74, 144, 226, 0.12) 0%, rgba(155, 136, 245, 0.12) 100%);
  border: 1px solid rgba(74, 144, 226, 0.2);
}

.session-item-icon {
  font-size: 16px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: rgba(74, 144, 226, 0.08);
  flex-shrink: 0;
}
.session-item.active .session-item-icon {
  background: linear-gradient(135deg, #4A90E2 0%, #9B88F5 100%);
  filter: saturate(1.2);
}

.session-item-body {
  flex: 1;
  min-width: 0;
}
.session-title {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-main);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.3;
}
.session-preview {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-item-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
  flex-shrink: 0;
}
.session-item:hover .session-item-actions,
.session-item.active .session-item-actions {
  opacity: 1;
}
.action-icon {
  font-size: 15px;
  padding: 4px;
  border-radius: 6px;
  color: var(--text-light);
  transition: all 0.15s;
}
.action-icon:hover {
  color: var(--primary);
  background: rgba(74, 144, 226, 0.1);
}
.action-icon.danger:hover {
  color: #f56c6c;
  background: rgba(245, 108, 108, 0.1);
}

.sidebar-footer {
  flex-shrink: 0;
  padding: 10px 12px;
  border-top: 1px solid var(--border-light);
  text-align: center;
}
.clear-all-btn {
  font-size: 12px;
  color: var(--text-light) !important;
}
.clear-all-btn:hover {
  color: #f56c6c !important;
}
.batch-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.batch-count {
  font-size: 12px;
  color: var(--text-light);
}
.batch-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.batch-cancel-btn {
  font-size: 12px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  min-width: 0;
  min-height: 0;
  position: relative;
}

.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(40px) saturate(1.5);
  -webkit-backdrop-filter: blur(40px) saturate(1.5);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 48px rgba(74, 144, 226, 0.08);
  overflow: hidden;
  min-height: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border-light);
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.5);
}
.chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.sidebar-toggle-btn {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(74, 144, 226, 0.06);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  color: var(--text-main);
  transition: all 0.2s;
}
.sidebar-toggle-btn:hover {
  background: rgba(74, 144, 226, 0.15);
  color: var(--primary);
}

.chat-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4A90E2 0%, #50C878 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 4px 12px rgba(74, 144, 226, 0.25);
}
.chat-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
}
.chat-status {
  font-size: 12px;
  color: var(--text-sub);
  margin-top: 2px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.chat-header-actions {
  display: flex;
  gap: 8px;
}
.icon-action-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(74, 144, 226, 0.06);
  border: none;
  border-radius: 10px;
  cursor: pointer;
  color: var(--text-sub);
  transition: all 0.2s;
}
.icon-action-btn:hover {
  background: var(--primary-gradient);
  color: #fff;
  transform: translateY(-1px);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 24px 16px;
}
.chat-messages::-webkit-scrollbar {
  width: 6px;
}
.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(74, 144, 226, 0.15);
  border-radius: 3px;
}

.msg-row {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  align-items: flex-start;
}
.msg-row.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  background: rgba(74, 144, 226, 0.08);
}
.msg-row.user .msg-avatar {
  background: linear-gradient(135deg, #50C878 0%, #4A90E2 100%);
  padding: 0;
  overflow: hidden;
}
.msg-avatar-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  display: block;
}

.msg-bubble {
  max-width: 72%;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.msg-text {
  padding: 12px 16px;
  font-size: 14.5px;
  line-height: 1.6;
  border-radius: 16px;
  word-break: break-word;
  white-space: pre-wrap;
}

.msg-row.ai .msg-text {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-top-left-radius: 6px;
  box-shadow: 0 4px 16px rgba(74, 144, 226, 0.08);
  border: 1px solid var(--border-light);
  color: var(--text-main);
}
.msg-row.user .msg-text {
  background: var(--primary-gradient);
  color: #ffffff;
  border-top-right-radius: 6px;
  box-shadow: 0 6px 20px rgba(74, 144, 226, 0.3);
}

/* 关键：抵消 .msg-text 的 pre-wrap，否则 marked 输出的换行会变成大片空白 */
.markdown-body {
  white-space: normal;
  line-height: 1.65;
}
.markdown-body :deep(p) { margin: 0 0 6px; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 12px 0 6px;
  font-weight: 600;
  color: var(--text-main);
}
.markdown-body :deep(h1) { font-size: 18px; }
.markdown-body :deep(h2) { font-size: 16px; }
.markdown-body :deep(h3) { font-size: 15px; }
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 4px 0;
}
.markdown-body :deep(li) { margin: 0 0 3px; line-height: 1.6; }
.markdown-body :deep(li p) { margin: 0; }
.markdown-body :deep(li:last-child) { margin-bottom: 0; }
.markdown-body :deep(strong) { color: var(--primary); font-weight: 600; }
.markdown-body :deep(em) { color: var(--primary-3); }
.markdown-body :deep(code) {
  background: rgba(74, 144, 226, 0.08);
  color: var(--primary);
  padding: 2px 7px;
  border-radius: 5px;
  font-size: 0.9em;
  font-family: 'SF Mono', Menlo, Monaco, Consolas, monospace;
}
.markdown-body :deep(pre) {
  background: rgba(74, 144, 226, 0.05);
  padding: 12px 14px;
  border-radius: 10px;
  overflow-x: auto;
  border: 1px solid rgba(74, 144, 226, 0.1);
}
.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: var(--text-main);
}
.markdown-body :deep(blockquote) {
  border-left: 3px solid var(--primary);
  padding: 6px 14px;
  margin: 6px 0;
  background: rgba(74, 144, 226, 0.04);
  border-radius: 0 8px 8px 0;
  color: var(--text-sub);
}
.markdown-body :deep(a) {
  color: var(--primary);
  text-decoration: none;
  border-bottom: 1px solid rgba(74, 144, 226, 0.3);
}
.markdown-body :deep(a:hover) {
  border-bottom-color: var(--primary);
}
.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
  font-size: 13.5px;
}
.markdown-body :deep(td),
.markdown-body :deep(th) {
  border: 1px solid var(--border-light);
  padding: 8px 12px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: rgba(74, 144, 226, 0.08);
  font-weight: 600;
}
.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 10px;
  margin: 8px 0;
}

.msg-time {
  font-size: 11px;
  color: var(--text-light);
  margin-top: 6px;
  padding: 0 4px;
  letter-spacing: 0.3px;
}
.msg-row.user .msg-time {
  text-align: right;
}

.welcome {
  text-align: center;
  padding: 60px 24px 32px;
  color: var(--text-sub);
}
.welcome p {
  margin: 18px auto 24px;
  max-width: 640px;
  font-size: 15px;
  line-height: 1.9;
  letter-spacing: 0.3px;
}
.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}
.suggestions .el-button {
  border-radius: 999px;
  padding: 0 20px;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  border-color: rgba(74, 144, 226, 0.25);
  color: var(--text-main);
  background: rgba(74, 144, 226, 0.05);
  transition: all 0.25s ease;
}
.suggestions .el-button:hover {
  background: var(--primary-gradient);
  border-color: transparent;
  color: #fff;
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(74, 144, 226, 0.3);
}

.chat-input {
  display: flex;
  gap: 14px;
  align-items: flex-end;
  padding: 18px 24px 20px;
  border-top: 1px solid var(--border-light);
  background: rgba(255, 255, 255, 0.5);
  flex-shrink: 0;
}
.chat-input .chat-textarea {
  flex: 1;
  border-radius: 16px;
  font-size: 15px;
  line-height: 1.7;
  padding: 14px 18px;
  min-height: 56px;
  background: rgba(74, 144, 226, 0.04);
  border: 1px solid rgba(74, 144, 226, 0.15);
  color: var(--text-main);
  resize: none;
  outline: none;
  font-family: inherit;
  transition: all 0.25s ease;
  box-sizing: border-box;
}
.chat-input .chat-textarea:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.12), 0 4px 14px rgba(74, 144, 226, 0.1);
  background: #fff;
}
.chat-input .chat-textarea::placeholder {
  color: var(--text-light);
}
.send-btn {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: 16px !important;
  background: var(--primary-gradient) !important;
  border: none !important;
  box-shadow: 0 8px 22px rgba(74, 144, 226, 0.35) !important;
  transition: all 0.25s ease;
}
.send-btn:hover {
  box-shadow: 0 12px 30px rgba(74, 144, 226, 0.45) !important;
  transform: translateY(-2px);
}
.send-btn:active {
  transform: translateY(0);
}

.cursor {
  display: inline-block;
  margin-left: 2px;
  animation: blink 0.8s step-end infinite;
  color: var(--primary);
  font-weight: 700;
}
@keyframes blink {
  50% { opacity: 0; }
}

.ctx-menu {
  position: fixed;
  z-index: 9999;
  list-style: none;
  margin: 0;
  padding: 6px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-light);
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(74, 144, 226, 0.15);
  min-width: 140px;
}
.ctx-menu li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  font-size: 13.5px;
  color: var(--text-main);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.ctx-menu li:hover {
  background: rgba(74, 144, 226, 0.08);
  color: var(--primary);
}
.ctx-menu li.danger {
  color: #f56c6c;
}
.ctx-menu li.danger:hover {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

@media (max-width: 768px) {
  .chat-root {
    height: 100%;
  }
  .chat-sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 82%;
    max-width: 300px;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
    box-shadow: 4px 0 20px rgba(74, 144, 226, 0.15);
  }
  .chat-sidebar {
    transform: translateX(0);
  }
  .chat-root:not(.sidebar-collapsed) .chat-sidebar {
    transform: translateX(-100%);
  }
  .chat-root .chat-sidebar {
    transform: translateX(0);
  }

  .chat-main {
    padding: 0;
    height: 100%;
  }
  .chat-window {
    border-radius: 0;
    border: none;
    box-shadow: none;
  }
  .chat-header {
    padding: 12px 16px;
    gap: 10px;
  }
  .chat-avatar {
    width: 36px;
    height: 36px;
    font-size: 18px;
    border-radius: 12px;
  }
  .chat-name {
    font-size: 15px;
  }
  .chat-status {
    font-size: 11px;
  }
  .chat-messages {
    padding: 18px 16px 14px;
  }
  .msg-row {
    gap: 8px;
    margin-bottom: 15px;
  }
  .msg-avatar {
    width: 32px;
    height: 32px;
    font-size: 18px;
    border-radius: 12px;
  }
  .msg-bubble {
    max-width: 86%;
  }
  .msg-text {
    padding: 12px 15px;
    font-size: 14.5px;
    line-height: 1.6;
  }
  .welcome {
    padding: 32px 16px 22px;
  }
  .welcome p {
    font-size: 14.5px;
    max-width: 100%;
  }
  .suggestions {
    gap: 8px;
  }
  .suggestions .el-button {
    height: 36px;
    font-size: 13px;
    padding: 0 15px;
  }
  .chat-input {
    padding: 12px 14px calc(12px + env(safe-area-inset-bottom));
    gap: 10px;
  }
  .chat-input .chat-textarea {
    font-size: 14px;
    padding: 10px 14px;
    min-height: 44px;
  }
  .send-btn {
    width: 44px;
    height: 44px;
    border-radius: 14px !important;
  }
  .sidebar-toggle-btn {
    width: 36px;
    height: 36px;
    padding: 0;
  }
}

/* 会员端主题：与首页青绿、米白配色保持一致 */
.chat-sidebar {
  background: rgba(250, 252, 250, 0.9);
  border-right-color: rgba(47, 125, 109, 0.11);
}

.session-item.selected,
.session-item.active {
  background: linear-gradient(135deg, rgba(47, 125, 109, 0.11), rgba(217, 150, 62, 0.055));
  border-color: rgba(47, 125, 109, 0.2);
}

.session-item:hover,
.session-item.selected:hover {
  background: rgba(47, 125, 109, 0.075);
}

.session-check.checked {
  background: #2f7d6d;
  border-color: #2f7d6d;
}

.session-item-icon {
  background: rgba(47, 125, 109, 0.08);
}

.session-item.active .session-item-icon,
.chat-avatar {
  background: linear-gradient(145deg, #e0b263 0%, #c3842d 100%);
  box-shadow: 0 4px 12px rgba(181, 117, 35, 0.2);
}

.action-icon:hover,
.sidebar-toggle-btn:hover {
  background: rgba(47, 125, 109, 0.1);
  color: #2f7d6d;
}

.sidebar-toggle-btn,
.icon-action-btn {
  background: rgba(47, 125, 109, 0.065);
}

.chat-window {
  background: rgba(255, 255, 255, 0.84);
  border-color: rgba(222, 233, 228, 0.9);
  box-shadow: 0 12px 42px rgba(38, 76, 65, 0.075);
}

.chat-header {
  background: rgba(250, 252, 250, 0.66);
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(47, 125, 109, 0.16);
}

.msg-avatar {
  background: rgba(47, 125, 109, 0.08);
}

.msg-row.user .msg-avatar {
  background: linear-gradient(145deg, #4a907f 0%, #2f7d6d 100%);
}

.msg-row.ai .msg-text {
  box-shadow: 0 4px 16px rgba(38, 76, 65, 0.065);
}

.msg-row.user .msg-text {
  background: linear-gradient(135deg, #347f70 0%, #1f6559 100%);
  box-shadow: 0 6px 20px rgba(31, 101, 89, 0.22);
}

.markdown-body :deep(code) {
  background: rgba(47, 125, 109, 0.08);
}

.markdown-body :deep(pre) {
  background: rgba(47, 125, 109, 0.05);
  border-color: rgba(47, 125, 109, 0.1);
}

.markdown-body :deep(blockquote) {
  background: rgba(47, 125, 109, 0.045);
}

.markdown-body :deep(a) {
  border-bottom-color: rgba(47, 125, 109, 0.3);
}

.markdown-body :deep(th) {
  background: rgba(47, 125, 109, 0.08);
}

.suggestions .el-button {
  border-color: rgba(47, 125, 109, 0.22);
  background: rgba(47, 125, 109, 0.055);
}

.suggestions .el-button:hover {
  box-shadow: 0 6px 18px rgba(31, 101, 89, 0.22);
}

.chat-input {
  background: rgba(250, 252, 250, 0.64);
}

.chat-input .chat-textarea {
  background: rgba(47, 125, 109, 0.04);
  border-color: rgba(47, 125, 109, 0.15);
}

.chat-input .chat-textarea:focus {
  box-shadow: 0 0 0 3px rgba(47, 125, 109, 0.1), 0 4px 14px rgba(31, 101, 89, 0.08);
}

.send-btn {
  box-shadow: 0 8px 22px rgba(31, 101, 89, 0.25) !important;
}

.send-btn:hover {
  box-shadow: 0 12px 30px rgba(31, 101, 89, 0.33) !important;
}

.ctx-menu {
  box-shadow: 0 10px 30px rgba(38, 76, 65, 0.12);
}

.ctx-menu li:hover {
  background: rgba(47, 125, 109, 0.08);
}
</style>
