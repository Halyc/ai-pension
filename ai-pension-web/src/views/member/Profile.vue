<template>
  <div class="page-container profile-page" v-loading="pageLoading">
    <!-- 顶部个人信息卡片 -->
    <div class="hero-card">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <div class="avatar-section">
          <div class="avatar-wrapper" @click="triggerAvatarUpload">
            <el-avatar :size="96" class="hero-avatar" :src="form.avatar || profileAvatar(profile) || ''">
              {{ avatarText }}
            </el-avatar>
            <div class="avatar-overlay">
              <el-icon :size="20"><Camera /></el-icon>
            </div>
            <input
              ref="avatarFileInput"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              style="display:none"
              @change="onAvatarFileChange"
            />
          </div>
          <el-tag :type="dictType(levelMap, profile.member_level)" effect="dark" round class="level-tag">
            {{ dictLabel(levelMap, profile.member_level) }}
          </el-tag>
        </div>
        <div class="info-section">
          <div class="user-name">{{ profile.real_name || '未命名用户' }}</div>
          <div class="user-phone">
            <el-icon><Phone /></el-icon>
            {{ profile.phone }}
          </div>
          <div class="user-register">
            <el-icon><Calendar /></el-icon>
            注册于 {{ formatDateTime(profile.create_time) }}
          </div>
        </div>
        <div class="stats-section">
          <div class="stat-card stat-points">
            <div class="stat-icon-wrap">
              <el-icon :size="22"><Coin /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ displayPoints }}</div>
              <div class="stat-label">我的积分</div>
            </div>
          </div>
          <div class="stat-card stat-appointment">
            <div class="stat-icon-wrap">
              <el-icon :size="22"><Tickets /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ appointmentCount }}</div>
              <div class="stat-label">预约记录</div>
            </div>
          </div>
          <div class="stat-card stat-activity">
            <div class="stat-icon-wrap">
              <el-icon :size="22"><Calendar /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ activityCount }}</div>
              <div class="stat-label">活动报名</div>
            </div>
          </div>
          <div class="stat-card stat-score">
            <div class="stat-icon-wrap">
              <el-icon :size="22"><DataAnalysis /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ assessmentCount }}</div>
              <div class="stat-label">评测次数</div>
            </div>
          </div>
          <div class="stat-card stat-health">
            <div class="stat-icon-wrap">
              <el-icon :size="22"><FirstAidKit /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ healthCount }}</div>
              <div class="stat-label">健康记录</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Tab 内容区 -->
    <div class="content-card">
      <el-tabs v-model="activeTab" class="profile-tabs">
        <!-- 基本资料 -->
        <el-tab-pane name="info">
          <template #label>
            <span class="tab-label"><el-icon><User /></el-icon>基本资料</span>
          </template>
          <div class="tab-content">
            <el-form ref="profileFormRef" :model="form" label-width="100px" class="profile-form" v-loading="loading">
              <el-form-item label="手机号">
                <el-input v-model="form.phone" placeholder="手机号" maxlength="11" />
              </el-form-item>
              <el-form-item label="姓名">
                <el-input v-model="form.real_name" placeholder="真实姓名" maxlength="20" />
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="form.gender">
                  <el-radio value="男">男</el-radio>
                  <el-radio value="女">女</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="出生日期">
                <el-date-picker
                  v-model="form.birthDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择出生日期"
                  style="width:100%"
                />
              </el-form-item>
              <el-form-item>
                <el-button class="btn-save" :icon="Check" :loading="saving" @click="saveProfile">
                  保存修改
                </el-button>
                <el-button class="btn-change-pwd" :icon="Lock" @click="pwdDialogVisible = true">
                  修改密码
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-tab-pane>

        <!-- 我的预约 -->
        <el-tab-pane name="appointments">
          <template #label>
            <span class="tab-label"><el-icon><Tickets /></el-icon>我的预约</span>
          </template>
          <div class="tab-content">
            <div class="mini-toolbar">
              <el-button size="small" :icon="Refresh" @click="loadMyAppointments" :loading="mineLoading">刷新</el-button>
            </div>
            <el-table :data="myAppointments" v-loading="mineLoading" stripe size="default">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column label="套餐" min-width="180">
                <template #default="{ row }">
                  <div class="mine-pkg">
                    <el-image :src="pkgImg(safePkg(row) || { name: pkgName(row) })" fit="cover" class="mine-cover" />
                    <span>{{ pkgName(row) }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="价格" width="110">
                <template #default="{ row }">
                  <span class="mono">{{ safePkg(row).price ?? 0 }} 积分</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="dictType(appointmentStatusMap, row.status)" size="small">
                    {{ dictLabel(appointmentStatusMap, row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="体检报告" width="140">
                <template #default="{ row }">
                  <div v-if="row.status === 'COMPLETED'">
                    <el-link v-if="row.reportUrl" type="primary" :underline="false" @click="openReport(row.reportUrl)">
                      <el-icon><View /></el-icon> 查看报告
                    </el-link>
                    <span v-else class="text-sub">暂无报告</span>
                  </div>
                  <span v-else class="text-sub">-</span>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="预约时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="140">
                <template #default="{ row }">
                  <el-button
                    v-if="row.status === 'CONFIRMED'"
                    link
                    type="danger"
                    size="small"
                    @click="cancelBook(row)"
                  >取消预约</el-button>
                  <span v-else class="text-sub">-</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!mineLoading && !myAppointments.length" description="暂无预约记录" />
          </div>
        </el-tab-pane>

        <!-- 我的报名 -->
        <el-tab-pane name="activities">
          <template #label>
            <span class="tab-label"><el-icon><Calendar /></el-icon>我的报名</span>
          </template>
          <div class="tab-content">
            <div class="mini-toolbar">
              <el-button size="small" :icon="Refresh" @click="loadMine" :loading="actMineLoading">刷新</el-button>
            </div>
            <div v-if="actMineLoading" class="simple-loading">加载中...</div>
            <div v-else-if="!mineActivities.length" class="simple-empty">还没有报名任何活动</div>
            <div v-else class="activity-mine-list">
              <div v-for="row in mineActivities" :key="row.registrationId" class="mine-item">
                <img :src="actImg(safeActivity(row) || { title: activityTitle(row) })" class="mine-cover-img" />
                <div class="mine-info">
                  <div class="mine-title">{{ activityTitle(row) }}</div>
                  <div class="mine-time">
                    <el-icon><Clock /></el-icon>
                    {{ formatDateTime(safeActivity(row).activityStart) }} ~ {{ formatDateTime(safeActivity(row).activityEnd) }}
                  </div>
                  <div class="mine-meta">
                    <span>报名ID: {{ row.registrationId }}</span>
                    <el-tag size="small" :type="dictType(checkInStatusMap, row.checkInStatus)">
                      {{ dictLabel(checkInStatusMap, row.checkInStatus) }}
                    </el-tag>
                  </div>
                </div>
                <div class="mine-actions">
                  <el-button
                    size="small"
                    type="danger"
                    plain
                    :disabled="row.checkInStatus === 'CHECKED_IN'"
                    @click="cancelMine(row)"
                  >取消报名</el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 我的评测记录 -->
        <el-tab-pane name="assessments">
          <template #label>
            <span class="tab-label"><el-icon><DataAnalysis /></el-icon>评测记录</span>
          </template>
          <div class="tab-content">
            <div class="mini-toolbar">
              <el-button size="small" :icon="Refresh" @click="loadAssessmentHistory" :loading="historyLoading">刷新</el-button>
            </div>
            <el-table :data="assessmentHistory" v-loading="historyLoading" stripe size="default">
              <el-table-column prop="id" label="ID" width="70" />
              <el-table-column prop="questionnaireTitle" label="问卷" min-width="150" />
              <el-table-column prop="aiScore" label="AI评分" width="140">
                <template #default="{ row }">
                  <template v-if="getScoreInfo(row.aiScore)">
                    <span
                      class="score-badge"
                      :style="{
                        color: getScoreInfo(row.aiScore).color,
                        background: getScoreInfo(row.aiScore).bg,
                        borderColor: getScoreInfo(row.aiScore).border
                      }"
                    >{{ getScoreInfo(row.aiScore).score }}分 · {{ getScoreInfo(row.aiScore).level }}</span>
                  </template>
                  <el-tag v-else type="info" size="small">分析中</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="提交时间" min-width="170">
                <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="140">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" :icon="View" @click="showAssessmentDetail(row)">
                    查看报告
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!historyLoading && !assessmentHistory.length" description="暂无评测记录" />
          </div>
        </el-tab-pane>

        <!-- 健康历史记录 -->
        <el-tab-pane name="health">
          <template #label>
            <span class="tab-label"><el-icon><FirstAidKit /></el-icon>健康记录</span>
          </template>
          <div class="tab-content">
            <div class="mini-toolbar">
              <el-radio-group v-model="healthMonths" size="small" @change="loadHealthHistory">
                <el-radio-button :value="3">近3月</el-radio-button>
                <el-radio-button :value="6">近6月</el-radio-button>
                <el-radio-button :value="12">近1年</el-radio-button>
              </el-radio-group>
              <el-button size="small" :icon="Refresh" @click="loadHealthHistory" :loading="healthLoading">刷新</el-button>
            </div>
            <el-table :data="healthHistory" v-loading="healthLoading" stripe size="default" max-height="500">
              <el-table-column prop="recorded_at" label="记录时间" min-width="160">
                <template #default="{ row }">{{ formatDateTime(row.recorded_at) }}</template>
              </el-table-column>
              <el-table-column label="血压(mmHg)" width="140">
                <template #default="{ row }">
                  <span class="mono">{{ row.systolic ?? '-' }} / {{ row.diastolic ?? '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="血糖(mmol/L)" width="130">
                <template #default="{ row }"><span class="mono">{{ row.blood_sugar ?? '-' }}</span></template>
              </el-table-column>
              <el-table-column label="心率(次/分)" width="130">
                <template #default="{ row }"><span class="mono">{{ row.heart_rate ?? '-' }}</span></template>
              </el-table-column>
              <el-table-column label="体重(kg)" width="110">
                <template #default="{ row }"><span class="mono">{{ row.weight ?? '-' }}</span></template>
              </el-table-column>
              <el-table-column label="BMI" width="90">
                <template #default="{ row }">
                  <el-tag :type="bmiType(row.bmi)" size="small">{{ row.bmi ?? '-' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="memo" label="备注" min-width="120">
                <template #default="{ row }">{{ row.memo || '-' }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!healthLoading && !healthHistory.length" description="暂无健康记录" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdDialogVisible" title="修改登录密码" width="460px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-16位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Key" :loading="pwdSaving" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 评测报告弹窗 -->
    <el-dialog v-model="assessmentReport.visible" title="评测报告" width="680px" top="6vh">
      <template v-if="assessmentReport.data">
        <el-descriptions :column="2" border class="mb">
          <el-descriptions-item label="问卷" :span="2">{{ assessmentReport.data.questionnaireTitle }}</el-descriptions-item>
          <el-descriptions-item label="AI评分">
            <template v-if="getScoreInfo(assessmentReport.data.aiScore)">
              <span
                class="score-badge"
                :style="{
                  color: getScoreInfo(assessmentReport.data.aiScore).color,
                  background: getScoreInfo(assessmentReport.data.aiScore).bg,
                  borderColor: getScoreInfo(assessmentReport.data.aiScore).border
                }"
              >{{ getScoreInfo(assessmentReport.data.aiScore).score }}分 · {{ getScoreInfo(assessmentReport.data.aiScore).level }}</span>
            </template>
            <el-tag v-else type="info" size="small">分析中</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDateTime(assessmentReport.data.createTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">答题详情</div>
        <div class="report-answers">
          <div v-for="(q, idx) in assessmentReport.data.questions" :key="q.id" class="report-item">
            <div class="report-q">{{ idx + 1 }}. {{ q.content }}</div>
            <div class="report-a">
              <el-tag
                v-for="(opt, i) in parseOptions(q.options)"
                :key="i"
                size="small"
                class="opt-tag"
                :type="isPicked(assessmentReport.data.answers, q.id, opt) ? 'success' : 'info'"
                :effect="isPicked(assessmentReport.data.answers, q.id, opt) ? 'dark' : 'plain'"
              >{{ opt }}</el-tag>
              <div v-if="q.type === 'TEXT'" class="text-answer">
                {{ getAnswer(assessmentReport.data.answers, q.id) || '（未作答）' }}
              </div>
              <div v-if="!parseOptions(q.options).length && q.type !== 'TEXT'" class="text-answer">
                {{ getAnswer(assessmentReport.data.answers, q.id) || '（未作答）' }}
              </div>
            </div>
          </div>
        </div>

        <div class="detail-section-title">AI 健康建议</div>
        <el-alert
          :title="replaceSalutation(assessmentReport.data.aiSuggestion) || 'AI 正在分析您的评测结果，请稍后再来查看'"
          type="success"
          :closable="false"
          show-icon
        />
      </template>
      <template #footer>
        <el-button @click="assessmentReport.visible = false">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 报告图片预览 -->
    <el-image-viewer
      v-if="reportViewer.visible"
      :url-list="[reportViewer.url]"
      :initial-index="0"
      @close="reportViewer.visible = false"
    />

    <!-- 头像裁剪弹窗 -->
    <el-dialog
      v-model="cropDialog.visible"
      title="裁剪图片"
      width="480px"
      :close-on-click-modal="false"
      :show-close="true"
      center
      destroy-on-close
      @close="resetCrop"
    >
      <div class="crop-wrapper">
        <div
          ref="cropStageRef"
          class="crop-stage"
          @mousedown="onCropStart"
          @wheel.prevent="onCropWheel"
        >
          <img
            v-if="cropDialog.imgSrc"
            ref="cropImgRef"
            class="crop-image"
            :src="cropDialog.imgSrc"
            :style="cropImageStyle"
            @load="onCropImageLoad"
            draggable="false"
          />
          <div class="crop-mask">
            <div class="crop-circle"></div>
            <div class="crop-grid-v crop-grid-v1"></div>
            <div class="crop-grid-v crop-grid-v2"></div>
            <div class="crop-grid-h crop-grid-h1"></div>
            <div class="crop-grid-h crop-grid-h2"></div>
          </div>
        </div>

        <div class="crop-toolbar">
          <el-tooltip content="缩小" placement="top">
            <button class="crop-btn" @click="zoomCrop(-0.15)">
              <el-icon :size="18"><ZoomOut /></el-icon>
            </button>
          </el-tooltip>
          <el-slider
            :model-value="cropDialog.scale"
            :min="cropMinScale"
            :max="cropMaxScale"
            :step="0.01"
            :show-tooltip="false"
            class="crop-slider"
            @update:model-value="onScaleChange"
          />
          <el-tooltip content="放大" placement="top">
            <button class="crop-btn" @click="zoomCrop(0.15)">
              <el-icon :size="18"><ZoomIn /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </div>
      <template #footer>
        <el-button @click="cropDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="cropSaving" @click="confirmCrop">保 存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Lock, Check, Key, Camera, Phone, Calendar, Coin, Tickets, Clock,
  DataAnalysis, FirstAidKit, User, Refresh, View, ZoomIn, ZoomOut
} from '@element-plus/icons-vue'
import { profileApi } from '@/api/profile'
import { uploadApi } from '@/api/upload'
import { memberAppointmentApi } from '@/api/memberAppointment'
import { memberActivityApi } from '@/api/memberActivity'
import { memberAssessmentApi } from '@/api/memberAssessment'
import { healthApi } from '@/api/health'
import { useUserStore } from '@/store/user'
import {
  formatDateTime,
  dictLabel,
  dictType,
  levelMap,
  userStatusMap,
  appointmentStatusMap,
  checkInStatusMap,
  buildAvatar,
  buildPkgImage,
  buildActImage,
  getScoreInfo,
  parseOptions,
  parseJsonSafe,
  replaceSalutation as _replaceSalutation
} from '@/utils/format'

const userStore = useUserStore()

function replaceSalutation(text) {
  return _replaceSalutation(text, userStore.user)
}

const pageLoading = ref(false)
const loading = ref(false)
const saving = ref(false)
const pwdSaving = ref(false)
const profileFormRef = ref()
const pwdFormRef = ref()
const pwdDialogVisible = ref(false)
const avatarFileInput = ref()
const activeTab = ref('info')

const imgRefreshTick = ref(0)
function profileAvatar(row) { imgRefreshTick.value; return buildAvatar(row) }
function pkgImg(row) { imgRefreshTick.value; return buildPkgImage(row) }
function actImg(row) { imgRefreshTick.value; return buildActImage(row) }
window.addEventListener('image-cache-ready', () => { imgRefreshTick.value++ })
window.addEventListener('user-avatar-updated', () => { imgRefreshTick.value++ })

const profile = reactive({})
const form = reactive({
  phone: '',
  real_name: '',
  gender: '男',
  birthDate: '',
  avatar: ''
})

const avatarText = computed(() =>
  (profile.real_name || profile.phone || 'U').charAt(0).toUpperCase()
)

const displayPoints = computed(() => {
  const fromStore = userStore.user?.points
  if (fromStore != null) return fromStore
  return profile.points ?? 0
})

// ---------- 我的预约 ----------
const myAppointments = ref([])
const mineLoading = ref(false)
const appointmentCount = computed(() => myAppointments.value.length)

async function loadMyAppointments() {
  mineLoading.value = true
  try {
    const res = await memberAppointmentApi.listMyAppointments()
    myAppointments.value = res.data || []
  } catch (e) {}
  finally { mineLoading.value = false }
}

const reportViewer = reactive({ visible: false, url: '' })
function openReport(url) {
  reportViewer.url = url
  reportViewer.visible = true
}

async function cancelBook(row) {
  try {
    await ElMessageBox.confirm(
      `确定取消预约「${pkgName(row)}」吗？取消后积分将自动返还。`,
      '取消预约',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' }
    )
    await memberAppointmentApi.cancelAppointment(row.id)
    ElMessage.success('取消成功，积分已返还')
    const refund = safePkg(row).price || 0
    userStore.setUser({ ...userStore.user, points: (userStore.user?.points || 0) + refund })
    loadMyAppointments()
  } catch (e) {}
}

// ---------- 我的报名 ----------
const mineActivities = ref([])
const actMineLoading = ref(false)
const activityCount = computed(() => mineActivities.value.length)

async function loadMine() {
  actMineLoading.value = true
  try {
    const res = await memberActivityApi.listMyActivities()
    mineActivities.value = res.data || []
  } catch (e) {}
  finally { actMineLoading.value = false }
}

function safeActivity(row) {
  const act = row.activity
  if (!act) return {}
  if (typeof act === 'string') {
    try { return JSON.parse(act) } catch { return {} }
  }
  if (typeof act === 'object') return act
  return {}
}

function safePkg(row) {
  const p = row.package
  if (!p) return {}
  if (typeof p === 'string') {
    try { return JSON.parse(p) } catch { return {} }
  }
  if (typeof p === 'object') return p
  return {}
}

function pkgName(row) {
  const p = safePkg(row)
  const n = p.name
  if (typeof n === 'string' && n.trim()) return n
  return '体检套餐'
}

function activityTitle(row) {
  const act = safeActivity(row)
  const t = act.title
  if (typeof t === 'string' && t.trim()) return t
  return `活动 #${row.activityId}`
}

async function cancelMine(row) {
  try {
    await ElMessageBox.confirm(
      `确认取消活动「${activityTitle(row)}」的报名吗？此操作不可撤销。`,
      '取消报名',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' }
    )
    await memberActivityApi.cancelRegistration(row.activityId)
    ElMessage.success('取消报名成功')
    loadMine()
  } catch (e) {}
}

// ---------- 我的评测记录 ----------
const assessmentHistory = ref([])
const historyLoading = ref(false)
const assessmentCount = computed(() => assessmentHistory.value.length)
const assessmentReport = reactive({ visible: false, data: null })

async function loadAssessmentHistory() {
  historyLoading.value = true
  try {
    const res = await memberAssessmentApi.history()
    assessmentHistory.value = res.data || []
  } catch (e) {}
  finally { historyLoading.value = false }
}

async function showAssessmentDetail(row) {
  assessmentReport.visible = true
  assessmentReport.data = null
  try {
    const res = await memberAssessmentApi.detail(row.id)
    assessmentReport.data = res.data
  } catch (e) {
    assessmentReport.visible = false
  }
}

function getAnswer(answers, questionId) {
  const ans = parseJsonSafe(answers, {})
  const v = ans?.[questionId]
  return Array.isArray(v) ? v.join('、') : v
}

function isPicked(answers, questionId, option) {
  const ans = parseJsonSafe(answers, {})
  const v = ans?.[questionId]
  if (Array.isArray(v)) return v.includes(option)
  return v === option
}

// ---------- 健康历史记录 ----------
const healthHistory = ref([])
const healthLoading = ref(false)
const healthMonths = ref(3)
const healthCount = computed(() => healthHistory.value.length)

function bmiType(bmi) {
  if (bmi == null) return 'info'
  if (bmi < 18.5 || bmi >= 28) return 'danger'
  if (bmi >= 24) return 'warning'
  return 'success'
}

async function loadHealthHistory() {
  healthLoading.value = true
  try {
    const res = await healthApi.history(healthMonths.value)
    healthHistory.value = res.data || []
  } catch (e) {}
  finally { healthLoading.value = false }
}

// ---------- 个人资料 ----------
async function loadProfile() {
  loading.value = true
  try {
    const res = await profileApi.getProfile()
    Object.assign(profile, res.data)
    if (res.data.points != null) {
      userStore.setUser({ ...userStore.user, points: res.data.points })
    }
    form.phone = res.data.phone || ''
    form.real_name = res.data.real_name || ''
    form.gender = res.data.gender || '男'
    form.birthDate = res.data.birth_date ? String(res.data.birth_date).slice(0, 10) : ''
    form.avatar = res.data.avatar || ''
  } catch (e) {}
  finally { loading.value = false }
}

async function saveProfile() {
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  saving.value = true
  try {
    if (form.avatar) {
      const { saveImage, userKey } = await import('@/utils/imageCache')
      saveImage(userKey(profile.id), form.avatar)
      window.dispatchEvent(new Event('user-avatar-updated'))
    }
    await profileApi.updateProfile({
      id: profile.id,
      phone: form.phone.trim(),
      real_name: form.real_name?.trim() || null,
      gender: form.gender,
      avatar: form.avatar || null,
      birth_date: form.birthDate ? `${form.birthDate}T00:00:00` : null,
      member_level: profile.member_level
    })
    ElMessage.success('资料更新成功')
    userStore.setUser({ ...profile, phone: form.phone, real_name: form.real_name, avatar: form.avatar || profile.avatar })
    loadProfile()
  } catch (e) {}
  finally { saving.value = false }
}

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^\w{6,16}$/, message: '密码为6-16位字母、数字或下划线', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        value === pwdForm.newPassword ? cb() : cb(new Error('两次输入的密码不一致')),
      trigger: 'blur'
    }
  ]
}

async function savePassword() {
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdSaving.value = true
    try {
      await profileApi.changePassword({
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      ElMessage.success('密码修改成功')
      pwdFormRef.value.resetFields()
      pwdDialogVisible.value = false
    } catch (e) {}
    finally { pwdSaving.value = false }
  })
}

function triggerAvatarUpload() {
  avatarFileInput.value?.click()
}

const cropDialog = reactive({
  visible: false,
  imgSrc: '',
  scale: 1,
  x: 0,
  y: 0,
  baseW: 0,
  baseH: 0
})
const cropStageRef = ref()
const cropImgRef = ref()
const cropSaving = ref(false)
const cropNaturalSize = reactive({ w: 0, h: 0 })
const cropStageSize = 360
const cropCircleSize = 260
let cropMinScale = 0.5
let cropMaxScale = 3
let cropDragState = null

const cropImageStyle = computed(() => ({
  width: cropDialog.baseW + 'px',
  height: cropDialog.baseH + 'px',
  transform: `translate(${cropDialog.x}px, ${cropDialog.y}px) scale(${cropDialog.scale})`
}))

async function onAvatarFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!/image\/(jpeg|png|webp)/.test(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、WEBP 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  try {
    cropDialog.imgSrc = await readFileAsDataURL(file)
    cropDialog.visible = true
  } catch (err) {
    ElMessage.error('图片读取失败')
  }
  e.target.value = ''
}

function readFileAsDataURL(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => resolve(e.target.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function onCropImageLoad(e) {
  const img = e.target
  const nw = img.naturalWidth
  const nh = img.naturalHeight
  cropNaturalSize.w = nw
  cropNaturalSize.h = nh

  const fitScale = Math.max(cropCircleSize / nw, cropCircleSize / nh)
  cropDialog.baseW = Math.round(nw * fitScale)
  cropDialog.baseH = Math.round(nh * fitScale)

  const longSide = Math.max(cropDialog.baseW, cropDialog.baseH)
  cropMinScale = cropCircleSize / longSide * 0.6
  cropMaxScale = Math.max(cropCircleSize / Math.min(cropDialog.baseW, cropDialog.baseH) * 3, 2)

  cropDialog.scale = 1
  centerCropImage()
}

function centerCropImage() {
  const scaledW = cropDialog.baseW * cropDialog.scale
  const scaledH = cropDialog.baseH * cropDialog.scale
  cropDialog.x = (cropStageSize - scaledW) / 2
  cropDialog.y = (cropStageSize - scaledH) / 2
}

function zoomCrop(delta) {
  const preScale = cropDialog.scale
  let next = cropDialog.scale + delta
  next = Math.max(cropMinScale, Math.min(cropMaxScale, next))
  if (next === preScale) return
  applyScaleAroundCenter(next, preScale)
}

function onScaleChange(next) {
  const preScale = cropDialog.scale
  if (next === preScale) return
  applyScaleAroundCenter(next, preScale)
}

function applyScaleAroundCenter(next, preScale) {
  const stageCenterX = cropStageSize / 2
  const stageCenterY = cropStageSize / 2
  const imgCenterX = cropDialog.x + (cropDialog.baseW * preScale) / 2
  const imgCenterY = cropDialog.y + (cropDialog.baseH * preScale) / 2
  const ratio = next / preScale
  const newImgCenterX = stageCenterX + (imgCenterX - stageCenterX) * ratio
  const newImgCenterY = stageCenterY + (imgCenterY - stageCenterY) * ratio
  cropDialog.x = newImgCenterX - (cropDialog.baseW * next) / 2
  cropDialog.y = newImgCenterY - (cropDialog.baseH * next) / 2
  cropDialog.scale = Number(next.toFixed(2))
}

function onCropWheel(e) {
  const delta = e.deltaY > 0 ? -0.08 : 0.08
  zoomCrop(delta)
}

function onCropStart(e) {
  if (e.button !== 0) return
  cropDragState = {
    startX: e.clientX,
    startY: e.clientY,
    originX: cropDialog.x,
    originY: cropDialog.y
  }
  window.addEventListener('mousemove', onCropMove)
  window.addEventListener('mouseup', onCropEnd)
}

function onCropMove(e) {
  if (!cropDragState) return
  const dx = e.clientX - cropDragState.startX
  const dy = e.clientY - cropDragState.startY
  cropDialog.x = cropDragState.originX + dx
  cropDialog.y = cropDragState.originY + dy
}

function onCropEnd() {
  cropDragState = null
  window.removeEventListener('mousemove', onCropMove)
  window.removeEventListener('mouseup', onCropEnd)
}

function resetCrop() {
  cropDialog.imgSrc = ''
  cropDialog.scale = 1
  cropDialog.x = 0
  cropDialog.y = 0
  cropDialog.baseW = 0
  cropDialog.baseH = 0
}

async function confirmCrop() {
  cropSaving.value = true
  try {
    const canvas = document.createElement('canvas')
    const ctx = canvas.getContext('2d')
    const outputSize = 300
    canvas.width = outputSize
    canvas.height = outputSize

    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.src = cropDialog.imgSrc
    await new Promise((res, rej) => { img.onload = res; img.onerror = rej })

    const scale = cropDialog.scale
    const px = cropDialog.x
    const py = cropDialog.y

    const topLeftOnImageDomX = (cropStageSize / 2 - px - cropCircleSize / 2) / scale
    const topLeftOnImageDomY = (cropStageSize / 2 - py - cropCircleSize / 2) / scale
    const sizeOnImageDom = cropCircleSize / scale

    const sx = topLeftOnImageDomX * (img.naturalWidth / cropDialog.baseW)
    const sy = topLeftOnImageDomY * (img.naturalHeight / cropDialog.baseH)
    const sw = sizeOnImageDom * (img.naturalWidth / cropDialog.baseW)
    const sh = sizeOnImageDom * (img.naturalHeight / cropDialog.baseH)

    ctx.save()
    ctx.beginPath()
    ctx.arc(outputSize / 2, outputSize / 2, outputSize / 2, 0, Math.PI * 2)
    ctx.clip()
    ctx.imageSmoothingEnabled = true
    ctx.imageSmoothingQuality = 'high'
    ctx.drawImage(img, sx, sy, sw, sh, 0, 0, outputSize, outputSize)
    ctx.restore()

    const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'))
    if (!blob) throw new Error('图片处理失败')
    const avatarFile = new File([blob], `avatar-${Date.now()}.png`, { type: 'image/png' })
    const res = await uploadApi.uploadAvatar(avatarFile)
    if (res.code !== 200 || !res.data?.url) {
      throw new Error(res.message || '头像上传失败')
    }

    form.avatar = res.data.url
    cropDialog.visible = false

    try {
      const { saveImage, userKey } = await import('@/utils/imageCache')
      saveImage(userKey(profile.id), form.avatar)
      window.dispatchEvent(new Event('user-avatar-updated'))
    } catch (e) {}

    await profileApi.updateProfile({
      id: profile.id,
      avatar: form.avatar
    })
    userStore.setUser({ ...userStore.user, avatar: form.avatar })
    ElMessage.success('头像修改成功')
    await loadProfile()
  } catch (err) {
    ElMessage.error('头像保存失败，请重试')
  } finally {
    cropSaving.value = false
  }
}

onMounted(async () => {
  pageLoading.value = true
  try {
    await Promise.all([
      loadProfile(),
      loadMyAppointments(),
      loadMine(),
      loadAssessmentHistory(),
      loadHealthHistory()
    ])
  } finally {
    pageLoading.value = false
  }
})
</script>

<style scoped>
.profile-page {
  padding: 20px;
  background:
    radial-gradient(circle at 96% 4%, rgba(217, 150, 62, 0.07), transparent 24%),
    linear-gradient(180deg, #f8f5ee 0%, #f1f6f2 52%, #faf7f0 100%);
  min-height: calc(100vh - 100px);
}

/* ---------- Hero Card ---------- */
.hero-card {
  position: relative;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-radius: 24px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 13px 34px rgba(38, 76, 65, 0.11), 0 2px 8px rgba(38, 76, 65, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.85);
}
.hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 88% 16%, rgba(242, 213, 157, 0.2), transparent 28%),
    linear-gradient(135deg, #245f54 0%, #2f7d6d 58%, #3f8a7a 100%);
  opacity: 1;
}
.hero-content {
  position: relative;
  padding: 32px 36px 28px;
  display: flex;
  align-items: flex-start;
  gap: 28px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
}
.hero-avatar {
  background: linear-gradient(145deg, #d9a552 0%, #b87826 100%) !important;
  color: #fff !important;
  font-size: 36px;
  border: 4px solid #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.25s ease;
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}
.level-tag {
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #6f4a13 !important;
  background: linear-gradient(135deg, #f8e4b8, #e8c37c) !important;
  border: 1px solid rgba(255, 255, 255, 0.28) !important;
}

.info-section {
  flex: 1;
  padding-top: 8px;
  color: #fff;
}
.user-name {
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}
.user-phone, .user-register {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  opacity: 0.92;
  margin-bottom: 4px;
}

.stats-section {
  display: flex;
  gap: 14px;
  padding-top: 4px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(20px) saturate(160%);
  -webkit-backdrop-filter: blur(20px) saturate(160%);
  border-radius: 16px;
  padding: 12px 18px;
  min-width: 110px;
  box-shadow: 0 4px 16px rgba(22, 67, 57, 0.10), inset 0 1px 0 rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(255, 255, 255, 0.9);
  transition: all 0.25s ease;
}
.stat-card:hover {
  transform: translateY(-3px);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 8px 24px rgba(22, 67, 57, 0.14), inset 0 1px 0 rgba(255, 255, 255, 0.9);
}
.stat-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.stat-points .stat-icon-wrap {
  background: linear-gradient(135deg, #e3b05c, #c9862c);
}
.stat-appointment .stat-icon-wrap {
  background: linear-gradient(135deg, #4b917f, #2f7d6d);
}
.stat-activity .stat-icon-wrap {
  background: linear-gradient(135deg, #72b39e, #4f9b7b);
}
.stat-score .stat-icon-wrap {
  background: linear-gradient(135deg, #f1d8ad, #e7c17e);
  color: #8a5a18;
}
.stat-health .stat-icon-wrap {
  background: linear-gradient(135deg, #8f7fa7, #74658d);
}
.stat-info {
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1.1;
}
.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

/* ---------- Content Card / Tabs ---------- */
.content-card {
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-radius: 24px;
  padding: 4px;
  box-shadow: 0 10px 30px rgba(38, 76, 65, 0.08), 0 2px 8px rgba(38, 76, 65, 0.03);
  border: 1px solid rgba(222, 233, 228, 0.9);
}

.profile-tabs :deep(.el-tabs__header) {
  margin: 0;
  border: none;
}
.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}
.profile-tabs :deep(.el-tabs__item) {
  height: 44px;
  line-height: 44px;
  font-weight: 500;
  font-size: 14px;
  color: #606266;
  background: transparent;
  border: none !important;
}
.profile-tabs :deep(.el-tabs__item:hover) {
  color: #2f7d6d;
}
.profile-tabs :deep(.el-tabs__item.is-active) {
  font-weight: 600;
  color: #2f7d6d;
}
.profile-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #347f70, #1f6559);
  height: 3px;
  border-radius: 2px;
}
.profile-tabs :deep(.el-tabs__nav) {
  border: none !important;
}
.profile-tabs :deep(.el-tabs__content) {
  padding: 20px 28px 28px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.tab-content {
  min-height: 300px;
}

/* ---------- Tables ---------- */
.profile-tabs :deep(.el-table) {
  background: transparent !important;
  --el-table-bg-color: rgba(255, 255, 255, 0.95);
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(47, 125, 109, 0.08);
  --el-table-border-color: rgba(47, 125, 109, 0.11);
  --el-table-header-text-color: #374151;
  --el-table-text-color: #2c2f3a;
  --el-table-row-hover-bg-color: rgba(47, 125, 109, 0.045);
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid rgba(47, 125, 109, 0.10);
}
.profile-tabs :deep(.el-table th.el-table__cell) {
  background: rgba(47, 125, 109, 0.08) !important;
  color: #374151;
  font-weight: 600;
}
.profile-tabs :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
  background: rgba(47, 125, 109, 0.02) !important;
}
.profile-tabs :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: rgba(47, 125, 109, 0.05) !important;
}

/* ---------- Form ---------- */
.profile-form {
  max-width: 520px;
  margin: 0 auto;
  padding-top: 12px;
}
.btn-save,
.btn-change-pwd {
  height: 40px;
  padding: 0 28px;
  font-weight: 600;
  font-size: 14px;
  border: none;
  border-radius: 10px;
  transition: all 0.2s ease;
}
.btn-save {
  color: #fff;
  background: linear-gradient(135deg, #347f70 0%, #1f6559 100%);
  box-shadow: 0 4px 14px rgba(31, 101, 89, 0.26);
}
.btn-save:hover {
  transform: translateY(-1px);
  box-shadow: 0 7px 20px rgba(31, 101, 89, 0.34);
}
.btn-change-pwd {
  color: #fff;
  background: linear-gradient(135deg, #deaa57 0%, #bd7c28 100%);
  box-shadow: 0 4px 14px rgba(189, 124, 40, 0.24);
}
.btn-change-pwd:hover {
  transform: translateY(-1px);
  box-shadow: 0 7px 20px rgba(189, 124, 40, 0.32);
}

:deep(.el-form-item__label) {
  color: #5f6368;
  font-size: 14px;
}
:deep(.el-input__wrapper),
:deep(.el-date-editor.el-input .el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dadce0 inset;
}
:deep(.el-input__wrapper.is-focus),
:deep(.el-input__wrapper.is-focus-within) {
  box-shadow: 0 0 0 2px #2f7d6d inset;
}

/* ---------- Tables ---------- */
.mini-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.mine-pkg {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mine-cover {
  width: 44px;
  height: 34px;
  border-radius: 8px;
  flex-shrink: 0;
}
.text-sub {
  color: #9ca3af;
}
.mono {
  font-variant-numeric: tabular-nums;
}

/* ---------- Activity Mine ---------- */
.simple-loading,
.simple-empty {
  text-align: center;
  padding: 60px 20px;
  color: #9ca3af;
}
.activity-mine-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mine-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  background: linear-gradient(135deg, rgba(47, 125, 109, 0.045), rgba(255, 255, 255, 0.84));
  border: 1px solid rgba(47, 125, 109, 0.11);
  border-radius: 14px;
  transition: all 0.25s ease;
}
.mine-item:hover {
  border-color: rgba(47, 125, 109, 0.27);
  box-shadow: 0 5px 18px rgba(38, 76, 65, 0.09);
  transform: translateY(-1px);
}
.mine-cover-img {
  width: 72px;
  height: 54px;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
}
.mine-info {
  flex: 1;
  min-width: 0;
}
.mine-title {
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
  margin-bottom: 4px;
}
.mine-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 6px;
}
.mine-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #9ca3af;
}
.mine-actions {
  flex-shrink: 0;
}

/* ---------- Score Badge ---------- */
.score-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.5px;
}

/* ---------- Report Answers ---------- */
.report-answers {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--card-border);
  border-radius: 10px;
  padding: 14px 18px;
  max-height: 300px;
  overflow-y: auto;
}
.report-item {
  margin-bottom: 14px;
}
.report-q {
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--text-main);
}
.opt-tag {
  margin: 0 8px 6px 0;
}
.text-answer {
  color: var(--text-sub);
  padding-left: 2px;
}
.detail-section-title {
  font-size: 15px;
  font-weight: 600;
  margin: 16px 0 10px;
}

/* ---------- Responsive ---------- */
@media (max-width: 1100px) {
  .hero-content {
    flex-wrap: wrap;
  }
  .stats-section {
    width: 100%;
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 calc(33.33% - 14px);
    min-width: 100px;
  }
}
@media (max-width: 640px) {
  .hero-content {
    padding: 24px 20px 20px;
    gap: 16px;
    flex-direction: column;
    align-items: center;
  }
  .info-section {
    text-align: center;
  }
  .stats-section {
    gap: 10px;
  }
  .stat-card {
    flex: 1 1 calc(50% - 10px);
  }
  .profile-tabs :deep(.el-tabs__content) {
    padding: 16px;
  }
}

/* ---------- Crop ---------- */
.crop-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}
.crop-stage {
  position: relative;
  width: 360px;
  height: 360px;
  background: #333;
  border-radius: 12px;
  overflow: hidden;
  cursor: grab;
  user-select: none;
  touch-action: none;
}
.crop-stage:active {
  cursor: grabbing;
}
.crop-image {
  position: absolute;
  top: 0;
  left: 0;
  max-width: none;
  max-height: none;
  transform-origin: 0 0;
  will-change: transform;
  pointer-events: none;
  -webkit-user-drag: none;
}
.crop-mask {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.crop-mask::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  -webkit-mask: radial-gradient(circle 130px at 50% 50%, transparent 0, transparent 100%, #000 100%);
  mask: radial-gradient(circle 130px at 50% 50%, transparent 0, transparent 100%, #000 100%);
}
.crop-circle {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 260px;
  height: 260px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.9), 0 0 0 9999px rgba(0, 0, 0, 0.45);
}
.crop-grid-v,
.crop-grid-h {
  position: absolute;
  background: rgba(255, 255, 255, 0.35);
}
.crop-grid-v {
  width: 1px;
  height: 260px;
  top: 50%;
  transform: translateY(-50%);
}
.crop-grid-v1 { left: calc(50% - 43px); }
.crop-grid-v2 { left: calc(50% + 43px); }
.crop-grid-h {
  height: 1px;
  width: 260px;
  left: 50%;
  transform: translateX(-50%);
}
.crop-grid-h1 { top: calc(50% - 43px); }
.crop-grid-h2 { top: calc(50% + 43px); }

.crop-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 4px 24px;
  box-sizing: border-box;
}
.crop-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  border: 1px solid #e0e3e8;
  background: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #606266;
  transition: all 0.2s ease;
  flex-shrink: 0;
}
.crop-btn:hover {
  color: #2f7d6d;
  border-color: #2f7d6d;
  box-shadow: 0 2px 8px rgba(47, 125, 109, 0.18);
}
.crop-slider {
  flex: 1;
}
.crop-slider :deep(.el-slider__runway) {
  background: linear-gradient(90deg, #347f70 0%, #1f6559 100%);
  border-radius: 4px;
  height: 6px;
}
.crop-slider :deep(.el-slider__bar) {
  background: transparent;
}
.crop-slider :deep(.el-slider__button) {
  border: 2px solid #2f7d6d;
  background: #fff;
  width: 16px;
  height: 16px;
}
.crop-slider :deep(.el-slider__button-wrapper) {
  top: -5px;
}
</style>
