<template>
  <div class="page-container">
    <!-- 积分提示条 -->
    <div class="points-bar page-card">
      <el-icon :size="20" color="#f6a23b"><Coin /></el-icon>
      <span>我的积分：<b class="mono">{{ userStore.user?.points ?? '-' }}</b></span>
      <span class="tip">预约体检套餐将扣除对应积分，取消预约后积分自动返还</span>
    </div>

    <el-tabs v-model="activeTab" class="page-card">
      <!-- ===== 可预约套餐 ===== -->
      <el-tab-pane name="packages">
        <template #label>
          <span class="tab-label"><el-icon><Calendar /></el-icon>体检套餐</span>
        </template>

        <div class="toolbar">
          <el-radio-group v-model="sortBy" @change="loadPackages">
            <el-radio-button value="">最新上架</el-radio-button>
            <el-radio-button value="price_asc">积分从低到高</el-radio-button>
            <el-radio-button value="price_desc">积分从高到低</el-radio-button>
          </el-radio-group>
          <el-button :icon="Refresh" circle @click="loadPackages" :loading="pkgLoading" />
        </div>

        <el-row :gutter="16" v-loading="pkgLoading">
          <el-col
            v-for="pkg in packages"
            :key="pkg.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="8"
            :xl="6"
          >
            <el-card class="pkg-card hover-card" shadow="hover">
              <div class="pkg-cover">
                <el-image
                  :src="pkgImg(pkg)"
                  fit="contain"
                  :preview-src-list="[pkgImg(pkg)]"
                  preview-teleported
                />
                <el-tag class="pkg-price" type="warning" effect="dark">
                  {{ pkg.price ?? 0 }} 积分
                </el-tag>
              </div>
              <div class="pkg-name">{{ pkg.name }}</div>
              <div class="pkg-people">
                <el-icon><User /></el-icon>{{ pkg.suitable_people || '全人群适用' }}
              </div>
              <div class="pkg-items">
                <el-tag
                  v-for="(item, i) in parseItems(pkg.items).slice(0, 3)"
                  :key="i"
                  size="small"
                  type="info"
                  effect="plain"
                  class="pkg-item-tag"
                >{{ item }}</el-tag>
                <span v-if="parseItems(pkg.items).length > 3" class="more-items">
                  等{{ parseItems(pkg.items).length }}项
                </span>
              </div>
              <el-button
                type="primary"
                class="book-btn"
                :icon="Calendar"
                @click="openSlotsDialog(pkg)"
              >
                查看时段并预约
              </el-button>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!pkgLoading && !packages.length" description="暂无可预约套餐" />
      </el-tab-pane>

      <!-- ===== 我的预约 ===== -->
      <el-tab-pane name="mine">
        <template #label>
          <span class="tab-label"><el-icon><Tickets /></el-icon>我的预约</span>
        </template>

        <div class="toolbar">
          <el-button :icon="Refresh" @click="loadMyAppointments" :loading="mineLoading">
            刷新
          </el-button>
        </div>

        <el-table :data="myAppointments" v-loading="mineLoading" stripe>
          <el-table-column prop="id" label="预约ID" min-width="60" />
          <el-table-column label="套餐" min-width="100">
            <template #default="{ row }">
              <div class="mine-pkg">
                <el-image
                  :src="pkgImg(row.package || { name: row.package?.name })"
                  fit="cover"
                  class="mine-cover"
                />
                <span>{{ row.package?.name || '-' }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="套餐价格" min-width="80">
            <template #default="{ row }">
              <span class="mono">{{ row.package?.price ?? 0 }} 积分</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" min-width="70">
            <template #default="{ row }">
              <el-tag :type="dictType(appointmentStatusMap, row.status)">
                {{ dictLabel(appointmentStatusMap, row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="体检报告" min-width="180">
            <template #default="{ row }">
              <div v-if="row.status === 'COMPLETED'">
                <el-link
                  v-if="row.reportUrl"
                  type="primary"
                  :underline="false"
                  @click="openReport(row.reportUrl)"
                >
                  <el-icon><View /></el-icon> 查看报告
                </el-link>
                <span v-else class="text-sub">暂无报告</span>
              </div>
              <span v-else class="text-sub">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="预约时间" min-width="100">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="80">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'CONFIRMED'"
                link
                type="danger"
                :icon="CircleClose"
                @click="cancelBook(row)"
              >取消预约</el-button>
              <span v-else class="text-sub">-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 时段选择弹窗 -->
    <el-dialog v-model="slotDialog.visible" title="选择预约时段" width="720px" top="8vh">
      <div v-if="slotDialog.pkg" class="slot-dialog-head">
        <span class="slot-pkg-name">{{ slotDialog.pkg.name }}</span>
        <el-tag type="warning" effect="dark">{{ slotDialog.pkg.price ?? 0 }} 积分</el-tag>
      </div>

      <div v-loading="slotDialog.loading" class="slot-groups">
        <template v-if="slotGroups.length">
          <div v-for="group in slotGroups" :key="group.date" class="slot-day">
            <div class="slot-date">
              <el-icon><Calendar /></el-icon>
              {{ formatDate(group.date) }}（{{ weekday(group.date) }}）
            </div>
            <div class="slot-chips">
              <el-button
                v-for="s in group.slots"
                :key="s.id"
                class="slot-chip"
                :disabled="!s.available"
                round
                @click="book(s)"
              >
                {{ s.timeRange }}
                <span class="slot-count">（剩 {{ s.maxCount - s.currentCount }}）</span>
              </el-button>
            </div>
          </div>
        </template>
        <el-empty v-else description="该套餐暂无可预约时段" />
      </div>
      <template #footer>
        <el-button @click="slotDialog.visible = false">关 闭</el-button>
      </template>
    </el-dialog>

    <el-image-viewer
      v-if="reportViewer.visible"
      :url-list="[reportViewer.url]"
      :initial-index="0"
      @close="reportViewer.visible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Calendar, Refresh, Coin, User, Tickets, CircleClose, View } from '@element-plus/icons-vue'
import { memberAppointmentApi } from '@/api/memberAppointment'
import { useUserStore } from '@/store/user'
import {
  formatDate,
  formatDateTime,
  dictLabel,
  dictType,
  appointmentStatusMap,
  parseItems,
  buildPkgImage
} from '@/utils/format'

const userStore = useUserStore()
const activeTab = ref('packages')

const imgRefreshTick = ref(0)
function pkgImg(row) { imgRefreshTick.value; return buildPkgImage(row) }
window.addEventListener('image-cache-ready', () => { imgRefreshTick.value++ })

// ---------- 套餐列表 ----------
const packages = ref([])
const pkgLoading = ref(false)
const sortBy = ref('')

async function loadPackages() {
  pkgLoading.value = true
  try {
    const res = await memberAppointmentApi.listPackages(sortBy.value || undefined)
    const list = res.data || []
    if (sortBy.value === 'price_asc') {
      list.sort((a, b) => (a.price ?? 0) - (b.price ?? 0))
    } else if (sortBy.value === 'price_desc') {
      list.sort((a, b) => (b.price ?? 0) - (a.price ?? 0))
    } else {
      list.sort((a, b) => {
        const ta = new Date(a.createTime || a.create_time || 0).getTime()
        const tb = new Date(b.createTime || b.create_time || 0).getTime()
        return tb - ta
      })
    }
    packages.value = list
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    pkgLoading.value = false
  }
}

// ---------- 时段 ----------
const slotDialog = reactive({ visible: false, pkg: null, loading: false })
const slotGroups = ref([])

const reportViewer = reactive({ visible: false, url: '' })
function openReport(url) {
  reportViewer.url = url
  reportViewer.visible = true
}

async function openSlotsDialog(pkg) {
  slotDialog.visible = true
  slotDialog.pkg = pkg
  slotGroups.value = []
  slotDialog.loading = true
  try {
    const res = await memberAppointmentApi.listSlots(pkg.id)
    slotGroups.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    slotDialog.loading = false
  }
}

async function book(slot) {
  try {
    await ElMessageBox.confirm(
      `确认预约「${slotDialog.pkg.name}」${formatDate(slot.appointDate)} ${slot.timeRange}？将扣除 ${
        slotDialog.pkg.price ?? 0
      } 积分。`,
      '预约确认',
      { type: 'info', confirmButtonText: '确认预约', cancelButtonText: '取消' }
    )
    await memberAppointmentApi.bookAppointment(slot.id, slotDialog.pkg.id)
    ElMessage.success('预约成功')
    slotDialog.visible = false
    // 刷新本地积分
    userStore.setUser({ ...userStore.user, points: (userStore.user?.points || 0) - (slotDialog.pkg.price || 0) })
    loadMyAppointments()
    openSlotsDialog(slotDialog.pkg)
  } catch (e) {
    /* 取消或失败（积分不足/已约满等后端提示） */
  }
}

// ---------- 我的预约 ----------
const myAppointments = ref([])
const mineLoading = ref(false)

async function loadMyAppointments() {
  mineLoading.value = true
  try {
    const res = await memberAppointmentApi.listMyAppointments()
    myAppointments.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    mineLoading.value = false
  }
}

async function cancelBook(row) {
  try {
    await ElMessageBox.confirm(
      `确定取消预约「${row.package?.name || ''}」吗？取消后积分将自动返还。`,
      '取消预约',
      { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' }
    )
    await memberAppointmentApi.cancelAppointment(row.id)
    ElMessage.success('取消成功，积分已返还')
    const refund = row.package?.price || 0
    userStore.setUser({ ...userStore.user, points: (userStore.user?.points || 0) + refund })
    loadMyAppointments()
  } catch (e) {
    /* 取消 */
  }
}

function weekday(dateStr) {
  const d = new Date(String(dateStr).replace(' ', 'T'))
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
}

onMounted(() => {
  loadPackages()
  loadMyAppointments()
})
</script>

<style scoped>
:deep(.el-row) {
  display: flex !important;
  flex-wrap: wrap !important;
  align-items: stretch !important;
}
:deep(.el-row > .el-col) {
  float: none !important;
  display: flex !important;
}
.points-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(120deg, rgba(246, 162, 59, 0.15) 0%, rgba(255, 255, 255, 0.85) 60%);
  font-size: 15px;
  border: 1px solid rgba(246, 162, 59, 0.3);
  position: relative;
  overflow: hidden;
}
.points-bar::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: var(--accent-gold);
  border-radius: 16px 0 0 16px;
}
.points-bar b {
  color: var(--accent-gold);
  font-weight: 700;
  font-size: 17px;
}
.points-bar .tip {
  color: var(--text-sub);
  font-size: 13px;
  margin-left: 8px;
}
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 500;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.pkg-card {
  width: 100%;
  margin-bottom: 16px;
  border-radius: 16px;
  border: 1px solid var(--card-border) !important;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex !important;
  flex-direction: column !important;
}
.pkg-card :deep(.el-card__body) {
  flex: 1 !important;
  display: flex !important;
  flex-direction: column !important;
}
.pkg-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.35) !important;
  border-color: rgba(102, 126, 234, 0.35) !important;
}
.pkg-cover {
  position: relative;
  height: 260px;
  border-radius: 12px;
  overflow: hidden;
  margin-bottom: 14px;
  background: #f4f7fb;
}
.pkg-cover .el-image {
  width: 100%;
  height: 100%;
}
.pkg-price {
  position: absolute;
  right: 12px;
  bottom: 12px;
  background: linear-gradient(135deg, #f6a23b, #ff8a5c) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(246, 162, 59, 0.4);
}
.pkg-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-main);
  letter-spacing: 0.3px;
}
.pkg-people {
  font-size: 13px;
  color: var(--text-sub);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
}
.pkg-items {
  min-height: 62px;
  margin-bottom: 14px;
  overflow: hidden;
}
.pkg-item-tag {
  margin: 0 6px 6px 0;
  border-radius: 8px;
}
.more-items {
  font-size: 12px;
  color: var(--text-muted);
}
.book-btn {
  width: 100% !important;
  height: 44px !important;
  border-radius: 10px !important;
  font-size: 15px !important;
  font-weight: 600;
  letter-spacing: 0.5px;
  margin-top: auto !important;
}
.text-sub {
  color: var(--text-muted);
}
.mine-pkg {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mine-cover {
  width: 48px;
  height: 36px;
  border-radius: 8px;
}
.inline-upload {
  display: inline-block;
  margin-left: 6px;
}
.slot-dialog-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-color);
}
.slot-pkg-name {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-main);
  letter-spacing: 0.3px;
}
.slot-day {
  margin-bottom: 20px;
}
.slot-date {
  font-weight: 600;
  color: var(--primary-color);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
}
.slot-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-left: 10px;
}
.slot-chip {
  margin-left: 0 !important;
  border-radius: 999px !important;
  border: 1px solid rgba(102, 126, 234, 0.35) !important;
  background: rgba(102, 126, 234, 0.08);
  color: var(--text-main);
  font-weight: 500;
  transition: all 0.2s ease;
}
.slot-chip:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.2) !important;
  border-color: var(--primary-color) !important;
  transform: translateY(-1px);
}
.slot-chip.is-disabled {
  opacity: 0.5;
}
.slot-count {
  font-size: 12px;
  color: var(--text-muted);
}

/* 会员端主题：与首页保持一致 */
.points-bar {
  background: linear-gradient(120deg, rgba(217, 150, 62, 0.12) 0%, rgba(255, 255, 255, 0.88) 58%);
  border-color: rgba(217, 150, 62, 0.24);
}

.pkg-card {
  border-color: rgba(219, 231, 225, 0.95) !important;
  background: rgba(255, 255, 255, 0.88);
}

.pkg-card:hover {
  box-shadow: 0 18px 38px rgba(38, 76, 65, 0.11) !important;
  border-color: rgba(47, 125, 109, 0.28) !important;
}

.pkg-cover {
  background: #f0f5f2;
}

.slot-date {
  color: var(--primary-color);
}

.slot-chip {
  border-color: rgba(47, 125, 109, 0.24) !important;
  background: rgba(47, 125, 109, 0.065);
}

.slot-chip:hover:not(:disabled) {
  background: rgba(47, 125, 109, 0.14) !important;
  border-color: var(--primary-color) !important;
}
</style>
