<template>
  <div class="login-page">
    <div class="ambient ambient-left"></div>
    <div class="ambient ambient-right"></div>
    <div class="dot-field"></div>

    <main class="login-shell">
      <section class="brand-panel">
        <div class="brand-decoration brand-decoration-one"></div>
        <div class="brand-decoration brand-decoration-two"></div>

        <div class="brand-top">
          <div class="brand-mark">
            <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                  d="M14 38C11.7909 38 10 36.2091 10 34C10 31.7909 11.7909 30 14 30C14.185 30 14.3664 30.0125 14.5424 30.0362C15.1283 26.689 18.189 24 22 24C23.252 24 24.4237 24.3394 25.4291 24.9107C26.3367 23.7772 27.8355 23 29.5 23C32.8137 23 35.5 25.6863 35.5 29C35.5 29.1203 35.5036 29.2394 35.5104 29.357C37.5784 29.6288 39.2 31.4384 39.2 33.6C39.2 36.0257 37.2257 38 34.8 38H14Z"
                  fill="currentColor"
              />
              <circle cx="18" cy="34" r="1.5" fill="#2f7d6d"/>
              <circle cx="24" cy="34" r="1.5" fill="#2f7d6d"/>
              <circle cx="30" cy="34" r="1.5" fill="#2f7d6d"/>
            </svg>
          </div>
          <div class="brand-name">
            <strong>颐康云</strong>
            <span>SMART ELDERLY CARE</span>
          </div>
        </div>

        <div class="brand-copy">
          <span class="eyebrow"><i></i> 智慧养老 · 温暖相伴</span>
          <h1>让关怀更近<br/><em>让健康更安心</em></h1>
          <p>
            连接健康档案、体检预约与社区活动，用更简单的服务方式，
            陪伴长辈享受安心、从容的每一天。
          </p>
        </div>

        <div class="care-scene" aria-hidden="true">
          <div class="scene-sun"></div>
          <div class="scene-cloud scene-cloud-one"></div>
          <div class="scene-cloud scene-cloud-two"></div>
          <div class="scene-hill scene-hill-back"></div>
          <div class="scene-hill scene-hill-front"></div>
          <div class="scene-house">
            <div class="house-roof"></div>
            <div class="house-body">
              <span class="house-window"></span>
              <span class="house-door"></span>
            </div>
          </div>
          <div class="scene-tree scene-tree-left"><span></span></div>
          <div class="scene-tree scene-tree-right"><span></span></div>
          <div class="scene-path"></div>

          <div class="health-float">
            <div class="health-float-icon">AI</div>
            <div>
              <span>健康助手</span>
              <strong>全天候贴心守护</strong>
            </div>
            <span class="health-float-dot"></span>
          </div>
        </div>

        <div class="feature-list">
          <div class="feature-item">
            <span class="feature-icon"><el-icon><Calendar/></el-icon></span>
            <div>
              <strong>便捷预约</strong>
              <small>体检时段一目了然</small>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon"><el-icon><FirstAidKit/></el-icon></span>
            <div>
              <strong>健康档案</strong>
              <small>身体变化随时掌握</small>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon"><el-icon><ChatDotRound/></el-icon></span>
            <div>
              <strong>AI 陪伴</strong>
              <small>健康疑问耐心解答</small>
            </div>
          </div>
        </div>
      </section>

      <section class="auth-panel">
        <div class="auth-topline">
          <div class="mobile-brand">
            <div class="mobile-brand-mark">
              <el-icon>
                <FirstAidKit/>
              </el-icon>
            </div>
            <strong>颐康云</strong>
          </div>
          <span class="security-note">
            <el-icon><Lock/></el-icon>
            健康服务专属入口
          </span>
        </div>

        <header class="auth-header">
          <span class="section-kicker">{{ activeTab === 'login' ? 'WELCOME BACK' : 'JOIN US' }}</span>
          <h2>{{ activeTab === 'login' ? '欢迎回来' : '创建会员账户' }}</h2>
          <p>
            {{
              activeTab === 'login'
                  ? '登录后，继续查看您的健康记录与服务安排。'
                  : '只需一分钟，开启您的专属健康服务。'
            }}
          </p>
        </header>

        <div class="auth-switch" role="tablist" aria-label="账户操作">
          <button
              type="button"
              role="tab"
              :aria-selected="activeTab === 'login'"
              :class="{ active: activeTab === 'login' }"
              @click="activeTab = 'login'"
          >
            登录
          </button>
          <button
              type="button"
              role="tab"
              :aria-selected="activeTab === 'register'"
              :class="{ active: activeTab === 'register' }"
              @click="activeTab = 'register'"
          >
            会员注册
          </button>
        </div>

        <div class="form-stage">
          <el-form
              v-show="activeTab === 'login'"
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              size="large"
              class="auth-form"
              @keyup.enter="handleLogin"
          >
            <div class="login-mode" role="tablist" aria-label="登录方式">
              <button
                  type="button"
                  role="tab"
                  :aria-selected="loginMode === 'password'"
                  :class="{ active: loginMode === 'password' }"
                  @click="switchLoginMode('password')"
              >
                密码登录
              </button>
              <button
                  type="button"
                  role="tab"
                  :aria-selected="loginMode === 'sms'"
                  :class="{ active: loginMode === 'sms' }"
                  @click="switchLoginMode('sms')"
              >
                验证码登录
              </button>
            </div>

            <div class="field-group">
              <label class="input-label" for="login-phone">手机号</label>
              <el-form-item prop="phone">
                <el-input
                    id="login-phone"
                    v-model="loginForm.phone"
                    placeholder="请输入您的手机号"
                    :prefix-icon="User"
                    clearable
                />
              </el-form-item>
            </div>

            <div v-if="loginMode === 'password'" class="field-group">
              <label class="input-label" for="login-password">登录密码</label>
              <el-form-item prop="password">
                <el-input
                    id="login-password"
                    v-model="loginForm.password"
                    type="password"
                    placeholder="请输入登录密码"
                    :prefix-icon="Lock"
                    show-password
                    clearable
                />
              </el-form-item>
            </div>

            <div v-else class="field-group">
              <label class="input-label" for="login-sms-code">短信验证码</label>
              <el-form-item prop="smsCode">
                <div class="sms-code-row">
                  <el-input
                      id="login-sms-code"
                      v-model="loginForm.smsCode"
                      placeholder="请输入 6 位验证码"
                      :prefix-icon="Message"
                      maxlength="6"
                      clearable
                  />
                  <button
                      type="button"
                      class="sms-btn"
                      :disabled="loginCountdown > 0 || smsSending"
                      @click="sendSmsCode(loginForm.phone, loginCountdown)"
                  >
                    {{ loginCountdown > 0 ? `${loginCountdown}s 后重发` : '获取验证码' }}
                  </button>
                </div>
              </el-form-item>
            </div>

            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住登录状态</el-checkbox>
              <button type="button" class="forgot-link" @click="showResetDialog = true">
                忘记密码？
              </button>
            </div>

            <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="loading"
                @click="handleLogin"
            >
              <span>进入颐康云</span>
              <el-icon v-if="!loading">
                <ArrowRight/>
              </el-icon>
            </el-button>

            <div class="role-note">
              <span><i class="role-dot member"></i>会员服务门户</span>
              <span><i class="role-dot admin"></i>管理后台</span>
            </div>
          </el-form>

          <el-form
              v-show="activeTab === 'register'"
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              size="large"
              class="auth-form register-form"
              @keyup.enter="handleRegister"
          >
            <div class="register-grid">
              <div class="field-group">
                <label class="input-label" for="register-phone">手机号</label>
                <el-form-item prop="phone">
                  <el-input
                      id="register-phone"
                      v-model="registerForm.phone"
                      placeholder="11 位手机号"
                      :prefix-icon="User"
                      clearable
                  />
                </el-form-item>
              </div>

              <div class="field-group">
                <label class="input-label" for="register-name">姓名</label>
                <el-form-item prop="real_name">
                  <el-input
                      id="register-name"
                      v-model="registerForm.real_name"
                      placeholder="选填，方便称呼您"
                      :prefix-icon="Postcard"
                      clearable
                  />
                </el-form-item>
              </div>
            </div>

            <div class="field-group">
              <label class="input-label" for="register-sms-code">短信验证码</label>
              <el-form-item prop="smsCode">
                <div class="sms-code-row">
                  <el-input
                      id="register-sms-code"
                      v-model="registerForm.smsCode"
                      placeholder="请输入 6 位验证码"
                      :prefix-icon="Message"
                      maxlength="6"
                      clearable
                  />
                  <button
                      type="button"
                      class="sms-btn"
                      :disabled="registerCountdown > 0 || smsSending"
                      @click="sendSmsCode(registerForm.phone, registerCountdown)"
                  >
                    {{ registerCountdown > 0 ? `${registerCountdown}s 后重发` : '获取验证码' }}
                  </button>
                </div>
              </el-form-item>
            </div>

            <div class="field-group">
              <label class="input-label" for="register-password">设置密码</label>
              <el-form-item prop="password">
                <el-input
                    id="register-password"
                    v-model="registerForm.password"
                    type="password"
                    placeholder="6-16 位字母、数字或下划线"
                    :prefix-icon="Lock"
                    show-password
                    clearable
                />
              </el-form-item>
            </div>

            <div class="field-group">
              <label class="input-label" for="register-confirm">确认密码</label>
              <el-form-item prop="confirmPassword">
                <el-input
                    id="register-confirm"
                    v-model="registerForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入密码"
                    :prefix-icon="Lock"
                    show-password
                    clearable
                />
              </el-form-item>
            </div>

            <el-button
                type="primary"
                size="large"
                class="submit-btn"
                :loading="loading"
                @click="handleRegister"
            >
              <span>创建账户</span>
              <el-icon v-if="!loading">
                <ArrowRight/>
              </el-icon>
            </el-button>

            <div class="welcome-gift">
              <span class="gift-icon">✦</span>
              注册即赠 <strong>1000 积分</strong>，可用于预约体检服务
            </div>
          </el-form>
        </div>

        <div class="auth-footer">
          <el-icon>
            <CircleCheck/>
          </el-icon>
          使用服务即表示您同意平台的服务与隐私说明
        </div>
      </section>
    </main>

    <footer class="page-footer">
      <span>© 2026 颐康云</span>
      <i></i>
      <span>科技赋能养老生活</span>
    </footer>

    <el-dialog
        v-model="showResetDialog"
        title="重置登录密码"
        width="min(430px, calc(100vw - 32px))"
        :close-on-click-modal="false"
        destroy-on-close
        class="reset-dialog"
    >
      <div class="dialog-intro">
        请填写注册手机号，获取短信验证码后设置新密码。
      </div>
      <el-form
          ref="resetFormRef"
          :model="resetForm"
          :rules="resetRules"
          label-position="top"
          size="large"
          @keyup.enter="handleResetPassword"
      >
        <el-form-item label="注册手机号" prop="phone">
          <el-input
              v-model="resetForm.phone"
              placeholder="请输入注册手机号"
              :prefix-icon="User"
              clearable
          />
        </el-form-item>
        <el-form-item label="短信验证码" prop="smsCode">
          <div class="sms-code-row">
            <el-input
                v-model="resetForm.smsCode"
                placeholder="请输入 6 位验证码"
                :prefix-icon="Message"
                maxlength="6"
                clearable
            />
            <button
                type="button"
                class="sms-btn"
                :disabled="resetCountdown > 0 || smsSending"
                @click="sendSmsCode(resetForm.phone, resetCountdown)"
            >
              {{ resetCountdown > 0 ? `${resetCountdown}s 后重发` : '获取验证码' }}
            </button>
          </div>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
              v-model="resetForm.newPassword"
              type="password"
              placeholder="请输入新密码（6-16 位）"
              :prefix-icon="Lock"
              show-password
              clearable
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
              v-model="resetForm.confirmPassword"
              type="password"
              placeholder="请再次输入新密码"
              :prefix-icon="Lock"
              show-password
              clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showResetDialog = false">取消</el-button>
        <el-button
            type="primary"
            class="reset-confirm-btn"
            :loading="resetLoading"
            @click="handleResetPassword"
        >
          确认重置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, onBeforeUnmount, reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {
  ArrowRight,
  Calendar,
  ChatDotRound,
  CircleCheck,
  FirstAidKit,
  Lock,
  Message,
  Postcard,
  User
} from '@element-plus/icons-vue'
import {authApi} from '@/api/auth'
import {useUserStore} from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loginMode = ref('password')
const loading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()
const rememberMe = ref(false)

