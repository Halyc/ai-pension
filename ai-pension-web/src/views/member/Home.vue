<template>
  <div class="home-root">
    <main class="home-wrap">
      <section class="welcome-section">
        <div class="welcome-glow welcome-glow-one"></div>
        <div class="welcome-glow welcome-glow-two"></div>

        <div class="welcome-copy">
          <div class="welcome-meta">
            <span class="welcome-badge"><i></i>{{ greeting }}</span>
            <span class="welcome-date">{{ todayLabel }}</span>
          </div>

          <h1>
            {{ displayName }}，
            <span>{{ greetingMessage }}</span>
          </h1>
          <p>
            您的健康记录与服务安排都在这里。今天也要照顾好自己，
            有需要时，我们一直都在。
          </p>

          <div class="welcome-actions">
            <button type="button" class="welcome-action primary" @click="go('/member/appointments')">
              <el-icon><Calendar /></el-icon>
              预约体检
            </button>
            <button type="button" class="welcome-action secondary" @click="go('/member/health')">
              <el-icon><FirstAidKit /></el-icon>
              记录健康
            </button>
          </div>

          <a class="hotline-link" href="tel:4008888888">
            <span class="hotline-icon"><el-icon><Phone /></el-icon></span>
            <span>
              <small>需要帮助？</small>
              <strong>颐康服务热线 400-888-8888</strong>
            </span>
            <el-icon class="hotline-arrow"><ArrowRight /></el-icon>
          </a>
        </div>

        <div class="wellness-card">
          <div class="wellness-head">
            <div>
              <span class="card-eyebrow">TODAY'S CARE</span>
              <h2>今日关怀</h2>
            </div>
            <span class="sync-state"><i></i> 数据已同步</span>
          </div>

          <div class="blood-pressure-block">
            <div>
              <span class="metric-label">最近血压</span>
              <strong class="blood-pressure">
                {{ latestHealth?.systolic || '--' }}
                <i>/</i>
                {{ latestHealth?.diastolic || '--' }}
                <small>mmHg</small>
              </strong>
            </div>
            <span class="health-state" :class="`state-${healthState.level}`">
              {{ healthState.label }}
            </span>
          </div>

          <div class="metric-grid">
            <div class="metric-item">
              <span>心率</span>
              <strong>{{ latestHealth?.heart_rate ?? '--' }}</strong>
              <small>次/分</small>
            </div>
            <div class="metric-item">
              <span>血糖</span>
              <strong>{{ latestHealth?.blood_sugar ?? '--' }}</strong>
              <small>mmol/L</small>
            </div>
            <div class="metric-item">
              <span>体重</span>
              <strong>{{ latestHealth?.weight ?? '--' }}</strong>
              <small>kg</small>
            </div>
          </div>

          <button
            type="button"
            class="appointment-reminder"
            :class="{ empty: !nextAppointment }"
            @click="go('/member/appointments')"
          >
            <span class="reminder-icon"><el-icon><Calendar /></el-icon></span>
            <span class="reminder-copy">
              <small>{{ nextAppointment ? '下一次体检' : '体检预约' }}</small>
              <strong>{{ nextAppointment?.package?.name || '暂无待体检预约' }}</strong>
              <em>{{ nextAppointment ? '预约已确认，请留意到院时间' : '现在可以查看可预约的体检套餐' }}</em>
            </span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
      </section>

      <section class="summary-strip" aria-label="我的服务概览">
        <button type="button" class="summary-item" @click="go('/member/appointments')">
          <span class="summary-icon appointment"><el-icon><Calendar /></el-icon></span>
          <span><strong>{{ stats.appointments }}</strong><small>预约记录</small></span>
        </button>
        <button type="button" class="summary-item" @click="go('/member/activities')">
          <span class="summary-icon activity"><el-icon><Flag /></el-icon></span>
          <span><strong>{{ stats.activities }}</strong><small>活动报名</small></span>
        </button>
        <button type="button" class="summary-item" @click="go('/member/profile')">
          <span class="summary-icon points"><el-icon><Coin /></el-icon></span>
          <span><strong>{{ stats.points }}</strong><small>可用积分</small></span>
        </button>
        <button type="button" class="summary-item" @click="go('/member/health')">
          <span class="summary-icon health"><el-icon><FirstAidKit /></el-icon></span>
          <span><strong>{{ stats.healthDays }}</strong><small>健康打卡天数</small></span>
        </button>
      </section>

      <section class="section-block service-section">
        <div class="section-head">
          <div>
            <span class="section-kicker">EVERYDAY SERVICES</span>
            <h2>常用服务</h2>
          </div>
          <p>需要什么，点一下就能开始</p>
        </div>

        <div class="service-grid">
          <button
            v-for="item in quickLinks"
            :key="item.path"
            type="button"
            class="service-card"
            :class="item.theme"
            @click="go(item.path)"
          >
            <span class="service-icon"><el-icon><component :is="item.icon" /></el-icon></span>
            <span class="service-content">
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </span>
            <span class="service-arrow"><el-icon><ArrowRight /></el-icon></span>
          </button>
        </div>
      </section>

      <div class="content-grid">
        <header class="section-head compact recommend-heading">
          <div>
            <span class="section-kicker">RECOMMENDED FOR YOU</span>
            <h2>为您推荐</h2>
          </div>
          <button type="button" class="text-action" @click="go('/member/activities')">
            查看更多
            <el-icon><ArrowRight /></el-icon>
          </button>
        </header>

        <section class="recommend-section">
          <div v-if="recommendations.length" class="recommend-grid" v-loading="loading">
            <button
              v-for="item in recommendations"
              :key="`${item.type}-${item.id}`"
              type="button"
              class="recommend-card"
              :class="`recommend-${item.type}`"
              @click="handleRecommendation(item)"
            >
              <span class="recommend-cover">
                <img :src="item.cover" :alt="item.title" />
                <span class="recommend-tag">{{ item.tag }}</span>
              </span>
              <span class="recommend-info">
                <span class="recommend-type">{{ item.type === 'activity' ? '社区活动' : '健康体检' }}</span>
                <strong>{{ item.title }}</strong>
                <small>{{ item.desc }}</small>
                <span class="recommend-footer">
                  <em v-if="item.price != null">{{ item.price }} 积分</em>
                  <em v-else>{{ item.timeText || '欢迎报名参加' }}</em>
                  <span>查看详情 <el-icon><ArrowRight /></el-icon></span>
                </span>
              </span>
            </button>
          </div>

          <div v-else-if="!loading" class="recommend-empty">
            <div class="empty-icon"><el-icon><Flag /></el-icon></div>
            <strong>暂时没有新的推荐</strong>
            <p>新的体检套餐和社区活动会在这里出现。</p>
            <button type="button" @click="go('/member/activities')">去看看社区活动</button>
          </div>
        </section>

        <aside class="care-column">
          <section class="care-card daily-care">
            <div class="care-card-head">
              <span class="care-icon"><el-icon><FirstAidKit /></el-icon></span>
              <div>
                <span class="section-kicker">DAILY HEALTH</span>
                <h2>今日健康提醒</h2>
              </div>
            </div>
            <p class="care-message">{{ careMessage }}</p>
            <div class="care-meta">
              <span>{{ isTodayRecorded ? '今天已记录' : '今天尚未记录' }}</span>
              <button type="button" @click="go('/member/health')">
                {{ isTodayRecorded ? '查看健康档案' : '去记录' }}
                <el-icon><ArrowRight /></el-icon>
              </button>
            </div>
          </section>

          <section class="care-card tip-card">
            <div class="care-card-head">
              <span class="care-icon warm"><el-icon><Sunny /></el-icon></span>
              <div>
                <span class="section-kicker">A LITTLE REMINDER</span>
                <h2>生活小贴士</h2>
              </div>
            </div>
            <div class="tip-list">
              <div v-for="tip in healthTips" :key="tip.title" class="tip-row">
                <span class="tip-dot" :class="tip.theme"></span>
                <div>
                  <strong>{{ tip.title }}</strong>
                  <small>{{ tip.text }}</small>
                </div>
              </div>
            </div>
          </section>
        </aside>
      </div>

      <section class="morning-note">
        <span class="note-mark">“</span>
        <p>规律作息、按时体检、保持好心情，是送给自己最踏实的关怀。</p>
        <span>颐康云 · 温暖相伴</span>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Calendar,
  ChatDotRound,
  Coin,
  EditPen,
  FirstAidKit,
  Flag,
  Phone,
  Sunny,
  UserFilled
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { healthApi } from '@/api/health'
import { memberAppointmentApi } from '@/api/memberAppointment'
import { memberActivityApi } from '@/api/memberActivity'
import { buildActImage, buildPkgImage, formatDateTime } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const latestHealth = ref(null)
const nextAppointment = ref(null)
const recommendations = ref([])

