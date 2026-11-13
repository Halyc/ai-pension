<template>
  <div class="page-container">
    <el-row :gutter="16" class="health-top-row">
      <!-- 体征录入 -->
      <el-col :xs="24" :md="9" :lg="8">
        <div class="page-card health-form-card">
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="92px"
            v-loading="submitting"
          >
            <el-form-item label="收缩压" prop="systolic">
              <el-input-number v-model="form.systolic" :min="50" :max="260" controls-position="right" style="width:100%">
                <template #append>mmHg</template>
              </el-input-number>
            </el-form-item>
            <el-form-item label="舒张压" prop="diastolic">
              <el-input-number v-model="form.diastolic" :min="30" :max="180" controls-position="right" style="width:100%">
                <template #append>mmHg</template>
              </el-input-number>
            </el-form-item>
            <el-form-item label="血糖" prop="blood_sugar">
              <el-input-number v-model="form.blood_sugar" :min="1" :max="40" :precision="1" :step="0.5" controls-position="right" style="width:100%">
                <template #append>mmol/L</template>
              </el-input-number>
            </el-form-item>
            <el-form-item label="心率" prop="heart_rate">
              <el-input-number v-model="form.heart_rate" :min="30" :max="220" controls-position="right" style="width:100%">
                <template #append>次/分</template>
              </el-input-number>
            </el-form-item>
            <el-form-item label="体重" prop="weight">
              <el-input-number v-model="form.weight" :min="20" :max="300" :precision="1" :step="0.5" controls-position="right" style="width:100%">
                <template #append>kg</template>
              </el-input-number>
            </el-form-item>
            <el-form-item label="身高" prop="height">
              <el-input-number v-model="form.height" :min="100" :max="230" :precision="1" controls-position="right" style="width:100%">
                <template #append>cm</template>
              </el-input-number>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.memo" type="textarea" :rows="2" placeholder="今日身体感受（选填）" maxlength="200" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :icon="Check" class="submit-btn" @click="submitRecord">
                保存记录
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 正常参考范围 -->
          <div class="reference-box">
            <div class="reference-toggle" @click="refOpen = !refOpen">
              <div class="reference-toggle-left">
                <el-icon class="ref-icon"><Reading /></el-icon>
                <span class="ref-title">正常参考范围</span>
                <el-tag size="small" type="info" effect="light">点击展开</el-tag>
              </div>
              <el-icon class="ref-arrow" :class="{ expanded: refOpen }"><ArrowDown /></el-icon>
            </div>
            <Transition name="fade-slide">
              <div v-if="refOpen" class="reference-table">
                <div class="ref-item">
                  <div class="ref-label">
                    <span class="ref-dot" style="background:#ff7a7a"></span>
                    收缩压（高压）
                  </div>
                  <div class="ref-value">
                    <span class="ref-normal">90 ~ 140</span>
                    <span class="ref-unit">mmHg</span>
                    <span class="ref-note">老年人可放宽至 90 ~ 150</span>
                  </div>
                </div>
                <div class="ref-item">
                  <div class="ref-label">
                    <span class="ref-dot" style="background:#4facfe"></span>
                    舒张压（低压）
                  </div>
                  <div class="ref-value">
                    <span class="ref-normal">60 ~ 90</span>
                    <span class="ref-unit">mmHg</span>
                  </div>
                </div>
                <div class="ref-item">
                  <div class="ref-label">
                    <span class="ref-dot" style="background:#f6a23b"></span>
                    空腹血糖
                  </div>
                  <div class="ref-value">
                    <span class="ref-normal">3.9 ~ 6.1</span>
                    <span class="ref-unit">mmol/L</span>
                    <span class="ref-note">餐后 2h 正常 < 7.8</span>
                  </div>
                </div>
                <div class="ref-item">
                  <div class="ref-label">
                    <span class="ref-dot" style="background:#e85fa8"></span>
                    静息心率
                  </div>
                  <div class="ref-value">
                    <span class="ref-normal">60 ~ 100</span>
                    <span class="ref-unit">次/分</span>
                    <span class="ref-note">老年人 55 ~ 100 也属正常</span>
                  </div>
                </div>
                <div class="ref-item">
                  <div class="ref-label">
                    <span class="ref-dot" style="background:#2f9e8f"></span>
                    BMI（身体质量指数）
                  </div>
                  <div class="ref-value">
                    <span class="ref-normal">18.5 ~ 23.9</span>
                    <span class="ref-unit">kg/m²</span>
                    <div class="ref-bmi">
                      <el-tag size="small" type="danger" effect="plain">过轻 &lt; 18.5</el-tag>
                      <el-tag size="small" type="success" effect="plain">正常 18.5-23.9</el-tag>
                      <el-tag size="small" type="warning" effect="plain">超重 24-27.9</el-tag>
                      <el-tag size="small" type="danger" effect="plain">肥胖 ≥ 28</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </Transition>
          </div>

        </div>
      </el-col>

      <!-- 趋势图表 -->
      <el-col :xs="24" :md="15" :lg="16">
        <div class="page-card">
          <div class="toolbar">
            <div class="page-title" style="margin:0">
              <el-icon><DataLine /></el-icon>健康趋势
            </div>
            <el-radio-group v-model="months" @change="reloadAll">
              <el-radio-button :value="1">近1月</el-radio-button>
              <el-radio-button :value="3">近3月</el-radio-button>
              <el-radio-button :value="6">近6月</el-radio-button>
              <el-radio-button :value="12">近1年</el-radio-button>
            </el-radio-group>
          </div>

          <el-row :gutter="12" v-loading="trendLoading" element-loading-text="加载健康趋势数据...">
            <template v-if="hasTrendData">
              <el-col :xs="24" :sm="12">
                <div ref="bpChartRef" class="chart-box"></div>
              </el-col>
              <el-col :xs="24" :sm="12">
                <div ref="sugarChartRef" class="chart-box"></div>
              </el-col>
              <el-col :xs="24" :sm="12">
                <div ref="heartChartRef" class="chart-box"></div>
              </el-col>
              <el-col :xs="24" :sm="12">
                <div ref="weightChartRef" class="chart-box"></div>
              </el-col>
            </template>
            <el-col v-else :span="24">
              <div class="trend-empty">
                <div class="trend-empty-icon">📊</div>
                <div class="trend-empty-title">暂无健康数据</div>
                <div class="trend-empty-desc">请在左侧录入您的血压、血糖、心率等体征数据，这里将自动生成趋势图</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-col>
    </el-row>

    <!-- AI 健康指导（最近 3 条） -->
    <div class="page-card" v-if="guidances.length">
      <div class="page-title"><el-icon><Cpu /></el-icon>AI 健康指导</div>
      <div class="guidance-list">
        <div
          v-for="g in guidances"
          :key="g.id"
          class="guidance-item"
          :class="{ unread: g.is_read === 0, orphan: g.record_deleted }"
          @click="openGuidance(g)"
        >
          <div class="guidance-head">
            <el-tag size="small" :type="guidanceTagType(g.type)">{{ g.type }}</el-tag>
            <span v-if="g.health_record_summary?.record_sequence" class="guidance-seq-tag">
              第 {{ g.health_record_summary.record_sequence }} 次
            </span>
            <span v-else-if="g.record_deleted" class="guidance-seq-tag orphan-tag">
              记录已删除
            </span>
            <el-tooltip :content="formatDateTime(g.create_time)" placement="top">
              <span class="guidance-time">{{ relativeTime(g.create_time) }}</span>
            </el-tooltip>
            <el-badge v-if="g.is_read === 0" is-dot class="unread-dot" />
          </div>
          <div v-if="g.health_record_summary" class="guidance-source-row">
            <el-icon class="guidance-source-icon"><FirstAidKit /></el-icon>
            <span class="guidance-source-time">{{ formatDateTime(g.health_record_summary.recorded_at) }}</span>
            <span class="guidance-source-data">
              血压 {{ g.health_record_summary.systolic ?? '-' }}/{{ g.health_record_summary.diastolic ?? '-' }}
              · 心率 {{ g.health_record_summary.heart_rate ?? '-' }}
            </span>
          </div>
          <div v-else-if="g.record_deleted" class="guidance-source-row orphan-source">
            <el-icon class="guidance-source-icon"><Warning /></el-icon>
            <span>此指导对应的健康记录已被删除</span>
          </div>
          <div class="guidance-preview">{{ stripHtml(g.content).slice(0, 80) }}...</div>
        </div>
      </div>
    </div>

    <!-- 历史记录 -->
    <div class="page-card">
      <div class="page-title"><el-icon><FirstAidKit /></el-icon>历史记录</div>
      <div class="page-subtitle">近 {{ months }} 个月的体征记录</div>
        <el-table :data="history" v-loading="historyLoading" stripe max-height="420">
        <el-table-column prop="recorded_at" label="记录时间" min-width="100">
          <template #default="{ row }">{{ formatDateTime(row.recorded_at) }}</template>
        </el-table-column>
        <el-table-column label="血压(mmHg)" min-width="80">
          <template #default="{ row }">
            <span class="mono">{{ row.systolic ?? '-' }} / {{ row.diastolic ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="血糖(mmol/L)" min-width="70">
          <template #default="{ row }"><span class="mono">{{ row.blood_sugar ?? '-' }}</span></template>
        </el-table-column>
        <el-table-column label="心率(次/分)" min-width="70">
          <template #default="{ row }"><span class="mono">{{ row.heart_rate ?? '-' }}</span></template>
        </el-table-column>
        <el-table-column label="体重(kg)" min-width="60">
          <template #default="{ row }"><span class="mono">{{ row.weight ?? '-' }}</span></template>
        </el-table-column>
        <el-table-column label="BMI" min-width="50">
          <template #default="{ row }">
            <el-tag :type="bmiType(row.bmi)" size="small">{{ row.bmi ?? '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="memo" label="备注" min-width="90">
          <template #default="{ row }">{{ row.memo || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              :icon="Delete"
              size="small"
              link
              @click="handleDelete(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!historyLoading && !history.length" description="暂无健康记录，快去录入第一条吧" />
    </div>

    <!-- 指导详情弹窗 -->
    <el-dialog v-model="guidanceDialogVisible" :title="currentGuidance?.type || '健康指导'" width="560px">
      <div v-if="currentGuidance?.health_record_summary" class="dialog-source-block">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="记录时间">{{ formatDateTime(currentGuidance.health_record_summary.recorded_at) }}</el-descriptions-item>
          <el-descriptions-item label="血压">
            {{ currentGuidance.health_record_summary.systolic ?? '-' }} / {{ currentGuidance.health_record_summary.diastolic ?? '-' }} mmHg
          </el-descriptions-item>
          <el-descriptions-item label="血糖">{{ currentGuidance.health_record_summary.blood_sugar ?? '-' }} mmol/L</el-descriptions-item>
          <el-descriptions-item label="心率">{{ currentGuidance.health_record_summary.heart_rate ?? '-' }} 次/分</el-descriptions-item>
          <el-descriptions-item label="体重">{{ currentGuidance.health_record_summary.weight ?? '-' }} kg</el-descriptions-item>
          <el-descriptions-item label="BMI">
            <el-tag :type="bmiType(currentGuidance.health_record_summary.bmi)" size="small">{{ currentGuidance.health_record_summary.bmi ?? '-' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentGuidance.health_record_summary.memo || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div class="markdown-body" v-html="renderMarkdown(currentGuidance?.content || '')"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Check,
  DataLine,
  FirstAidKit,
  Delete,
  Cpu,
  Warning,
  Reading,
  ArrowDown
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { marked } from 'marked'
import { healthApi } from '@/api/health'
import { formatDateTime, replaceSalutation as _replaceSalutation } from '@/utils/format'
import { useUserStore } from '@/store/user'

marked.setOptions({ breaks: true, gfm: true })

const userStore = useUserStore()
const formRef = ref()
const submitting = ref(false)
const months = ref(3)
const refOpen = ref(false)

const form = reactive({
  systolic: null,
  diastolic: null,
  blood_sugar: null,
  heart_rate: null,
  weight: null,
  height: null,
  memo: ''
})

const rules = {
  systolic: [{ required: true, message: '请输入收缩压', trigger: 'blur' }],
  diastolic: [{ required: true, message: '请输入舒张压', trigger: 'blur' }],
  heart_rate: [{ required: true, message: '请输入心率', trigger: 'blur' }]
}

let guidancePollTimer = null

async function submitRecord() {
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const res = await healthApi.record({
        systolic: form.systolic,
        diastolic: form.diastolic,
        blood_sugar: form.blood_sugar,
        heart_rate: form.heart_rate,
        weight: form.weight,
        height: form.height,
        memo: form.memo || null
      })
      const data = res.data || {}
      ElMessage.success(res.message || '健康数据记录成功')
      formRef.value.resetFields()
      Object.assign(form, {
        systolic: null,
        diastolic: null,
        blood_sugar: null,
        heart_rate: null,
        weight: null,
        height: null,
        memo: ''
      })
      reloadAll()
      if (data.guidanceGenerating) {
        const recordId = data.health?.id
        pollLatestGuidance(recordId)
      } else if (data.guidance) {
        const current = guidances.value
        guidances.value = [data.guidance, ...current].slice(0, 3)
      }
    } catch (e) {
      /* 拦截器提示 */
    } finally {
      submitting.value = false
    }
  })
}

function pollLatestGuidance(targetRecordId) {
  if (guidancePollTimer) clearInterval(guidancePollTimer)
  let attempts = 0
  const maxAttempts = 30
  const interval = 2000
  guidancePollTimer = setInterval(async () => {
    attempts++
    try {
      const res = await healthApi.guidances()
      const list = res.data || []
      const matched = list.find((g) => g.health_record_id === targetRecordId)
      if (matched) {
        guidances.value = list.slice(0, 3)
        clearInterval(guidancePollTimer)
        guidancePollTimer = null
        reloadAll()
        return
      }
      if (attempts >= maxAttempts) {
        clearInterval(guidancePollTimer)
        guidancePollTimer = null
      }
    } catch (e) {
      if (attempts >= maxAttempts) {
        clearInterval(guidancePollTimer)
        guidancePollTimer = null
      }
    }
  }, interval)
}

function renderMarkdown(text) {
  if (!text) return ''
  try {
    return marked.parse(replaceSalutation(text))
  } catch {
    return replaceSalutation(text)
  }
}

function replaceSalutation(text) {
  return _replaceSalutation(text, userStore.user)
}

function stripHtml(html) {
  return (html || '').replace(/<[^>]+>/g, '')
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

function relativeTime(input) {
  const d = new Date(input)
  if (isNaN(d.getTime())) return '-'
  const diff = (Date.now() - d.getTime()) / 1000
  if (diff < 60) return '刚刚'
  if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前'
  if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前'
  return formatDateTime(input)
}

// ---------- 历史记录 ----------
const history = ref([])
const historyLoading = ref(false)

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await healthApi.history(months.value)
    history.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    historyLoading.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${formatDateTime(row.recorded_at)} 的健康记录吗？删除后趋势图将自动更新。`,
      '删除确认',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await healthApi.delete(row.id)
    ElMessage.success('删除成功')
    reloadAll()
  } catch (e) {
    if (e !== 'cancel') {
      /* 拦截器提示 */
    }
  }
}

// ---------- 健康指导列表 ----------
const guidances = ref([])
const guidanceDialogVisible = ref(false)
const currentGuidance = ref(null)

async function loadGuidances() {
  try {
    const res = await healthApi.guidances()
    guidances.value = (res.data || []).slice(0, 3)
  } catch (e) { /* 拦截器提示 */ }
}

async function openGuidance(g) {
  currentGuidance.value = g
  guidanceDialogVisible.value = true
  if (g.is_read === 0) {
    try {
      await healthApi.markGuidanceRead(g.id)
      g.is_read = 1
    } catch (e) {}
  }
}

// ---------- 趋势图表 ----------
const bpChartRef = ref(null)
const sugarChartRef = ref(null)
const heartChartRef = ref(null)
const weightChartRef = ref(null)
const trendLoading = ref(false)
const hasTrendData = ref(false)
let charts = []

function baseOption(title, labels, series, color) {
  const axisStyle = {
    axisLine: { lineStyle: { color: 'rgba(255,255,255,0.2)' } },
    axisLabel: { color: '#a0a3b8', fontSize: 10 },
    splitLine: { lineStyle: { color: 'rgba(255,255,255,0.06)' } }
  }
  return {
    backgroundColor: 'transparent',
    title: {
      text: title,
      left: 12,
      top: 8,
      textStyle: { fontSize: 14, color: '#1f2937', fontWeight: 60 }
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: 'rgba(0, 0, 0, 0.06)',
      textStyle: { color: '#1f2937' },
      formatter: (params) => {
        if (!params || !params.length) return ''
        let html = `<div style="font-weight:600;margin-bottom:6px">${params[0].axisValue}</div>`
        params.forEach((p) => {
          const marker = `<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color};margin-right:6px"></span>`
          html += `<div>${marker}${p.seriesName}：<b>${p.value}</b></div>`
        })
        return html
      }
    },
    legend: series.length > 1 ? {
      data: series.map((s) => s.name),
      top: 6,
      right: 12,
      textStyle: { color: '#5b6478', fontSize: 11 },
      itemWidth: 14,
      itemHeight: 8
    } : undefined,
    grid: { left: 46, right: 16, top: series.length > 1 ? 62 : 46, bottom: 28 },
    xAxis: { type: 'category', data: labels, ...axisStyle },
    yAxis: { type: 'value', scale: true, ...axisStyle },
    series: series.map((s, i) => ({
      type: 'line',
      smooth: true,
      symbolSize: 6,
      lineStyle: { width: 2.5 },
      itemStyle: { color: color[i] || color[0] },
      ...s
    })),
    color
  }
}

