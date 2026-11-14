<template>
  <div class="page-container">
    <div class="page-card welcome-card">
      <div>
        <div class="welcome-title">欢迎回来，{{ userStore.displayName }} 👋</div>
        <div class="welcome-sub">智慧养老服务平台运营数据总览 · {{ todayLabel }}</div>
      </div>
      <el-button type="primary" :icon="Refresh" @click="loadAll" :loading="loading">
        刷新数据
      </el-button>
    </div>

    <el-row :gutter="16">
      <el-col
        v-for="card in cards"
        :key="card.key"
        :xs="12"
        :sm="12"
        :md="8"
        :lg="6"
        :xl="6"
      >
        <div class="stat-card hover-card" :style="{ '--stat-accent': card.color }">
          <div class="stat-icon">
            <el-icon :size="26"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value mono">{{ summary[card.key] ?? 0 }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 4px">
      <el-col :xs="24" :lg="12">
        <div class="page-card chart-card">
          <div class="card-head">
            <div class="page-title"><el-icon><TrendCharts /></el-icon>运营趋势</div>
            <div class="page-subtitle">近 12 个月会员新增与预约数量</div>
          </div>
          <div ref="trendChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="page-card chart-card">
          <div class="card-head">
            <div class="page-title"><el-icon><PieChart /></el-icon>业务概览</div>
            <div class="page-subtitle">核心运营指标占比</div>
          </div>
          <div ref="overviewPieRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="page-card chart-card">
          <div class="card-head">
            <div class="page-title"><el-icon><PieChart /></el-icon>活动状态分布</div>
            <div class="page-subtitle">当前平台活动各状态占比</div>
          </div>
          <div ref="activityPieRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 4px">
      <el-col :xs="24" :lg="12">
        <div class="page-card chart-card">
          <div class="card-head">
            <div class="page-title"><el-icon><DataLine /></el-icon>热门活动 TOP5</div>
            <div class="page-subtitle">报名中活动 · 按报名人数排行</div>
          </div>
          <div ref="topActChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="page-card chart-card">
          <div class="card-head">
            <div class="page-title"><el-icon><Histogram /></el-icon>会员等级分布</div>
            <div class="page-subtitle">各等级会员数量与占比</div>
          </div>
          <div ref="levelChartRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 4px" class="equal-row">
      <el-col :xs="24" :lg="12" class="equal-col">
        <div class="page-card">
          <div class="page-title"><el-icon><DataAnalysis /></el-icon>业务概览</div>
          <div class="page-subtitle">平台核心运营指标实时统计</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="会员总数">
              <span class="mono">{{ summary.totalMembers ?? 0 }}</span> 人
            </el-descriptions-item>
            <el-descriptions-item label="今日新增会员">
              <span class="mono highlight">{{ summary.todayNewMembers ?? 0 }}</span> 人
            </el-descriptions-item>
            <el-descriptions-item label="预约总数">
              <span class="mono">{{ summary.totalAppointments ?? 0 }}</span> 单
            </el-descriptions-item>
            <el-descriptions-item label="今日预约">
              <span class="mono highlight">{{ summary.todayAppointments ?? 0 }}</span> 单
            </el-descriptions-item>
            <el-descriptions-item label="活动总数">
              <span class="mono">{{ summary.totalActivities ?? 0 }}</span> 场
            </el-descriptions-item>
            <el-descriptions-item label="进行中活动">
              <span class="mono highlight">{{ summary.ongoingActivities ?? 0 }}</span> 场
            </el-descriptions-item>
            <el-descriptions-item label="AI 对话总次数">
              <span class="mono">{{ summary.aiChatCount ?? 0 }}</span> 次
            </el-descriptions-item>
            <el-descriptions-item label="平台状态">
              <el-tag type="success" effect="light" round>运行正常</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12" class="equal-col">
        <div class="page-card quick-card">
          <div class="page-title"><el-icon><Menu /></el-icon>快捷入口</div>
          <div class="page-subtitle">常用管理功能一键直达</div>
          <div class="quick-grid">
            <div
              v-for="q in quickEntries"
              :key="q.path"
              class="quick-item hover-card"
              @click="router.push(q.path)"
            >
              <el-icon :size="24" :color="q.color"><component :is="q.icon" /></el-icon>
              <span>{{ q.label }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Refresh,
  User,
  Plus,
  Tickets,
  Calendar,
  Flag,
  ChatDotRound,
  DataAnalysis,
  Menu,
  TrendCharts,
  PieChart,
  DataLine,
  Histogram,
  Odometer
} from '@element-plus/icons-vue'
import { dashboardApi } from '@/api/dashboard'
import { memberApi } from '@/api/member'
import { adminActivityApi } from '@/api/adminActivity'
import { adminAppointmentApi } from '@/api/adminAppointment'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const summary = reactive({})

