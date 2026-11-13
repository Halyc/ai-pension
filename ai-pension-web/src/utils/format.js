/**
 * 格式化工具：日期时间、状态字典（取值均来自后端代码常量）
 */

// ---------- 日期时间 ----------
function parseDateTimeParts(input) {
  if (input === null || input === undefined || input === '') return null
  if (typeof input === 'string') {
    let normalized = input.replace(' ', 'T')
    const hasTimezone = normalized.endsWith('Z') || /[+-]\d{2}:?\d{2}$/.test(normalized)
    const hasTime = /T\d{1,2}:\d{1,2}/.test(normalized)
    let y, mo, d, h, mi, s
    if (hasTimezone) {
      const date = new Date(normalized)
      if (isNaN(date.getTime())) return null
      y = date.getFullYear()
      mo = date.getMonth() + 1
      d = date.getDate()
      h = date.getHours()
      mi = date.getMinutes()
      s = date.getSeconds()
    } else {
      const m = normalized.match(/^(\d{4})-(\d{1,2})-(\d{1,2})(?:T(\d{1,2}):(\d{1,2})(?::(\d{1,2}))?)?/)
      if (!m) return null
      const y0 = Number(m[1])
      const mo0 = Number(m[2])
      const d0 = Number(m[3])
      const h0 = m[4] != null ? Number(m[4]) : 0
      const mi0 = m[5] != null ? Number(m[5]) : 0
      const s0 = m[6] != null ? Number(m[6]) : 0
      const hasT = m[4] != null
      if (hasT) {
        const utcStr = `${y0}-${pad(mo0)}-${pad(d0)}T${pad(h0)}:${pad(mi0)}:${pad(s0)}Z`
        const date = new Date(utcStr)
        if (isNaN(date.getTime())) return null
        y = date.getFullYear()
        mo = date.getMonth() + 1
        d = date.getDate()
        h = date.getHours()
        mi = date.getMinutes()
        s = date.getSeconds()
      } else {
        y = y0
        mo = mo0
        d = d0
        h = 0
        mi = 0
        s = 0
      }
    }
    return { y, mo, d, h, mi, s, hasTime }
  }
  if (Array.isArray(input)) {
    return {
      y: input[0],
      mo: (input[1] || 1),
      d: input[2] || 1,
      h: input[3] || 0,
      mi: input[4] || 0,
      s: input[5] || 0,
      hasTime: input.length >= 4
    }
  }
  if (input instanceof Date) {
    if (isNaN(input.getTime())) return null
    return {
      y: input.getFullYear(),
      mo: input.getMonth() + 1,
      d: input.getDate(),
      h: input.getHours(),
      mi: input.getMinutes(),
      s: input.getSeconds(),
      hasTime: true
    }
  }
  return null
}

function pad(n) {
  return String(n).padStart(2, '0')
}

export function formatDateTime(input) {
  const p = parseDateTimeParts(input)
  if (!p) return '-'
  const date = `${p.y}-${pad(p.mo)}-${pad(p.d)}`
  const time = `${pad(p.h)}:${pad(p.mi)}`
  return p.hasTime ? `${date} ${time}` : date
}

export function formatDate(input) {
  const p = parseDateTimeParts(input)
  if (!p) return '-'
  return `${p.y}-${pad(p.mo)}-${pad(p.d)}`
}

export function formatTime(input) {
  const p = parseDateTimeParts(input)
  if (!p || !p.hasTime) return '-'
  return `${pad(p.h)}:${pad(p.mi)}`
}

// 把 ISO 字符串转 el-datetime-picker 需要的值（datetime-local 能识别 2026-08-31T10:00）
export function toPickerValue(input) {
  const p = parseDateTimeParts(input)
  if (!p) return null
  return `${p.y}-${pad(p.mo)}-${pad(p.d)} ${pad(p.h)}:${pad(p.mi)}`
}

// ---------- 状态字典：{ label, type(ElTag 类型) } ----------
export const userStatusMap = {
  ENABLED: { label: '正常', type: 'success' },
  DISABLED: { label: '已禁用', type: 'danger' }
}

export const levelMap = {
  NORMAL: { label: '普通会员', type: 'info' },
  SILVER: { label: '银卡会员', type: 'info' },
  GOLD: { label: '金卡会员', type: 'warning' },
  PLATINUM: { label: '铂金会员', type: 'primary' },
  DIAMOND: { label: '钻石会员', type: 'danger' }
}

