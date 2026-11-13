<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-title"><el-icon><Document /></el-icon>操作日志</div>

      <div class="search-bar log-filter">
        <el-input
          v-model="query.keyword"
          placeholder="操作人 / 操作内容 / 地址 / IP"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-select v-model="query.module" placeholder="全部模块" clearable>
          <el-option v-for="item in moduleOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.status" placeholder="全部状态" clearable>
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-select v-model="query.operateType" placeholder="全部类型" clearable>
          <el-option
            v-for="item in operateTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-date-picker
          v-model="query.timeRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          :default-time="defaultTime"
        />
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
      </div>

      <el-table :data="logs" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column label="操作时间" width="168">
          <template #default="{ row }">{{ formatLogTime(row.operate_time) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="150">
          <template #default="{ row }">
            <div class="operator-name">{{ row.user_real_name || row.user_name || '匿名用户' }}</div>
            <div class="operator-meta">
              <span>{{ row.user_name || '-' }}</span>
              <el-tag v-if="row.role" size="small" :type="row.role === 'ADMIN' ? 'danger' : 'info'">
                {{ roleText(row.role) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作内容" min-width="180">
          <template #default="{ row }">
            <div class="operation-name">{{ row.operation || '-' }}</div>
            <div class="operation-meta">
              {{ row.module || '未分类' }}
              <span>·</span>
              {{ operateTypeText(row.operate_type) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="请求地址" min-width="240">
          <template #default="{ row }">
            <div class="request-line">
              <el-tag size="small" :type="methodTagType(row.request_method)">
                {{ row.request_method || '-' }}
              </el-tag>
              <el-tooltip :content="row.request_url || '-'" placement="top">
                <span class="request-url">{{ row.request_url || '-' }}</span>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="86">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }">
            <span class="duration" :class="{ slow: row.cost_time >= 1000 }">
              {{ row.cost_time ?? '-' }} ms
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" min-width="130">
          <template #default="{ row }">{{ row.ip || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadLogs"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="操作日志详情" width="760px">
      <div v-if="detail" class="log-detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="日志ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="执行状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'danger'" size="small">
              {{ detail.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ formatLogTime(detail.operate_time) }}</el-descriptions-item>
          <el-descriptions-item label="执行耗时">{{ detail.cost_time ?? '-' }} ms</el-descriptions-item>
          <el-descriptions-item label="操作人">
            {{ detail.user_real_name || detail.user_name || '匿名用户' }}
          </el-descriptions-item>
          <el-descriptions-item label="账号 / 角色">
            {{ detail.user_name || '-' }} / {{ roleText(detail.role) }}
          </el-descriptions-item>
          <el-descriptions-item label="模块">{{ detail.module || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作">{{ detail.operation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作类型">{{ operateTypeText(detail.operate_type) }}</el-descriptions-item>
          <el-descriptions-item label="客户端 IP">{{ detail.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求方式">{{ detail.request_method || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求地址">
            <span class="detail-url">{{ detail.request_url || '-' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="后端方法" :span="2">
            <code class="method-code">{{ detail.method || '-' }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="User-Agent" :span="2">
            <span class="ua-text">{{ detail.user_agent || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-block">
          <div class="detail-heading">请求参数</div>
          <pre class="code-panel">{{ formatJson(detail.request_params) }}</pre>
        </div>

        <div v-if="detail.status === 0" class="detail-block">
          <div class="detail-heading error-heading">异常信息</div>
          <el-alert :title="detail.error_msg || '未记录异常信息'" type="error" :closable="false" show-icon />
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Document, Search, RefreshLeft, View } from '@element-plus/icons-vue'
import { adminOperLogApi } from '@/api/adminOperLog'

const moduleOptions = [
  '会员账号',
  '用户管理',
  '预约服务',
  '社区活动',
  '健康管理',
  '健康评测',
  'AI助手',
  '系统配置',
  '文件管理'
]

const operateTypeOptions = [
  { value: 1, label: '新增' },
  { value: 2, label: '修改' },
  { value: 3, label: '删除' },
  { value: 4, label: '业务操作' },
  { value: 5, label: '删除' }
]

const defaultTime = [
  new Date(2000, 0, 1, 0, 0, 0),
  new Date(2000, 0, 1, 23, 59, 59)
]

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  module: '',
  status: null,
  operateType: null,
  timeRange: []
})

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref(null)

async function loadLogs() {
  loading.value = true
  try {
    const res = await adminOperLogApi.list(buildParams())
    logs.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    // 统一异常由请求拦截器提示
  } finally {
    loading.value = false
  }
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.keyword?.trim()) params.keyword = query.keyword.trim()
  if (query.module) params.module = query.module
  if (query.status !== null && query.status !== '') params.status = query.status
  if (query.operateType !== null && query.operateType !== '') params.operateType = query.operateType
  if (Array.isArray(query.timeRange) && query.timeRange.length === 2) {
    params.startTime = query.timeRange[0]
    params.endTime = query.timeRange[1]
  }
  return params
}

function handleSearch() {
  query.page = 1
  loadLogs()
}

function handleReset() {
  query.page = 1
  query.keyword = ''
  query.module = ''
  query.status = null
  query.operateType = null
  query.timeRange = []
  loadLogs()
}

function handleSizeChange() {
  query.page = 1
  loadLogs()
}

async function showDetail(row) {
  try {
    const res = await adminOperLogApi.detail(row.id)
    detail.value = res.data
    detailVisible.value = true
  } catch (e) {
    // 统一异常由请求拦截器提示
  }
}

function formatLogTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19)
}

function formatJson(value) {
  if (!value) return '无'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function roleText(role) {
  return role === 'ADMIN' ? '管理员' : role === 'MEMBER' ? '会员' : role || '-'
}

function operateTypeText(type) {
  return operateTypeOptions.find((item) => item.value === type)?.label || `类型 ${type ?? '-'}`
}

function methodTagType(method) {
  const map = {
    GET: 'info',
    POST: 'success',
    PUT: 'warning',
    PATCH: 'warning',
    DELETE: 'danger'
  }
  return map[method] || 'info'
}

onMounted(loadLogs)
</script>

<style scoped>
.log-filter :deep(.el-date-editor) {
  width: 360px;
}

.operator-name,
.operation-name {
  color: #1f2937;
  font-weight: 600;
}

.operator-meta,
.operation-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 3px;
  color: #9aa3b2;
  font-size: 12px;
}

.request-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.request-url {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #5b6478;
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 12px;
}

.duration {
  color: #475569;
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 12px;
}

.duration.slow {
  color: #e67e22;
  font-weight: 600;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.log-detail {
  color: #374151;
}

.detail-url {
  word-break: break-all;
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 12px;
}

.method-code {
  color: #53627c;
  font-size: 12px;
  word-break: break-all;
}

.ua-text {
  color: #7b8496;
  font-size: 12px;
  word-break: break-all;
}

.detail-block {
  margin-top: 18px;
}

.detail-heading {
  margin-bottom: 8px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
}

.detail-heading.error-heading {
  color: #d14343;
}

.code-panel {
  max-height: 280px;
  overflow: auto;
  margin: 0;
  padding: 14px;
  border: 1px solid rgba(74, 144, 226, 0.12);
  border-radius: 12px;
  background: rgba(30, 41, 59, 0.96);
  color: #dbeafe;
  font-family: 'SF Mono', Consolas, monospace;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1100px) {
  .log-filter :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