const todayLabel = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const cards = [
  { key: 'totalMembers', label: '会员总数', icon: User, color: '#2f7d6d' },
  { key: 'todayNewMembers', label: '今日新增会员', icon: Plus, color: '#608ca5' },
  { key: 'totalAppointments', label: '预约总数', icon: Tickets, color: '#d9963e' },
  { key: 'todayAppointments', label: '今日预约', icon: Calendar, color: '#cf7d70' },
  { key: 'totalActivities', label: '活动总数', icon: Flag, color: '#7c8e67' },
  { key: 'ongoingActivities', label: '进行中活动', icon: Odometer, color: '#4f9b7b' },
  { key: 'aiChatCount', label: 'AI 对话次数', icon: ChatDotRound, color: '#9b7e91' }
]

const quickEntries = [
  { label: '用户管理', path: '/admin/members', icon: User, color: '#2f7d6d' },
  { label: '套餐与时段', path: '/admin/packages', icon: Calendar, color: '#d9963e' },
  { label: '预约记录', path: '/admin/appointments', icon: Tickets, color: '#608ca5' },
  { label: '活动与报名', path: '/admin/activities', icon: Flag, color: '#7c8e67' },
  { label: '问卷评测', path: '/admin/assessments', icon: DataAnalysis, color: '#9b7e91' },
  { label: '系统配置', path: '/admin/config', icon: Menu, color: '#60736d' }
]

const trendChartRef = ref(null)
const overviewPieRef = ref(null)
const activityPieRef = ref(null)
const topActChartRef = ref(null)
const levelChartRef = ref(null)
const charts = []

function disposeCharts() {
  charts.forEach((c) => c && !c.isDisposed() && c.dispose())
  charts.length = 0
}

function initChart(el) {
  if (!el) return null
  const c = echarts.init(el, null, { renderer: 'canvas' })
  charts.push(c)
  return c
}