const stats = reactive({
  appointments: 0,
  activities: 0,
  points: userStore.user?.points ?? 0,
  healthDays: 0
})

const todayLabel = new Intl.DateTimeFormat('zh-CN', {
  month: 'long',
  day: 'numeric',
  weekday: 'long'
}).format(new Date())

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了，注意休息'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const greetingMessage = computed(() => {
  const hour = new Date().getHours()
  if (hour < 11) return '今天也要照顾好自己'
  if (hour < 18) return '愿您今天过得舒心'
  return '辛苦了，记得早点休息'
})

const displayName = computed(() => userStore.displayName || '亲爱的朋友')

const healthState = computed(() => {
  const record = latestHealth.value
  if (!record || record.systolic == null || record.diastolic == null) {
    return { level: 'empty', label: '待记录' }
  }
  if (record.systolic >= 160 || record.diastolic >= 100) {
    return { level: 'attention', label: '建议关注' }
  }
  if (record.systolic >= 140 || record.diastolic >= 90) {
    return { level: 'attention', label: '稍偏高' }
  }
  if (record.systolic < 90 || record.diastolic < 60) {
    return { level: 'attention', label: '稍偏低' }
  }
  return { level: 'normal', label: '平稳' }
})

const isTodayRecorded = computed(() => {
  const recordedAt = latestHealth.value?.recorded_at
  if (!recordedAt) return false
  return String(recordedAt).slice(0, 10) === localDateString(new Date())
})

