<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" class="page-card">
      <!-- ===== 可参与问卷 ===== -->
      <el-tab-pane name="list">
        <template #label>
          <span class="tab-label"><el-icon><EditPen /></el-icon>健康评测</span>
        </template>

        <div class="toolbar">
          <el-button :icon="Refresh" @click="loadQuestionnaires" :loading="loading">刷新</el-button>
        </div>

        <div class="q-grid" v-loading="loading">
          <div v-for="q in questionnaires" :key="q.id" class="q-col">
            <el-card class="q-card hover-card" shadow="hover">
              <el-icon :size="30" color="#2f9e8f"><Document /></el-icon>
              <div class="q-title">{{ q.title }}</div>
              <div class="q-desc">{{ q.description || '健康状况自评问卷' }}</div>
              <div class="q-time">发布时间：{{ formatDateTime(q.createTime) }}</div>
              <el-button
                type="primary"
                class="q-btn"
                :icon="EditPen"
                @click="startQuiz(q)"
              >
                开始评测
              </el-button>
            </el-card>
          </div>
        </div>
        <el-empty v-if="!loading && !questionnaires.length" description="暂无可参与的评测问卷" />
      </el-tab-pane>

      <!-- ===== 我的评测记录 ===== -->
      <el-tab-pane name="history">
        <template #label>
          <span class="tab-label"><el-icon><DataAnalysis /></el-icon>我的评测记录</span>
        </template>

        <div class="toolbar">
          <el-button :icon="Refresh" @click="loadHistory" :loading="historyLoading">刷新</el-button>
        </div>

        <el-table :data="history" v-loading="historyLoading" stripe>
          <el-table-column prop="id" label="记录ID" min-width="60" />
          <el-table-column prop="questionnaireTitle" label="问卷" min-width="100" />
          <el-table-column prop="aiScore" label="AI评分" min-width="100">
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
          <el-table-column prop="createTime" label="提交时间" min-width="100">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="70">
            <template #default="{ row }">
              <el-button link type="primary" :icon="View" @click="showDetail(row)">查看报告</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 答题弹窗 -->
    <el-dialog v-model="quiz.visible" :title="quiz.title" width="680px" top="6vh">
      <div v-loading="quiz.loading" class="quiz-body">
        <el-alert
          v-if="!quiz.loading && !quiz.questions.length"
          title="该问卷还没有题目，请联系管理员"
          type="warning"
          :closable="false"
        />
        <div v-for="(q, idx) in quiz.questions" :key="q.id" class="quiz-question">
          <div class="quiz-q-title">
            <span class="quiz-no">{{ idx + 1 }}.</span>
            {{ q.content }}
            <el-tag size="small" class="quiz-type">{{ dictLabel(questionTypeMap, q.type) }}</el-tag>
          </div>

          <!-- 单选 -->
          <el-radio-group
            v-if="q.type === 'SINGLE'"
            v-model="quiz.answers[q.id]"
            class="quiz-options"
          >
            <el-radio v-for="(opt, i) in parseOptions(q.options)" :key="i" :value="opt" border>
              {{ opt }}
            </el-radio>
          </el-radio-group>

          <!-- 多选 -->
          <el-checkbox-group
            v-else-if="q.type === 'MULTIPLE'"
            v-model="quiz.answers[q.id]"
            class="quiz-options"
          >
            <el-checkbox v-for="(opt, i) in parseOptions(q.options)" :key="i" :value="opt" border>
              {{ opt }}
            </el-checkbox>
          </el-checkbox-group>

          <!-- 简答 -->
          <el-input
            v-else
            v-model="quiz.answers[q.id]"
            type="textarea"
            :rows="2"
            placeholder="请输入您的回答"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="quiz.visible = false">取消</el-button>
        <el-button type="primary" :loading="quiz.submitting" @click="submitQuiz">提交评测</el-button>
      </template>
    </el-dialog>

    <!-- 评测报告弹窗 -->
    <el-dialog v-model="report.visible" title="评测报告" width="680px" top="6vh">
      <template v-if="report.data">
        <el-descriptions :column="2" border class="mb">
          <el-descriptions-item label="问卷" :span="2">
            {{ report.data.questionnaireTitle }}
          </el-descriptions-item>
          <el-descriptions-item label="AI评分">
            <template v-if="getScoreInfo(report.data.aiScore)">
              <span
                class="score-badge"
                :style="{
                  color: getScoreInfo(report.data.aiScore).color,
                  background: getScoreInfo(report.data.aiScore).bg,
                  borderColor: getScoreInfo(report.data.aiScore).border
                }"
              >{{ getScoreInfo(report.data.aiScore).score }}分 · {{ getScoreInfo(report.data.aiScore).level }}</span>
            </template>
            <el-tag v-else type="info" size="small">分析中</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(report.data.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">答题详情</div>
        <div class="report-answers">
          <div v-for="(q, idx) in report.data.questions" :key="q.id" class="report-item">
            <div class="report-q">{{ idx + 1 }}. {{ q.content }}</div>
            <div class="report-a">
              <el-tag v-for="(opt, i) in parseOptions(q.options)" :key="i" size="small" class="opt-tag"
                :type="isPicked(report.data.answers, q.id, opt) ? 'success' : 'info'"
                :effect="isPicked(report.data.answers, q.id, opt) ? 'dark' : 'plain'"
              >{{ opt }}</el-tag>
              <div v-if="q.type === 'TEXT'" class="text-answer">
                {{ getAnswer(report.data.answers, q.id) || '（未作答）' }}
              </div>
              <div v-if="!parseOptions(q.options).length && q.type !== 'TEXT'" class="text-answer">
                {{ getAnswer(report.data.answers, q.id) || '（未作答）' }}
              </div>
            </div>
          </div>
        </div>

        <div class="detail-section-title">AI 健康建议</div>
        <el-alert
          :title="replaceSalutation(report.data.aiSuggestion) || 'AI 正在分析您的评测结果，请稍后再来查看'"
          type="success"
          :closable="false"
          show-icon
        />
      </template>
      <template #footer>
        <el-button @click="report.visible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  View,
  EditPen,
  Document,
  DataAnalysis
} from '@element-plus/icons-vue'
import { memberAssessmentApi } from '@/api/memberAssessment'
import {
  formatDateTime,
  dictLabel,
  questionTypeMap,
  parseOptions,
  parseJsonSafe,
  getScoreInfo,
  replaceSalutation as _replaceSalutation
} from '@/utils/format'
import { useUserStore } from '@/store/user'

