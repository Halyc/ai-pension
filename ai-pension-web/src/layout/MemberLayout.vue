<template>
  <el-container class="layout-wrapper" :class="{ 'is-mobile': isMobile, 'is-home': isHome }">
    <el-aside
      v-if="!isMobile && !isHome"
      width="232px"
      class="sidebar"
    >
      <div class="sidebar-inner">
        <div class="logo">
          <div class="logo-icon-wrap">
            <svg class="logo-svg" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
              <path d="M10 23.5 24 12l14 11.5" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M14 21.5V36a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V21.5" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M24 24.5V33M20 28.5h8" stroke="#f2d59d" stroke-width="3" stroke-linecap="round"/>
            </svg>
          </div>
          <div class="logo-text-wrap">
            <span class="logo-text">颐康云</span>
            <span class="logo-sub">ELDERLY CARE</span>
          </div>
        </div>

        <el-menu
          :default-active="activeMenu"
          background-color="transparent"
          text-color="#5a607a"
          active-text-color="#ffffff"
          router
        >
          <el-menu-item
            v-for="item in menuItems"
            :key="item.path"
            :index="item.path"
            class="menu-item"
          >
            <el-icon class="menu-icon"><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-menu>

        <div class="sidebar-footer">
          <div class="footer-divider"></div>
          <div class="sidebar-tip">
            <span class="tip-dot"></span>
            <span>服务热线 · 400-888-8888</span>
          </div>
        </div>
      </div>
    </el-aside>

    <el-container class="layout-main">
      <el-header class="header" :class="{ 'header-slim': isHome && !isMobile }">
        <template v-if="!isMobile">
          <div class="header-left">
            <div v-if="isHome" class="home-header-brand">
              <div class="home-logo-icon-wrap">
                <svg class="home-logo-svg" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                  <path d="M10 23.5 24 12l14 11.5" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M14 21.5V36a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V21.5" stroke="#fff" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M24 24.5V33M20 28.5h8" stroke="#f2d59d" stroke-width="3" stroke-linecap="round"/>
                </svg>
              </div>
              <span class="home-logo-text">颐康云</span>
            </div>
            <el-breadcrumb v-else separator="/">
              <el-breadcrumb-item :to="{ path: '/member/home' }">服务门户</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
        </template>
        <template v-else>
          <div class="header-left mobile-header-left">
            <span class="mobile-app-name">颐康云</span>
            <span class="mobile-page-title">{{ currentTitle }}</span>
          </div>
        </template>

        <div class="header-right">
          <el-dropdown @command="onCommand" popper-class="user-menu-popper">
            <span class="user-info">
              <el-avatar :size="36" class="user-avatar" :src="headerAvatar">
                {{ avatarText }}
              </el-avatar>
              <span v-if="!isMobile" class="user-details">
                <span class="user-name">{{ userStore.displayName }}</span>
                <span class="user-level">{{ userStore.memberLevel }}</span>
              </span>
              <el-icon v-if="!isMobile" class="arrow-icon"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="home" v-if="!isHome">
                  <el-icon><HomeFilled /></el-icon>返回首页
                </el-dropdown-item>
                <el-dropdown-item command="profile">
                  <el-icon><UserFilled /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main-content" :class="{ 'mobile-main': isMobile, 'home-main': isHome && !isMobile }">
        <component :is="currentView" :key="route.fullPath + '-' + globalRefreshTick" />
      </el-main>

      <nav v-if="isMobile" class="mobile-tabbar">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="tab-item"
          :class="{ active: activeMenu === item.path }"
        >
          <el-icon class="tab-icon"><component :is="item.icon" /></el-icon>
          <span class="tab-label">{{ item.title }}</span>
        </router-link>
      </nav>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, watch, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { HomeFilled, UserFilled, SwitchButton, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api/auth'
import { useResponsive } from '@/utils/useResponsive'
import { buildAvatar } from '@/utils/format'
import { getImage, userKey } from '@/utils/imageCache'
import MemberHome from '@/views/member/Home.vue'
import MemberAppointments from '@/views/member/Appointments.vue'
import MemberActivities from '@/views/member/Activities.vue'
import MemberHealth from '@/views/member/Health.vue'
import MemberAssessment from '@/views/member/Assessment.vue'
import MemberChat from '@/views/member/Chat.vue'
import MemberProfile from '@/views/member/Profile.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { isMobile } = useResponsive()

const globalRefreshTick = ref(0)

watch(
  () => route.path,
  () => {
    cleanupTeleports()
    avatarRefreshTick.value++
  },
  { immediate: false }
)

window.addEventListener('storage', (e) => {
  if (e.key && (e.key.startsWith('img:') || e.key.startsWith('imgcache:'))) {
    globalRefreshTick.value++
    avatarRefreshTick.value++
  }
})

window.addEventListener('user-avatar-updated', () => {
  globalRefreshTick.value++
  avatarRefreshTick.value++
})

window.addEventListener('image-cache-ready', () => {
  globalRefreshTick.value++
  avatarRefreshTick.value++
  const u = userStore.user
  if (u?.id && !u.avatar) {
    const cached = getImage(userKey(u.id))
    if (cached) {
      userStore.setUser({ ...u, avatar: cached })
    }
  }
})

function cleanupTeleports() {
  const selectors = ['.el-overlay', '.el-image-viewer', '.el-image-viewer__mask', '.el-message-box', '.el-loading-mask', '.el-drawer']
  selectors.forEach((sel) => { document.querySelectorAll(sel).forEach((el) => { el.remove() }) })
  document.body.classList.remove('el-popup-parent--hidden')
}

onMounted(() => { cleanupTeleports() })

const menuItems = computed(() => {
  const memberRoute = router.options.routes.find((r) => r.path === '/member')
  return (memberRoute?.children || []).filter((c) => c.path !== 'home').map((c) => ({
    path: `/member/${c.path}`,
    title: c.meta?.title,
    icon: c.meta?.icon || 'Menu'
  }))
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')
const isHome = computed(() => route.path === '/member/home')
const avatarRefreshTick = ref(0)

const avatarText = computed(() => (userStore.displayName || 'U').charAt(0).toUpperCase())
const headerAvatar = computed(() => {
  avatarRefreshTick.value
  const u = userStore.user
  if (u?.id) {
    const cached = getImage(userKey(u.id))
    if (cached) return cached
  }
  return buildAvatar(u)
})

const currentView = computed(() => {
  const p = route.path
  if (p.startsWith('/member/home')) return MemberHome
  if (p.startsWith('/member/appointments')) return MemberAppointments
  if (p.startsWith('/member/activities')) return MemberActivities
  if (p.startsWith('/member/health')) return MemberHealth
  if (p.startsWith('/member/assessment')) return MemberAssessment
  if (p.startsWith('/member/chat')) return MemberChat
  if (p.startsWith('/member/profile')) return MemberProfile
  return MemberHome
})

async function onCommand(cmd) {
  if (cmd === 'home') { router.push('/member/home'); return }
  if (cmd === 'profile') { router.push('/member/profile'); return }
  if (cmd === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '退出', cancelButtonText: '取消', type: 'warning'
      })
      await authApi.logout(userStore.refreshToken)
      ElMessage.success('已退出登录')
    } catch (e) {} finally {
      userStore.logout()
      router.replace('/login')
    }
  }
}
</script>

