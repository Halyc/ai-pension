<template>
  <div class="page-container">
    <div class="page-card">
      <el-tabs v-model="activeTab">
        <!-- ========== 活动管理 ========== -->
        <el-tab-pane name="activities">
          <template #label>
            <span class="tab-label"><el-icon><Flag /></el-icon>活动管理</span>
          </template>

          <div class="page-title"><el-icon><Flag /></el-icon>社区活动管理</div>
          <div class="page-subtitle">发布社区活动、设置报名与活动时间、维护活动状态</div>

          <div class="search-bar">
            <el-input
              v-model="actQuery.keyword"
              placeholder="按活动标题搜索"
              clearable
              :prefix-icon="Search"
              class="filter-input"
              style="width: 240px"
              @keyup.enter="searchActivities"
            />
            <el-radio-group v-model="statusFilter" size="default" @change="searchActivities">
              <el-radio-button value="">全部</el-radio-button>
              <el-radio-button value="RECRUITING">报名中</el-radio-button>
              <el-radio-button value="REGISTRATION_CLOSED">报名已截止</el-radio-button>
              <el-radio-button value="ONGOING">进行中</el-radio-button>
              <el-radio-button value="ENDED">已结束</el-radio-button>
              <el-radio-button value="CANCELLED">已取消</el-radio-button>
            </el-radio-group>
            <el-button type="primary" :icon="Search" @click="searchActivities">查询</el-button>
            <el-button :icon="RefreshLeft" @click="resetActQuery">重置</el-button>
            <div style="flex:1"></div>
            <el-button type="primary" :icon="Plus" @click="openActivityDialog()">新增活动</el-button>
          </div>

          <el-table :data="filteredActivities" v-loading="actLoading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="封面" width="90">
              <template #default="{ row }">
                <el-image
                  :src="buildActImage(row)"
                  fit="cover"
                  style="width:56px;height:40px;border-radius:6px"
                  :preview-src-list="[buildActImage(row)]"
                  preview-teleported
                />
              </template>
            </el-table-column>
            <el-table-column prop="title" label="活动标题" min-width="170" />
            <el-table-column label="报名时间" min-width="260">
              <template #default="{ row }">
                <div class="mono small">{{ formatDateTime(row.registrationStart) }}</div>
                <div class="mono small">至 {{ formatDateTime(row.registrationEnd) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="活动时间" min-width="260">
              <template #default="{ row }">
                <div class="mono small">{{ formatDateTime(row.activityStart) }}</div>
                <div class="mono small">至 {{ formatDateTime(row.activityEnd) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="报名人数" width="100">
              <template #default="{ row }">
                <span class="mono">{{ row.currentParticipants }} / {{ row.maxParticipants }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="160">
              <template #default="{ row }">
                <el-dropdown trigger="click" @command="(cmd) => quickStatusChange(row, cmd)">
                  <el-tag
                    :type="dictType(activityStatusMap, calcActivityStatus(row))"
                    style="cursor:pointer"
                    effect="light"
                  >
                    {{ dictLabel(activityStatusMap, calcActivityStatus(row)) }}
                    <el-icon style="margin-left:4px"><ArrowDown /></el-icon>
                  </el-tag>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="RECRUITING" :class="{ 'is-active': calcActivityStatus(row) === 'RECRUITING' }">
                        <el-tag type="primary" size="small">报名中</el-tag>
                      </el-dropdown-item>
                      <el-dropdown-item command="REGISTRATION_CLOSED" :class="{ 'is-active': calcActivityStatus(row) === 'REGISTRATION_CLOSED' }">
                        <el-tag type="warning" size="small">报名已截止</el-tag>
                      </el-dropdown-item>
                      <el-dropdown-item command="ONGOING" :class="{ 'is-active': calcActivityStatus(row) === 'ONGOING' }">
                        <el-tag type="success" size="small">进行中</el-tag>
                      </el-dropdown-item>
                      <el-dropdown-item command="ENDED" :class="{ 'is-active': calcActivityStatus(row) === 'ENDED' }">
                        <el-tag type="info" size="small">已结束</el-tag>
                      </el-dropdown-item>
                      <el-dropdown-item command="CANCELLED" :class="{ 'is-active': calcActivityStatus(row) === 'CANCELLED' }">
                        <el-tag type="danger" size="small">已取消</el-tag>
                      </el-dropdown-item>
                      <el-dropdown-item command="AUTO" :class="{ 'is-active': !row.status }" divided>
                        <el-icon><Refresh /></el-icon> 恢复自动计算
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="270">
              <template #default="{ row }">
                <el-button link type="primary" :icon="Tickets" @click="viewRegistrations(row)">
                  报名管理
                </el-button>
                <el-button link type="warning" :icon="Edit" @click="openActivityDialog(row)">
                  编辑
                </el-button>
                <el-button link type="danger" :icon="Delete" @click="removeActivity(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ========== 报名管理 ========== -->
        <el-tab-pane name="registrations">
          <template #label>
            <span class="tab-label"><el-icon><Tickets /></el-icon>报名管理</span>
          </template>

          <div class="page-title"><el-icon><Tickets /></el-icon>活动报名与签到</div>
          <div class="page-subtitle">按活动筛选报名记录，查看报名详情并进行现场签到</div>

          <div class="search-bar">
            <el-select
              v-model="regQuery.activityId"
              placeholder="选择活动（默认全部）"
              clearable
              filterable
              style="width: 320px"
              @change="loadRegistrations"
            >
              <el-option
                v-for="a in activities"
                :key="a.id"
                :label="a.title"
                :value="a.id"
              />
            </el-select>
            <el-button type="primary" :icon="Search" @click="loadRegistrations">查询</el-button>
            <el-button :icon="RefreshLeft" @click="loadRegistrations">刷新</el-button>
          </div>

          <el-table :data="registrations" v-loading="regLoading" stripe>
            <el-table-column prop="id" label="报名ID" width="90" />
            <el-table-column label="会员" min-width="120">
              <template #default="{ row }">
                {{ row.userName || `会员#${row.userId}` }}
              </template>
            </el-table-column>
            <el-table-column label="活动标题" min-width="180">
              <template #default="{ row }">{{ row.activityTitle || activityTitle(row.activityId) }}</template>
            </el-table-column>
            <el-table-column prop="checkInStatus" label="签到状态" width="110">
              <template #default="{ row }">
                <el-tag :type="dictType(checkInStatusMap, row.checkInStatus)">
                  {{ dictLabel(checkInStatusMap, row.checkInStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="200">
              <template #default="{ row }">
                <el-button link type="primary" :icon="View" @click="showRegDetail(row)">详情</el-button>
                <el-button
                  link
                  type="success"
                  :icon="CircleCheck"
                  :disabled="row.checkInStatus === 'CHECKED_IN' || !isActivityOngoing(row.activityId)"
                  @click="doCheckIn(row)"
                >
                  {{ row.checkInStatus === 'CHECKED_IN' ? '已签到' : (isActivityOngoing(row.activityId) ? '签到' : '签到未开放') }}
                </el-button>
                <el-button
                  link
                  type="danger"
                  :disabled="row.checkInStatus === 'CHECKED_IN'"
                  @click="doCancelRegistration(row)"
                >
                  取消报名
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 活动新增/编辑弹窗 -->
    <el-dialog
      v-model="actDialog.visible"
      :title="actDialog.isEdit ? '编辑活动' : '新增活动'"
      width="680px"
      @closed="resetActForm"
    >
      <el-form ref="actFormRef" :model="actForm" :rules="actRules" label-width="110px">
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="actForm.title" placeholder="如：重阳节茶话会" maxlength="100" />
        </el-form-item>
        <el-form-item label="封面图">
          <div style="display:flex;gap:16px;align-items:flex-start;width:100%">
            <ImageUploader
              v-model="actForm.coverUrl"
              :width="160"
              :height="100"
              accept="image/jpeg,image/png,image/webp"
              :max-size="5"
              placeholder="点击上传封面图"
            />
            <div style="flex:1">
              <el-input
                v-model="actForm.coverUrl"
                placeholder="或粘贴图片URL，留空则自动根据标题生成"
                clearable
              />
              <div class="form-hint">
                预览：{{ actForm.coverUrl ? '使用自定义图片' : 'AI 根据标题自动生成' }}
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="活动内容">
          <el-input v-model="actForm.content" type="textarea" :rows="3" placeholder="活动详细介绍" />
        </el-form-item>
        <el-form-item label="报名开始" prop="registrationStart">
          <el-date-picker
            v-model="actForm.registrationStart"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="报名开始时间"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="报名结束" prop="registrationEnd">
          <el-date-picker
            v-model="actForm.registrationEnd"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="报名结束时间"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="活动开始" prop="activityStart">
          <el-date-picker
            v-model="actForm.activityStart"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="活动开始时间"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="活动结束" prop="activityEnd">
          <el-date-picker
            v-model="actForm.activityEnd"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="活动结束时间"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="人数上限" prop="maxParticipants">
          <el-input-number v-model="actForm.maxParticipants" :min="1" :max="9999" />
        </el-form-item>
        <el-form-item label="活动状态">
          <el-select v-model="actForm.status" style="width:200px" placeholder="自动计算（推荐）" clearable>
            <el-option label="自动计算（根据时间）" value="" />
            <el-option label="报名中" value="RECRUITING" />
            <el-option label="报名已截止" value="REGISTRATION_CLOSED" />
            <el-option label="进行中" value="ONGOING" />
            <el-option label="已结束" value="ENDED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="actDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="actDialog.saving" @click="submitActivity">保存</el-button>
      </template>
    </el-dialog>

    <!-- 报名详情弹窗 -->
    <el-dialog v-model="regDetailVisible" title="报名详情" width="440px">
      <el-descriptions v-if="regDetail" :column="1" border>
        <el-descriptions-item label="报名ID">{{ regDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="会员">{{ regDetail.userName || `会员#${regDetail.userId}` }}</el-descriptions-item>
        <el-descriptions-item label="活动标题">{{ regDetail.activityTitle || activityTitle(regDetail.activityId) }}</el-descriptions-item>
        <el-descriptions-item label="签到状态">
          <el-tag :type="dictType(checkInStatusMap, regDetail.checkInStatus)">
            {{ dictLabel(checkInStatusMap, regDetail.checkInStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报名时间">{{ formatDateTime(regDetail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(regDetail.updateTime) }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="regDetailVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Edit,
  Delete,
  Search,
  RefreshLeft,
  View,
  CircleCheck,
  Flag,
  MagicStick,
  ArrowDown,
  Refresh
} from '@element-plus/icons-vue'
import { adminActivityApi } from '@/api/adminActivity'
import ImageUploader from '@/components/ImageUploader.vue'
import {
  formatDateTime,
  dictLabel,
  dictType,
  activityStatusMap,
  checkInStatusMap,
  buildActImage
} from '@/utils/format'

const activeTab = ref('activities')

watch(activeTab, (val) => {
  if (val === 'registrations') {
    loadRegistrations()
  }
})

// ---------- 活动列表 ----------
const actLoading = ref(false)
const activities = ref([])
const statusFilter = ref('')
const actQuery = reactive({ keyword: '' })

const filteredActivities = computed(() => {
  const kw = actQuery.keyword.trim().toLowerCase()
  return activities.value.filter((a) => {
    if (kw && !(a.title || '').toLowerCase().includes(kw)) return false
    if (statusFilter.value && !matchesActivityFilter(a, statusFilter.value)) return false
    return true
  })
})

function searchActivities() {
  // computed 已自动响应 actQuery / statusFilter 变化
}

function resetActQuery() {
  actQuery.keyword = ''
  statusFilter.value = ''
}

function parseTs(v) {
  return v ? new Date(v).getTime() : NaN
}

function isCancelled(row) {
  if (row.status === 'CANCELLED' || row.status === 'PAUSED') return true
  return false
}
function isEnded(row) {
  if (row.status === 'ENDED' || row.status === 'FINISHED') return true
  const actEnd = parseTs(row.activityEnd)
  if (!isNaN(actEnd)) return Date.now() > actEnd
  return false
}
function isOngoing(row) {
  if (row.status === 'ONGOING' || row.status === 'ACTIVE') return true
  if (isCancelled(row) || isEnded(row)) return false
  const now = Date.now()
  const actStart = parseTs(row.activityStart)
  const actEnd = parseTs(row.activityEnd)
  return !isNaN(actStart) && now >= actStart && (isNaN(actEnd) || now <= actEnd)
}
function isRecruiting(row) {
  if (row.status === 'RECRUITING' || row.status === 'REGISTRATION_OPEN') return true
  if (isCancelled(row) || isEnded(row) || isOngoing(row)) return false
  const now = Date.now()
  const regStart = parseTs(row.registrationStart)
  const regEnd = parseTs(row.registrationEnd)
  const withinReg = !isNaN(regStart) && !isNaN(regEnd) && now >= regStart && now <= regEnd
  return withinReg
}
function isRegistrationClosed(row) {
  if (row.status === 'REGISTRATION_CLOSED') return true
  if (isCancelled(row) || isEnded(row) || isOngoing(row)) return false
  const now = Date.now()
  const regEnd = parseTs(row.registrationEnd)
  const actStart = parseTs(row.activityStart)
  const regPast = !isNaN(regEnd) && now > regEnd
  const actNotStarted = !isNaN(actStart) && now < actStart
  return regPast && actNotStarted
}

function matchesActivityFilter(row, filter) {
  switch (filter) {
    case 'RECRUITING': return isRecruiting(row)
    case 'REGISTRATION_CLOSED': return isRegistrationClosed(row)
    case 'ONGOING': return isOngoing(row)
    case 'ENDED': return isEnded(row)
    case 'CANCELLED': return isCancelled(row)
    default: return true
  }
}

function calcActivityStatus(row) {
  if (row.status) {
    if (activityStatusMap[row.status]) return row.status
  }
  if (isCancelled(row)) return 'CANCELLED'
  if (isEnded(row)) return 'ENDED'
  if (isOngoing(row)) return 'ONGOING'
  if (isRecruiting(row)) return 'RECRUITING'
  if (isRegistrationClosed(row)) return 'REGISTRATION_CLOSED'
  return 'RECRUITING'
}

async function loadActivities() {
  actLoading.value = true
  try {
    const res = await adminActivityApi.listActivities()
    activities.value = (res.data || []).sort((a, b) => {
      const ta = new Date(a.createTime || 0).getTime()
      const tb = new Date(b.createTime || 0).getTime()
      return tb - ta
    })
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    actLoading.value = false
  }
}

function activityTitle(activityId) {
  return activities.value.find((a) => a.id === activityId)?.title || `活动#${activityId}`
}
function activityById(activityId) {
  return activities.value.find((a) => a.id === activityId) || null
}
function isActivityOngoing(activityId) {
  const act = activityById(activityId)
  return act ? isOngoing(act) : false
}

const actFormRef = ref()
const actDialog = reactive({ visible: false, isEdit: false, saving: false, id: null, originalCoverUrl: '' })
const actForm = reactive({
  title: '',
  coverUrl: '',
  content: '',
  registrationStart: '',
  registrationEnd: '',
  activityStart: '',
  activityEnd: '',
  maxParticipants: 50,
  status: ''
})
const actPreviewCover = computed(() => buildActImage({ title: actForm.title, coverUrl: '' }))
const actRules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  registrationStart: [{ required: true, message: '请选择报名开始时间', trigger: 'change' }],
  registrationEnd: [{ required: true, message: '请选择报名结束时间', trigger: 'change' }],
  activityStart: [{ required: true, message: '请选择活动开始时间', trigger: 'change' }],
  activityEnd: [{ required: true, message: '请选择活动结束时间', trigger: 'change' }],
  maxParticipants: [{ required: true, message: '请设置人数上限', trigger: 'blur' }]
}

function resetActForm() {
  Object.assign(actForm, {
    title: '',
    coverUrl: '',
    content: '',
    registrationStart: '',
    registrationEnd: '',
    activityStart: '',
    activityEnd: '',
    maxParticipants: 50,
    status: ''
  })
  actDialog.id = null
  actDialog.originalCoverUrl = ''
  actFormRef.value?.clearValidate()
}

function openActivityDialog(row) {
  resetActForm()
  if (row) {
    actDialog.isEdit = true
    actDialog.id = row.id
    actDialog.originalCoverUrl = row.coverUrl || ''
    Object.assign(actForm, {
      title: row.title,
      coverUrl: row.coverUrl || '',
      content: row.content || '',
      registrationStart: toPicker(row.registrationStart),
      registrationEnd: toPicker(row.registrationEnd),
      activityStart: toPicker(row.activityStart),
      activityEnd: toPicker(row.activityEnd),
      maxParticipants: row.maxParticipants ?? 50,
      status: row.status || 'RECRUITING'
    })
  } else {
    actDialog.isEdit = false
  }
  actDialog.visible = true
}

function toPicker(v) {
  if (!v) return ''
  return typeof v === 'string' ? v.slice(0, 19) : ''
}

async function submitActivity() {
  await actFormRef.value.validate(async (valid) => {
    if (!valid) return
    const f = actForm
    const savedId = actDialog.isEdit ? actDialog.id : null
    if (f.coverUrl) {
      const { saveImage, actKey } = await import('@/utils/imageCache')
      saveImage(actKey(savedId || 'new:' + Date.now()), f.coverUrl)
    }

    const isEditCoverUnchanged = actDialog.isEdit &&
      (f.coverUrl || '') === (actDialog.originalCoverUrl || '')

    const body = {
      title: f.title?.trim(),
      content: f.content?.trim() || null,
      registrationStart: f.registrationStart || null,
      registration_start: f.registrationStart || null,
      registrationEnd: f.registrationEnd || null,
      registration_end: f.registrationEnd || null,
      activityStart: f.activityStart || null,
      activity_start: f.activityStart || null,
      activityEnd: f.activityEnd || null,
      activity_end: f.activityEnd || null,
      maxParticipants: f.maxParticipants,
      max_participants: f.maxParticipants,
      status: f.status || null
    }
    if (!isEditCoverUnchanged) {
      body.coverUrl = f.coverUrl || null
      body.cover_url = f.coverUrl || null
    }

    actDialog.saving = true
    try {
      let savedIdActual = savedId
      if (actDialog.isEdit) {
        await adminActivityApi.updateActivity(actDialog.id, body)
        ElMessage.success('更新成功')
      } else {
        const res = await adminActivityApi.createActivity(body)
        savedIdActual = res?.data?.id || res?.data?.activityId || null
        ElMessage.success('创建成功')
      }
      if (f.coverUrl && savedIdActual && savedId !== String(savedIdActual)) {
        const { saveImage, actKey, removeImage } = await import('@/utils/imageCache')
        if (savedId) removeImage(actKey(savedId))
        saveImage(actKey(savedIdActual), f.coverUrl)
      }
      actDialog.visible = false
      loadActivities()
    } catch (e) {
      /* 拦截器提示 */
    } finally {
      actDialog.saving = false
    }
  })
}

async function quickStatusChange(row, command) {
  let newStatus = command
  if (command === 'AUTO') newStatus = null
  try {
    const label = newStatus ? dictLabel(activityStatusMap, newStatus) : '自动计算'
    await ElMessageBox.confirm(
      `确定将活动「${row.title}」的状态修改为「${label}」吗？`,
      '修改状态',
      { type: 'warning', confirmButtonText: '确定修改', cancelButtonText: '取消' }
    )
    await adminActivityApi.updateActivity(row.id, { status: newStatus })
    ElMessage.success(`状态已修改为「${label}」`)
    loadActivities()
  } catch (e) {
    /* 取消或请求失败 */
  }
}

async function removeActivity(row) {
  try {
    await ElMessageBox.confirm(`确定删除活动「${row.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await adminActivityApi.deleteActivity(row.id)
    const { removeImage, actKey } = await import('@/utils/imageCache')
    removeImage(actKey(row.id))
    ElMessage.success('删除成功')
    loadActivities()
  } catch (e) {
    /* 取消 */
  }
}

function viewRegistrations(row) {
  regQuery.activityId = row.id
  activeTab.value = 'registrations'
  loadRegistrations()
}

// ---------- 报名管理 ----------
const regLoading = ref(false)
const registrations = ref([])
const regQuery = reactive({ activityId: null })
const regDetailVisible = ref(false)
const regDetail = ref(null)

async function loadRegistrations() {
  regLoading.value = true
  try {
    const res = await adminActivityApi.listRegistrations(regQuery.activityId || null)
    registrations.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    regLoading.value = false
  }
}

async function showRegDetail(row) {
  regDetailVisible.value = true
  regDetail.value = null
  try {
    const res = await adminActivityApi.getRegistrationDetail(row.id)
    regDetail.value = res.data
  } catch (e) {
    regDetailVisible.value = false
  }
}

async function doCheckIn(row) {
  try {
    if (!isActivityOngoing(row.activityId)) {
      ElMessage.warning('签到失败：活动未在进行中，无法签到')
      return
    }
    await ElMessageBox.confirm(
      `确定为会员 #${row.userId} 的报名（ID:${row.id}）办理签到吗？需在活动时间范围内。`,
      '签到确认',
      { type: 'success', confirmButtonText: '确认签到', cancelButtonText: '取消' }
    )
    await adminActivityApi.checkIn(row.id)
    ElMessage.success('签到成功')
    loadRegistrations()
  } catch (e) {
    /* 取消或不在活动时间范围（后端返回提示） */
  }
}

async function doCancelRegistration(row) {
  try {
    await ElMessageBox.confirm(
      `确定取消会员 #${row.userId} 的报名（ID:${row.id}）吗？此操作不可撤销。`,
      '取消报名确认',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '返回' }
    )
    await adminActivityApi.cancelRegistration(row.id)
    ElMessage.success('已取消报名')
    loadRegistrations()
    loadActivities()
  } catch (e) {
    /* 取消 */
  }
}

onMounted(loadActivities)
</script>

<style scoped>
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}
.form-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-muted);
}
.small {
  font-size: 12px;
  color: var(--text-sub);
  line-height: 1.7;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 10px 0 14px;
  flex-wrap: wrap;
  gap: 10px;
}
</style>