function resizeCharts() {
  charts.forEach((c) => c && !c.isDisposed() && c.resize())
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

function last12Months() {
  const arr = []
  const now = new Date()
  for (let i = 11; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    arr.push({
      key: `${d.getFullYear()}-${pad2(d.getMonth() + 1)}`,
      label: `${pad2(d.getMonth() + 1)}月`
    })
  }
  return arr
}

function drawTrendChart(members, appointments) {
  const chart = initChart(trendChartRef.value)
  if (!chart) return
  const memberOnly = members.filter((m) => (m.role || '').toUpperCase() === 'MEMBER')
  const months = last12Months()
  const memberByMonth = {}
  memberOnly.forEach((m) => {
    const t = m.create_time || m.createTime
    if (!t) return
    const k = String(t).slice(0, 7)
    memberByMonth[k] = (memberByMonth[k] || 0) + 1
  })
  const apptByMonth = {}
  appointments.forEach((a) => {
    const t = a.createTime || a.create_time
    if (!t) return
    const k = String(t).slice(0, 7)
    apptByMonth[k] = (apptByMonth[k] || 0) + 1
  })
  const memberData = months.map((m) => memberByMonth[m.key] || 0)
  const apptData = months.map((m) => apptByMonth[m.key] || 0)

  chart.setOption({
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(25,53,47,0.94)', borderColor: 'transparent', textStyle: { color: '#fff' } },
    legend: { data: ['会员新增', '预约数量'], top: 4, textStyle: { color: '#5f736d' } },
    grid: { left: 44, right: 24, top: 44, bottom: 32, containLabel: false },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: months.map((m) => m.label),
      axisLine: { lineStyle: { color: '#dee9e4' } },
      axisLabel: { color: '#8b9b96' }
    },
    yAxis: [
      {
        type: 'value',
        name: '会员',
        nameTextStyle: { color: '#8b9b96' },
        splitLine: { lineStyle: { color: '#dee9e4', type: 'dashed' } },
        axisLabel: { color: '#8b9b96' }
      },
      {
        type: 'value',
        name: '预约',
        nameTextStyle: { color: '#8b9b96' },
        splitLine: { show: false },
        axisLabel: { color: '#8b9b96' }
      }
    ],
    series: [
      {
        name: '会员新增',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: memberData,
        itemStyle: { color: '#2f7d6d' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(47,125,109,0.28)' },
            { offset: 1, color: 'rgba(47,125,109,0.015)' }
          ])
        }
      },
      {
        name: '预约数量',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        data: apptData,
        itemStyle: { color: '#d9963e' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(217,150,62,0.24)' },
            { offset: 1, color: 'rgba(217,150,62,0.015)' }
          ])
        }
      }
    ]
  })
}

function drawOverviewPie() {
  const chart = initChart(overviewPieRef.value)
  if (!chart) return

  const indicators = [
    { key: 'totalMembers', label: '会员总数', color: '#2f7d6d' },
    { key: 'todayNewMembers', label: '今日新增', color: '#608ca5' },
    { key: 'totalAppointments', label: '预约总数', color: '#d9963e' },
    { key: 'todayAppointments', label: '今日预约', color: '#cf7d70' },
    { key: 'totalActivities', label: '活动总数', color: '#7c8e67' },
    { key: 'ongoingActivities', label: '进行中', color: '#4f9b7b' },
    { key: 'aiChatCount', label: 'AI 对话', color: '#9b7e91' }
  ]

  const data = indicators.map((ind) => ({
    name: ind.label,
    value: summary[ind.key] || 0,
    itemStyle: { color: ind.color }
  }))

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      orient: 'vertical',
      right: 4,
      top: 'middle',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 6,
      textStyle: { color: '#5f736d', fontSize: 11 }
    },
    series: [
      {
        name: '业务概览',
        type: 'pie',
        radius: ['45%', '72%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fffdf8', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 13, fontWeight: 'bold', color: '#19352f' }
        },
        data
      }
    ]
  })
}

function drawActivityPie(activities) {
  const chart = initChart(activityPieRef.value)
  if (!chart) return
  const statusColors = {
    RECRUITING: '#608ca5',
    REGISTRATION_OPEN: '#608ca5',
    REGISTRATION_CLOSED: '#d9963e',
    ONGOING: '#4f9b7b',
    ACTIVE: '#4f9b7b',
    ENDED: '#7c8e67',
    FINISHED: '#7c8e67',
    CANCELLED: '#cf7d70',
    PAUSED: '#b98b4d'
  }
  const labelMap = {
    RECRUITING: '报名中', REGISTRATION_OPEN: '报名中',
    REGISTRATION_CLOSED: '报名已截止',
    ONGOING: '进行中', ACTIVE: '进行中',
    ENDED: '已结束', FINISHED: '已结束',
    CANCELLED: '已取消', PAUSED: '已暂停'
  }
  const counter = {}
  activities.forEach((a) => {
    const s = a.status || 'RECRUITING'
    counter[s] = (counter[s] || 0) + 1
  })
  const data = Object.keys(counter).map((k) => ({
    name: labelMap[k] || k,
    value: counter[k],
    itemStyle: { color: statusColors[k] || '#999' }
  }))

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c}场 ({d}%)' },
    legend: {
      orient: 'vertical',
      right: 8,
      top: 'middle',
      textStyle: { color: '#5f736d' }
    },
    series: [
      {
        name: '活动状态',
        type: 'pie',
        radius: ['50%', '75%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: { borderRadius: 6, borderColor: '#fffdf8', borderWidth: 2 },
        label: { show: false },
        emphasis: {
          label: { show: true, fontSize: 14, fontWeight: 'bold', color: '#19352f' }
        },
        data
      }
    ]
  })
}

