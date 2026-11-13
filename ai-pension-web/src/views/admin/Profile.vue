<template>
  <div class="page-container google-style">
    <div class="profile-wrap">
      <div class="profile-hero">
        <div class="avatar-wrapper" @click="triggerAvatarUpload">
          <el-avatar :size="108" class="hero-avatar" :src="profileAvatar(profile) || form.avatar || ''">
            {{ avatarText }}
          </el-avatar>
          <div class="avatar-overlay">
            <el-icon :size="28"><Camera /></el-icon>
            <span>更换头像</span>
          </div>
          <input
            ref="avatarFileInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            style="display:none"
            @change="onAvatarFileChange"
          />
        </div>
        <div class="hero-name">{{ profile.real_name || profile.phone }}</div>
        <div class="hero-phone">{{ profile.phone }}</div>
        <el-tag type="danger" effect="dark" round size="large">系统管理员</el-tag>

        <div class="hero-stats">
          <div class="stat-item">
            <div class="stat-label">账号角色</div>
            <div class="stat-value small">ADMIN</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">账号状态</div>
            <el-tag size="small" :type="dictType(userStatusMap, profile.status)">
              {{ dictLabel(userStatusMap, profile.status) }}
            </el-tag>
          </div>
          <div class="stat-item">
            <div class="stat-label">创建时间</div>
            <div class="stat-value small">{{ formatDateTime(profile.create_time) }}</div>
          </div>
        </div>
      </div>

      <div class="page-card profile-form-card" v-loading="loading">
        <el-form ref="profileFormRef" :model="form" label-width="100px" class="profile-form">
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="手机号" maxlength="11" />
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="form.real_name" placeholder="真实姓名" maxlength="20" />
          </el-form-item>
          <el-form-item label="性别">
            <el-radio-group v-model="form.gender">
              <el-radio value="男">男</el-radio>
              <el-radio value="女">女</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="出生日期">
            <el-date-picker
              v-model="form.birthDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择出生日期"
              style="width:100%"
            />
          </el-form-item>
          <el-form-item class="form-actions">
            <el-button class="btn-save" :icon="Check" :loading="saving" @click="saveProfile">
              保存修改
            </el-button>
            <el-button class="btn-change-pwd" :icon="Lock" @click="pwdDialogVisible = true">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <el-dialog v-model="pwdDialogVisible" title="修改登录密码" width="460px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-16位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :icon="Key" :loading="pwdSaving" @click="savePassword">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Lock, Check, Key, Camera } from '@element-plus/icons-vue'
import { adminProfileApi } from '@/api/adminProfile'
import { uploadApi } from '@/api/upload'
import { useUserStore } from '@/store/user'
import {
  formatDateTime,
  dictLabel,
  dictType,
  userStatusMap,
  buildAvatar
} from '@/utils/format'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const pwdSaving = ref(false)
const profileFormRef = ref()
const pwdFormRef = ref()
const pwdDialogVisible = ref(false)
const avatarFileInput = ref()

const imgRefreshTick = ref(0)
function profileAvatar(row) { imgRefreshTick.value; return buildAvatar(row) }
window.addEventListener('image-cache-ready', () => { imgRefreshTick.value++ })
window.addEventListener('user-avatar-updated', () => { imgRefreshTick.value++ })

const profile = reactive({})
const form = reactive({
  phone: '',
  real_name: '',
  gender: '男',
  birthDate: '',
  avatar: ''
})

const avatarText = computed(() =>
  (profile.real_name || profile.phone || 'A').charAt(0).toUpperCase()
)

async function loadProfile() {
  loading.value = true
  try {
    const res = await adminProfileApi.getProfile()
    Object.assign(profile, res.data)
    form.phone = res.data.phone || ''
    form.real_name = res.data.real_name || ''
    form.gender = res.data.gender || '男'
    form.birthDate = res.data.birth_date ? String(res.data.birth_date).slice(0, 10) : ''
    form.avatar = res.data.avatar || ''
  } catch (e) {
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!/^1\d{10}$/.test(form.phone)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  saving.value = true
  try {
    if (form.avatar) {
      const { saveImage, userKey } = await import('@/utils/imageCache')
      saveImage(userKey(profile.id), form.avatar)
      window.dispatchEvent(new Event('user-avatar-updated'))
    }
    await adminProfileApi.updateProfile({
      id: profile.id,
      phone: form.phone.trim(),
      real_name: form.real_name?.trim() || null,
      gender: form.gender,
      avatar: form.avatar || null,
      birth_date: form.birthDate ? `${form.birthDate}T00:00:00` : null
    })
    ElMessage.success('资料更新成功')
    userStore.setUser({ ...profile, phone: form.phone, real_name: form.real_name, avatar: form.avatar || profile.avatar })
    loadProfile()
  } catch (e) {
  } finally {
    saving.value = false
  }
}

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^\w{6,16}$/, message: '密码为6-16位字母、数字或下划线', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, cb) =>
        value === pwdForm.newPassword ? cb() : cb(new Error('两次输入的密码不一致')),
      trigger: 'blur'
    }
  ]
}