<style scoped>
.layout-wrapper {
  --primary: #2f7d6d;
  --primary-color: #2f7d6d;
  --primary-2: #d9963e;
  --primary-3: #4b8c7d;
  --accent-gold: #d9963e;
  --accent-gradient: linear-gradient(135deg, #d9963e 0%, #c47d26 100%);
  --primary-gradient: linear-gradient(135deg, #347f70 0%, #1f6559 100%);
  --mint-gradient: linear-gradient(135deg, #6fb39d 0%, #2f7d6d 100%);
  --warm-gradient: linear-gradient(135deg, #fbf1df 0%, #ecd09d 100%);
  --bg-gradient: linear-gradient(135deg, #f7f4ed 0%, #eef5f1 52%, #f8f5ee 100%);
  --bg-deep: #f4f7f3;
  --card-bg: rgba(255, 255, 255, 0.82);
  --card-border: rgba(222, 233, 228, 0.9);
  --card-shadow: 0 8px 28px rgba(38, 76, 65, 0.07), 0 2px 8px rgba(38, 76, 65, 0.035);
  --card-shadow-hover: 0 15px 36px rgba(38, 76, 65, 0.11);
  --text-main: #19352f;
  --text-sub: #5f736d;
  --text-muted: #8b9b96;
  --text-light: #8b9b96;
  --border-light: rgba(40, 74, 65, 0.09);
  --nav-bg: rgba(255, 255, 255, 0.72);
  --nav-item-hover: rgba(47, 125, 109, 0.07);
  --nav-item-active: rgba(47, 125, 109, 0.12);
  --success: #4f9b7b;
  --success-color: #4f9b7b;
  --warning: #d9963e;
  --danger: #d86464;
  --el-color-primary: #2f7d6d;
  --el-color-primary-light-3: #6aa596;
  --el-color-primary-light-5: #96c0b4;
  --el-color-primary-light-7: #c2dbd4;
  --el-color-primary-light-8: #d7e8e3;
  --el-color-primary-light-9: #edf5f2;
  --el-color-primary-dark-2: #1f6559;
  height: 100%;
  background:
    radial-gradient(circle at 96% 3%, rgba(217, 150, 62, 0.07), transparent 24%),
    linear-gradient(135deg, #f8f5ee 0%, #eff6f2 52%, #f9f6ef 100%);
}

.sidebar {
  overflow: hidden;
  position: relative;
  border-right: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 4px 0 24px rgba(74, 144, 226, 0.06);
}

.sidebar-inner {
  height: 100%;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  display: flex;
  flex-direction: column;
  padding: 20px 12px 16px;
  position: relative;
  z-index: 1;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  margin-bottom: 16px;
}

.logo-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(145deg, #347f70 0%, #1f6559 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 18px rgba(31, 101, 89, 0.24);
  flex-shrink: 0;
}

.logo-svg {
  width: 28px;
  height: 28px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.15));
}

.logo-text-wrap { display: flex; flex-direction: column; gap: 3px; }

.logo-text {
  font-weight: 700;
  font-size: 17px;
  letter-spacing: 1px;
  color: var(--text-main);
}

.logo-sub {
  color: var(--text-muted);
  font-size: 10px;
  letter-spacing: 2px;
  font-weight: 500;
}

.menu-item {
  margin: 4px 0;
  border-radius: 12px;
  height: 46px;
  padding: 0 14px !important;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.menu-item:hover {
  background: rgba(47, 125, 109, 0.07) !important;
  border-radius: 12px;
}

.menu-icon {
  font-size: 19px;
  margin-right: 10px;
  color: #5a607a;
  transition: color 0.3s;
}

:deep(.el-menu-item.is-active) {
  background: var(--primary-gradient) !important;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(31, 101, 89, 0.24);
  border: none;
}

:deep(.el-menu-item.is-active) .menu-icon { color: #fff; }
:deep(.el-menu-item.is-active) { color: #fff !important; }

.sidebar-footer { margin-top: auto; padding: 0 8px; }

.footer-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 0, 0, 0.1), transparent);
  margin-bottom: 12px;
}

.sidebar-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  font-size: 12px;
  padding: 6px 0;
}

.tip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--success-color);
  box-shadow: 0 0 10px rgba(80, 200, 120, 0.5);
}

.layout-main { display: flex; flex-direction: column; height: 100%; }

.header {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px !important;
}

.header.header-slim {
  background: transparent;
  backdrop-filter: none;
  border-bottom: none;
}

.home-header-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.home-logo-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(145deg, #347f70 0%, #1f6559 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(31, 101, 89, 0.22);
}

.home-logo-svg {
  width: 20px;
  height: 20px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.15));
}

