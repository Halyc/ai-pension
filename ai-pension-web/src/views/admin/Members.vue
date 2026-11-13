<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-title"><el-icon><User /></el-icon>用户管理</div>
      <div class="page-subtitle">会员账号查询、启禁用、会员等级与积分管理</div>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input v-model="query.phone" placeholder="手机号" clearable @keyup.enter="handleSearch" />
        <el-input v-model="query.name" placeholder="会员姓名" clearable @keyup.enter="handleSearch" />
        <el-select v-model="query.status" placeholder="账号状态" clearable>
          <el-option label="正常" value="ENABLED" />
          <el-option label="已禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        <div style="flex:1"></div>
        <el-button type="primary" :icon="Plus" @click="openAddDialog">新增用户</el-button>
      </div>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" min-width="100">
          <template #default="{ row }">
            <span>{{ row.id }}</span>
            <el-tag
              v-if="isCurrentUser(row)"
              size="small"
              type="success"
              effect="light"
              style="margin-left:6px"
            >当前登录</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" :src="buildAvatar(row)">
              {{ (row.real_name || row.phone || 'U').charAt(0).toUpperCase() }}
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="real_name" label="姓名" min-width="100">
          <template #default="{ row }">{{ row.real_name || '未填写' }}</template>
        </el-table-column>
        <el-table-column prop="role" label="身份" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" effect="light">
              {{ row.role === 'ADMIN' ? '管理员' : '会员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="member_level" label="会员等级" min-width="110">
          <template #default="{ row }">
            <template v-if="row.role === 'ADMIN'">-</template>
            <el-tag v-else :type="dictType(levelMap, row.member_level)" effect="light">
              {{ dictLabel(levelMap, row.member_level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="points" label="积分" min-width="80">
          <template #default="{ row }">
            <span v-if="row.role === 'ADMIN'">-</span>
            <span v-else class="mono">{{ row.points }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="dictType(userStatusMap, row.status)">
              {{ dictLabel(userStatusMap, row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="注册时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.create_time) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="320">
          <template #default="{ row }">
            <template v-if="canOperate(row)">
              <el-button v-if="row.role !== 'ADMIN'" link type="primary" :icon="View" @click="showDetail(row)">详情</el-button>
              <el-button v-if="row.role !== 'ADMIN'" link type="warning" :icon="Medal" @click="openLevelDialog(row)">等级</el-button>
              <el-button v-if="row.role !== 'ADMIN'" link type="success" :icon="Coin" @click="openPointsDialog(row)">积分</el-button>
              <el-button
                v-if="row.status === 'ENABLED'"
                link
                type="danger"
                :icon="CircleClose"
                @click="toggleStatus(row, false)"
              >禁用</el-button>
              <el-button
                v-else
                link
                type="success"
                :icon="CircleCheck"
                @click="toggleStatus(row, true)"
              >启用</el-button>
              <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </template>
            <span v-else class="text-muted">无权限</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页（后端格式：records/total/current/size/pages） -->
      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :page-sizes="[5, 10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </div>

    <!-- 会员详情抽屉 -->
    <el-drawer v-model="detailVisible" title="会员详情" size="420px">
      <template v-if="detail">
        <div class="detail-header">
          <el-avatar
            :size="64"
            :src="buildAvatar(detail.member)"
            class="member-avatar"
          >
            {{ (detail.member.real_name || detail.member.phone || 'U').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="detail-header-info">
            <div class="detail-name">{{ detail.member.real_name || '未填写' }}</div>
            <div class="detail-phone">{{ detail.member.phone }}</div>
          </div>
        </div>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="会员ID">{{ detail.member.id }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.member.phone }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ detail.member.real_name || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ genderText(detail.member.gender) }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ formatDate(detail.member.birthdate) }}</el-descriptions-item>
          <el-descriptions-item label="会员等级">
            <el-tag :type="dictType(levelMap, detail.member.member_level)">
              {{ dictLabel(levelMap, detail.member.member_level) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="积分">
            <span class="mono">{{ detail.member.points }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="dictType(userStatusMap, detail.member.status)">
              {{ dictLabel(userStatusMap, detail.member.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">健康统计（近12个月）</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-statistic title="评测记录数" :value="detail.healthStats.recentRecordsCount" />
          </el-col>
          <el-col :span="12">
            <el-statistic
              title="最近预约时间"
              :value="formatDateTime(detail.healthStats.lastRecordTime)"
              value-style="font-size:15px"
            />
          </el-col>
        </el-row>

        <el-divider content-position="left">预约统计</el-divider>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-statistic title="预约总数" :value="detail.appointmentStats.totalCount" />
          </el-col>
          <el-col :span="12">
            <el-statistic title="已完成体检" :value="detail.appointmentStats.completedCount" />
          </el-col>
        </el-row>

        <div class="reset-password-section">
          <el-button type="warning" :icon="Key" :loading="resetting" @click="handleResetPassword">
            重置密码
          </el-button>
          <div class="form-hint">将该会员密码重置为 123456</div>
        </div>
      </template>
    </el-drawer>

    <!-- 调整等级弹窗 -->
    <el-dialog v-model="levelDialog.visible" title="调整会员等级" width="400px">
      <el-form label-width="90px">
        <el-form-item label="会员">
          <span>{{ levelDialog.row?.real_name || levelDialog.row?.phone }}</span>
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="levelDialog.levelCode" placeholder="请选择等级" style="width:100%">
            <el-option
              v-for="lv in levels"
              :key="lv.code"
              :label="lv.name"
              :value="lv.code"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="levelDialog.saving" @click="submitLevel">确定</el-button>
      </template>
    </el-dialog>

    <!-- 积分调整弹窗 -->
    <el-dialog v-model="pointsDialog.visible" title="调整会员积分" width="400px">
      <el-form label-width="100px">
        <el-form-item label="会员">
          <span>{{ pointsDialog.row?.real_name || pointsDialog.row?.phone }}</span>
        </el-form-item>
        <el-form-item label="当前积分">
          <span class="mono">{{ pointsDialog.row?.points }}</span>
        </el-form-item>
        <el-form-item label="积分变更量">
          <el-input-number v-model="pointsDialog.points" :step="100" :min="-99999" :max="99999" />
          <div class="form-hint">正数为增加积分，负数为扣减积分；不能为 0</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="pointsDialog.saving" @click="submitPoints">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增用户弹窗 -->
    <el-dialog v-model="addDialog.visible" title="新增用户" width="440px">
      <el-form :model="addDialog.form" label-width="90px" :rules="addDialog.rules" ref="addFormRef">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addDialog.form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addDialog.form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="用户身份" prop="role">
          <el-select v-model="addDialog.form.role" placeholder="请选择用户身份" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="会员" value="MEMBER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="addDialog.form.role === 'MEMBER'" label="初始积分">
          <el-input-number v-model="addDialog.form.points" :min="0" :max="999999" :step="50" />
          <div class="form-hint">仅会员可设置初始积分，默认为 0</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="addDialog.saving" @click="submitAddUser">确定新增</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  RefreshLeft,
  View,
  Medal,
  Coin,
  CircleCheck,
  CircleClose,
  User,
  Key,
  Plus,
  Delete
} from '@element-plus/icons-vue'
import { memberApi } from '@/api/member'
import { useUserStore } from '@/store/user'
import {
  formatDateTime,
  formatDate,
  dictLabel,
  dictType,
  userStatusMap,
  levelMap,
  genderText,
  buildAvatar
} from '@/utils/format'

const userStore = useUserStore()

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentUserId = ref(null)

const effectiveCurrentUserId = computed(() => currentUserId.value ?? userStore.user?.id)

const query = reactive({ page: 1, size: 10, phone: '', name: '', status: '' })

const detailVisible = ref(false)
const detail = ref(null)

const levels = ref([])

const levelDialog = reactive({ visible: false, row: null, levelCode: '', saving: false })
const pointsDialog = reactive({ visible: false, row: null, points: 100, saving: false })
const resetting = ref(false)

const addFormRef = ref(null)
const addDialog = reactive({
  visible: false,
  saving: false,
  form: { phone: '', password: '', role: '', points: 0 },
  rules: {
    phone: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^\d{11}$/, message: '请输入 11 位手机号', trigger: 'blur' }
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
    ],
    role: [{ required: true, message: '请选择用户身份', trigger: 'change' }]
  }
})

function canOperate(row) {
  return true
}

function isCurrentUser(row) {
  return row.id == effectiveCurrentUserId.value
}

async function loadList() {
  loading.value = true
  try {
    const res = await memberApi.getMemberList({
      page: query.page,
      size: query.size,
      phone: query.phone || undefined,
      name: query.name || undefined,
      status: query.status || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
    currentUserId.value = res.data.currentUserId
  } catch (e) {
    // 拦截器提示
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function handleReset() {
  query.phone = ''
  query.name = ''
  query.status = ''
  query.page = 1
  loadList()
}

async function showDetail(row) {
  detailVisible.value = true
  detail.value = null
  try {
    const res = await memberApi.getMemberDetail(row.id)
    detail.value = res.data
  } catch (e) {
    detailVisible.value = false
  }
}

async function toggleStatus(row, enable) {
  try {
    await ElMessageBox.confirm(
      `确定要${enable ? '启用' : '禁用'}会员「${row.real_name || row.phone}」吗？`,
      '操作确认',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    if (enable) {
      await memberApi.enableMember(row.id)
    } else {
      await memberApi.disableMember(row.id)
    }
    ElMessage.success(enable ? '启用成功' : '禁用成功')
    loadList()
  } catch (e) {
    // 取消或失败
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${row.real_name || row.phone}」吗？删除后将从列表隐藏，但数据库中仍保留记录。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消', confirmButtonClass: 'el-button--danger' }
    )
    await memberApi.deleteMember(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    // 取消或失败
  }
}

async function loadLevels() {
  try {
    const res = await memberApi.getMemberLevels()
    levels.value = res.data || []
  } catch (e) {
    /* ignore */
  }
}

function openLevelDialog(row) {
  levelDialog.row = row
  levelDialog.levelCode = row.member_level || 'NORMAL'
  levelDialog.visible = true
}

async function submitLevel() {
  if (!levelDialog.levelCode) {
    ElMessage.warning('请选择会员等级')
    return
  }
  levelDialog.saving = true
  try {
    await memberApi.updateMemberLevel(levelDialog.row.id, levelDialog.levelCode)
    ElMessage.success('等级更新成功')
    levelDialog.visible = false
    loadList()
  } catch (e) {
    // 拦截器提示
  } finally {
    levelDialog.saving = false
  }
}

function openPointsDialog(row) {
  pointsDialog.row = row
  pointsDialog.points = 100
  pointsDialog.visible = true
}

async function submitPoints() {
  if (!pointsDialog.points) {
    ElMessage.warning('积分变更量不能为 0')
    return
  }
  pointsDialog.saving = true
  try {
    const res = await memberApi.updateMemberPoints(pointsDialog.row.id, pointsDialog.points)
    ElMessage.success(
      `积分更新成功，当前积分：${res.data?.points ?? pointsDialog.row.points + pointsDialog.points}`
    )
    pointsDialog.visible = false
    loadList()
  } catch (e) {
    // 拦截器提示
  } finally {
    pointsDialog.saving = false
  }
}

async function handleResetPassword() {
  if (!detail.value?.member) return
  try {
    await ElMessageBox.confirm(
      `确定要将会员「${detail.value.member.real_name || detail.value.member.phone}」的密码重置为 123456 吗？`,
      '重置密码',
      { type: 'warning', confirmButtonText: '确定重置', cancelButtonText: '取消' }
    )
    resetting.value = true
    await memberApi.resetMemberPassword(detail.value.member.id)
    ElMessage.success('密码已重置为 123456')
  } catch (e) {
    if (e !== 'cancel') {
      // 拦截器提示
    }
  } finally {
    resetting.value = false
  }
}

function openAddDialog() {
  addDialog.form = { phone: '', password: '', role: '', points: 0 }
  addDialog.visible = true
  nextTick(() => addFormRef.value?.clearValidate())
}

async function submitAddUser() {
  if (!addFormRef.value) return
  const valid = await addFormRef.value.validate().catch(() => false)
  if (!valid) return
  addDialog.saving = true
  try {
    const payload = {
      phone: addDialog.form.phone,
      password: addDialog.form.password,
      role: addDialog.form.role
    }
    if (addDialog.form.role === 'MEMBER') {
      payload.points = addDialog.form.points
    }
    await memberApi.addMember(payload)
    ElMessage.success('新增用户成功')
    addDialog.visible = false
    loadList()
  } catch (e) {
    // 拦截器提示
  } finally {
    addDialog.saving = false
  }
}

onMounted(() => {
  loadLevels()
  loadList()
})
</script>

<style scoped>
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.form-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
.detail-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  margin-bottom: 14px;
  background: linear-gradient(120deg, rgba(102, 126, 234, 0.12), rgba(255, 255, 255, 0.03));
  border-radius: 12px;
}
.member-avatar {
  background: var(--primary-gradient) !important;
  color: #fff !important;
  font-size: 24px;
  flex-shrink: 0;
  box-shadow: 0 6px 18px rgba(102, 126, 234, 0.4);
}
.detail-header-info {
  flex: 1;
  min-width: 0;
}
.detail-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-main);
}
.detail-phone {
  font-size: 13px;
  color: var(--text-sub);
  margin-top: 2px;
}
.reset-password-section {
  margin-top: 18px;
  padding: 14px 16px;
  background: #fdf6ec;
  border-radius: 8px;
  border: 1px solid #faecd8;
  text-align: center;
}
.reset-password-section .form-hint {
  margin-top: 6px;
}
.text-muted {
  color: #c0c4cc;
  font-size: 13px;
}
</style>