async function savePassword() {
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdSaving.value = true
    try {
      await adminProfileApi.changePassword({
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      ElMessage.success('密码修改成功')
      pwdFormRef.value.resetFields()
      pwdDialogVisible.value = false
    } catch (e) {
    } finally {
      pwdSaving.value = false
    }
  })
}

function triggerAvatarUpload() {
  avatarFileInput.value?.click()
}

async function onAvatarFileChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!/image\/(jpeg|png|webp)/.test(file.type)) {
    ElMessage.warning('仅支持 JPG、PNG、WEBP 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  try {
    const res = await uploadApi.uploadImage(file, 'avatars')
    if (res.code === 200 && res.data?.url) {
      form.avatar = res.data.url
      ElMessage.success('头像上传成功，保存后生效')
    } else {
      ElMessage.error(res.message || '头像上传失败')
    }
  } catch (err) {
    ElMessage.error('头像上传失败，请重试')
  } finally {
    e.target.value = ''
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.google-style {
  padding: 48px 24px 64px;
  background: #fafafa;
  min-height: 100%;
}
.profile-wrap {
  max-width: 680px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 28px;
}
.profile-hero {
  text-align: center;
  padding: 40px 32px 32px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.avatar-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
  cursor: pointer;
  border-radius: 50%;
}
.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.25s ease;
  pointer-events: none;
  font-size: 12px;
}
.avatar-wrapper:hover .avatar-overlay { opacity: 1; }
.hero-avatar {
  background: linear-gradient(135deg, #ff5c72 0%, #ff8a5c 100%) !important;
  color: #fff !important;
  font-size: 40px;
  box-shadow: 0 8px 24px rgba(255, 92, 114, 0.35);
}
.hero-name {
  font-size: 24px;
  font-weight: 600;
  color: #202124;
  margin-bottom: 4px;
}
.hero-phone {
  font-size: 14px;
  color: #5f6368;
  margin-bottom: 12px;
}
.hero-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #eee;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.stat-label {
  font-size: 12px;
  color: #80868b;
  letter-spacing: 0.5px;
}
.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #202124;
  font-variant-numeric: tabular-nums;
}
.stat-value.small {
  font-size: 13px;
  font-weight: 500;
  color: #5f6368;
}

.profile-form-card {
  padding: 40px 48px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.profile-form {
  max-width: 480px;
  margin: 0 auto;
}
.form-actions { margin-top: 24px; }
.form-actions :deep(.el-form-item__label) { display: none; }
.form-actions :deep(.el-form-item__content) {
  justify-content: center;
  width: 100%;
}

.btn-save,
.btn-change-pwd {
  height: 40px;
  padding: 0 28px;
  font-weight: 600;
  font-size: 14px;
  letter-spacing: 0.5px;
  border: none;
  border-radius: 10px;
  transition: all 0.2s ease;
}
.btn-save {
  color: #fff;
  background: linear-gradient(135deg, #ff5c72 0%, #ff8a5c 100%);
  box-shadow: 0 4px 14px rgba(255, 92, 114, 0.4);
}
.btn-save:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(255, 92, 114, 0.55);
}
.btn-change-pwd {
  color: #fff;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35);
}
.btn-change-pwd:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(102, 126, 234, 0.5);
}

:deep(.el-form-item__label) {
  color: #5f6368;
  font-size: 14px;
}
:deep(.el-input__wrapper),
:deep(.el-date-editor.el-input .el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dadce0 inset;
}
:deep(.el-input__wrapper.is-focus),
:deep(.el-input__wrapper.is-focus-within) {
  box-shadow: 0 0 0 2px #1a73e8 inset;
}
</style>