.home-logo-text {
  font-size: 18px;
  font-weight: 700;
  color: #286e61;
  letter-spacing: 2px;
}

.header-left { display: flex; align-items: center; gap: 16px; }

.mobile-header-left { gap: 10px; flex: 1; min-width: 0; }

.mobile-app-name {
  font-size: 17px;
  font-weight: 700;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

.mobile-page-title {
  font-size: 14px;
  color: var(--text-sub);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-right { display: flex; align-items: center; }

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  outline: none;
  padding: 6px 16px 6px 6px;
  border-radius: 999px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.user-info:hover {
  background: rgba(74, 144, 226, 0.08);
  border-color: rgba(74, 144, 226, 0.25);
}

.user-avatar {
  background: var(--primary-gradient) !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(74, 144, 226, 0.3);
}

.user-details { display: flex; flex-direction: column; line-height: 1.3; }

.user-name {
  font-size: 14px;
  color: var(--text-main);
  font-weight: 600;
}

.user-level {
  font-size: 11px;
  background: var(--accent-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 600;
}

.arrow-icon { font-size: 12px; color: var(--text-muted); }

.main-content {
  padding: 0;
  overflow: auto;
  flex: 1;
  position: relative;
  min-height: 0;
}

.main-content.home-main {
  background: transparent;
  overflow-y: auto;
}

.main-content.mobile-main { padding-bottom: 72px; }

.mobile-tabbar {
  display: flex;
  align-items: stretch;
  height: 64px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  padding-bottom: env(safe-area-inset-bottom);
  z-index: 10;
  position: relative;
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  color: var(--text-muted);
  text-decoration: none;
  font-size: 11px;
  transition: all 0.3s ease;
  min-width: 0;
  position: relative;
}

.tab-item.active { color: var(--primary-color); }

.tab-item.active::before {
  content: '';
  position: absolute;
  top: 6px;
  width: 22px;
  height: 3px;
  border-radius: 3px;
  background: var(--primary-gradient);
  box-shadow: 0 0 8px rgba(74, 144, 226, 0.4);
}

.tab-icon { font-size: 22px; }
.tab-label { font-size: 11px; line-height: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.fade-enter-from { opacity: 0; transform: translateY(10px); }
.fade-leave-to { opacity: 0; transform: translateY(-5px); }
</style>

<style>
.user-menu-popper {
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  border-radius: 14px !important;
  padding: 6px !important;
  box-shadow: 0 16px 48px rgba(74, 144, 226, 0.15) !important;
}

.user-menu-popper .el-dropdown-menu__item {
  color: var(--text-sub) !important;
  border-radius: 10px;
  padding: 8px 14px !important;
  font-size: 14px;
  transition: all 0.2s ease;
}

.user-menu-popper .el-dropdown-menu__item:hover {
  background: rgba(74, 144, 226, 0.08) !important;
  color: var(--primary-color) !important;
}

.user-menu-popper .el-dropdown-menu__item.is-divided {
  margin-top: 4px !important;
  border-top: 1px solid rgba(0, 0, 0, 0.06) !important;
  padding-top: 12px !important;
}

.user-menu-popper .el-dropdown-menu__item .el-icon {
  margin-right: 8px;
  font-size: 16px;
  color: var(--text-muted);
}

.user-menu-popper .el-dropdown-menu__item:hover .el-icon { color: var(--primary-color); }
</style>
