<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-title"><el-icon><Calendar /></el-icon>体检套餐管理</div>
      <div class="page-subtitle">维护体检套餐内容，并为套餐批量生成可预约时段</div>

      <div class="search-bar">
        <el-input
          v-model="query.keyword"
          placeholder="套餐名称关键字"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        <div style="flex:1"></div>
        <el-button type="primary" :icon="Plus" @click="openPackageDialog()">新增套餐</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="封面" width="90">
          <template #default="{ row }">
            <el-image
              :src="buildPkgImage(row)"
              fit="cover"
              style="width:56px;height:40px;border-radius:6px"
              :preview-src-list="[buildPkgImage(row)]"
              preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="套餐名称" min-width="160" />
        <el-table-column prop="price" label="价格(积分)" width="110">
          <template #default="{ row }"><span class="mono">{{ row.price ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column prop="suitable_people" label="适用人群" min-width="130">
          <template #default="{ row }">{{ row.suitable_people || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(packageStatusMap, row.status)">
              {{ dictLabel(packageStatusMap, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="创建时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.create_time) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="260">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Clock" @click="openSlotsDialog(row)">
              时段管理
            </el-button>
            <el-button link type="warning" :icon="Edit" @click="openPackageDialog(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="removePackage(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </div>

    <!-- 套餐新增/编辑弹窗 -->
    <el-dialog
      v-model="pkgDialog.visible"
      :title="pkgDialog.isEdit ? '编辑套餐' : '新增套餐'"
      width="640px"
      @closed="resetPkgForm"
    >
      <el-form
        ref="pkgFormRef"
        :model="pkgForm"
        :rules="pkgRules"
        label-width="100px"
      >
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="pkgForm.name" placeholder="如：老年人体检基础套餐" maxlength="50" />
        </el-form-item>
        <el-form-item label="封面图">
          <div style="display:flex;gap:16px;align-items:flex-start">
            <ImageUploader
              v-model="pkgForm.coverUrl"
              :width="160"
              :height="110"
              accept="image/jpeg,image/png,image/webp"
              :max-size="5"
              placeholder="点击上传封面图"
              tip="支持 JPG/PNG/WebP，建议尺寸 160×110，不超过 5MB"
            />
            <el-input
              v-model="pkgForm.coverUrl"
              placeholder="或粘贴图片URL"
              size="small"
              style="flex:1"
              clearable
            />
          </div>
        </el-form-item>
        <el-form-item label="价格(积分)" prop="price">
          <el-input-number v-model="pkgForm.price" :min="0" :step="100" />
        </el-form-item>
        <el-form-item label="适用人群" prop="suitablePeople">
          <el-input v-model="pkgForm.suitablePeople" placeholder="如：60岁以上老年人" />
        </el-form-item>
        <el-form-item label="服务项目" prop="itemsText">
          <el-input
            v-model="pkgForm.itemsText"
            type="textarea"
            :rows="4"
            placeholder="每行一个服务项目，如：血常规检查"
          />
        </el-form-item>
        <el-form-item label="套餐描述" prop="description">
          <el-input v-model="pkgForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="pkgForm.status">
            <el-radio value="ENABLED">上架</el-radio>
            <el-radio value="DISABLED">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pkgDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="pkgDialog.saving" @click="submitPackage">保存</el-button>
      </template>
    </el-dialog>

    <!-- 时段管理弹窗 -->
    <el-dialog v-model="slotDialog.visible" title="预约时段管理" width="820px" top="6vh">
      <div v-if="slotDialog.pkg" class="slot-head">
        <span class="slot-pkg-name">套餐：{{ slotDialog.pkg.name }}</span>
        <el-tag :type="dictType(packageStatusMap, slotDialog.pkg.status)">
          {{ dictLabel(packageStatusMap, slotDialog.pkg.status) }}
        </el-tag>
      </div>

      <!-- 批量生成时段 -->
      <el-card class="slot-generate" shadow="never">
        <template #header><b>批量生成时段</b></template>
        <el-form :model="slotForm" inline label-width="80px">
          <el-form-item label="开始日期">
            <el-date-picker
              v-model="slotForm.startDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="开始日期"
              :disabled-date="disablePastDate"
            />
          </el-form-item>
          <el-form-item label="结束日期">
            <el-date-picker
              v-model="slotForm.endDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="结束日期"
              :disabled-date="disablePastDate"
            />
          </el-form-item>
          <el-form-item label="每日容量">
            <el-input-number v-model="slotForm.maxCount" :min="1" :max="500" />
          </el-form-item>
          <el-form-item label="时段" style="width:100%">
            <div class="slot-picker">
              <el-checkbox-group v-model="slotForm.selectedSlots" class="slot-checkboxes">
                <el-checkbox
                  v-for="s in hourlySlots"
                  :key="s"
                  :value="s"
                  :disabled="isSlotDisabled(s)"
                  border
                  class="slot-checkbox"
                >{{ s }}</el-checkbox>
              </el-checkbox-group>
              <div class="slot-picker-actions">
                <el-button link type="primary" @click="selectAllSlots">全选</el-button>
                <el-button link type="danger" @click="clearSlots">清空</el-button>
                <span class="slot-count-tip">已选 {{ validSelectedSlots.length }} / {{ slotForm.selectedSlots.length }} 个有效时段</span>
              </div>
              <div v-if="disabledSlotTip" class="slot-warning-tip">
                <el-icon><Warning /></el-icon> {{ disabledSlotTip }}
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="MagicStick" :loading="slotDialog.generating" @click="generateSlots">
              批量生成
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 时段列表 -->
      <div class="slot-toolbar">
        <el-button
          type="danger"
          plain
          :icon="Delete"
          :disabled="!selectedSlots.length"
          @click="batchRemoveSlots"
        >
          批量删除（{{ selectedSlots.length }}）
        </el-button>
        <el-button :icon="Refresh" @click="loadSlots">刷新</el-button>
      </div>
      <el-table
        :data="slotList"
        v-loading="slotDialog.loading"
        stripe
        max-height="360"
        @selection-change="(rows) => (selectedSlots = rows)"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="appointDate" label="预约日期" width="130">
          <template #default="{ row }">{{ formatDate(row.appointDate) }}</template>
        </el-table-column>
        <el-table-column prop="timeRange" label="时间段" width="140" />
        <el-table-column label="预约情况" width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.currentCount }} / {{ row.maxCount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(slotStatusMap, row.status)">
              {{ dictLabel(slotStatusMap, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="danger" :icon="Delete" @click="removeSlot(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="slotDialog.visible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  RefreshLeft,
  Plus,
  Edit,
  Delete,
  Calendar,
  Clock,
  MagicStick,
  Refresh,
  Warning
} from '@element-plus/icons-vue'
import { adminAppointmentApi } from '@/api/adminAppointment'
import ImageUploader from '@/components/ImageUploader.vue'
import {
  formatDateTime,
  formatDate,
  dictLabel,
  dictType,
  packageStatusMap,
  slotStatusMap,
  parseItems,
  buildPkgImage
} from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, keyword: '' })

// ---------- 套餐弹窗 ----------
const pkgFormRef = ref()
const pkgDialog = reactive({ visible: false, isEdit: false, saving: false, id: null })
const pkgForm = reactive({
  name: '',
  coverUrl: '',
  description: '',
  price: 0,
  suitablePeople: '',
  itemsText: '',
  status: 'ENABLED'
})
const pkgRules = {
  name: [{ required: true, message: '请输入套餐名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

function resetPkgForm() {
  Object.assign(pkgForm, {
    name: '',
    coverUrl: '',
    description: '',
    price: 0,
    suitablePeople: '',
    itemsText: '',
    status: 'ENABLED'
  })
  pkgDialog.id = null
  pkgFormRef.value?.clearValidate()
}

function openPackageDialog(row) {
  resetPkgForm()
  if (row) {
    pkgDialog.isEdit = true
    pkgDialog.id = row.id
    Object.assign(pkgForm, {
      name: row.name,
      coverUrl: row.cover_url || '',
      description: row.description || '',
      price: row.price ?? 0,
      suitablePeople: row.suitable_people || '',
      itemsText: parseItems(row.items).join('\n'),
      status: row.status || 'ENABLED'
    })
  } else {
    pkgDialog.isEdit = false
  }
  pkgDialog.visible = true
}

async function submitPackage() {
  await pkgFormRef.value.validate(async (valid) => {
    if (!valid) return
    const itemsArr = pkgForm.itemsText
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean)
    const pkgId = pkgDialog.isEdit ? pkgDialog.id : null
    if (pkgForm.coverUrl) {
      const { saveImage, pkgKey } = await import('@/utils/imageCache')
      saveImage(pkgKey(pkgId || 'new:' + Date.now()), pkgForm.coverUrl)
    }
    const body = {
      name: pkgForm.name.trim(),
      coverUrl: pkgForm.coverUrl || null,
      cover_url: pkgForm.coverUrl || null,
      description: pkgForm.description?.trim() || null,
      price: pkgForm.price,
      suitablePeople: pkgForm.suitablePeople?.trim() || null,
      suitable_people: pkgForm.suitablePeople?.trim() || null,
      items: itemsArr.length > 1 ? JSON.stringify(itemsArr) : itemsArr[0] || '',
      status: pkgForm.status
    }
    pkgDialog.saving = true
    try {
      let pkgIdActual = pkgId
      if (pkgDialog.isEdit) {
        await adminAppointmentApi.updatePackage(pkgDialog.id, body)
        ElMessage.success('更新成功')
      } else {
        const res = await adminAppointmentApi.createPackage(body)
        pkgIdActual = res?.data?.id || res?.data?.packageId || null
        ElMessage.success('创建成功')
      }
      if (pkgForm.coverUrl && pkgIdActual && pkgId !== String(pkgIdActual)) {
        const { saveImage, pkgKey, removeImage } = await import('@/utils/imageCache')
        if (pkgId) removeImage(pkgKey(pkgId))
        saveImage(pkgKey(pkgIdActual), pkgForm.coverUrl)
      }
      pkgDialog.visible = false
      loadList()
    } catch (e) {
      // 拦截器提示
    } finally {
      pkgDialog.saving = false
    }
  })
}

async function removePackage(row) {
  try {
    await ElMessageBox.confirm(`确定删除套餐「${row.name}」吗？删除后不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await adminAppointmentApi.deletePackage(row.id)
    const { removeImage, pkgKey } = await import('@/utils/imageCache')
    removeImage(pkgKey(row.id))
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    /* 取消 */
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await adminAppointmentApi.listPackages({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined
    })
    tableData.value = (res.data.list || []).sort((a, b) => {
      const ta = new Date(a.create_time || a.createTime || 0).getTime()
      const tb = new Date(b.create_time || b.createTime || 0).getTime()
      return tb - ta
    })
    total.value = res.data.total || 0
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}
function handleReset() {
  query.keyword = ''
  query.page = 1
  loadList()
}

// ---------- 时段管理 ----------
const slotDialog = reactive({
  visible: false,
  pkg: null,
  loading: false,
  generating: false
})
const slotList = ref([])
const selectedSlots = ref([])
const hourlySlots = Array.from({ length: 24 }, (_, i) => {
  const h = String(i).padStart(2, '0')
  const next = String((i + 1) % 24).padStart(2, '0')
  return `${h}:00-${next}:00`
})

const slotForm = reactive({
  startDate: '',
  endDate: '',
  maxCount: 10,
  selectedSlots: ['09:00-10:00', '10:00-11:00', '14:00-15:00', '15:00-16:00']
})

const todayStr = computed(() => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
})

function disablePastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function getCurrentHour() {
  return new Date().getHours()
}

function isSlotDisabled(slot) {
  const isToday = slotForm.startDate === todayStr.value || slotForm.endDate === todayStr.value
  if (!isToday) return false
  const currentHour = getCurrentHour()
  const slotStartHour = parseInt(slot.split('-')[0], 10)
  return slotStartHour <= currentHour
}

const disabledSlotTip = computed(() => {
  const disabled = hourlySlots.filter(s => isSlotDisabled(s))
  if (disabled.length === 0) return ''
  const currentHour = getCurrentHour()
  return `当前时间 ${String(currentHour).padStart(2, '0')}:00 已过以下时段：${disabled.join('、')}，无法添加`
})

const validSelectedSlots = computed(() => {
  return slotForm.selectedSlots.filter(s => !isSlotDisabled(s))
})

function selectAllSlots() {
  slotForm.selectedSlots = hourlySlots.filter(s => !isSlotDisabled(s))
}
function clearSlots() {
  slotForm.selectedSlots = []
}

async function openSlotsDialog(pkg) {
  slotDialog.visible = true
  slotDialog.pkg = pkg
  slotList.value = []
  selectedSlots.value = []
  await loadSlots()
}

async function loadSlots() {
  if (!slotDialog.pkg) return
  slotDialog.loading = true
  try {
    const res = await adminAppointmentApi.listSlots(slotDialog.pkg.id)
    slotList.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    slotDialog.loading = false
  }
}

async function generateSlots() {
  if (!slotForm.startDate || !slotForm.endDate) {
    ElMessage.warning('请选择开始与结束日期')
    return
  }
  if (slotForm.startDate < todayStr.value) {
    ElMessage.warning('开始日期不能早于今天')
    return
  }

  const timeRanges = validSelectedSlots.value
  if (!slotForm.selectedSlots.length) {
    ElMessage.warning('请至少选择一个时段')
    return
  }
  if (!timeRanges.length) {
    ElMessage.warning('所选时段均已过期，无法生成')
    return
  }

  const filteredCount = slotForm.selectedSlots.length - timeRanges.length
  if (filteredCount > 0) {
    ElMessage.info(`已自动过滤 ${filteredCount} 个过期时段`)
  }

  slotDialog.generating = true
  try {
    const res = await adminAppointmentApi.batchCreateSlots({
      packageId: slotDialog.pkg.id,
      startDate: slotForm.startDate,
      endDate: slotForm.endDate,
      timeRanges,
      maxCount: slotForm.maxCount
    })
    ElMessage.success(res.message || '批量生成成功')
    loadSlots()
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    slotDialog.generating = false
  }
}

async function removeSlot(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除 ${formatDate(row.appointDate)} ${row.timeRange} 这个时段吗？`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    await adminAppointmentApi.deleteSlot(row.id)
    ElMessage.success('删除成功')
    loadSlots()
  } catch (e) {
    /* 取消或存在预约记录时后端会返回错误提示 */
  }
}

async function batchRemoveSlots() {
  if (!selectedSlots.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedSlots.value.length} 个时段吗？存在预约记录的时段将跳过。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
    const ids = selectedSlots.value.map((s) => s.id)
    const res = await adminAppointmentApi.batchDeleteSlots(ids)
    ElMessage.success(
      `批量删除完成：成功 ${res.data?.successCount ?? 0} 个，失败 ${res.data?.failCount ?? 0} 个`
    )
    loadSlots()
  } catch (e) {
    /* 取消 */
  }
}

onMounted(loadList)
</script>

<style scoped>
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.img-placeholder {
  width: 56px;
  height: 40px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--text-muted);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.slot-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.slot-pkg-name {
  font-size: 16px;
  font-weight: 600;
}
.slot-generate {
  margin-bottom: 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--card-border);
}
.slot-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.slot-picker {
  width: 100%;
}
.slot-checkboxes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.slot-checkbox {
  margin: 0 !important;
}
.slot-checkbox :deep(.el-checkbox__label) {
  padding: 4px 10px;
  font-size: 13px;
}
.slot-picker-actions {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.slot-count-tip {
  font-size: 13px;
  color: var(--text-muted);
  margin-left: 8px;
}
.slot-warning-tip {
  margin-top: 8px;
  padding: 8px 12px;
  background: var(--el-color-warning-light-9);
  border-radius: 4px;
  font-size: 12px;
  color: var(--el-color-warning);
  display: flex;
  align-items: center;
  gap: 6px;
}
</style>