const careMessage = computed(() => {
  const record = latestHealth.value
  if (!record) {
    return '今天还没有健康记录。花一分钟记录血压和心率，能更清楚地了解身体变化。'
  }

  if (
    record.systolic >= 140 ||
    record.diastolic >= 90 ||
    record.heart_rate >= 100 ||
    record.heart_rate <= 55
  ) {
    return '最近一次记录中有指标需要多留意。请先安静休息后复测，如有持续不适及时联系医生。'
  }

  if (isTodayRecorded.value) {
    return '今天的健康记录已经完成，最近一次体征整体平稳。保持规律作息和适量活动。'
  }

  return '最近一次记录整体平稳。今天还没有打卡，记得抽空测量血压和心率。'
})

const quickLinks = [
  {
    title: '预约服务',
    desc: '查看体检套餐与可预约时段',
    icon: Calendar,
    theme: 'service-appointment',
    path: '/member/appointments'
  },
  {
    title: '社区活动',
    desc: '发现身边的兴趣与陪伴',
    icon: Flag,
    theme: 'service-activity',
    path: '/member/activities'
  },
  {
    title: '健康档案',
    desc: '记录体征，查看变化趋势',
    icon: FirstAidKit,
    theme: 'service-health',
    path: '/member/health'
  },
  {
    title: '健康评测',
    desc: '完成问卷，获得健康建议',
    icon: EditPen,
    theme: 'service-assessment',
    path: '/member/assessment'
  },
  {
    title: 'AI 健康助手',
    desc: '随时咨询日常健康问题',
    icon: ChatDotRound,
    theme: 'service-ai',
    path: '/member/chat'
  },
  {
    title: '个人中心',
    desc: '管理资料、积分与账户',
    icon: UserFilled,
    theme: 'service-profile',
    path: '/member/profile'
  }
]

const healthTips = [
  {
    title: '晨起先喝一杯温水',
    text: '少量多次补水，让身体慢慢醒来。',
    theme: 'sun'
  },
  {
    title: '饭后散步十五分钟',
    text: '步速以能正常交谈为宜。',
    theme: 'walk'
  },
  {
    title: '睡前放松心情',
    text: '减少看手机，保持卧室安静舒适。',
    theme: 'sleep'
  }
]

