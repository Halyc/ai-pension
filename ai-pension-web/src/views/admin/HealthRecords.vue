<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-title"><el-icon><FirstAidKit /></el-icon>健康档案管理</div>

      <div class="search-bar">
        <el-input v-model="recordQuery.keyword" placeholder="姓名 / 手机号 / 记录ID" clearable @keyup.enter="loadRecords" />
        <el-input-number v-model="recordQuery.userId" :min="1" placeholder="指定用户ID" controls-position="right" />
        <el-button type="primary" :icon="Search" @click="loadRecords">查询</el-button>
        <el-button :icon="RefreshLeft" @click="resetRecordQuery">重置</el-button>
      </div>

      <el-table :data="records" v-loading="recordLoading" stripe style="width:100%">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column label="会员" min-width="130">
          <template #default="{ row }">
            <div v-if="row.user">
              <div class="user-name">{{ row.user.real_name || '未填写' }}</div>
              <div class="user-phone">{{ row.user.phone }}</div>
            </div>
            <span v-else class="muted">用户已删除</span>
          </template>
        </el-table-column>
        <el-table-column label="记录时间" min-width="140">
          <template #default="{ row }">{{ formatDateTime(row.recorded_at) }}</template>
        </el-table-column>
        <el-table-column label="血压(mmHg)" min-width="110">
          <template #default="{ row }">
            <span class="mono">{{ row.systolic ?? '-' }} / {{ row.diastolic ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="血糖" width="70">
          <template #default="{ row }"><span class="mono">{{ row.blood_sugar ?? '-' }}</span></template>
        </el-table-column>
        <el-table-column label="心率" width="60">
          <template #default="{ row }"><span class="mono">{{ row.heart_rate ?? '-' }}</span></template>
        </el-table-column>
        <el-table-column label="体重/BMI" min-width="110">
          <template #default="{ row }">
            <span class="mono">{{ row.weight ?? '-' }}kg</span>
            <span class="divider">/</span>
            <el-tag :type="bmiType(row.bmi)" size="small">{{ row.bmi ?? '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="AI 指导" min-width="80">
          <template #default="{ row }">
            <el-tag v-if="row.guidance" type="success" effect="light" size="small">已生成</el-tag>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div style="display:flex; align-items:center; white-space:nowrap; gap:4px">
              <el-button link type="primary" :icon="View" @click="showRecordDetail(row)">详情</el-button>
              <el-button link type="danger" :icon="Delete" @click="deleteRecord(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="recordQuery.page"
          v-model:page-size="recordQuery.size"
          :page-sizes="[10, 20, 50]"
          :total="recordTotal"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadRecords"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="健康记录详情" width="680px">
      <div v-if="detailRecord">
        <el-descriptions v-if="detailRecord.user" :column="2" border size="small" title="会员信息">
          <el-descriptions-item label="姓名">{{ detailRecord.user.real_name || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detailRecord.user.phone }}</el-descriptions-item>
          <el-descriptions-item label="会员等级">{{ detailRecord.user.member_level || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions class="detail-section" :column="2" border size="small" title="体征数据">
          <el-descriptions-item label="记录时间" :span="2">{{ formatDateTime(detailRecord.recorded_at) }}</el-descriptions-item>
          <el-descriptions-item label="血压">{{ detailRecord.systolic ?? '-' }} / {{ detailRecord.diastolic ?? '-' }} mmHg</el-descriptions-item>
          <el-descriptions-item label="血糖">{{ detailRecord.blood_sugar ?? '-' }} mmol/L</el-descriptions-item>
          <el-descriptions-item label="心率">{{ detailRecord.heart_rate ?? '-' }} 次/分</el-descriptions-item>
          <el-descriptions-item label="体重">{{ detailRecord.weight ?? '-' }} kg</el-descriptions-item>
          <el-descriptions-item label="BMI">
            <el-tag :type="bmiType(detailRecord.bmi)">{{ detailRecord.bmi ?? '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailRecord.memo || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section" v-if="detailRecord.guidance">
          <div class="detail-subtitle">
            AI 健康指导
            <el-tag :type="guidanceTagType(detailRecord.guidance.type)" size="small" style="margin-left:8px">{{ detailRecord.guidance.type }}</el-tag>
          </div>
          <div class="markdown-body" v-html="renderMarkdown(detailRecord.guidance.content)"></div>
        </div>
        <div v-else class="detail-muted">暂未生成 AI 健康指导</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FirstAidKit, Search, RefreshLeft, View, Delete } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { adminHealthApi } from '@/api/adminHealth'
import { formatDateTime } from '@/utils/format'

marked.setOptions({ breaks: true, gfm: true })

const recordQuery = reactive({ page: 1, size: 10, keyword: '', userId: null })
const recordLoading = ref(false)
const records = ref([])
const recordTotal = ref(0)

const detailVisible = ref(false)
const detailRecord = ref(null)

function loadRecords() {
  recordLoading.value = true
  const params = { page: recordQuery.page, size: recordQuery.size }
  if (recordQuery.keyword) params.keyword = recordQuery.keyword
  if (recordQuery.userId) params.userId = recordQuery.userId
  adminHealthApi.listRecords(params).then(res => {
    records.value = res.data.list || []
    recordTotal.value = res.data.total || 0
  }).catch(() => {}).finally(() => recordLoading.value = false)
}

function resetRecordQuery() {
  recordQuery.keyword = ''
  recordQuery.userId = null
  recordQuery.page = 1
  loadRecords()
}

function showRecordDetail(row) {
  adminHealthApi.getRecordDetail(row.id).then(res => {
    detailRecord.value = res.data
    detailVisible.value = true
  }).catch(() => {})
}

function deleteRecord(row) {
  ElMessageBox.confirm(
    `确定删除 ${row.user?.real_name || ''} 在 ${formatDateTime(row.recorded_at)} 的健康记录？关联的 AI 指导也会一并删除。`,
    '删除确认',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
  ).then(() => {
    adminHealthApi.deleteRecord(row.id).then(() => {
      ElMessage.success('删除成功')
      loadRecords()
    }).catch(() => {})
  }).catch(() => {})
}

function renderMarkdown(text) {
  if (!text) return ''
  try { return marked.parse(text) } catch { return text }
}

function bmiType(bmi) {
  if (bmi == null) return 'info'
  if (bmi < 18.5 || bmi >= 28) return 'danger'
  if (bmi >= 24) return 'warning'
  return 'success'
}

function guidanceTagType(type) {
  const map = {
    '血压关注': 'danger',
    '血糖关注': 'warning',
    '心率关注': 'warning',
    '体重管理': 'warning',
    '日常健康': 'success',
    '通用指导': 'info'
  }
  return map[type] || 'info'
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.search-bar :deep(.el-input-number) {
  width: 180px;
}

.search-bar :deep(.el-button) {
  margin-left: 4px;
}

.user-name { font-weight: 600; color: #1f2937; }
.user-phone { font-size: 12px; color: #9ca3af; }
.mono { font-family: 'SF Mono', Consolas, monospace; }
.muted { color: #9ca3af; }
.divider { color: #d1d5db; margin: 0 4px; }

.detail-section { margin-top: 16px; }
.detail-subtitle { font-weight: 600; color: #374151; margin-bottom: 8px; font-size: 14px; }
.detail-muted { color: #9ca3af; font-size: 13px; padding: 8px 0; }
</style>