function drawTopActivities(activities) {
  const chart = initChart(topActChartRef.value)
  if (!chart) return
  const recruitingStatuses = ['RECRUITING', 'REGISTRATION_OPEN']
  const top = [...activities]
    .filter((a) => recruitingStatuses.includes((a.status || '').toUpperCase()))
    .sort((a, b) => (b.currentParticipants || 0) - (a.currentParticipants || 0))
    .slice(0, 5)
    .reverse()
  const titles = top.map((a) => a.title || '')
  const values = top.map((a) => a.currentParticipants || 0)
  const maxes = top.map((a) => a.maxParticipants || 1)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const p = params[0]
        const idx = p.dataIndex
        return `${top[idx].title}<br/>报名：${values[idx]} / ${maxes[idx]}`
      }
    },
    grid: { left: 10, right: 40, top: 20, bottom: 24, containLabel: true },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: '#dee9e4', type: 'dashed' } },
      axisLabel: { color: '#8b9b96' }
    },
    yAxis: {
      type: 'category',
      data: titles,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#5f736d' }
    },
    series: [
      {
        type: 'bar',
        data: values.map((v, i) => ({
          value: v,
          itemStyle: {
            borderRadius: [0, 8, 8, 0],
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#7c8e67' },
              { offset: 1, color: '#d9963e' }
            ]),
            opacity: Math.min(1, 0.45 + (v / Math.max(1, maxes[i])) * 0.55)
          }
        })),
        barWidth: 18,
        label: { show: true, position: 'right', color: '#5f736d', fontSize: 12 }
      }
    ]
  })
}

function drawLevelChart(members) {
  const chart = initChart(levelChartRef.value)
  if (!chart) return
  const memberOnly = members.filter((m) => (m.role || '').toUpperCase() === 'MEMBER')
  const levelColors = {
    NORMAL: '#a2aea9',
    SILVER: '#7d8f89',
    GOLD: '#d9963e',
    PLATINUM: '#6aa596',
    DIAMOND: '#9b7e91'
  }
  const levelLabel = {
    NORMAL: '普通',
    SILVER: '银卡',
    GOLD: '金卡',
    PLATINUM: '铂金',
    DIAMOND: '钻石'
  }
  const counter = {}
  memberOnly.forEach((m) => {
    const lv = (m.member_level || m.level || 'NORMAL').toUpperCase()
    counter[lv] = (counter[lv] || 0) + 1
  })
  const all = ['NORMAL', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND']
  const data = all
    .map((k) => ({ key: k, count: counter[k] || 0 }))
    .filter((d) => d.count > 0)

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 40, right: 24, top: 28, bottom: 30, containLabel: false },
    xAxis: {
      type: 'category',
      data: data.map((d) => levelLabel[d.key] || d.key),
      axisLine: { lineStyle: { color: '#dee9e4' } },
      axisLabel: { color: '#8b9b96' }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#dee9e4', type: 'dashed' } },
      axisLabel: { color: '#8b9b96' }
    },
    series: [
      {
        type: 'bar',
        barWidth: '40%',
        data: data.map((d) => ({
          value: d.count,
          itemStyle: {
            borderRadius: [8, 8, 0, 0],
            color: levelColors[d.key] || '#2f7d6d'
          }
        })),
        label: { show: true, position: 'top', color: '#5f736d', fontSize: 12 }
      }
    ]
  })
}

async function loadSummary() {
  try {
    const res = await dashboardApi.getSummary()
    Object.assign(summary, res.data)
  } catch (e) {}
}

