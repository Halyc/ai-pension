<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-title"><el-icon><Tickets /></el-icon>预约记录管理</div>
      <div class="page-subtitle">查看会员体检预约，登记体检报告并标记完成</div>

      <div class="search-bar">
        <el-select v-model="query.status" placeholder="预约状态" clearable>
          <el-option label="待体检" value="CONFIRMED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-date-picker
          v-model="query.date"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="预约日期（按创建时间）"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        <div style="flex:1"></div>
      </div>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="预约ID" width="80" />
        <el-table-column prop="userName" label="会员姓名" width="120">
          <template #default="{ row }">{{ row.userName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="packageName" label="体检套餐" min-width="160">
          <template #default="{ row }">{{ row.packageName || '-' }}</template>
        </el-table-column>
        <el-table-column label="预约时段" min-width="180">
          <template #default="{ row }">
            <template v-if="row.appointDate || row.timeRange">
              {{ row.appointDate || '' }} <span v-if="row.appointDate && row.timeRange">·</span> {{ row.timeRange || '' }}
            </template>
            <span v-else class="text-sub">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(appointmentStatusMap, row.status)">
              {{ dictLabel(appointmentStatusMap, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reportUrl" label="体检报告" min-width="180">
          <template #default="{ row }">
            <el-link
              v-if="row.reportUrl"
              type="primary"
              :underline="false"
              @click="openReport(row.reportUrl)"
            >
              <el-icon><View /></el-icon> 查看报告
            </el-link>
            <span v-else class="text-sub">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="预约时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <template v-if="row.status === 'CONFIRMED'">
              <el-button link type="primary" :icon="UploadFilled" @click="openUploadDialog(row)">
                {{ row.reportUrl ? '更换报告' : '上传报告' }}
              </el-button>
              <el-button link type="success" :icon="CircleCheck" @click="openCompleteDialog(row)">
                完成体检
              </el-button>
            </template>
            <el-button
              v-else-if="row.status === 'COMPLETED'"
              link
              type="warning"
              :icon="UploadFilled"
              @click="openUploadDialog(row)"
            >更换报告</el-button>
            <span v-else class="text-sub">-</span>
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

    <!-- 完成体检弹窗（带上传报告功能） -->
    <el-dialog v-model="completeDialog.visible" title="完成体检登记" width="560px">
      <div class="confirm-info">
        <div class="info-row">
          <span class="info-label">预约ID</span>
          <span class="info-value mono">{{ completeDialog.row?.id }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">会员姓名</span>
          <span class="info-value highlight">{{ completeDialog.row?.userName || '-' }}</span>
          <span class="info-sub">（ID: {{ completeDialog.row?.userId }}）</span>
        </div>
        <div class="info-row">
          <span class="info-label">体检套餐</span>
          <span class="info-value">{{ completeDialog.row?.packageName || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">预约时间</span>
          <span class="info-value">{{ completeDialog.row?.appointDate }} {{ completeDialog.row?.timeRange }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">当前状态</span>
          <el-tag type="warning" size="small">{{ statusText(completeDialog.row?.status) }}</el-tag>
        </div>

        <!-- 体检报告上传区 -->
        <div class="upload-section">
          <div class="upload-label"><el-icon><Document /></el-icon> 上传体检报告</div>
          <el-upload
            :show-file-list="false"
            :before-upload="(f) => handleUploadReport(f)"
            accept=".pdf,.jpg,.jpeg,.png,.bmp,.gif"
          >
            <el-button size="small" :loading="completeDialog.uploading" :icon="Upload">
              {{ completeDialog.reportUrl ? '重新上传' : '选择文件上传' }}
            </el-button>
          </el-upload>
          <div v-if="completeDialog.reportUrl" class="report-preview">
            <el-link type="primary" :underline="false" @click="openReport(completeDialog.reportUrl)">
              <el-icon><View /></el-icon> 预览已上传报告
            </el-link>
            <span class="upload-hint">（上传会替换之前的报告）</span>
          </div>
          <div v-else class="text-sub" style="font-size:12px;margin-top:4px">可选上传 PDF / 图片，也可后续在列表中单独上传</div>
        </div>

        <div class="confirm-tip">确认将该预约标记为「已完成」？</div>
      </div>
      <template #footer>
        <el-button @click="completeDialog.visible = false">取消</el-button>
        <el-button type="success" :loading="completeDialog.saving" @click="submitComplete">
          确认完成
        </el-button>
      </template>
    </el-dialog>

    <!-- 上传/更换报告弹窗（通用：待体检 & 已完成均可上传或重新上传） -->
    <el-dialog v-model="uploadDialog.visible" title="上传体检报告" width="520px">
      <div class="confirm-info">
        <div class="info-row">
          <span class="info-label">预约ID</span>
          <span class="info-value mono">{{ uploadDialog.row?.id }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">会员</span>
          <span class="info-value highlight">{{ uploadDialog.row?.userName || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">当前报告</span>
          <template v-if="uploadDialog.row?.reportUrl">
            <el-link type="primary" :underline="false" @click="openReport(uploadDialog.row.reportUrl)">
              <el-icon><View /></el-icon> 查看现有报告
            </el-link>
          </template>
          <span v-else class="text-sub">（尚未上传）</span>
        </div>

        <div class="upload-section">
          <div class="upload-label"><el-icon><Document /></el-icon> 选择要上传的文件</div>
          <el-upload
            :show-file-list="false"
            :before-upload="(f) => handleUploadReport(f, true)"
            accept=".pdf,.jpg,.jpeg,.png,.bmp,.gif"
          >
            <el-button type="primary" :loading="uploadDialog.uploading" :icon="Upload">
              选择文件上传
            </el-button>
          </el-upload>
          <div v-if="uploadDialog.tempUrl" class="report-preview">
            <el-link type="success" :underline="false" @click="openReport(uploadDialog.tempUrl)">
              <el-icon><View /></el-icon> 预览新上传的报告
            </el-link>
            <span class="upload-hint">（确认后自动替换原有报告）</span>
          </div>
          <div v-else class="text-sub" style="font-size:12px;margin-top:4px">支持 PDF / JPG / PNG 等图片格式，单文件建议不超过 10MB</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="uploadDialog.visible = false">取消</el-button>
        <el-button type="primary" :disabled="!uploadDialog.tempUrl" @click="confirmUpload">
          确认使用该报告
        </el-button>
      </template>
    </el-dialog>

    <!-- 图片预览（Element Plus 内置） -->
    <el-image-viewer
      v-if="viewer.visible"
      :url-list="[viewer.url]"
      :initial-index="0"
      @close="viewer.visible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search,
  RefreshLeft,
  Tickets,
  CircleCheck,
  View,
  Upload,
  UploadFilled,
  Document
} from '@element-plus/icons-vue'
import { adminAppointmentApi } from '@/api/adminAppointment'
import {
  formatDateTime,
  dictLabel,
  dictType,
  appointmentStatusMap
} from '@/utils/format'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: '', date: '' })

const completeDialog = reactive({
  visible: false,
  row: null,
  saving: false,
  uploading: false,
  reportUrl: ''
})

const uploadDialog = reactive({
  visible: false,
  row: null,
  uploading: false,
  tempUrl: ''
})

const viewer = reactive({
  visible: false,
  url: ''
})

function openReport(url) {
  viewer.url = url
  viewer.visible = true
}

async function loadList() {
  loading.value = true
  try {
    const res = await adminAppointmentApi.listAppointments({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      date: query.date || undefined
    })
    tableData.value = res.data.list || []
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
  query.status = ''
  query.date = ''
  query.page = 1
  loadList()
}

function openCompleteDialog(row) {
  completeDialog.row = row
  completeDialog.reportUrl = row.reportUrl || ''
  completeDialog.visible = true
}

function openUploadDialog(row) {
  uploadDialog.row = row
  uploadDialog.tempUrl = ''
  uploadDialog.visible = true
}

function handleUploadReport(file, isStandalone = false) {
  const row = isStandalone ? uploadDialog.row : completeDialog.row
  const target = isStandalone ? uploadDialog : completeDialog
  if (!row) return false

  const isImage = /image\/.*/.test(file.type)
  const isPdf = file.type === 'application/pdf' || /\.pdf$/i.test(file.name)
  if (!isImage && !isPdf) {
    ElMessage.error('仅支持 PDF 或图片格式文件')
    return false
  }

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }

  target.uploading = true
  adminAppointmentApi.uploadReport(row.id, file).then((res) => {
    const url = res.data?.report_url || res.data?.reportUrl || ''
    if (isStandalone) {
      uploadDialog.tempUrl = url
    } else {
      completeDialog.reportUrl = url
    }
    ElMessage.success('体检报告上传成功')
  }).catch(() => {
  }).finally(() => {
    target.uploading = false
  })
  return false
}

async function confirmUpload() {
  if (!uploadDialog.tempUrl) return
  uploadDialog.row.reportUrl = uploadDialog.tempUrl
  uploadDialog.visible = false
  ElMessage.success('报告已更新')
  await loadList()
}

function statusText(status) {
  return appointmentStatusMap[status]?.label || status || '-'
}

async function submitComplete() {
  completeDialog.saving = true
  try {
    await adminAppointmentApi.completeAppointment(completeDialog.row.id)
    ElMessage.success('体检已完成')
    completeDialog.visible = false
    loadList()
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    completeDialog.saving = false
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
.text-sub {
  color: #b6bdc9;
}
.confirm-info {
  font-size: 14px;
  line-height: 2;
}
.info-row {
  display: flex;
  align-items: center;
  padding: 4px 0;
  border-bottom: 1px dashed #f0f0f0;
}
.info-row:last-of-type {
  border-bottom: none;
}
.info-label {
  width: 80px;
  color: #909399;
  flex-shrink: 0;
}
.info-value {
  color: #303133;
  font-weight: 500;
}
.info-value.highlight {
  color: #409eff;
  font-size: 15px;
}
.info-sub {
  color: #b6bdc9;
  font-size: 12px;
  margin-left: 8px;
}
.confirm-tip {
  margin-top: 12px;
  color: #f56c6c;
  font-weight: 500;
}
.upload-hint {
  margin-left: 8px;
  color: #909399;
  font-size: 12px;
}
.upload-section {
  margin-top: 12px;
  padding: 12px;
  background: #f7f9fc;
  border-radius: 6px;
  border: 1px dashed #dcdfe6;
}
.upload-label {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.report-preview {
  margin-top: 8px;
  display: flex;
  align-items: center;
}
</style>