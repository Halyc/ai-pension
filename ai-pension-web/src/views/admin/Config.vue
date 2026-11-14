<template>
  <div class="page-container">
    <div class="page-card">
      <div class="page-title"><el-icon><Setting /></el-icon>系统配置</div>
      <div class="page-subtitle">维护平台运行参数（键值对形式，键不存在时自动新建）</div>

      <div class="toolbar">
        <el-input
          v-model="keyQuery"
          placeholder="按配置键筛选"
          clearable
          style="width: 260px"
          :prefix-icon="Search"
        />
        <div>
          <el-button :icon="Refresh" @click="loadList">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openDialog()">新增配置</el-button>
        </div>
      </div>

      <el-table :data="filteredList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="configKey" label="配置键" min-width="200">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.configKey }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="configValue" label="配置值" min-width="240">
          <template #default="{ row }">
            <span class="config-value">{{ row.configValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="150" align="center" header-align="center">
          <template #default="{ row }">
            <el-button link type="warning" :icon="Edit" @click="openDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 编辑/新增弹窗 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.isEdit ? '编辑配置' : '新增配置'"
      width="520px"
    >
      <el-form :model="form" label-width="90px">
        <el-form-item label="配置键" required>
          <el-input
            v-model="form.key"
            placeholder="如：points.register.gift"
            :disabled="dialog.isEdit"
          />
        </el-form-item>
        <el-form-item label="配置值" required>
          <el-input v-model="form.value" type="textarea" :rows="4" placeholder="配置内容（必填）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="配置说明（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Edit, Refresh, Setting } from '@element-plus/icons-vue'
import { configApi } from '@/api/config'

const loading = ref(false)
const list = ref([])
const keyQuery = ref('')

const filteredList = computed(() =>
  keyQuery.value
    ? list.value.filter((c) => c.configKey?.toLowerCase().includes(keyQuery.value.toLowerCase()))
    : list.value
)

const dialog = reactive({ visible: false, isEdit: false, saving: false })
const form = reactive({ key: '', value: '', description: '' })

async function loadList() {
  loading.value = true
  try {
    const res = await configApi.listConfigs()
    list.value = res.data || []
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    dialog.isEdit = true
    form.key = row.configKey
    form.value = row.configValue
    form.description = row.description || ''
  } else {
    dialog.isEdit = false
    form.key = ''
    form.value = ''
    form.description = ''
  }
  dialog.visible = true
}

async function submit() {
  if (!form.key.trim()) {
    ElMessage.warning('请输入配置键')
    return
  }
  if (!form.value?.trim()) {
    ElMessage.warning('配置值不能为空（后端校验：value 必填）')
    return
  }
  dialog.saving = true
  try {
    // PUT /api/admin/config/{key}：不存在自动新建，存在则更新
    await configApi.updateConfig(form.key.trim(), {
      value: form.value,
      description: form.description || null
    })
    ElMessage.success(dialog.isEdit ? '更新成功' : '创建成功')
    dialog.visible = false
    loadList()
  } catch (e) {
    /* 拦截器提示 */
  } finally {
    dialog.saving = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 10px;
}
.config-value {
  word-break: break-all;
  color: var(--text-main);
}
</style>
