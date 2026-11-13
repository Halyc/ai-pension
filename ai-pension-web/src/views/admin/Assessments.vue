<template>
  <div class="page-container">
    <div class="page-card">
      <el-tabs v-model="activeTab">
        <!-- ========== 问卷管理 ========== -->
        <el-tab-pane name="questionnaires">
          <template #label>
            <span class="tab-label"><el-icon><Document /></el-icon>问卷管理</span>
          </template>

          <div class="page-title"><el-icon><Document /></el-icon>健康评估问卷</div>
          <div class="page-subtitle">创建问卷、维护题目、发布后会员即可参与健康评测</div>

          <div class="search-bar">
            <el-input
              v-model="qqQuery.keyword"
              placeholder="按标题搜索问卷"
              clearable
              :prefix-icon="Search"
              class="filter-input"
            />
            <el-select
              v-model="qqQuery.status"
              placeholder="全部状态"
              clearable
              class="filter-select"
            >
              <el-option
                v-for="(info, value) in questionnaireStatusMap"
                :key="value"
                :label="info.label"
                :value="value"
              />
            </el-select>
            <el-button type="primary" :icon="Search" @click="searchQuestionnaires">查询</el-button>
            <el-button :icon="Refresh" @click="resetQqQuery">重置</el-button>
            <div style="flex:1"></div>
            <el-button :icon="Refresh" @click="loadQuestionnaires">刷新</el-button>
            <el-button type="primary" :icon="Plus" @click="openQnaireDialog()">新增问卷</el-button>
          </div>

          <el-table :data="filteredQuestionnaires" v-loading="qnaireLoading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="问卷标题" min-width="180" />
            <el-table-column prop="description" label="描述" min-width="220">
              <template #default="{ row }">{{ row.description || '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="dictType(questionnaireStatusMap, row.status)">
                  {{ dictLabel(questionnaireStatusMap, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="310">
              <template #default="{ row }">
                <el-button link type="primary" :icon="EditPen" @click="manageQuestions(row)">
                  题目管理
                </el-button>
                <el-button link type="warning" :icon="Edit" @click="openQnaireDialog(row)">编辑</el-button>
                <el-button
                  v-if="row.status !== 'PUBLISHED'"
                  link
                  type="success"
                  :icon="Promotion"
                  @click="publishQnaire(row)"
                >发布</el-button>
                <el-button link type="danger" :icon="Delete" @click="removeQnaire(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ========== 题目管理 ========== -->
        <el-tab-pane name="questions">
          <template #label>
            <span class="tab-label"><el-icon><EditPen /></el-icon>题目管理</span>
          </template>

          <div class="page-title"><el-icon><EditPen /></el-icon>问卷列表</div>
          <div class="page-subtitle">选择问卷，点击「题目管理」弹出窗口维护题目</div>

          <div class="search-bar">
            <el-input
              v-model="qqQuery.keyword"
              placeholder="按标题搜索问卷"
              clearable
              :prefix-icon="Search"
              class="filter-input"
            />
            <el-button type="primary" :icon="Search" @click="searchQuestionnaires">查询</el-button>
            <div style="flex:1"></div>
            <el-button :icon="Refresh" @click="loadQuestionnaires">刷新</el-button>
          </div>

          <el-table :data="filteredQuestionnaires" v-loading="qnaireLoading" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="title" label="问卷标题" min-width="180" />
            <el-table-column prop="description" label="描述" min-width="220">
              <template #default="{ row }">{{ row.description || '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="dictType(questionnaireStatusMap, row.status)">
                  {{ dictLabel(questionnaireStatusMap, row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button link type="primary" :icon="EditPen" @click="manageQuestions(row)">
                  题目管理
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- ========== 评测结果 ========== -->
        <el-tab-pane name="results">
          <template #label>
            <span class="tab-label"><el-icon><DataAnalysis /></el-icon>评测结果</span>
          </template>

          <div class="page-title"><el-icon><DataAnalysis /></el-icon>会员评测结果</div>
          <div class="page-subtitle">查看会员提交的健康评测答案、AI 评分与健康建议</div>

          <div class="search-bar">
            <el-select
              v-model="resultQuery.userId"
              placeholder="全部会员"
              clearable
              filterable
              class="filter-select"
            >
              <el-option
                v-for="m in memberList"
                :key="m.id"
                :label="`${m.real_name || '未命名'}（ID:${m.id}）`"
                :value="m.id"
              />
            </el-select>
            <el-select
              v-model="resultQuery.questionnaireId"
              placeholder="全部问卷"
              clearable
              filterable
              class="filter-select"
            >
              <el-option
                v-for="q in questionnaires"
                :key="q.id"
                :label="q.title"
                :value="q.id"
              />
            </el-select>
            <el-button type="primary" :icon="Search" @click="loadResults">查询</el-button>
            <el-button :icon="Refresh" @click="resetResultQuery">重置</el-button>
            <div style="flex:1"></div>
            <el-button :icon="Refresh" @click="loadResults">刷新</el-button>
          </div>

          <el-table :data="filteredResults" v-loading="resultLoading" stripe>
            <el-table-column prop="id" label="结果ID" width="90" />
            <el-table-column label="会员" min-width="140">
              <template #default="{ row }">
                {{ getMemberName(row.userId) }}
              </template>
            </el-table-column>
            <el-table-column prop="questionnaireId" label="问卷ID" width="100" />
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
                <span v-else class="text-sub">分析中</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" min-width="160">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="150">
              <template #default="{ row }">
                <el-button link type="primary" :icon="View" @click="showResultDetail(row)">
                  详情
                </el-button>
                <el-button link type="danger" :icon="Delete" @click="removeResult(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 问卷新增/编辑弹窗 -->
    <el-dialog
      v-model="qnaireDialog.visible"
      :title="qnaireDialog.isEdit ? '编辑问卷' : '新增问卷'"
      width="500px"
    >
      <el-form :model="qnaireForm" label-width="80px">
        <el-form-item label="标题" required>
          <el-input v-model="qnaireForm.title" placeholder="如：老年人健康状况评估表" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="qnaireForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="qnaireDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="qnaireDialog.saving" @click="submitQnaire">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题目管理（大弹窗） -->
    <el-dialog
      v-model="qMgrDialog.visible"
      :title="`题目管理：${qMgrDialog.title}`"
      width="1100px"
      top="6vh"
      destroy-on-close
    >
      <div class="q-mgr-header">
        <span class="q-mgr-sub">共 {{ questions.length }} 题</span>
        <el-button type="primary" :icon="Plus" @click="openQuestionDialog()">新增题目</el-button>
      </div>

      <el-table :data="questions" v-loading="questionLoading" stripe max-height="520">
        <el-table-column prop="sortOrder" label="序号" width="70" />
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="content" label="题干" min-width="240" />
        <el-table-column prop="type" label="题型" width="100">
          <template #default="{ row }">
            <el-tag :type="dictType(questionTypeMap, row.type)">
              {{ dictLabel(questionTypeMap, row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="选项" min-width="220">
          <template #default="{ row }">
            <template v-if="parseOptions(row.options).length">
              <el-tag
                v-for="(opt, i) in parseOptions(row.options)"
                :key="i"
                size="small"
                class="opt-tag"
              >{{ opt }}</el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="warning" :icon="Edit" @click="openQuestionDialog(row)">
              编辑
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="removeQuestion(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="qMgrDialog.visible = false">关 闭</el-button>
      </template>
    </el-dialog>

    <!-- 题目新增/编辑弹窗 -->
    <el-dialog
      v-model="questionDialog.visible"
      :title="questionDialog.isEdit ? '编辑题目' : '新增题目'"
      width="600px"
    >
      <el-form :model="questionForm" label-width="80px">
        <el-form-item label="题干" required>
          <el-input v-model="questionForm.content" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="题型" required>
          <el-radio-group v-model="questionForm.type">
            <el-radio value="SINGLE">单选题</el-radio>
            <el-radio value="MULTIPLE">多选题</el-radio>
            <el-radio value="TEXT">简答题</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="questionForm.type !== 'TEXT'" label="选项">
          <el-input
            v-model="questionForm.optionsText"
            type="textarea"
            :rows="5"
            placeholder="每行一个选项，如：&#10;A. 没有&#10;B. 偶尔&#10;C. 经常"
          />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="questionForm.sortOrder" :min="0" :max="999" />
          <div class="form-hint">数字越小越靠前</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="questionDialog.saving" @click="submitQuestion">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 评测结果详情弹窗 -->
    <el-dialog v-model="resultDetailVisible" title="评测结果详情" width="640px">
      <template v-if="resultDetail">
        <el-descriptions :column="2" border class="mb">
          <el-descriptions-item label="结果ID">{{ resultDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="会员ID">{{ resultDetail.userId }}</el-descriptions-item>
          <el-descriptions-item label="问卷ID">{{ resultDetail.questionnaireId }}</el-descriptions-item>
          <el-descriptions-item label="AI评分">
            <template v-if="getScoreInfo(resultDetail.aiScore)">
              <span
                class="score-badge"
                :style="{
                  color: getScoreInfo(resultDetail.aiScore).color,
                  background: getScoreInfo(resultDetail.aiScore).bg,
                  borderColor: getScoreInfo(resultDetail.aiScore).border
                }"
              >{{ getScoreInfo(resultDetail.aiScore).score }}分 · {{ getScoreInfo(resultDetail.aiScore).level }}</span>
            </template>
            <span v-else>分析中</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间" :span="2">
            {{ formatDateTime(resultDetail.createTime) }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">答题内容</div>
        <div class="answers-box">
          <template v-if="answerEntries.length">
            <div v-for="([k, v], i) in answerEntries" :key="i" class="answer-item">
              <span class="answer-q">题目{{ k }}：</span>
              <span class="answer-a">{{ Array.isArray(v) ? v.join('、') : v }}</span>
            </div>
          </template>
          <pre v-else class="raw-json">{{ resultDetail.answers || '-' }}</pre>
        </div>

        <div class="detail-section-title">AI 健康建议</div>
        <el-alert
          :title="resultDetail.aiSuggestion || 'AI 分析尚未生成，请稍后查看'"
          type="success"
          :closable="false"
          show-icon
        />
      </template>
      <template #footer>
        <el-button @click="resultDetailVisible = false">关 闭</el-button>
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
  Refresh,
  View,
  Document,
  EditPen,
  Promotion,
  DataAnalysis
} from '@element-plus/icons-vue'
import { adminAssessmentApi } from '@/api/adminAssessment'
import { memberApi } from '@/api/member'
import {
  formatDateTime,
  dictLabel,
  dictType,
  questionnaireStatusMap,
  questionTypeMap,
  parseOptions,
  parseJsonSafe,
  getScoreInfo
} from '@/utils/format'

const activeTab = ref('questionnaires')

// ---------- 问卷 ----------
const qnaireLoading = ref(false)
const questionnaires = ref([])
const qqQuery = reactive({ keyword: '', status: null })

const filteredQuestionnaires = computed(() => {
  const kw = qqQuery.keyword.trim().toLowerCase()
  return [...questionnaires.value]
    .filter((r) => {
      if (kw && !(r.title || '').toLowerCase().includes(kw)) return false
      if (qqQuery.status != null && r.status !== qqQuery.status) return false
      return true
    })
    .sort((a, b) => {
      const ta = new Date(a.createTime).getTime() || 0
      const tb = new Date(b.createTime).getTime() || 0
      return tb - ta
    })
})

function searchQuestionnaires() {
  // computed 已自动响应 qqQuery 变化，此处仅用于显式触发（可扩展为后端查询）
}

function resetQqQuery() {
  qqQuery.keyword = ''
  qqQuery.status = null
}

async function loadQuestionnaires() {
  qnaireLoading.value = true
  try {
    const res = await adminAssessmentApi.listQuestionnaires()
    questionnaires.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    qnaireLoading.value = false
  }
}

const qnaireDialog = reactive({ visible: false, isEdit: false, saving: false, id: null })
const qnaireForm = reactive({ title: '', description: '' })

function openQnaireDialog(row) {
  qnaireForm.title = row?.title || ''
  qnaireForm.description = row?.description || ''
  qnaireDialog.isEdit = !!row
  qnaireDialog.id = row?.id || null
  qnaireDialog.visible = true
}

async function submitQnaire() {
  if (!qnaireForm.title.trim()) {
    ElMessage.warning('请输入问卷标题')
    return
  }
  qnaireDialog.saving = true
  try {
    if (qnaireDialog.isEdit) {
      await adminAssessmentApi.updateQuestionnaire(qnaireDialog.id, {
        title: qnaireForm.title.trim(),
        description: qnaireForm.description
      })
      ElMessage.success('更新成功')
    } else {
      await adminAssessmentApi.createQuestionnaire({
        title: qnaireForm.title.trim(),
        description: qnaireForm.description
      })
      ElMessage.success('创建成功，默认为草稿状态')
    }
    qnaireDialog.visible = false
    loadQuestionnaires()
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    qnaireDialog.saving = false
  }
}

async function publishQnaire(row) {
  try {
    await ElMessageBox.confirm(
      `发布后问卷将对会员可见且不可再编辑题目，确定发布「${row.title}」吗？`,
      '发布确认',
      { type: 'warning', confirmButtonText: '发布', cancelButtonText: '取消' }
    )
    await adminAssessmentApi.publishQuestionnaire(row.id)
    ElMessage.success('发布成功')
    loadQuestionnaires()
  } catch (e) {
    /* 取消 */
  }
}

async function removeQnaire(row) {
  try {
    await ElMessageBox.confirm(`确定删除问卷「${row.title}」吗？其题目与结果引用将受影响。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await adminAssessmentApi.deleteQuestionnaire(row.id)
    ElMessage.success('删除成功')
    loadQuestionnaires()
  } catch (e) {
    /* 取消 */
  }
}

// ---------- 题目 ----------
const questionLoading = ref(false)
const questions = ref([])
const qQuery = reactive({ questionnaireId: null })
const qMgrDialog = reactive({ visible: false, title: '' })

function manageQuestions(row) {
  qQuery.questionnaireId = row.id
  qMgrDialog.title = row.title || ''
  qMgrDialog.visible = true
  loadQuestions()
}

async function loadQuestions() {
  questionLoading.value = true
  try {
    const res = await adminAssessmentApi.listQuestions(qQuery.questionnaireId)
    questions.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    questionLoading.value = false
  }
}

const questionDialog = reactive({ visible: false, isEdit: false, saving: false, id: null })
const questionForm = reactive({
  content: '',
  type: 'SINGLE',
  optionsText: '',
  sortOrder: 0
})

function openQuestionDialog(row) {
  questionForm.content = row?.content || ''
  questionForm.type = row?.type || 'SINGLE'
  questionForm.optionsText = row ? parseOptions(row.options).join('\n') : ''
  questionForm.sortOrder = row?.sortOrder ?? questions.value.length + 1
  questionDialog.isEdit = !!row
  questionDialog.id = row?.id || null
  questionDialog.visible = true
}

async function submitQuestion() {
  if (!questionForm.content.trim()) {
    ElMessage.warning('请输入题干')
    return
  }
  const options =
    questionForm.type === 'TEXT'
      ? null
      : JSON.stringify(
          questionForm.optionsText
            .split('\n')
            .map((s) => s.trim())
            .filter(Boolean)
        )
  if (questionForm.type !== 'TEXT' && !options) {
    ElMessage.warning('请至少填写一个选项')
    return
  }
  questionDialog.saving = true
  try {
    const body = {
      content: questionForm.content.trim(),
      type: questionForm.type,
      options,
      sortOrder: questionForm.sortOrder
    }
    if (questionDialog.isEdit) {
      await adminAssessmentApi.updateQuestion(questionDialog.id, body)
      ElMessage.success('更新成功')
    } else {
      await adminAssessmentApi.createQuestion({
        questionnaireId: qQuery.questionnaireId,
        ...body
      })
      ElMessage.success('创建成功')
    }
    questionDialog.visible = false
    loadQuestions()
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    questionDialog.saving = false
  }
}

async function removeQuestion(row) {
  try {
    await ElMessageBox.confirm(`确定删除题目「${row.content.slice(0, 20)}...」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await adminAssessmentApi.deleteQuestion(row.id)
    ElMessage.success('删除成功')
    loadQuestions()
  } catch (e) {
    /* 取消 */
  }
}

// ---------- 评测结果 ----------
const resultLoading = ref(false)
const results = ref([])
const resultDetailVisible = ref(false)
const resultDetail = ref(null)
const memberList = ref([])
const resultQuery = reactive({ userId: null, questionnaireId: null })

const filteredResults = computed(() => {
  return results.value.filter((r) => {
    if (resultQuery.userId != null && r.userId !== resultQuery.userId) return false
    if (resultQuery.questionnaireId != null && r.questionnaireId !== resultQuery.questionnaireId) return false
    return true
  })
})

function getMemberName(userId) {
  const m = memberList.value.find((x) => x.id === userId)
  if (!m) return `ID:${userId}`
  return `${m.real_name || '未命名'}（ID:${m.id}）`
}

function resetResultQuery() {
  resultQuery.userId = null
  resultQuery.questionnaireId = null
  loadResults()
}

async function loadMembers() {
  try {
    const res = await memberApi.getMemberList({ page: 1, size: 9999 })
    memberList.value = res.data?.records || res.data?.list || []
  } catch (e) {
    memberList.value = []
  }
}

async function loadResults() {
  resultLoading.value = true
  try {
    const res = await adminAssessmentApi.listResults()
    results.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    resultLoading.value = false
  }
}

async function showResultDetail(row) {
  resultDetailVisible.value = true
  resultDetail.value = null
  try {
    const res = await adminAssessmentApi.getResultDetail(row.id)
    resultDetail.value = res.data
  } catch (e) {
    resultDetailVisible.value = false
  }
}

const answerEntries = computed(() => {
  const answers = parseJsonSafe(resultDetail.value?.answers, {})
  if (!answers || typeof answers !== 'object') return []
  return Object.entries(answers)
})

async function removeResult(row) {
  try {
    await ElMessageBox.confirm(`确定删除结果 ID:${row.id} 吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await adminAssessmentApi.deleteResult(row.id)
    ElMessage.success('删除成功')
    loadResults()
  } catch (e) {
    /* 取消 */
  }
}

watch(activeTab, (tab) => {
  if (tab === 'questions') loadQuestionnaires()
})

onMounted(() => {
  loadQuestionnaires()
  loadResults()
  loadMembers()
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
  margin-bottom: 12px;
}
.opt-tag {
  margin: 2px 6px 2px 0;
}
.form-hint {
  font-size: 12px;
  color: var(--text-muted);
}
.text-sub {
  color: var(--text-muted);
}
.mb {
  margin-bottom: 16px;
}
.detail-section-title {
  font-weight: 600;
  margin: 14px 0 8px;
  color: var(--text-main);
}
.answers-box {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--card-border);
  border-radius: 8px;
  padding: 12px 16px;
  max-height: 260px;
  overflow-y: auto;
}
.answer-item {
  line-height: 2;
  font-size: 14px;
}
.answer-q {
  color: var(--text-muted);
}
.answer-a {
  color: var(--text-main);
}
.raw-json {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 13px;
  color: var(--text-sub);
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
.q-mgr-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.q-mgr-sub {
  font-size: 13px;
  color: var(--text-muted);
}
</style>