async function loadAll() {
  loading.value = true
  disposeCharts()
  try {
    await loadSummary()

    const [memberRes, actRes, apptRes] = await Promise.allSettled([
      memberApi.getMemberList({ page: 1, size: 9999 }),
      adminActivityApi.listActivities(),
      adminAppointmentApi.listAppointments({ page: 1, size: 9999 })
    ])

    const members = memberRes.status === 'fulfilled'
      ? (memberRes.value.data?.records || memberRes.value.data?.list || memberRes.value.data || [])
      : []
    const activities = actRes.status === 'fulfilled' ? (actRes.value.data || []) : []
    const appointments = apptRes.status === 'fulfilled'
      ? (apptRes.value.data?.list || apptRes.value.data?.records || apptRes.value.data || [])
      : []

    await nextTick()
    drawTrendChart(members, appointments)
    drawOverviewPie()
    drawActivityPie(activities)
    drawTopActivities(activities)
    drawLevelChart(members)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  disposeCharts()
  window.removeEventListener('resize', resizeCharts)
})
</script>

<style scoped>
.welcome-card {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 132px;
  color: #fff;
  background:
    radial-gradient(circle at 88% 2%, rgba(241, 203, 136, 0.22), transparent 31%),
    linear-gradient(135deg, #1f6155 0%, #286f61 54%, #327d6d 100%) !important;
  border: 1px solid rgba(31, 101, 89, 0.2) !important;
  box-shadow: 0 18px 42px rgba(31, 101, 89, 0.14) !important;
}
.welcome-card::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 36px 36px;
  mask-image: linear-gradient(90deg, #000, transparent 76%);
  -webkit-mask-image: linear-gradient(90deg, #000, transparent 76%);
}
.welcome-card > div,
.welcome-card > .el-button {
  position: relative;
  z-index: 1;
}
.welcome-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}
.welcome-sub {
  font-size: 13px;
  color: rgba(242, 255, 251, 0.68);
  margin-top: 6px;
}
.welcome-card :deep(.el-button--primary) {
  color: #214f46 !important;
  background: #f2d7a6 !important;
  box-shadow: 0 8px 22px rgba(8, 48, 40, 0.18) !important;
}
.welcome-card :deep(.el-button--primary:hover) {
  background: #f7dfb5 !important;
  box-shadow: 0 10px 26px rgba(8, 48, 40, 0.23) !important;
}
.stat-card {
  --stat-accent: #2f7d6d;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 9px 26px rgba(38, 76, 65, 0.07);
  border: 1px solid rgba(222, 233, 228, 0.92);
  border-top: 3px solid var(--stat-accent) !important;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  margin-bottom: 16px;
  position: relative;
  overflow: hidden;
}
.stat-card::after {
  content: '';
  position: absolute;
  width: 92px;
  height: 92px;
  right: -42px;
  bottom: -50px;
  border-radius: 50%;
  background: color-mix(in srgb, var(--stat-accent) 8%, transparent);
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--stat-accent);
  background: color-mix(in srgb, var(--stat-accent) 13%, #fff);
  flex-shrink: 0;
  border: 1px solid color-mix(in srgb, var(--stat-accent) 10%, transparent);
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--text-main);
}
.stat-label {
  font-size: 13px;
  color: var(--text-sub);
  margin-top: 2px;
}
.chart-card {
  padding: 18px 20px;
}
.card-head {
  display: flex;
  flex-direction: column;
  margin-bottom: 8px;
}
.chart-box {
  width: 100%;
  height: 280px;
}
.highlight {
  color: var(--accent-color);
  font-weight: 700;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}
.quick-item {
  background: rgba(255, 255, 255, 0.58);
  border: 1px solid rgba(222, 233, 228, 0.94);
  border-radius: 12px;
  padding: 20px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--text-sub);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.quick-item:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(47, 125, 109, 0.22);
  color: var(--text-main);
  box-shadow: 0 10px 24px rgba(38, 76, 65, 0.08);
}
.equal-row {
  display: flex;
  align-items: stretch;
}
.equal-row > .equal-col {
  display: flex;
}
.equal-row > .equal-col > .page-card {
  flex: 1;
}
</style>