const activeTab = ref('list')
const userStore = useUserStore()

function replaceSalutation(text) {
  return _replaceSalutation(text, userStore.user)
}

// ---------- 问卷列表 ----------
const loading = ref(false)
const questionnaires = ref([])

async function loadQuestionnaires() {
  loading.value = true
  try {
    const res = await memberAssessmentApi.questionnaires()
    questionnaires.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    loading.value = false
  }
}

// ---------- 答题 ----------
const quiz = reactive({
  visible: false,
  loading: false,
  submitting: false,
  questionnaireId: null,
  title: '',
  questions: [],
  answers: {}
})

async function startQuiz(q) {
  quiz.visible = true
  quiz.title = q.title
  quiz.questionnaireId = q.id
  quiz.questions = []
  quiz.answers = {}
  quiz.loading = true
  try {
    const res = await memberAssessmentApi.questions(q.id)
    quiz.questions = res.data || []
    // 初始化答案结构：多选初始化为数组
    quiz.questions.forEach((question) => {
      quiz.answers[question.id] = question.type === 'MULTIPLE' ? [] : ''
    })
  } catch (e) {
    quiz.visible = false
  } finally {
    quiz.loading = false
  }
}

async function submitQuiz() {
  for (const q of quiz.questions) {
    const a = quiz.answers[q.id]
    if (a === '' || a === null || (Array.isArray(a) && !a.length)) {
      ElMessage.warning(`请完成第「${q.content.slice(0, 15)}...」题`)
      return
    }
  }
  quiz.submitting = true
  try {
    await memberAssessmentApi.submit({
      questionnaireId: quiz.questionnaireId,
      answers: quiz.answers
    })
    ElMessage.success('提交成功！AI正在分析您的评测结果，请稍候查看')
    quiz.visible = false
    activeTab.value = 'history'
    loadHistory()
  } catch (e) {
  } finally {
    quiz.submitting = false
  }
}

// ---------- 历史 & 报告 ----------
const historyLoading = ref(false)
const history = ref([])
const report = reactive({ visible: false, data: null })

async function loadHistory() {
  historyLoading.value = true
  try {
    const res = await memberAssessmentApi.history()
    history.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    historyLoading.value = false
  }
}

async function showDetail(row) {
  report.visible = true
  report.data = null
  try {
    const res = await memberAssessmentApi.detail(row.id)
    report.data = res.data
  } catch (e) {
    report.visible = false
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

onMounted(() => {
  loadQuestionnaires()
  loadHistory()
})
</script>

<style scoped>
.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}
.toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 14px;
}
.q-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  align-items: stretch;
}
.q-col {
  display: flex;
  height: 100%;
}
.q-card {
  border-radius: 16px;
  text-align: center;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--card-border) !important;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
}
.q-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  flex: 1;
  height: 100%;
  padding: 22px 20px 18px;
}
.q-card :deep(.el-card__body) > * {
  flex-shrink: 0;
}
.q-card :deep(.el-card__body) > .q-desc {
  flex-shrink: 1;
  flex-grow: 1;
}
.q-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 14px 34px rgba(38, 76, 65, 0.12) !important;
  border-color: rgba(47, 125, 109, 0.27) !important;
}
.q-title {
  font-size: 17px;
  font-weight: 700;
  margin: 10px 0 6px;
  color: var(--text-main);
}
.q-desc {
  font-size: 13px;
  color: var(--text-sub);
  flex: 1;
  display: flex;
  align-items: center;
  line-height: 1.6;
}
.q-time {
  font-size: 12px;
  color: var(--text-muted);
  margin: 8px 0 12px;
}
.q-btn {
  width: 100%;
  border-radius: 8px;
  margin-top: auto;
}
.quiz-body {
  max-height: 62vh;
  overflow-y: auto;
  padding-right: 6px;
}
.quiz-question {
  margin-bottom: 22px;
}
.quiz-q-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
  line-height: 1.6;
}
.quiz-no {
  color: var(--primary-color);
  margin-right: 4px;
}
.quiz-type {
  margin-left: 8px;
}
.quiz-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-left: 22px;
}
.mb {
  margin-bottom: 14px;
}
.detail-section-title {
  font-weight: 600;
  margin: 16px 0 10px;
}
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
.score-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.5px;
}
</style>