function localDateString(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function recordTimestamp(record) {
  return new Date(record.recorded_at || record.recordedAt || 0).getTime()
}

function isRegistrationOpen(activity) {
  const statusOk = activity.status === 'RECRUITING' || activity.status === 'REGISTRATION_OPEN'
  const now = Date.now()
  const start = activity.registrationStart ? new Date(activity.registrationStart).getTime() : 0
  const end = activity.registrationEnd ? new Date(activity.registrationEnd).getTime() : NaN
  return statusOk && !isNaN(end) && now >= start && now <= end
}

function activityTimeText(activity) {
  const time = formatDateTime(activity.activityStart)
  return time && time !== '-' ? time : '欢迎报名参加'
}

async function loadHomeData() {
  loading.value = true
  try {
    const [healthResult, appointmentResult, myActivityResult, activityResult, packageResult] =
      await Promise.allSettled([
        healthApi.history(12),
        memberAppointmentApi.listMyAppointments(),
        memberActivityApi.listMyActivities(),
        memberActivityApi.listActivities(),
        memberAppointmentApi.listPackages()
      ])

    if (healthResult.status === 'fulfilled') {
      const records = [...(healthResult.value.data || [])]
        .filter((record) => record.recorded_at)
        .sort((a, b) => recordTimestamp(b) - recordTimestamp(a))
      latestHealth.value = records[0] || null
      stats.healthDays = new Set(
        records.map((record) => String(record.recorded_at).slice(0, 10)).filter(Boolean)
      ).size
    }

    if (appointmentResult.status === 'fulfilled') {
      const appointments = appointmentResult.value.data || []
      stats.appointments = appointments.length
      nextAppointment.value =
        appointments.find((appointment) => appointment.status === 'CONFIRMED') || null
    }

    if (myActivityResult.status === 'fulfilled') {
      stats.activities = (myActivityResult.value.data || []).length
    }

    stats.points = userStore.user?.points ?? stats.points

    const activityCards =
      activityResult.status === 'fulfilled'
        ? (activityResult.value.data || [])
            .filter(isRegistrationOpen)
            .sort((a, b) => new Date(b.createTime || 0) - new Date(a.createTime || 0))
            .slice(0, 2)
            .map((activity) => ({
              id: activity.id,
              type: 'activity',
              tag: '报名中',
              title: activity.title,
              desc: (activity.content || '丰富社区生活，认识更多朋友')
                .replace(/<[^>]+>/g, '')
                .trim()
                .slice(0, 54),
              cover: buildActImage(activity),
              timeText: activityTimeText(activity)
            }))
        : []

    const packageCards =
      packageResult.status === 'fulfilled'
        ? (packageResult.value.data || [])
            .sort((a, b) => new Date(b.create_time || 0) - new Date(a.create_time || 0))
            .slice(0, 2)
            .map((pkg) => ({
              id: pkg.id,
              type: 'package',
              tag: '健康体检',
              title: pkg.name,
              desc:
                pkg.description ||
                (pkg.suitable_people ? `适合：${pkg.suitable_people}` : '专业体检服务，了解身体近况'),
              cover: buildPkgImage(pkg),
              price: pkg.price
            }))
        : []

    recommendations.value = []
    const maxLength = Math.max(activityCards.length, packageCards.length)
    for (let index = 0; index < maxLength; index += 1) {
      if (packageCards[index]) recommendations.value.push(packageCards[index])
      if (activityCards[index]) recommendations.value.push(activityCards[index])
    }
    recommendations.value = recommendations.value.slice(0, 3)
  } finally {
    loading.value = false
  }
}

function go(path) {
  router.push(path)
}

function handleRecommendation(item) {
  if (item.type === 'activity') {
    router.push({ path: '/member/activities', query: { actId: item.id } })
    return
  }
  router.push({ path: '/member/appointments', query: { pkgId: item.id } })
}

onMounted(loadHomeData)
</script>

<style scoped>
.home-root {
  --home-ink: #19352f;
  --home-muted: #6f827d;
  --home-line: #e8eeeb;
  --home-teal: #2f7d6d;
  --home-teal-dark: #1f6559;
  --home-cream: #f7f3eb;
  --home-gold: #d9963e;
  min-height: 100%;
  padding: 24px 32px 48px;
  color: var(--home-ink);
  background:
    radial-gradient(circle at 94% 4%, rgba(217, 150, 62, 0.09), transparent 24%),
    linear-gradient(180deg, #f8faf8 0%, #f4f7f4 100%);
}

.home-wrap {
  width: min(1320px, 100%);
  margin: 0 auto;
}

.welcome-section {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.17fr) minmax(360px, 0.83fr);
  gap: 42px;
  min-height: 350px;
  padding: 44px 48px;
  overflow: hidden;
  border-radius: 30px;
  color: #fff;
  background:
    radial-gradient(circle at 88% 2%, rgba(241, 203, 136, 0.23), transparent 30%),
    linear-gradient(135deg, #1f6155 0%, #286f61 54%, #327d6d 100%);
  box-shadow: 0 22px 54px rgba(31, 101, 89, 0.16);
  animation: home-in 0.55s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.welcome-section::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.038) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.038) 1px, transparent 1px);
  background-size: 40px 40px;
  mask-image: linear-gradient(90deg, #000, transparent 74%);
  -webkit-mask-image: linear-gradient(90deg, #000, transparent 74%);
}

.welcome-glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.welcome-glow-one {
  width: 280px;
  height: 280px;
  right: 22%;
  bottom: -190px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 54px rgba(255, 255, 255, 0.02);
}

.welcome-glow-two {
  width: 150px;
  height: 150px;
  right: -54px;
  top: -60px;
  background: rgba(244, 209, 151, 0.08);
}

.welcome-copy,
.wellness-card {
  position: relative;
  z-index: 1;
}

.welcome-copy {
  align-self: center;
}

.welcome-meta {
  display: flex;
  align-items: center;
  gap: 14px;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  color: #f9e5bd;
  background: rgba(255, 255, 255, 0.07);
}

.welcome-badge i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #8cdabd;
  box-shadow: 0 0 0 4px rgba(140, 218, 189, 0.12);
}

.welcome-date {
  font-size: 12px;
  color: rgba(240, 255, 250, 0.57);
}