function hasAnyLabels(raw) {
  const keys = ['bloodPressure', 'bloodSugar', 'heartRate', 'weight']
  return keys.some((k) => {
    const item = raw?.[k]
    return Array.isArray(item?.labels) && item.labels.length > 0
  })
}

function renderAllCharts(d) {
  renderChart(bpChartRef, baseOption(
    '血压趋势 (mmHg)',
    d.bloodPressure.labels,
    [
      { name: '收缩压', data: d.bloodPressure.systolic },
      { name: '舒张压', data: d.bloodPressure.diastolic }
    ],
    ['#ff7a7a', '#4facfe']
  ))
  renderChart(sugarChartRef, baseOption(
    '血糖趋势 (mmol/L)',
    d.bloodSugar.labels,
    [{ name: '血糖', data: d.bloodSugar.values, areaStyle: { opacity: 0.12 } }],
    ['#f6a23b']
  ))
  renderChart(heartChartRef, baseOption(
    '心率趋势 (次/分)',
    d.heartRate.labels,
    [{ name: '心率', data: d.heartRate.values, areaStyle: { opacity: 0.12 } }],
    ['#e85fa8']
  ))
  renderChart(weightChartRef, baseOption(
    '体重趋势 (kg)',
    d.weight.labels,
    [{ name: '体重', data: d.weight.values, areaStyle: { opacity: 0.12 } }],
    ['#2f9e8f']
  ))
}