const smsSending = ref(false)
const loginCountdown = ref(0)
const registerCountdown = ref(0)
const resetCountdown = ref(0)
const countdownTimers = []

const showResetDialog = ref(false)
const resetLoading = ref(false)
const resetFormRef = ref()

const phoneValidator = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入手机号'))
  if (!/^1\d{10}$/.test(value)) return callback(new Error('手机号格式不正确'))
  callback()
}

const passwordValidator = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入密码'))
  if (!/^\w{6,16}$/.test(value)) return callback(new Error('密码为6-16位字母、数字或下划线'))
  callback()
}

const smsCodeValidator = (rule, value, callback) => {
  if (!value) return callback(new Error('请输入短信验证码'))
  if (!/^\d{6}$/.test(value)) return callback(new Error('验证码为 6 位数字'))
  callback()
}

const loginForm = reactive({phone: '', password: '', smsCode: ''})
const loginRules = computed(() => ({
  phone: [{validator: phoneValidator, trigger: 'blur'}],
  password: loginMode.value === 'password' ? [{validator: passwordValidator, trigger: 'blur'}] : [],
  smsCode: loginMode.value === 'sms' ? [{validator: smsCodeValidator, trigger: 'blur'}] : []
}))

const registerForm = reactive({
  phone: '',
  real_name: '',
  smsCode: '',
  password: '',
  confirmPassword: ''
})
const registerRules = {
  phone: [{validator: phoneValidator, trigger: 'blur'}],
  smsCode: [{validator: smsCodeValidator, trigger: 'blur'}],
  password: [{validator: passwordValidator, trigger: 'blur'}],
  confirmPassword: [
    {
      validator: (rule, value, callback) => {
        if (!value) return callback(new Error('请再次输入密码'))
        if (value !== registerForm.password) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const resetForm = reactive({
  phone: '',
  smsCode: '',
  newPassword: '',
  confirmPassword: ''
})
const resetRules = {
  phone: [{validator: phoneValidator, trigger: 'blur'}],
  smsCode: [{validator: smsCodeValidator, trigger: 'blur'}],
  newPassword: [{validator: passwordValidator, trigger: 'blur'}],
  confirmPassword: [
    {
      validator: (rule, value, callback) => {
        if (!value) return callback(new Error('请再次输入新密码'))
        if (value !== resetForm.newPassword) return callback(new Error('两次输入的密码不一致'))
        callback()
      },
      trigger: 'blur'
    }
  ]
}

function switchLoginMode(mode) {
  if (loginMode.value === mode) return
  loginMode.value = mode
  loginFormRef.value?.clearValidate()
}

function startCountdown(countdownRef) {
  countdownRef.value = 60
  const timer = setInterval(() => {
    countdownRef.value -= 1
    if (countdownRef.value <= 0) {
      clearInterval(timer)
      const index = countdownTimers.indexOf(timer)
      if (index > -1) countdownTimers.splice(index, 1)
    }
  }, 1000)
  countdownTimers.push(timer)
}

onBeforeUnmount(() => {
  countdownTimers.forEach(timer => clearInterval(timer))
})

async function sendSmsCode(phone, countdownRef) {
  if (countdownRef.value > 0 || smsSending.value) return
  if (!phone || !phone.trim()) {
    ElMessage.warning('请先输入手机号')
    return
  }
  if (!/^1\d{10}$/.test(phone.trim())) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  smsSending.value = true
  try {
    const res = await authApi.sendSmsCode({phone: phone.trim()})
    ElMessage.success(res.message || '验证码已发送')
    startCountdown(countdownRef)
  } catch (e) {
    // 统一请求拦截器已展示错误信息
  } finally {
    smsSending.value = false
  }
}

function redirectByRole() {
  const redirect = route.query.redirect
  if (redirect && typeof redirect === 'string') {
    router.replace(redirect)
    return
  }
  router.replace(userStore.isAdmin ? '/admin/dashboard' : '/member/home')
}

async function handleLogin() {
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const payload = {phone: loginForm.phone.trim()}
      if (loginMode.value === 'sms') {
        payload.smsCode = loginForm.smsCode.trim()
      } else {
        payload.password = loginForm.password
      }
      const res = await authApi.login(payload)
      userStore.setLogin(res.data, rememberMe.value)
      ElMessage.success(res.message || '登录成功')
      redirectByRole()
    } catch (e) {
      // 统一请求拦截器已展示错误信息
    } finally {
      loading.value = false
    }
  })
}

async function handleRegister() {
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authApi.register({
        phone: registerForm.phone.trim(),
        password: registerForm.password,
        smsCode: registerForm.smsCode.trim(),
        real_name: registerForm.real_name?.trim() || null
      })
      ElMessage.success('注册成功，请登录')
      loginForm.phone = registerForm.phone
      loginForm.password = ''
      loginForm.smsCode = ''
      activeTab.value = 'login'
    } catch (e) {
      // 统一请求拦截器已展示错误信息
    } finally {
      loading.value = false
    }
  })
}