export const packageStatusMap = {
  ENABLED: { label: '已上架', type: 'success' },
  DISABLED: { label: '已下架', type: 'info' }
}

export const slotStatusMap = {
  AVAILABLE: { label: '可预约', type: 'success' },
  FULL: { label: '已满', type: 'warning' },
  EXPIRED: { label: '已过期', type: 'info' }
}

export const appointmentStatusMap = {
  CONFIRMED: { label: '待体检', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' }
}

export const activityStatusMap = {
  REGISTRATION_OPEN: { label: '报名中', type: 'primary' },
  RECRUITING: { label: '报名中', type: 'primary' },
  REGISTRATION_CLOSED: { label: '报名已截止', type: 'warning' },
  ONGOING: { label: '进行中', type: 'success' },
  ACTIVE: { label: '进行中', type: 'success' },
  PAUSED: { label: '已暂停', type: 'info' },
  ENDED: { label: '已结束', type: 'info' },
  FINISHED: { label: '已结束', type: 'info' },
  CANCELLED: { label: '已取消', type: 'danger' }
}

export const checkInStatusMap = {
  NOT_CHECKED_IN: { label: '未签到', type: 'warning' },
  CHECKED_IN: { label: '已签到', type: 'success' }
}

export const questionnaireStatusMap = {
  DRAFT: { label: '草稿', type: 'info' },
  PUBLISHED: { label: '已发布', type: 'success' }
}

export const questionTypeMap = {
  SINGLE: { label: '单选题', type: 'primary' },
  MULTIPLE: { label: '多选题', type: 'warning' },
  TEXT: { label: '简答题', type: 'success' }
}

/** 通用字典渲染 */
export function dictLabel(map, key) {
  return map[key]?.label || key || '-'
}
export function dictType(map, key) {
  return map[key]?.type || 'info'
}

// ---------- 业务解析 ----------
/** 套餐服务项目 items：后端存 JSON 字符串，也可能是普通文本 */
export function parseItems(items) {
  if (!items) return []
  if (Array.isArray(items)) return items
  try {
    const v = JSON.parse(items)
    return Array.isArray(v) ? v : [String(v)]
  } catch (e) {
    return String(items)
      .split(/[\n,，;；]/)
      .map((s) => s.trim())
      .filter(Boolean)
  }
}

/** 题目选项 options：后端为字符串（JSON 数组或换行分隔） */
export function parseOptions(options) {
  if (!options) return []
  if (Array.isArray(options)) return options
  try {
    const v = JSON.parse(options)
    return Array.isArray(v) ? v : [String(v)]
  } catch (e) {
    return String(options)
      .split(/\n/)
      .map((s) => s.trim())
      .filter(Boolean)
  }
}

/** 评测 answers：后端存 JSON 字符串 */
export function parseJsonSafe(v, fallback = null) {
  if (v === null || v === undefined || v === '') return fallback
  if (typeof v === 'object') return v
  try {
    return JSON.parse(v)
  } catch (e) {
    return fallback
  }
}

// ---------- 问卷评分 ----------
/**
 * 统一评分规则：A 永远最优，后续依次递减。
 * 单题满分 = 100 / 问卷总题数（所有题满分一致）
 * 设该题共 N 个选项，用户选中第 k 个（A=1, B=2, …）
 * 单题得分 = 单题满分 × (N - k) / (N - 1)
 * 总分 = 所有题目得分累加，自动落在 0~100。
 *
 * 支持 SINGLE（单选）和 MULTIPLE（多选），TEXT 不计分。
 * 多选时取所有选中选项中最靠后的那个 k（严格模式：只要有一个坏习惯就扣分）。
 */
export function calcQuestionnaireScore(questions, answers) {
  const qs = Array.isArray(questions) ? questions : []
  if (!qs.length) return { totalScore: 0, maxScore: 0, perQuestion: [], detail: [] }

  const scoreable = qs.filter((q) => q.type !== 'TEXT')
  const totalQuestions = scoreable.length
  if (!totalQuestions) return { totalScore: 0, maxScore: 0, perQuestion: [], detail: [] }

  const perQuestionFull = 100 / totalQuestions
  const answersMap = parseJsonSafe(answers, {})

  let total = 0
  const detail = []

  for (const q of scoreable) {
    const opts = parseOptions(q.options)
    const N = opts.length
    let k = null

    if (N >= 2) {
      const raw = answersMap[q.id]
      if (q.type === 'MULTIPLE' && Array.isArray(raw)) {
        let maxIdx = -1
        for (const picked of raw) {
          const idx = opts.indexOf(picked)
          if (idx > maxIdx) maxIdx = idx
        }
        if (maxIdx >= 0) k = maxIdx + 1
      } else if (typeof raw === 'string' || typeof raw === 'number') {
        const idx = opts.indexOf(raw)
        if (idx >= 0) k = idx + 1
      }
    }

    const ratio = k === null ? 0 : (N - k) / (N - 1)
    const score = perQuestionFull * ratio
    total += score

    detail.push({
      questionId: q.id,
      questionContent: q.content,
      optionCount: N,
      pickedRank: k,
      pickedOption: k ? opts[k - 1] : null,
      fullMark: perQuestionFull,
      ratio,
      score
    })
  }

  const totalScore = Math.round(total * 100) / 100
  return { totalScore, maxScore: 100, perQuestionFull, perQuestion: detail }
}

/**
 * 分数等级（对应问卷评分结果，等级区间可按需调整）
 * 80~100  生活方式优秀
 * 60~79   生活方式尚可，少量需要调整
 * <60     生活方式有待改善，需要健康干预
 */
export function getQuestionnaireLevel(score) {
  const s = Number(score) || 0
  if (s >= 80) {
    return {
      level: '生活方式优秀',
      hint: '继续保持！您的健康习惯值得肯定。',
      color: '#4ade80',
      bg: 'rgba(74,222,128,0.12)',
      border: 'rgba(74,222,128,0.35)'
    }
  }
  if (s >= 60) {
    return {
      level: '生活方式尚可',
      hint: '整体不错，但仍有少量习惯可以调整优化。',
      color: '#fbbf24',
      bg: 'rgba(251,191,36,0.12)',
      border: 'rgba(251,191,36,0.35)'
    }
  }
  return {
    level: '生活方式有待改善',
    hint: '建议进行健康干预，逐步调整生活方式。',
    color: '#f87171',
    bg: 'rgba(248,113,113,0.12)',
    border: 'rgba(248,113,113,0.35)'
  }
}

/** 性别 */
export const genderMap = {
  MALE: '男',
  FEMALE: '女',
  male: '男',
  female: '女',
  m: '男',
  f: '女',
  M: '男',
  F: '女',
  男: '男',
  女: '女',
  1: '男',
  2: '女'
}
export function genderText(g) {
  return genderMap[g] || g || '-'
}

export function normalizeGender(g) {
  if (g == null) return null
  if (typeof g === 'number') g = String(g)
  return genderMap[g] || null
}

export function buildSalutation(user) {
  const fullName = user?.real_name || ''
  const rawGender = user?.gender
  const cnGender = normalizeGender(rawGender)
  const surname = fullName ? fullName.charAt(0) : ''
  if (!surname) return '您'
  if (cnGender === '男') return surname + '叔叔'
  if (cnGender === '女') return surname + '阿姨'
  return surname + '长辈'
}

const SALUTATION_REGEX = /[\u4e00-\u9fa5](先生\/女士|亲爱的老朋友|亲爱的朋友|老朋友|叔叔阿姨|爷爷奶奶|先生|女士|叔叔|阿姨|大爷|大妈)|先生\/女士|亲爱的老朋友|亲爱的朋友|老朋友|叔叔阿姨|爷爷奶奶|先生|女士|叔叔|阿姨|大爷|大妈/g

const PLACEHOLDER = '\u0000\u0000SALUTATION\u0000\u0000'

export function replaceSalutation(text, user) {
  if (!text) return text
  const sal = buildSalutation(user)
  const withPlaceholder = text.replace(SALUTATION_REGEX, PLACEHOLDER)
  return withPlaceholder.split(PLACEHOLDER).join(sal)
}

export function getScoreInfo(raw) {
  if (raw === null || raw === undefined || raw === '') return null
  let score = Math.round(Number(raw))
  score = Math.max(0, Math.min(100, score))
  const levelInfo = getQuestionnaireLevel(score)
  return { score, level: levelInfo.level, color: levelInfo.color, bg: levelInfo.bg, border: levelInfo.border }
}

import { getImage, actKey, pkgKey, userKey } from './imageCache'

// ---------- 智能封面：本地 SVG 动态占位（永不依赖外网） ----------

const TYPE_PALETTES = {
  checkup: { c1: '#667eea', c2: '#764ba2', icon: '🏥', label: '体检' },
  exercise: { c1: '#11998e', c2: '#38ef7d', icon: '🧘', label: '运动' },
  travel: { c1: '#56ab2f', c2: '#a8e063', icon: '🌿', label: '踏青' },
  party: { c1: '#f7971e', c2: '#ffd200', icon: '🍵', label: '茶话' },
  seminar: { c1: '#373B44', c2: '#4286f4', icon: '📖', label: '讲座' },
  festival: { c1: '#f12711', c2: '#f5af19', icon: '🏮', label: '节日' },
  art: { c1: '#8E2DE2', c2: '#4A00E0', icon: '🎨', label: '艺术' },
  choir: { c1: '#ee9ca7', c2: '#ffdde1', icon: '🎵', label: '合唱' },
  tech: { c1: '#2193b0', c2: '#6dd5ed', icon: '📱', label: '数字课堂' },
  dance: { c1: '#ff6b6b', c2: '#feca57', icon: '💃', label: '舞蹈' },
  cardiac: { c1: '#eb3349', c2: '#f45c43', icon: '❤️', label: '心脑血管' },
  cancer: { c1: '#a18cd1', c2: '#fbc2eb', icon: '🔬', label: '肿瘤筛查' },
  brain: { c1: '#667eea', c2: '#764ba2', icon: '🧠', label: '脑健康' },
  bone: { c1: '#606c88', c2: '#3f4c6b', icon: '🦴', label: '骨关节' },
  eye: { c1: '#11998e', c2: '#38ef7d', icon: '👁️', label: '眼科' },
  dental: { c1: '#56ccf2', c2: '#2f80ed', icon: '🦷', label: '口腔' },
  female: { c1: '#ee9ca7', c2: '#ffdde1', icon: '🌸', label: '女性健康' },
  male: { c1: '#2c3e50', c2: '#4ca1af', icon: '💪', label: '男性健康' },
  tcm: { c1: '#c31432', c2: '#240b36', icon: '🌿', label: '中医' },
  full: { c1: '#134e5e', c2: '#71b280', icon: '🩺', label: '全身体检' },
  basic: { c1: '#614385', c2: '#516395', icon: '📋', label: '基础体检' },
  default: { c1: '#667eea', c2: '#764ba2', icon: '🏥', label: '健康养老' }
}

/** 生成 SVG data URI：渐变背景 + emoji 图标 + 标题 */
function genPlaceholder(type = 'default', title = '') {
  const p = TYPE_PALETTES[type] || TYPE_PALETTES.default
  const display = (title || p.label).slice(0, 12)
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300">
<defs>
<linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
<stop offset="0%" stop-color="${p.c1}"/>
<stop offset="100%" stop-color="${p.c2}"/>
</linearGradient>
</defs>
<rect width="400" height="300" fill="url(#g)"/>
<circle cx="200" cy="140" r="70" fill="rgba(255,255,255,0.18)"/>
<text x="200" y="165" font-size="64" text-anchor="middle" dominant-baseline="middle">${p.icon}</text>
<text x="200" y="250" font-size="22" fill="#fff" text-anchor="middle" font-family="'PingFang SC','Microsoft YaHei',sans-serif" font-weight="600">${display}</text>
</svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}

/** 封面字段校验：兼容 camelCase / snake_case，并过滤被截断的无效值 */
function pickCover(row) {
  if (!row) return ''
  const raw = row.coverUrl || row.cover_url || row.image_url || ''
  if (!raw) return ''
  if (raw.startsWith('data:')) {
    if (raw.length < 200) return ''
    return raw
  }
  if (raw.startsWith('http')) return raw
  return ''
}

/** 活动关键词 → 占位类型 */
function actTypeOf(title) {
  const t = (title || '').toLowerCase()
  if (/体检|checkup|check[_ -]?up/.test(t)) return 'checkup'
  if (/广场|太极|健身|运动|锻炼|tai.?chi|yoga|sports|跑步/.test(t)) return 'exercise'
  if (/踏青|春游|秋游|郊游|出游|trip|travel|hik|picnic/.test(t)) return 'travel'
  if (/茶话|座谈|聚会|tea|party/.test(t)) return 'party'
  if (/讲座|健康知识|科普|seminar|lecture|培训|课堂/.test(t)) return 'seminar'
  if (/重阳|中秋|端午|春节|节日|festival|元宵/.test(t)) return 'festival'
  if (/书法|绘画|艺术|手工|calligraphy|paint|art/.test(t)) return 'art'
  if (/合唱|歌|音乐|唱|choir|music|concert/.test(t)) return 'choir'
  if (/手机|电脑|数码|smartphone|computer|tech/.test(t)) return 'tech'
  if (/舞蹈|dance|芭蕾/.test(t)) return 'dance'
  return 'default'
}

/** 套餐关键词 → 占位类型 */
function pkgTypeOf(name, suitable, desc) {
  const t = `${name || ''} ${suitable || ''} ${desc || ''}`.toLowerCase()
  if (/心脑血管|心血管|心脏|高血压|血糖|糖尿病|cardiac|diabetes|heart/.test(t)) return 'cardiac'
  if (/肿瘤|癌|cancer|tumor|筛查/.test(t)) return 'cancer'
  if (/老年痴呆|阿尔茨海默|memory|brain|neurological|神经|脑/.test(t)) return 'brain'
  if (/骨|关节|骨密度|bone|joint|osteoporosis|arthrit/.test(t)) return 'bone'
  if (/视力|眼睛|vision|eye|眼科/.test(t)) return 'eye'
  if (/口腔|牙|dental|mouth|teeth/.test(t)) return 'dental'
  if (/女性|妇科|woman|female|gynecolog/.test(t)) return 'female'
  if (/男性|男科|man|male|prostate|前列腺/.test(t)) return 'male'
  if (/中医|中药|针灸|推拿|chinese|herbal|acupunct|tcm/.test(t)) return 'tcm'
  if (/全身体检|全面|综合|general|complete|comprehensive/.test(t)) return 'full'
  if (/基础|入门|初级|basic|starter|entry/.test(t)) return 'basic'
  return 'default'
}

/** 活动封面：localStorage 缓存 → 数据库字段 → SVG 占位 */
export function buildActImage(row) {
  if (row?.id) {
    const local = getImage(actKey(row.id))
    if (local) return local
  }
  const custom = pickCover(row)
  if (custom) return custom
  const type = actTypeOf(row?.title)
  return genPlaceholder(type, row?.title)
}

/** 套餐封面：localStorage 缓存 → 数据库字段 → SVG 占位 */
export function buildPkgImage(row) {
  if (row?.id) {
    const local = getImage(pkgKey(row.id))
    if (local) return local
  }
  const custom = pickCover(row)
  if (custom) return custom
  const type = pkgTypeOf(row?.name, row?.suitable_people, row?.description)
  return genPlaceholder(type, row?.name)
}

/** 头像：localStorage 缓存 → 数据库字段 → 渐变占位 */
export function buildAvatar(row) {
  if (row?.id) {
    const local = getImage(userKey(row.id))
    if (local) return local
  }
  if (row?.avatar && row.avatar.startsWith('data:')) return row.avatar
  if (row?.avatar && row.avatar.startsWith('http')) return row.avatar
  return genAvatarPlaceholder(row?.real_name || row?.nickname || row?.phone || 'U')
}

/** 头像占位 SVG（圆形渐变 + 首字） */
function genAvatarPlaceholder(name = '') {
  const letter = (name || 'U').trim().charAt(0).toUpperCase()
  const colors = [
    ['#667eea', '#764ba2'], ['#f093fb', '#f5576c'], ['#4facfe', '#00f2fe'],
    ['#43e97b', '#38f9d7'], ['#fa709a', '#fee140'], ['#30cfd0', '#330867'],
    ['#a8edea', '#fed6e3'], ['#ff9a9e', '#fecfef']
  ]
  let hash = 0
  for (let i = 0; i < letter.length; i++) hash = (hash * 31 + letter.charCodeAt(i)) >>> 0
  const [c1, c2] = colors[hash % colors.length]
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100">
<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
<stop offset="0%" stop-color="${c1}"/><stop offset="100%" stop-color="${c2}"/>
</linearGradient></defs>
<circle cx="50" cy="50" r="50" fill="url(#g)"/>
<text x="50" y="58" font-size="42" fill="#fff" text-anchor="middle" font-family="'PingFang SC','Microsoft YaHei',sans-serif" font-weight="700">${letter}</text>
</svg>`
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`
}