function disposeAllCharts() {
  charts.forEach((c) => c.dispose())
  charts = []
}

async function loadTrend() {
  trendLoading.value = true
  try {
    const res = await healthApi.trend(months.value)
    const raw = res.data || {}
    if (hasAnyLabels(raw)) {
      hasTrendData.value = true
      await nextTick()
      renderAllCharts({
        bloodPressure: raw.bloodPressure,
        bloodSugar: raw.bloodSugar,
        heartRate: raw.heartRate,
        weight: raw.weight
      })
    } else {
      hasTrendData.value = false
      disposeAllCharts()
    }
  } catch (e) {
    hasTrendData.value = false
    disposeAllCharts()
  } finally {
    trendLoading.value = false
  }
}

function renderChart(refEl, option) {
  if (!refEl || !refEl.value) return
  const dom = refEl.value
  let chart = echarts.getInstanceByDom(dom)
  if (!chart) {
    chart = echarts.init(dom)
    charts.push(chart)
  }
  chart.setOption(option, true)
  chart.resize()
}

function handleResize() {
  charts.forEach((c) => c.resize())
}

function reloadAll() {
  loadHistory()
  loadTrend()
  loadGuidances()
}

function initChartsIfNeeded() {
  const refs = [bpChartRef, sugarChartRef, heartChartRef, weightChartRef]
  refs.forEach((refEl) => {
    if (!refEl.value) return
    let chart = echarts.getInstanceByDom(refEl.value)
    if (!chart) {
      chart = echarts.init(refEl.value)
      charts.push(chart)
    }
  })
}

