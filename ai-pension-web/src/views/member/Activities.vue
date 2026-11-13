<template>
  <div class="page-activities">
    <div class="page-card">
      <div class="tabs-bar">
        <button class="tab-btn" :class="{ active: activeTab === 'hall' }" @click="activeTab = 'hall'">活动大厅</button>
        <button class="tab-btn" :class="{ active: activeTab === 'mine' }" @click="activeTab = 'mine'">我的报名</button>
      </div>

      <div v-if="activeTab === 'hall'" class="tab-body">
        <div class="toolbar">
          <div class="filter-group">
            <button class="filter-btn" :class="{ active: statusFilter === '' }" @click="statusFilter = ''; loadActivities()">全部</button>
            <button class="filter-btn" :class="{ active: statusFilter === 'RECRUITING' }" @click="statusFilter = 'RECRUITING'; loadActivities()">报名中</button>
            <button class="filter-btn" :class="{ active: statusFilter === 'UPCOMING' }" @click="statusFilter = 'UPCOMING'; loadActivities()">报名已截止即将开始</button>
            <button class="filter-btn" :class="{ active: statusFilter === 'ONGOING' }" @click="statusFilter = 'ONGOING'; loadActivities()">进行中</button>
            <button class="filter-btn" :class="{ active: statusFilter === 'ENDED' }" @click="statusFilter = 'ENDED'; loadActivities()">已结束</button>
          </div>
          <button class="refresh-btn" :disabled="loading" @click="loadActivities">{{ loading ? '加载中...' : '刷新' }}</button>
        </div>

        <div v-if="loading" class="simple-loading">加载中...</div>

        <div v-else-if="!filteredActivities.length" class="simple-empty">暂无活动</div>

        <div v-else class="act-grid">
          <div v-for="act in filteredActivities" :key="act.id" class="act-card">
            <div class="act-cover">
              <img :src="actImg(act)" alt="" class="cover-img" />
              <div class="act-status-wrap">
                <span v-for="tag in getStatusTags(act, statusFilter)" :key="tag.label" class="act-tag" :class="'tag-' + tag.type">{{ tag.label }}</span>
              </div>
            </div>
            <div class="act-title">{{ act.title }}</div>
            <div class="act-line">活动：{{ formatDateTime(act.activityStart) }} 起</div>
            <div class="act-line">报名截止：{{ formatDateTime(act.registrationEnd) }}</div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: progressPercent(act) + '%' }" :class="{ exception: progressPercent(act) >= 100 }"></div>
              <span class="progress-text">{{ progressPercent(act) }}%</span>
            </div>
            <div class="act-people">{{ act.currentParticipants || 0 }} / {{ act.maxParticipants || 0 }} 人报名</div>
            <div class="act-actions">
              <button class="btn btn-outline" @click="showDetail(act)">查看详情</button>
              <button class="btn btn-primary" :disabled="!isRegistrationOpen(act)" @click="register(act)">
                {{ isRegistrationOpen(act) ? '立即报名' : '暂不可报' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="activeTab === 'mine'" class="tab-body">
        <div class="toolbar">
          <button class="refresh-btn" :disabled="mineLoading" @click="loadMine">{{ mineLoading ? '加载中...' : '刷新' }}</button>
        </div>
        <div v-if="mineLoading" class="simple-loading">加载中...</div>
        <div v-else-if="!mine.length" class="simple-empty">还没有报名任何活动</div>
        <div v-else class="mine-list">
          <div v-for="row in mine" :key="row.registrationId" class="mine-item">
            <img :src="actImg(row.activity || { title: mineTitle(row) })" class="mine-cover-img" />
            <div class="mine-info">
              <div class="mine-title">{{ mineTitle(row) }}</div>
              <div class="mine-time">{{ formatDateTime(row.activity?.activityStart) }} ~ {{ formatDateTime(row.activity?.activityEnd) }}</div>
              <div class="mine-meta">
                <span>报名ID: {{ row.registrationId }}</span>
                <span class="mine-status" :class="'status-' + (row.checkInStatus || '')">{{ checkInLabel(row.checkInStatus) }}</span>
                <span v-if="!isOngoing(row.activity || {})" class="mine-status status-warning">非活动时间</span>
              </div>
            </div>
            <div class="mine-actions">
              <button class="btn btn-success" :disabled="row.checkInStatus === 'CHECKED_IN' || !isOngoing(row.activity || {})" @click="checkIn(row)">
                {{ row.checkInStatus === 'CHECKED_IN' ? '已签到' : (isOngoing(row.activity || {}) ? '活动签到' : '签到未开放') }}
              </button>
              <button class="btn btn-danger" :disabled="row.checkInStatus === 'CHECKED_IN'" @click="cancelMine(row)">取消报名</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="detailVisible && detail" class="detail-mask" @click.self="detailVisible = false">
      <div class="detail-box">
        <div class="detail-head">
          <h2>{{ detail.title }}</h2>
          <button class="close-btn" @click="detailVisible = false">×</button>
        </div>
        <img :src="actImg(detail)" class="detail-cover-img" />
        <div class="detail-meta">
          <div v-for="tag in getStatusTags(detail)" :key="tag.label" class="act-tag" :class="'tag-' + tag.type">{{ tag.label }}</div>
        </div>
        <div class="detail-line"><strong>报名时间：</strong>{{ formatDateTime(detail.registrationStart) }} ~ {{ formatDateTime(detail.registrationEnd) }}</div>
        <div class="detail-line"><strong>活动时间：</strong>{{ formatDateTime(detail.activityStart) }} ~ {{ formatDateTime(detail.activityEnd) }}</div>
        <div class="detail-line"><strong>报名情况：</strong>{{ detail.currentParticipants || 0 }} / {{ detail.maxParticipants || 0 }} 人</div>
        <div class="detail-content-title">活动介绍</div>
        <div class="detail-content">{{ detail.content || '暂无详细介绍' }}</div>
        <div v-if="registeredInfo.registered" class="registered-tip">
          ✓ 您已报名（报名ID: {{ registeredInfo.registrationId }}），状态：{{ checkInLabel(registeredInfo.checkInStatus) }}
        </div>
        <div class="detail-foot">
          <button class="btn btn-outline" @click="detailVisible = false">关闭</button>
          <template v-if="registeredInfo.registered">
            <button class="btn btn-danger" :disabled="registeredInfo.checkInStatus === 'CHECKED_IN'" @click="cancelFromDetail">取消报名</button>
            <button class="btn btn-primary" disabled>已报名</button>
          </template>
          <template v-else>
            <button class="btn btn-primary" :disabled="!isRegistrationOpen(detail)" @click="register(detail)">
              {{ isRegistrationOpen(detail) ? '立即报名' : '暂不可报' }}
            </button>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { memberActivityApi } from '@/api/memberActivity'
import {
  formatDateTime,
  dictLabel,
  dictType,
  activityStatusMap,
  checkInStatusMap,
  buildActImage
} from '@/utils/format'

const route = useRoute()

const imgRefreshTick = ref(0)
function actImg(row) { imgRefreshTick.value; return buildActImage(row) }
window.addEventListener('image-cache-ready', () => { imgRefreshTick.value++ })

const nowTick = ref(Date.now())
let nowTimer = null

const activeTab = ref('hall')
watch(activeTab, (val) => { if (val === 'mine') loadMine() })

const loading = ref(false)
const activities = ref([])
const statusFilter = ref('')

async function loadActivities() {
  loading.value = true
  try {
    const res = await memberActivityApi.listActivities()
    activities.value = (res.data || []).sort((a, b) => {
      const ta = new Date(a.createTime || 0).getTime()
      const tb = new Date(b.createTime || 0).getTime()
      return tb - ta
    })
  } catch (e) {}
  finally { loading.value = false }
}

const filteredActivities = computed(() => {
  nowTick.value
  if (!statusFilter.value) return activities.value
  return activities.value.filter((act) => matchesFilter(act, statusFilter.value))
})

function parseTs(v) { return v ? new Date(v).getTime() : NaN }
function nowTs() { nowTick.value; return Date.now() }
function isCancelled(act) { if (act.status === 'CANCELLED' || act.status === 'PAUSED') return true; return false }
function isEnded(act) {
  if (act.status === 'ENDED' || act.status === 'FINISHED') return true
  const actEnd = parseTs(act.activityEnd)
  if (!isNaN(actEnd)) return nowTs() > actEnd
  return false
}
function isOngoing(act) {
  if (act.status === 'ONGOING' || act.status === 'ACTIVE') return true
  if (isCancelled(act) || isEnded(act)) return false
  const actStart = parseTs(act.activityStart)
  const actEnd = parseTs(act.activityEnd)
  const now = nowTs()
  return !isNaN(actStart) && now >= actStart && (isNaN(actEnd) || now <= actEnd)
}
function isRecruiting(act) {
  if (act.status === 'RECRUITING' || act.status === 'REGISTRATION_OPEN') return true
  if (isCancelled(act) || isEnded(act) || isOngoing(act)) return false
  const regStart = parseTs(act.registrationStart)
  const regEnd = parseTs(act.registrationEnd)
  const now = nowTs()
  return !isNaN(regStart) && !isNaN(regEnd) && now >= regStart && now <= regEnd
}
function isUpcoming(act) {
  if (act.status === 'REGISTRATION_CLOSED') return true
  if (isCancelled(act) || isEnded(act) || isOngoing(act)) return false
  const now = nowTs()
  const regEnd = parseTs(act.registrationEnd)
  const actStart = parseTs(act.activityStart)
  const regClosed = !isNaN(regEnd) && now > regEnd
  const notYetStarted = !isNaN(actStart) && now < actStart
  return regClosed && notYetStarted
}
function matchesFilter(act, filter) {
  if (filter === 'RECRUITING') return isRecruiting(act)
  if (filter === 'UPCOMING') return isUpcoming(act)
  if (filter === 'ONGOING') return isOngoing(act)
  if (filter === 'ENDED') return isEnded(act)
  return true
}

function progressPercent(act) {
  if (!act.maxParticipants) return 0
  return Math.min(100, Math.round((act.currentParticipants / act.maxParticipants) * 100))
}

function isRegistrationOpen(act) {
  if (isCancelled(act) || isEnded(act) || isOngoing(act)) return false
  const regStart = parseTs(act.registrationStart)
  const regEnd = parseTs(act.registrationEnd)
  const now = nowTs()
  return !isNaN(regStart) && !isNaN(regEnd) && now >= regStart && now <= regEnd
}

function getStatusTags(act, filter) {
  if (!act) return []
  const now = nowTs()
  const regStart = parseTs(act.registrationStart)
  const regEnd = parseTs(act.registrationEnd)
  const regClosed = !isNaN(regEnd) && now > regEnd
  const regNotStart = !isNaN(regStart) && now < regStart

  if (filter === 'RECRUITING') {
    if (isRecruiting(act)) return [{ label: '报名中', type: 'primary' }]
    if (regNotStart) return [{ label: '报名未开始', type: 'info' }]
    if (regClosed) return [{ label: '报名已截止', type: 'warning' }]
    return []
  }
  if (filter === 'UPCOMING') {
    if (isUpcoming(act)) return [{ label: '报名已截止即将开始', type: 'warning' }]
    return []
  }
  if (filter === 'ONGOING') {
    if (isOngoing(act)) return [{ label: '进行中', type: 'success' }]
    return []
  }
  if (filter === 'ENDED') {
    if (isEnded(act)) return [{ label: '已结束', type: 'info' }]
    return []
  }
  if (isCancelled(act)) return [{ label: '已取消', type: 'danger' }]
  if (isEnded(act)) return [{ label: '已结束', type: 'info' }]
  if (isOngoing(act)) return [{ label: '进行中', type: 'success' }]
  if (isRecruiting(act)) return [{ label: '报名中', type: 'primary' }]
  if (isUpcoming(act)) return [{ label: '报名已截止即将开始', type: 'warning' }]
  if (regNotStart) return [{ label: '报名未开始', type: 'info' }]
  if (regClosed) return [{ label: '报名已截止', type: 'warning' }]
  if (activityStatusMap[act.status]) return [activityStatusMap[act.status]]
  return [{ label: act.status || '未知', type: 'info' }]
}

function checkInLabel(s) {
  return dictLabel(checkInStatusMap, s) || s || '未签到'
}

const detailVisible = ref(false)
const detail = ref(null)
const registeredInfo = reactive({ registered: false, registrationId: null, checkInStatus: '' })
const regLoading = ref(false)

async function showDetail(act) {
  detailVisible.value = true
  detail.value = null
  Object.assign(registeredInfo, { registered: false, registrationId: null, checkInStatus: '' })
  try {
    const [detailRes, regRes] = await Promise.all([
      memberActivityApi.getActivityDetail(act.id),
      memberActivityApi.checkRegistered(act.id).catch(() => null)
    ])
    detail.value = detailRes.data
    if (regRes?.data) {
      registeredInfo.registered = !!regRes.data.registered
      registeredInfo.registrationId = regRes.data.registrationId
      registeredInfo.checkInStatus = regRes.data.checkInStatus
    }
  } catch (e) {
    detailVisible.value = false
  }
}

async function register(act) {
  try {
    if (!confirm(`确认报名活动「${act.title}」吗？`)) return
    regLoading.value = true
    await memberActivityApi.registerActivity(act.id)
    alert('报名成功')
    detailVisible.value = false
    loadActivities()
    loadMine()
  } catch (e) {}
  finally { regLoading.value = false }
}

const mineLoading = ref(false)
const mine = ref([])

async function loadMine() {
  mineLoading.value = true
  try {
    const res = await memberActivityApi.listMyActivities()
    mine.value = res.data || []
  } catch (e) {}
  finally { mineLoading.value = false }
}

function mineTitle(row) {
  const t = row.activity?.title
  if (typeof t === 'string') return t
  const matched = activities.value.find((a) => a.id === row.activityId)
  return matched?.title || `活动 #${row.activityId}`
}

async function checkIn(row) {
  try {
    const act = row.activity || {}
    if (!isOngoing(act)) {
      alert('签到失败：签到只能在活动进行期间进行')
      return
    }
    if (!confirm('确认参加活动并签到吗？')) return
    await memberActivityApi.checkIn(row.activityId)
    alert('签到成功')
    loadMine()
  } catch (e) {}
}

async function cancelMine(row) {
  try {
    if (!confirm(`确认取消活动「${mineTitle(row)}」的报名吗？此操作不可撤销。`)) return
    await memberActivityApi.cancelRegistration(row.activityId)
    alert('取消报名成功')
    loadMine()
    loadActivities()
  } catch (e) {}
}

async function cancelFromDetail() {
  try {
    if (!confirm(`确认取消活动「${detail.value.title}」的报名吗？此操作不可撤销。`)) return
    await memberActivityApi.cancelRegistration(detail.value.id)
    alert('取消报名成功')
    Object.assign(registeredInfo, { registered: false, registrationId: null, checkInStatus: '' })
    detailVisible.value = false
    loadMine()
    loadActivities()
  } catch (e) {}
}

onMounted(async () => {
  await loadActivities()
  loadMine()
  const actId = route.query.actId
  if (actId) {
    const matched = activities.value.find((a) => String(a.id) === String(actId))
    if (matched) showDetail(matched)
  }
  nowTimer = setInterval(() => { nowTick.value = Date.now() }, 60 * 1000)
})

onBeforeUnmount(() => {
  detailVisible.value = false
  document.body.classList.remove('el-popup-parent--hidden')
  if (nowTimer) { clearInterval(nowTimer); nowTimer = null }
})
</script>

<style scoped>
.page-activities { padding: 20px; color: var(--text-main); }
.page-card {
  background: var(--card-bg);
  border: 1px solid var(--card-border);
  border-radius: 20px;
  backdrop-filter: blur(18px) saturate(1.6);
  -webkit-backdrop-filter: blur(18px) saturate(1.6);
  box-shadow: var(--card-shadow);
  padding: 24px;
}
.tabs-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
  padding-bottom: 14px;
}
.tab-btn {
  background: transparent;
  border: none;
  color: var(--text-sub);
  padding: 10px 20px;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.25s;
}
.tab-btn:hover { color: var(--primary); background: rgba(47, 125, 109, 0.07); }
.tab-btn.active {
  background: var(--primary-gradient);
  color: #fff;
  box-shadow: 0 5px 16px rgba(31, 101, 89, 0.23);
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.filter-group { display: flex; gap: 6px; flex-wrap: wrap; }
.filter-btn {
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid var(--border-light);
  color: var(--text-sub);
  padding: 6px 16px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.25s;
}
.filter-btn:hover { color: var(--primary); border-color: rgba(47, 125, 109, 0.3); }
.filter-btn.active {
  background: var(--primary-gradient);
  border-color: transparent;
  color: #fff;
  box-shadow: 0 3px 10px rgba(31, 101, 89, 0.22);
}
.refresh-btn {
  background: rgba(47, 125, 109, 0.08);
  border: 1px solid rgba(47, 125, 109, 0.2);
  color: var(--primary);
  padding: 6px 16px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.25s;
}
.refresh-btn:hover { background: rgba(47, 125, 109, 0.13); }
.refresh-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.simple-loading {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-muted);
}
.simple-empty {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-muted);
}
.act-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
.act-card {
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 18px;
  padding: 14px;
  transition: all 0.3s;
  box-shadow: 0 4px 20px rgba(38, 76, 65, 0.05);
}
.act-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 14px 34px rgba(38, 76, 65, 0.11);
  border-color: rgba(47, 125, 109, 0.2);
}
.act-cover { position: relative; height: 180px; border-radius: 12px; overflow: hidden; margin-bottom: 12px; background: #eff5f2; }
.cover-img { width: 100%; height: 100%; object-fit: cover; display: block; }
.act-status-wrap {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-end;
}
.act-tag {
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 999px;
  color: #fff;
  font-weight: 600;
}
.tag-primary { background: var(--primary-gradient); }
.tag-success { background: linear-gradient(135deg, #50C878, #3da860); }
.tag-warning { background: linear-gradient(135deg, #f5a623, #ff8a5c); }
.tag-info { background: rgba(0, 0, 0, 0.35); }
.act-title { font-size: 16px; font-weight: 600; color: var(--text-main); margin-bottom: 8px; }
.act-line { font-size: 12px; color: var(--text-sub); margin-bottom: 4px; }
.progress-bar {
  position: relative;
  height: 8px;
  background: rgba(47, 125, 109, 0.08);
  border-radius: 4px;
  margin: 10px 0 4px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: var(--primary-gradient);
  border-radius: 4px;
  transition: width 0.3s;
}
.progress-fill.exception { background: linear-gradient(90deg, #f5a623, #ff8a5c); }
.progress-text {
  position: absolute;
  right: 0;
  top: -18px;
  font-size: 11px;
  color: var(--text-sub);
}
.act-people { font-size: 12px; color: var(--text-muted); margin-bottom: 12px; }
.act-actions { display: flex; flex-direction: column; gap: 8px; }
.btn {
  padding: 10px 18px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  border: none;
}
.btn:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-primary {
  background: var(--primary-gradient);
  color: #fff;
  box-shadow: 0 4px 14px rgba(31, 101, 89, 0.24);
}
.btn-primary:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 22px rgba(31, 101, 89, 0.32); }
.btn-outline {
  background: rgba(255, 255, 255, 0.7);
  color: var(--primary);
  border: 1px solid rgba(47, 125, 109, 0.24);
}
.btn-outline:hover:not(:disabled) { background: rgba(47, 125, 109, 0.08); border-color: var(--primary); }
.btn-success {
  background: linear-gradient(135deg, #50C878, #3da860);
  color: #fff;
  padding: 8px 14px;
  font-size: 13px;
  box-shadow: 0 3px 10px rgba(80, 200, 120, 0.3);
}
.btn-danger {
  background: linear-gradient(135deg, #ff6b6b, #c92a2a);
  color: #fff;
  padding: 8px 14px;
  font-size: 13px;
  box-shadow: 0 3px 10px rgba(201, 42, 42, 0.3);
}
.btn-danger:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 6px 18px rgba(201, 42, 42, 0.45); }

.mine-actions { display: flex; flex-direction: column; gap: 6px; align-items: stretch; }

.mine-list { display: flex; flex-direction: column; gap: 10px; }
.mine-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(38, 76, 65, 0.05);
}
.mine-cover-img { width: 64px; height: 48px; border-radius: 10px; object-fit: cover; flex-shrink: 0; }
.mine-info { flex: 1; min-width: 0; }
.mine-title { font-weight: 600; color: var(--text-main); margin-bottom: 2px; font-size: 14px; }
.mine-time { font-size: 12px; color: var(--text-sub); margin-bottom: 4px; }
.mine-meta { display: flex; gap: 12px; font-size: 11px; color: var(--text-muted); }
.mine-status.status-CHECKED_IN { color: var(--success); font-weight: 600; }
.mine-status.status-PENDING { color: var(--warning); font-weight: 600; }

.detail-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.detail-box {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.9);
  border-radius: 24px;
  padding: 28px;
  max-width: 600px;
  width: 90%;
  max-height: 85vh;
  overflow-y: auto;
  position: relative;
  box-shadow: 0 24px 60px rgba(31, 70, 60, 0.17);
}
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.detail-head h2 { margin: 0; color: var(--text-main); font-size: 20px; font-weight: 700; }
.close-btn {
  background: rgba(0, 0, 0, 0.04);
  border: none;
  color: var(--text-sub);
  font-size: 28px;
  cursor: pointer;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  line-height: 1;
  transition: all 0.25s;
}
.close-btn:hover { background: rgba(47, 125, 109, 0.1); color: var(--primary); }
.detail-cover-img { width: 100%; height: 200px; object-fit: cover; border-radius: 14px; margin-bottom: 16px; }
.detail-meta { display: flex; gap: 6px; margin-bottom: 16px; flex-wrap: wrap; }
.detail-line { font-size: 13px; color: var(--text-sub); margin-bottom: 6px; }
.detail-line strong { color: var(--text-main); }
.detail-content-title { font-weight: 600; color: var(--text-main); margin: 16px 0 8px; font-size: 14px; }
.detail-content {
  background: rgba(47, 125, 109, 0.04);
  border: 1px solid rgba(47, 125, 109, 0.1);
  border-radius: 12px;
  padding: 14px;
  color: var(--text-sub);
  line-height: 1.8;
  white-space: pre-wrap;
  font-size: 13px;
}
.registered-tip {
  margin-top: 14px;
  padding: 10px 14px;
  background: rgba(80, 200, 120, 0.12);
  border: 1px solid rgba(80, 200, 120, 0.25);
  border-radius: 10px;
  color: #2d8a4e;
  font-size: 13px;
  font-weight: 500;
}
.detail-foot {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