.welcome-copy h1 {
  max-width: 660px;
  margin: 21px 0 13px;
  font-family: "STSong", "Songti SC", "Noto Serif SC", serif;
  font-size: clamp(34px, 3.6vw, 49px);
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: 1px;
}

.welcome-copy h1 span {
  display: block;
  margin-top: 4px;
  color: #f0d19a;
}

.welcome-copy > p {
  max-width: 570px;
  font-size: 14px;
  line-height: 1.85;
  color: rgba(242, 255, 251, 0.7);
}

.welcome-actions {
  display: flex;
  gap: 12px;
  margin-top: 26px;
}

.welcome-action {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 0 20px;
  border-radius: 13px;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.welcome-action.primary {
  border: 0;
  color: #214f46;
  background: #f2d7a6;
  box-shadow: 0 9px 24px rgba(8, 48, 40, 0.18);
}

.welcome-action.secondary {
  color: #f1fffb;
  border: 1px solid rgba(255, 255, 255, 0.22);
  background: rgba(255, 255, 255, 0.08);
}

.welcome-action:hover {
  transform: translateY(-2px);
}

.welcome-action.primary:hover {
  background: #f7dfb5;
  box-shadow: 0 12px 28px rgba(8, 48, 40, 0.23);
}

.welcome-action.secondary:hover {
  background: rgba(255, 255, 255, 0.13);
}

.hotline-link {
  width: fit-content;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 24px;
  color: inherit;
  text-decoration: none;
}

.hotline-icon {
  width: 35px;
  height: 35px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  color: #f1d19a;
  background: rgba(255, 255, 255, 0.08);
}

.hotline-link > span:nth-child(2) {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.hotline-link small {
  font-size: 10px;
  color: rgba(238, 255, 249, 0.5);
}

.hotline-link strong {
  margin-top: 2px;
  font-size: 12px;
  font-weight: 600;
  color: rgba(248, 255, 253, 0.86);
}

.hotline-arrow {
  font-size: 13px;
  color: rgba(241, 209, 154, 0.8);
  transition: transform 0.2s ease;
}

.hotline-link:hover .hotline-arrow {
  transform: translateX(3px);
}

.wellness-card {
  align-self: stretch;
  display: flex;
  flex-direction: column;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 23px;
  background: rgba(14, 62, 53, 0.46);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.1), 0 18px 36px rgba(8, 48, 40, 0.15);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.wellness-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.card-eyebrow,
.section-kicker {
  display: block;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2px;
}

.card-eyebrow {
  color: #e8c98f;
}

.wellness-head h2 {
  margin-top: 5px;
  font-size: 18px;
  font-weight: 700;
}

.sync-state {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 8px;
  border-radius: 999px;
  font-size: 9px;
  color: rgba(238, 255, 250, 0.7);
  background: rgba(255, 255, 255, 0.07);
}

.sync-state i {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #82d7b7;
}

.blood-pressure-block {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 22px;
  padding: 16px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.blood-pressure-block > div {
  display: flex;
  flex-direction: column;
}

.metric-label {
  font-size: 10px;
  color: rgba(240, 255, 250, 0.55);
}

.blood-pressure {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.5px;
}

.blood-pressure i {
  margin: 0 3px;
  font-style: normal;
  font-weight: 300;
  color: rgba(255, 255, 255, 0.4);
}

.blood-pressure small {
  margin-left: 6px;
  font-size: 9px;
  font-weight: 500;
  letter-spacing: 0;
  color: rgba(240, 255, 250, 0.52);
}

.health-state {
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 600;
}

.state-normal {
  color: #baf0d6;
  background: rgba(87, 197, 147, 0.16);
}

.state-attention {
  color: #ffe2ae;
  background: rgba(238, 176, 70, 0.17);
}

.state-empty {
  color: rgba(236, 255, 249, 0.62);
  background: rgba(255, 255, 255, 0.08);
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 13px;
}

.metric-item {
  display: flex;
  flex-direction: column;
  padding: 10px 11px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.055);
}

.metric-item span,
.metric-item small {
  font-size: 9px;
  color: rgba(240, 255, 250, 0.52);
}

.metric-item strong {
  margin: 4px 0 2px;
  font-size: 16px;
  font-weight: 650;
}

.appointment-reminder {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 11px;
  margin-top: auto;
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 14px;
  font: inherit;
  text-align: left;
  color: #f7fffd;
  background: rgba(255, 255, 255, 0.075);
  cursor: pointer;
  transition: background 0.2s ease;
}

.appointment-reminder:hover {
  background: rgba(255, 255, 255, 0.12);
}

.reminder-icon {
  width: 36px;
  height: 36px;
  flex: 0 0 36px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  color: #285f53;
  background: #ecd09d;
}

.reminder-copy {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.reminder-copy small {
  font-size: 9px;
  color: rgba(239, 255, 250, 0.5);
}

.reminder-copy strong {
  margin-top: 3px;
  overflow: hidden;
  font-size: 12px;
  font-weight: 650;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.reminder-copy em {
  margin-top: 2px;
  overflow: hidden;
  font-size: 9px;
  font-style: normal;
  color: rgba(239, 255, 250, 0.48);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.appointment-reminder > .el-icon {
  font-size: 13px;
  color: rgba(240, 255, 250, 0.55);
}

.summary-strip {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0;
  margin: -24px 22px 0;
  overflow: hidden;
  border: 1px solid rgba(223, 233, 228, 0.95);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 13px 32px rgba(42, 77, 67, 0.09);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.summary-item {
  min-height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 11px;
  padding: 16px;
  border: 0;
  border-right: 1px solid #edf2ef;
  font: inherit;
  text-align: left;
  color: var(--home-ink);
  background: transparent;
  cursor: pointer;
  transition: background 0.2s ease;
}

.summary-item:last-child {
  border-right: 0;
}

.summary-item:hover {
  background: #f7faf8;
}

.summary-icon {
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  display: grid;
  place-items: center;
  border-radius: 13px;
  font-size: 18px;
}

.summary-icon.appointment {
  color: #326a8b;
  background: #e8f2f8;
}

.summary-icon.activity {
  color: #327867;
  background: #e9f4ef;
}

.summary-icon.points {
  color: #a66e21;
  background: #fbf1df;
}

.summary-icon.health {
  color: #805b92;
  background: #f3edf7;
}

.summary-item > span:last-child {
  display: flex;
  flex-direction: column;
}

.summary-item strong {
  font-size: 22px;
  font-weight: 750;
  line-height: 1.1;
}

.summary-item small {
  margin-top: 4px;
  font-size: 10px;
  color: var(--home-muted);
}

.section-block {
  margin-top: 38px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.section-head.compact {
  align-items: center;
}

.section-kicker {
  color: #8c9c97;
}

.section-head h2 {
  margin-top: 6px;
  font-family: "STSong", "Songti SC", "Noto Serif SC", serif;
  font-size: 25px;
  line-height: 1.2;
  letter-spacing: 0.5px;
  color: var(--home-ink);
}

.section-head > p {
  font-size: 12px;
  color: var(--home-muted);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
}

.service-card {
  position: relative;
  min-width: 0;
  min-height: 164px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  padding: 18px 17px;
  border: 1px solid #e5ece8;
  border-radius: 18px;
  font: inherit;
  text-align: left;
  color: var(--home-ink);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 5px 16px rgba(41, 73, 64, 0.035);
  cursor: pointer;
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.service-card:hover {
  transform: translateY(-4px);
  border-color: #cbded7;
  box-shadow: 0 14px 28px rgba(41, 73, 64, 0.09);
}

.service-icon {
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  display: grid;
  place-items: center;
  border-radius: 15px;
  font-size: 22px;
}

.service-appointment .service-icon {
  color: #376d8b;
  background: #e9f2f7;
}

.service-activity .service-icon {
  color: #2d7761;
  background: #e8f4ef;
}

.service-health .service-icon {
  color: #a16b24;
  background: #fbf0dc;
}

.service-assessment .service-icon {
  color: #7b588c;
  background: #f2ecf6;
}

.service-ai .service-icon {
  color: #42756f;
  background: #e7f2ef;
}

.service-profile .service-icon {
  color: #8d5b4f;
  background: #f7ece8;
}

.service-content {
  min-width: 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  margin-top: auto;
}

.service-content strong {
  font-size: 15px;
  font-weight: 700;
}

.service-content small {
  margin-top: 5px;
  overflow: hidden;
  font-size: 10px;
  color: var(--home-muted);
  white-space: nowrap;
  text-overflow: ellipsis;
}

.service-arrow {
  position: absolute;
  top: 19px;
  right: 16px;
  color: #a3b0ac;
  transition: transform 0.2s ease, color 0.2s ease;
}

.service-card:hover .service-arrow {
  color: var(--home-teal);
  transform: translateX(3px);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(310px, 0.75fr);
  grid-template-areas:
    "recommend-head ."
    "recommend-body care";
  column-gap: 18px;
  align-items: stretch;
  margin-top: 38px;
}

.recommend-heading {
  grid-area: recommend-head;
  margin: 0 0 18px;
}

.recommend-section {
  grid-area: recommend-body;
  min-width: 0;
  min-height: 100%;
  display: flex;
}

.text-action {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 0;
  border: 0;
  font: inherit;
  font-size: 11px;
  font-weight: 600;
  color: var(--home-teal);
  background: transparent;
  cursor: pointer;
}

.text-action:hover {
  color: var(--home-teal-dark);
}

.recommend-grid {
  min-height: 210px;
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  align-items: stretch;
}

.recommend-card {
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #e4ebe7;
  border-radius: 18px;
  padding: 0;
  text-align: left;
  color: var(--home-ink);
  background: #fff;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.recommend-card:hover {
  transform: translateY(-3px);
  border-color: #caddd6;
  box-shadow: 0 13px 28px rgba(41, 73, 64, 0.09);
}

.recommend-cover {
  position: relative;
  display: block;
  width: 100%;
  height: 158px;
  min-height: 158px;
  overflow: hidden;
  background: #eef4f1;
}

.recommend-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.recommend-card:hover .recommend-cover img {
  transform: scale(1.035);
}

.recommend-tag {
  position: absolute;
  left: 10px;
  top: 10px;
  padding: 5px 9px;
  border-radius: 999px;
  font-size: 9px;
  font-weight: 700;
  color: #295d51;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
}

.recommend-info {
  min-width: 0;
  min-height: 155px;
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 16px 17px 15px;
}

.recommend-type {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.7px;
  color: var(--home-teal);
}

.recommend-info > strong {
  display: -webkit-box;
  margin-top: 7px;
  overflow: hidden;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.recommend-info > small {
  display: -webkit-box;
  margin-top: 7px;
  overflow: hidden;
  font-size: 10px;
  line-height: 1.65;
  color: var(--home-muted);
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.recommend-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 7px;
  margin-top: auto;
  padding-top: 13px;
}

.recommend-footer em {
  overflow: hidden;
  font-size: 10px;
  font-style: normal;
  color: #a16b24;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.recommend-footer > span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  flex-shrink: 0;
  font-size: 10px;
  font-weight: 600;
  color: var(--home-teal);
}

.recommend-empty {
  min-height: 230px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px dashed #dbe5e0;
  border-radius: 18px;
  text-align: center;
  background: rgba(255, 255, 255, 0.62);
}

.empty-icon {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  margin-bottom: 11px;
  border-radius: 15px;
  font-size: 22px;
  color: var(--home-teal);
  background: #edf6f2;
}

.recommend-empty strong {
  font-size: 14px;
}

.recommend-empty p {
  margin-top: 6px;
  font-size: 11px;
  color: var(--home-muted);
}

.recommend-empty button {
  margin-top: 15px;
  padding: 8px 14px;
  border: 1px solid #cbded7;
  border-radius: 999px;
  font: inherit;
  font-size: 11px;
  color: var(--home-teal);
  background: #fff;
  cursor: pointer;
}

.care-column {
  grid-area: care;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.care-card {
  padding: 20px;
  border: 1px solid #e2eae6;
  border-radius: 19px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 7px 20px rgba(41, 73, 64, 0.045);
}

.care-card-head {
  display: flex;
  align-items: center;
  gap: 11px;
}

.care-card-head h2 {
  margin-top: 4px;
  font-size: 15px;
  font-weight: 700;
}

.care-icon {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  display: grid;
  place-items: center;
  border-radius: 12px;
  color: #2f7d6d;
  background: #eaf4f0;
}

.care-icon.warm {
  color: #9c6a22;
  background: #fbf0dc;
}

.care-message {
  margin-top: 16px;
  font-size: 12px;
  line-height: 1.8;
  color: #5f736d;
}

.care-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 17px;
  padding-top: 13px;
  border-top: 1px solid #edf2ef;
}

.care-meta > span {
  font-size: 9px;
  color: #8c9b97;
}

.care-meta button {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 0;
  border: 0;
  font: inherit;
  font-size: 10px;
  font-weight: 700;
  color: var(--home-teal);
  background: transparent;
  cursor: pointer;
}

.tip-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 17px;
}

.tip-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.tip-dot {
  width: 8px;
  height: 8px;
  flex: 0 0 8px;
  margin-top: 5px;
  border-radius: 50%;
  background: #d9a64d;
  box-shadow: 0 0 0 4px rgba(217, 166, 77, 0.1);
}

.tip-dot.walk {
  background: #6fa894;
  box-shadow: 0 0 0 4px rgba(111, 168, 148, 0.1);
}

.tip-dot.sleep {
  background: #8a7bb0;
  box-shadow: 0 0 0 4px rgba(138, 123, 176, 0.1);
}

.tip-row div {
  display: flex;
  flex-direction: column;
}

.tip-row strong {
  font-size: 11px;
  font-weight: 650;
  color: #344d47;
}

.tip-row small {
  margin-top: 3px;
  font-size: 9px;
  line-height: 1.55;
  color: #83938f;
}

.morning-note {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 35px;
  padding: 22px;
  overflow: hidden;
  border: 1px solid #ece4d6;
  border-radius: 18px;
  text-align: center;
  background: linear-gradient(120deg, #fcf8ef, #f7f8f3);
}

.note-mark {
  position: absolute;
  left: 28px;
  top: 5px;
  font-family: Georgia, serif;
  font-size: 68px;
  line-height: 1;
  color: rgba(217, 150, 62, 0.12);
}

.morning-note p {
  font-family: "STSong", "Songti SC", "Noto Serif SC", serif;
  font-size: 15px;
  color: #4b625c;
}

.morning-note > span:last-child {
  font-size: 9px;
  letter-spacing: 1px;
  color: #9b8d76;
}

@keyframes home-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1120px) {
  .home-root {
    padding-inline: 22px;
  }

  .welcome-section {
    grid-template-columns: minmax(0, 1fr) minmax(335px, 0.82fr);
    gap: 24px;
    padding: 38px 36px;
  }

  .service-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .content-grid {
    grid-template-columns: 1fr;
    grid-template-areas:
      "recommend-head"
      "recommend-body"
      "care";
    row-gap: 0;
  }

  .care-column {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    margin-top: 18px;
  }
}

@media (max-width: 860px) {
  .welcome-section {
    grid-template-columns: 1fr;
    padding: 34px;
  }

  .welcome-copy h1 {
    max-width: none;
  }

  .wellness-card {
    max-width: none;
  }

  .summary-strip {
    margin-inline: 12px;
  }
}

@media (max-width: 640px) {
  .home-root {
    padding: 12px 13px 26px;
  }

  .welcome-section {
    gap: 26px;
    padding: 27px 23px 24px;
    border-radius: 23px;
  }

  .welcome-meta {
    align-items: flex-start;
    flex-direction: column;
    gap: 7px;
  }

  .welcome-copy h1 {
    margin-top: 17px;
    font-size: 31px;
    line-height: 1.3;
  }

  .welcome-copy > p {
    font-size: 13px;
  }

  .welcome-actions {
    margin-top: 21px;
  }

  .welcome-action {
    min-height: 46px;
    flex: 1;
    padding: 0 12px;
    font-size: 13px;
  }

  .hotline-link {
    margin-top: 20px;
  }

  .wellness-card {
    padding: 19px;
    border-radius: 19px;
  }

  .blood-pressure {
    font-size: 25px;
  }

  .metric-item {
    padding: 9px;
  }

  .metric-item strong {
    font-size: 15px;
  }

  .summary-strip {
    grid-template-columns: repeat(2, 1fr);
    margin: -18px 8px 0;
  }

  .summary-item {
    min-height: 76px;
    justify-content: flex-start;
    padding: 12px;
  }

  .summary-item:nth-child(2) {
    border-right: 0;
  }

  .summary-item:nth-child(-n + 2) {
    border-bottom: 1px solid #edf2ef;
  }

  .summary-icon {
    width: 35px;
    height: 35px;
    flex-basis: 35px;
    border-radius: 11px;
    font-size: 16px;
  }

  .summary-item strong {
    font-size: 19px;
  }

  .section-block {
    margin-top: 28px;
  }

  .section-head {
    align-items: flex-start;
    margin-bottom: 14px;
  }

  .section-head > p {
    display: none;
  }

  .section-head h2 {
    font-size: 22px;
  }

  .service-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .service-card {
    min-height: 150px;
    gap: 10px;
    padding: 14px;
  }

  .service-icon {
    width: 43px;
    height: 43px;
    flex-basis: 43px;
    border-radius: 13px;
    font-size: 19px;
  }

  .service-content {
    width: 100%;
  }

  .service-content strong {
    font-size: 14px;
  }

  .service-content small {
    white-space: normal;
    line-height: 1.5;
  }

  .service-arrow {
    right: 14px;
    top: 15px;
  }

  .service-content {
    padding-right: 18px;
  }

  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .recommend-card {
    display: grid;
    grid-template-columns: 108px minmax(0, 1fr);
  }

  .recommend-cover {
    width: 108px;
    min-height: 153px;
  }

  .recommend-info {
    min-height: 153px;
  }

  .recommend-info > strong {
    font-size: 16px;
  }

  .care-column {
    grid-template-columns: 1fr;
  }

  .morning-note {
    align-items: flex-start;
    flex-direction: column;
    gap: 7px;
    margin-top: 25px;
    padding: 20px 20px 20px 24px;
    text-align: left;
  }

  .note-mark {
    left: 12px;
  }

  .morning-note p {
    font-size: 14px;
    line-height: 1.65;
  }
}

@media (max-width: 390px) {
  .summary-item {
    gap: 8px;
    padding: 10px;
  }

  .summary-icon {
    display: none;
  }

  .welcome-action {
    font-size: 12px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .welcome-section {
    animation: none;
  }

  .recommend-cover img,
  .service-card,
  .recommend-card {
    transition: none;
  }
}
</style>