onMounted(async () => {
  await nextTick()
  initChartsIfNeeded()
  reloadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach((c) => c.dispose())
  charts = []
})
</script>

<style scoped>
.health-form-card {
  padding-top: 48px;
  padding-bottom: 48px;
}
.health-top-row :deep(.el-col) > .page-card {
  height: 100%;
}
.health-top-row {
  margin-bottom: 16px;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 10px;
}
.submit-btn {
  width: 100%;
  border-radius: 8px;
}
.chart-box {
  width: 100%;
  height: 260px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--card-border);
  border-radius: 12px;
  margin-bottom: 12px;
  transition: all 0.3s ease;
}
.chart-box:hover {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgba(47, 125, 109, 0.28);
}
.trend-empty {
  height: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.6);
  border: 1px dashed rgba(47, 125, 109, 0.22);
  border-radius: 12px;
  margin-bottom: 12px;
}
.trend-empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.6;
}
.trend-empty-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-sub);
  margin-bottom: 8px;
}
.trend-empty-desc {
  font-size: 13px;
  color: var(--text-muted);
  max-width: 360px;
  text-align: center;
  line-height: 1.6;
}

.reference-box {
  margin-top: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(47, 125, 109, 0.08), rgba(217, 150, 62, 0.055));
  border: 1px solid rgba(47, 125, 109, 0.18);
  overflow: hidden;
}
.reference-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  user-select: none;
  transition: background 0.2s ease;
}
.reference-toggle:hover {
  background: rgba(47, 125, 109, 0.06);
}
.reference-toggle-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.ref-icon {
  color: #2f7d6d;
  font-size: 17px;
}
.ref-title {
  font-size: 14px;
  font-weight: 700;
  color: #1f2937;
}
.ref-arrow {
  color: #9aa3b2;
  font-size: 16px;
  transition: transform 0.3s ease;
}
.ref-arrow.expanded {
  transform: rotate(180deg);
}
.reference-table {
  padding: 4px 16px 16px;
  border-top: 1px solid rgba(47, 125, 109, 0.14);
}
.ref-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed rgba(47, 125, 109, 0.11);
}
.ref-item:last-child {
  border-bottom: none;
}
.ref-label {
  flex: 0 0 130px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}