async function handleResetPassword() {
  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return
    resetLoading.value = true
    try {
      await authApi.resetPassword({
        phone: resetForm.phone.trim(),
        newPassword: resetForm.newPassword,
        smsCode: resetForm.smsCode.trim()
      })
      ElMessage.success('密码重置成功，请使用新密码登录')
      loginForm.phone = resetForm.phone
      loginForm.password = ''
      loginForm.smsCode = ''
      showResetDialog.value = false
      resetFormRef.value?.resetFields()
    } catch (e) {
      // 统一请求拦截器已展示错误信息
    } finally {
      resetLoading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  --ink: #19352f;
  --ink-soft: #526b65;
  --muted: #7d918c;
  --teal: #2f7d6d;
  --teal-deep: #1f6559;
  --teal-soft: #e8f3ef;
  --cream: #f7f3eb;
  --orange: #d9963e;
  position: relative;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 28px;
  overflow: auto;
  background: radial-gradient(circle at 8% 12%, rgba(47, 125, 109, 0.12), transparent 28%),
  radial-gradient(circle at 92% 88%, rgba(217, 150, 62, 0.13), transparent 26%),
  linear-gradient(145deg, #f8f5ee 0%, #f2f7f3 50%, #fbf8f1 100%);
}

.ambient {
  position: fixed;
  border-radius: 50%;
  pointer-events: none;
  filter: blur(1px);
}

.ambient-left {
  width: 340px;
  height: 340px;
  left: -140px;
  bottom: -150px;
  border: 1px solid rgba(47, 125, 109, 0.12);
  box-shadow: 0 0 0 54px rgba(47, 125, 109, 0.025),
  0 0 0 108px rgba(47, 125, 109, 0.018);
}

.ambient-right {
  width: 240px;
  height: 240px;
  right: -90px;
  top: -90px;
  background: rgba(217, 150, 62, 0.08);
}

.dot-field {
  position: fixed;
  inset: 0;
  pointer-events: none;
  opacity: 0.38;
  background-image: radial-gradient(rgba(31, 101, 89, 0.2) 0.7px, transparent 0.7px);
  background-size: 24px 24px;
  mask-image: linear-gradient(115deg, transparent 0%, #000 36%, transparent 72%);
  -webkit-mask-image: linear-gradient(115deg, transparent 0%, #000 36%, transparent 72%);
}

.login-shell {
  position: relative;
  z-index: 1;
  width: min(1180px, 100%);
  min-height: min(760px, calc(100vh - 100px));
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(430px, 0.92fr);
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(255, 255, 255, 0.95);
  border-radius: 32px;
  box-shadow: 0 36px 90px rgba(40, 76, 66, 0.14),
  0 4px 16px rgba(40, 76, 66, 0.05);
  overflow: hidden;
  animation: shell-in 0.65s cubic-bezier(0.16, 1, 0.3, 1) both;
}

@keyframes shell-in {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.99);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.brand-panel {
  position: relative;
  min-height: 100%;
  padding: 42px 48px 36px;
  display: flex;
  flex-direction: column;
  color: #f7fffc;
  overflow: hidden;
  background: radial-gradient(circle at 84% 10%, rgba(248, 205, 133, 0.23), transparent 24%),
  radial-gradient(circle at 16% 86%, rgba(121, 198, 175, 0.2), transparent 32%),
  linear-gradient(145deg, #266f62 0%, #1e5c52 52%, #174c44 100%);
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image: linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
  linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: linear-gradient(to bottom, #000, transparent 82%);
  -webkit-mask-image: linear-gradient(to bottom, #000, transparent 82%);
}

.brand-decoration {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.1);
  pointer-events: none;
}

.brand-decoration-one {
  width: 280px;
  height: 280px;
  right: -145px;
  top: 34%;
  box-shadow: 0 0 0 52px rgba(255, 255, 255, 0.025);
}

.brand-decoration-two {
  width: 130px;
  height: 130px;
  left: -72px;
  bottom: 118px;
  box-shadow: 0 0 0 34px rgba(255, 255, 255, 0.02);
}

.brand-top,
.brand-copy,
.care-scene,
.feature-list {
  position: relative;
  z-index: 1;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 13px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 15px;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.brand-mark svg {
  width: 29px;
  height: 29px;
}

.brand-name {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-name strong {
  font-size: 21px;
  letter-spacing: 3px;
}

.brand-name span {
  margin-top: 5px;
  font-size: 9px;
  letter-spacing: 2.2px;
  color: rgba(239, 255, 250, 0.58);
}

.brand-copy {
  margin-top: 52px;
  max-width: 510px;
}

.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 1.2px;
  color: #f1d19b;
}

.eyebrow i {
  width: 22px;
  height: 1px;
  background: #e9c684;
}

.brand-copy h1 {
  margin: 18px 0 20px;
  font-family: "STSong", "Songti SC", "Noto Serif SC", serif;
  font-size: clamp(38px, 4.1vw, 58px);
  font-weight: 700;
  line-height: 1.24;
  letter-spacing: 1px;
  color: #fff;
}

.brand-copy h1 em {
  font-style: normal;
  color: #f0cd90;
}

.brand-copy p {
  max-width: 450px;
  font-size: 15px;
  line-height: 1.9;
  color: rgba(244, 255, 251, 0.72);
}

.care-scene {
  position: relative;
  height: 180px;
  margin: 18px 0 6px;
  overflow: hidden;
  opacity: 0.95;
}

.scene-sun {
  position: absolute;
  top: 16px;
  right: 54px;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: linear-gradient(145deg, #f7dca5, #e9b865);
  box-shadow: 0 0 0 13px rgba(247, 220, 165, 0.08);
}

.scene-cloud {
  position: absolute;
  height: 5px;
  border-radius: 999px;
  background: rgba(240, 255, 250, 0.2);
}

.scene-cloud::before,
.scene-cloud::after {
  content: '';
  position: absolute;
  width: 28px;
  height: 16px;
  bottom: 0;
  border-radius: 50%;
  background: inherit;
}

.scene-cloud-one {
  top: 42px;
  left: 52px;
  width: 58px;
}

.scene-cloud-two {
  top: 72px;
  right: 146px;
  width: 44px;
  opacity: 0.68;
}

.scene-hill {
  position: absolute;
  border-radius: 50%;
}

.scene-hill-back {
  width: 118%;
  height: 150px;
  left: -9%;
  bottom: -78px;
  background: #2e786a;
  transform: rotate(-3deg);
}

.scene-hill-front {
  width: 112%;
  height: 128px;
  left: -6%;
  bottom: -72px;
  background: #3c8878;
  transform: rotate(3deg);
}

.scene-house {
  position: absolute;
  left: 112px;
  bottom: 25px;
  width: 108px;
  height: 92px;
  z-index: 2;
}

.house-roof {
  width: 0;
  height: 0;
  margin: 0 auto;
  border-left: 61px solid transparent;
  border-right: 61px solid transparent;
  border-bottom: 48px solid #f0d3a3;
  transform: translateX(-7px);
}

.house-body {
  position: absolute;
  left: 15px;
  right: 15px;
  bottom: 0;
  height: 52px;
  border-radius: 5px 5px 2px 2px;
  background: #fff3df;
  box-shadow: 0 8px 22px rgba(10, 52, 45, 0.19);
}

.house-window {
  position: absolute;
  left: 12px;
  top: 12px;
  width: 21px;
  height: 17px;
  border-radius: 3px;
  background: #8bc6b6;
  box-shadow: inset 0 0 0 2px #dda95a;
}

.house-door {
  position: absolute;
  right: 13px;
  bottom: 0;
  width: 20px;
  height: 34px;
  border-radius: 3px 3px 0 0;
  background: #c79150;
}

.scene-tree {
  position: absolute;
  bottom: 27px;
  width: 8px;
  height: 62px;
  border-radius: 6px;
  background: #8e6844;
  z-index: 2;
}

.scene-tree span,
.scene-tree span::before,
.scene-tree span::after {
  position: absolute;
  content: '';
  border-radius: 50%;
  background: #77b39f;
}

.scene-tree span {
  width: 48px;
  height: 48px;
  left: -20px;
  top: -28px;
}

.scene-tree span::before {
  width: 32px;
  height: 32px;
  left: -14px;
  top: 16px;
}

.scene-tree span::after {
  width: 35px;
  height: 35px;
  right: -16px;
  top: 14px;
}

.scene-tree-left {
  left: 42px;
  transform: scale(0.82);
}

.scene-tree-right {
  right: 66px;
  transform: scale(0.66);
}

.scene-path {
  position: absolute;
  width: 92px;
  height: 28px;
  left: 124px;
  bottom: -2px;
  border-radius: 50% 50% 0 0;
  background: #e7c897;
  clip-path: polygon(38% 0, 62% 0, 100% 100%, 0 100%);
  z-index: 3;
}

.health-float {
  position: absolute;
  right: 18px;
  bottom: 24px;
  z-index: 4;
  min-width: 210px;
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 12px 14px;
  border: 1px solid rgba(255, 255, 255, 0.19);
  border-radius: 16px;
  color: #edfff9;
  background: rgba(16, 67, 58, 0.66);
  box-shadow: 0 14px 30px rgba(7, 42, 36, 0.22);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.health-float-icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.7px;
  color: #215f53;
  background: #f0d096;
}

.health-float div:nth-child(2) {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.health-float span {
  font-size: 10px;
  color: rgba(232, 255, 248, 0.57);
}

.health-float strong {
  margin-top: 3px;
  font-size: 12px;
  font-weight: 600;
}

.health-float-dot {
  width: 7px;
  height: 7px;
  margin-left: auto;
  border-radius: 50%;
  background: #80d8b9;
  box-shadow: 0 0 0 4px rgba(128, 216, 185, 0.13);
}

.feature-list {
  margin-top: auto;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.feature-item {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.11);
}

.feature-icon {
  width: 34px;
  height: 34px;
  flex: 0 0 34px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  font-size: 16px;
  color: #f0d19a;
  background: rgba(255, 255, 255, 0.08);
}

.feature-item div {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.feature-item strong {
  font-size: 13px;
  font-weight: 600;
  color: #f6fffc;
}

.feature-item small {
  margin-top: 3px;
  font-size: 10px;
  color: rgba(232, 255, 248, 0.54);
  white-space: nowrap;
}

.auth-panel {
  position: relative;
  padding: 36px 54px 30px;
  display: flex;
  flex-direction: column;
  background: radial-gradient(circle at 100% 0, rgba(47, 125, 109, 0.055), transparent 25%),
  #fff;
}

.auth-topline {
  min-height: 32px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.security-note {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 11px;
  border: 1px solid #dfeae6;
  border-radius: 999px;
  font-size: 11px;
  color: #617b74;
  background: #f8fbfa;
}

.security-note .el-icon {
  font-size: 14px;
  color: var(--teal);
}

.mobile-brand {
  display: none;
  align-items: center;
  gap: 9px;
}

.mobile-brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 11px;
  color: #fff;
  background: var(--teal);
}

.mobile-brand strong {
  font-size: 18px;
  letter-spacing: 2px;
  color: var(--ink);
}

.auth-header {
  margin-top: 42px;
}

.section-kicker {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2.4px;
  color: var(--teal);
}

.auth-header h2 {
  margin: 12px 0 10px;
  font-family: "STSong", "Songti SC", "Noto Serif SC", serif;
  font-size: 34px;
  line-height: 1.2;
  letter-spacing: 1px;
  color: var(--ink);
}

.auth-header p {
  font-size: 14px;
  line-height: 1.75;
  color: var(--muted);
}

.auth-switch {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin-top: 26px;
  padding: 4px;
  border-radius: 14px;
  background: #f1f5f3;
}

.auth-switch button {
  position: relative;
  z-index: 1;
  height: 42px;
  border: 0;
  border-radius: 11px;
  font: inherit;
  font-size: 14px;
  font-weight: 600;
  color: #70847f;
  background: transparent;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.auth-switch button.active {
  color: var(--ink);
  background: #fff;
  box-shadow: 0 3px 10px rgba(36, 75, 65, 0.08);
}

.auth-switch button:focus-visible,
.login-mode button:focus-visible,
.sms-btn:focus-visible,
.forgot-link:focus-visible {
  outline: 3px solid rgba(47, 125, 109, 0.2);
  outline-offset: 2px;
}

.login-mode {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  margin-bottom: 18px;
  padding: 3px;
  border-radius: 12px;
  background: #f1f5f3;
}

.login-mode button {
  height: 34px;
  border: 0;
  border-radius: 9px;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  color: #70847f;
  background: transparent;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
}

.login-mode button.active {
  color: var(--ink);
  background: #fff;
  box-shadow: 0 3px 10px rgba(36, 75, 65, 0.08);
}

.sms-code-row {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
}

.sms-code-row .el-input {
  flex: 1;
  min-width: 0;
}

.sms-btn {
  flex: 0 0 auto;
  min-width: 112px;
  height: 52px;
  padding: 0 14px;
  border: 1px solid #dfe7e4;
  border-radius: 13px;
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  color: var(--teal);
  background: #f8fbfa;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.sms-btn:hover:not(:disabled) {
  border-color: #bfd1cb;
  background: var(--teal-soft);
}

.sms-btn:disabled {
  color: #a2afab;
  background: #f5f8f7;
  cursor: not-allowed;
}

.form-stage {
  margin-top: 24px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.field-group {
  margin-bottom: 17px;
}

.input-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #314a44;
}

.auth-form :deep(.el-input__wrapper) {
  min-height: 52px;
  padding: 0 15px !important;
  border-radius: 13px !important;
  background: #fbfcfb !important;
  box-shadow: 0 0 0 1px #dfe7e4 inset !important;
  transition: box-shadow 0.2s ease, background 0.2s ease !important;
}

.auth-form :deep(.el-input__wrapper:hover) {
  background: #fff !important;
  box-shadow: 0 0 0 1px #bfd1cb inset !important;
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  background: #fff !important;
  box-shadow: 0 0 0 1.5px var(--teal) inset,
  0 5px 16px rgba(47, 125, 109, 0.08) !important;
}

.auth-form :deep(.el-input__inner) {
  font-size: 15px;
  color: var(--ink);
}

.auth-form :deep(.el-input__inner::placeholder) {
  color: #a2afab;
}

.auth-form :deep(.el-input__prefix-inner) {
  color: #7e928d;
  font-size: 17px;
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 21px;
}

.form-options :deep(.el-checkbox__label) {
  font-size: 12px;
  color: #72857f;
}

.form-options :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  border-color: var(--teal);
  background: var(--teal);
}

.forgot-link {
  padding: 4px 0;
  border: 0;
  font: inherit;
  font-size: 12px;
  font-weight: 600;
  color: var(--teal);
  background: transparent;
  cursor: pointer;
}

.forgot-link:hover {
  color: var(--teal-deep);
}

.submit-btn {
  width: 100%;
  height: 54px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  border: 0 !important;
  border-radius: 14px !important;
  font-size: 15px !important;
  font-weight: 700 !important;
  letter-spacing: 1px;
  color: #fff !important;
  background: linear-gradient(135deg, #347f70 0%, #1f6559 100%) !important;
  box-shadow: 0 12px 26px rgba(31, 101, 89, 0.2) !important;
  transition: transform 0.2s ease, box-shadow 0.2s ease !important;
}

.submit-btn:hover:not(.is-disabled) {
  transform: translateY(-2px);
  box-shadow: 0 16px 32px rgba(31, 101, 89, 0.27) !important;
}

.submit-btn:active:not(.is-disabled) {
  transform: translateY(0);
}

.submit-btn .el-icon {
  font-size: 16px;
}

.role-note {
  display: flex;
  justify-content: center;
  gap: 22px;
  margin-top: 17px;
  font-size: 11px;
  color: #899a96;
}

.role-note span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.role-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.role-dot.member {
  background: var(--teal);
}

.role-dot.admin {
  background: var(--orange);
}

.register-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 12px;
}

.register-form .submit-btn {
  margin-top: 1px;
}

.welcome-gift {
  margin-top: 18px;
  padding: 11px 13px;
  border: 1px solid #efe2ca;
  border-radius: 11px;
  text-align: center;
  font-size: 11px;
  line-height: 1.65;
  color: #7e735f;
  background: #fcf8ef;
}

.welcome-gift strong {
  color: #a36b1e;
}

.gift-icon {
  margin-right: 4px;
  color: var(--orange);
}

.auth-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  margin-top: auto;
  padding-top: 25px;
  font-size: 10px;
  color: #a1adaa;
}

.auth-footer .el-icon {
  font-size: 12px;
  color: #88a59d;
}

.page-footer {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  font-size: 11px;
  color: #83928e;
  letter-spacing: 0.7px;
}

.page-footer i {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #a6b5b1;
}

.dialog-intro {
  margin: -4px 0 20px;
  padding: 11px 13px;
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.7;
  color: #687d77;
  background: #f5f8f7;
}

.reset-dialog :deep(.el-dialog) {
  border-radius: 22px;
  overflow: hidden;
}

.reset-dialog :deep(.el-dialog__header) {
  padding: 21px 24px 16px;
  border-bottom: 1px solid #edf2f0;
}

.reset-dialog :deep(.el-dialog__title) {
  font-size: 18px;
  color: #233d37;
}

.reset-dialog :deep(.el-dialog__body) {
  padding: 22px 24px 6px;
}

.reset-dialog :deep(.el-dialog__footer) {
  padding: 12px 24px 22px;
}

.reset-dialog :deep(.el-form-item__label) {
  margin-bottom: 5px;
  font-size: 12px;
  color: #536b65;
}

.reset-dialog :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 11px !important;
}

.reset-dialog .sms-btn {
  height: 48px;
  border-radius: 11px;
}

:global(.reset-dialog .reset-confirm-btn.el-button--primary) {
  border: 0 !important;
  border-radius: 10px !important;
  font-weight: 600;
  color: #fff !important;
  background: linear-gradient(135deg, #347f70 0%, #1f6559 100%) !important;
  box-shadow: 0 8px 18px rgba(31, 101, 89, 0.22) !important;
}

:global(.reset-dialog .reset-confirm-btn.el-button--primary:hover),
:global(.reset-dialog .reset-confirm-btn.el-button--primary:focus-visible) {
  background: linear-gradient(135deg, #3d8b7b 0%, #276e61 100%) !important;
  box-shadow: 0 10px 22px rgba(31, 101, 89, 0.28) !important;
}

@media (max-width: 980px) {
  .login-page {
    padding: 20px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .brand-panel {
    min-height: 300px;
    padding: 34px 42px 28px;
  }

  .brand-copy {
    margin-top: 32px;
  }

  .brand-copy h1 {
    font-size: 40px;
  }

  .brand-copy p {
    max-width: 620px;
  }

  .care-scene,
  .feature-list {
    display: none;
  }

  .auth-panel {
    min-height: 570px;
    padding: 30px 50px 28px;
  }

  .auth-header {
    margin-top: 28px;
  }
}

@media (max-width: 640px) {
  .login-page {
    justify-content: flex-start;
    padding: 0;
    background: #fff;
  }

  .ambient,
  .dot-field,
  .brand-panel {
    display: none;
  }

  .login-shell {
    width: 100%;
    min-height: 100%;
    border: 0;
    border-radius: 0;
    box-shadow: none;
    background: radial-gradient(circle at 100% 0, rgba(47, 125, 109, 0.08), transparent 24%),
    #fff;
  }

  .auth-panel {
    min-height: 100vh;
    padding: 24px 23px 26px;
  }

  .auth-topline {
    justify-content: space-between;
  }

  .mobile-brand {
    display: flex;
  }

  .security-note {
    padding: 7px 9px;
    font-size: 10px;
  }

  .auth-header {
    margin-top: 46px;
  }

  .auth-header h2 {
    font-size: 30px;
  }

  .auth-switch {
    margin-top: 24px;
  }

  .register-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .sms-btn {
    min-width: 100px;
    padding: 0 10px;
    font-size: 12px;
  }

  .auth-footer {
    margin-top: 42px;
  }

  .page-footer {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-shell {
    animation: none;
  }

  .auth-switch button,
  .login-mode button,
  .sms-btn,
  .submit-btn {
    transition: none !important;
  }
}
</style>
