import { defineStore } from 'pinia'

const TOKEN_KEY = 'pension_tokens'
const USER_KEY = 'pension_user'
const REMEMBER_KEY = 'pension_remember'

function getStorage(remember) {
  return remember ? localStorage : sessionStorage
}

function detectRemember() {
  return localStorage.getItem(REMEMBER_KEY) === 'true'
}

function loadTokens() {
  const remember = detectRemember()
  try {
    const raw = getStorage(remember).getItem(TOKEN_KEY)
    return JSON.parse(raw) || { accessToken: '', refreshToken: '' }
  } catch (e) {
    return { accessToken: '', refreshToken: '' }
  }
}

function loadUser() {
  const remember = detectRemember()
  try {
    const raw = getStorage(remember).getItem(USER_KEY)
    return JSON.parse(raw) || null
  } catch (e) {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    accessToken: loadTokens().accessToken || '',
    refreshToken: loadTokens().refreshToken || '',
    user: loadUser()
  }),

  getters: {
    isLogin: (state) => !!state.accessToken,
    isAdmin: (state) => state.user?.role === 'ADMIN',
    isMember: (state) => state.user?.role === 'MEMBER',
    displayName: (state) => state.user?.real_name || state.user?.phone || '用户',
    memberLevel: (state) => {
      const level = state.user?.member_level
      if (!level) return '普通会员'
      const map = {
        NORMAL: '普通会员',
        SILVER: '银卡会员',
        GOLD: '金卡会员',
        PLATINUM: '铂金会员',
        DIAMOND: '钻石会员'
      }
      return map[level] || level
    }
  },

  actions: {
    setLogin(data, remember = false) {
      this.accessToken = data.tokenPair.accessToken
      this.refreshToken = data.tokenPair.refreshToken
      this.user = data.user
      localStorage.setItem(REMEMBER_KEY, String(remember))
      const storage = getStorage(remember)
      storage.setItem(
        TOKEN_KEY,
        JSON.stringify({ accessToken: this.accessToken, refreshToken: this.refreshToken })
      )
      storage.setItem(USER_KEY, JSON.stringify(this.user))
      if (remember) {
        sessionStorage.removeItem(TOKEN_KEY)
        sessionStorage.removeItem(USER_KEY)
      } else {
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(USER_KEY)
      }
    },

    setTokens(tokenPair) {
      const remember = detectRemember()
      this.accessToken = tokenPair.accessToken
      this.refreshToken = tokenPair.refreshToken
      const storage = getStorage(remember)
      storage.setItem(
        TOKEN_KEY,
        JSON.stringify({ accessToken: this.accessToken, refreshToken: this.refreshToken })
      )
    },

    setUser(user) {
      const remember = detectRemember()
      this.user = user
      getStorage(remember).setItem(USER_KEY, JSON.stringify(this.user))
    },

    logout() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(REMEMBER_KEY)
      sessionStorage.removeItem(TOKEN_KEY)
      sessionStorage.removeItem(USER_KEY)
    }
  }
})