.ref-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.ref-value {
  flex: 1;
  font-size: 13px;
  color: #1f2937;
}
.ref-normal {
  font-weight: 700;
  color: #2f9e8f;
  font-size: 14px;
}
.ref-unit {
  margin-left: 4px;
  color: #6b7280;
  font-size: 12px;
}
.ref-note {
  display: block;
  margin-top: 2px;
  color: #9aa3b2;
  font-size: 11px;
}
.ref-bmi {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.dialog-source-block {
  margin-bottom: 14px;
}

.guidance-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
}
.guidance-item {
  flex: 1 1 calc(33.33% - 12px);
  min-width: 220px;
  padding: 14px 16px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--card-border);
  cursor: pointer;
  transition: all 0.2s ease;
}
.guidance-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 7px 18px rgba(38, 76, 65, 0.11);
  border-color: rgba(47, 125, 109, 0.28);
}
.guidance-item.unread {
  background: linear-gradient(135deg, rgba(47, 125, 109, 0.07), rgba(255, 255, 255, 0.74));
  border-color: rgba(47, 125, 109, 0.24);
}
.guidance-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.guidance-seq-tag {
  font-size: 11px;
  font-weight: 600;
  color: #2f7d6d;
  background: rgba(47, 125, 109, 0.1);
  border: 1px solid rgba(47, 125, 109, 0.22);
  border-radius: 4px;
  padding: 2px 8px;
  line-height: 1;
  white-space: nowrap;
}
.guidance-seq-tag.orphan-tag {
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.12);
  border-color: rgba(230, 162, 60, 0.3);
}
.guidance-source-icon {
  color: #2f7d6d;
  font-size: 13px;
}
.guidance-source-row.orphan-source {
  background: rgba(230, 162, 60, 0.08);
  color: #9e7738;
  border-radius: 4px;
}
.guidance-source-row.orphan-source .guidance-source-icon {
  color: #e6a23c;
}
.guidance-item.orphan {
  opacity: 0.7;
  border-style: dashed;
}
.guidance-item.orphan:hover {
  opacity: 0.85;
}
.guidance-source-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  padding: 4px 8px;
  background: rgba(47, 125, 109, 0.05);
  border-radius: 4px;
  font-size: 11px;
}
.guidance-source-time {
  color: #2f7d6d;
  font-weight: 600;
}
.guidance-source-data {
  color: #6b7280;
}
.guidance-time {
  font-size: 12px;
  color: var(--text-muted);
  margin-left: auto;
}
.unread-dot {
  margin-left: 4px;
}
.guidance-preview {
  font-size: 13px;
  color: var(--text-sub);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.markdown-body :deep(p) { margin: 6px 0; }
.markdown-body :deep(ul) { padding-left: 18px; margin: 4px 0; }
.markdown-body :deep(li) { margin: 2px 0; }
.markdown-body :deep(strong) { color: #286b5f; }

.fade-slide-enter-active, .fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from, .fade-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
