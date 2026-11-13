<template>
  <el-container class="layout-wrapper">
    <el-aside :width="isCollapse ? '72px' : '240px'" class="sidebar">
      <div class="logo">
        <div class="logo-icon-wrap">
          <svg class="logo-svg" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M14 38C11.7909 38 10 36.2091 10 34C10 31.7909 11.7909 30 14 30C14.185 30 14.3664 30.0125 14.5424 30.0362C15.1283 26.689 18.189 24 22 24C23.252 24 24.4237 24.3394 25.4291 24.9107C26.3367 23.7772 27.8355 23 29.5 23C32.8137 23 35.5 25.6863 35.5 29C35.5 29.1203 35.5036 29.2394 35.5104 29.357C37.5784 29.6288 39.2 31.4384 39.2 33.6C39.2 36.0257 37.2257 38 34.8 38H14Z" fill="url(#admin-cloud-grad)"/>
            <circle cx="18" cy="34" r="1.5" fill="#fff"/>
            <circle cx="24" cy="34" r="1.5" fill="#fff"/>
            <circle cx="30" cy="34" r="1.5" fill="#fff"/>
            <defs>
              <linearGradient id="admin-cloud-grad" x1="10" y1="23" x2="39.2" y2="38" gradientUnits="userSpaceOnUse">
                <stop stop-color="#ffffff"/>
                <stop offset="1" stop-color="#e8f0fe"/>
              </linearGradient>
            </defs>
          </svg>
        </div>
        <transition name="fade">
          <span v-show="!isCollapse" class="logo-text">颐康云</span>
        </transition>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
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
      <div v-show="!isCollapse" class="sidebar-footer">
        <div class="footer-divider"></div>
        <div class="footer-text">AI · Health · Care</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <div class="collapse-btn" @click="isCollapse = !isCollapse">
            <el-icon :size="20">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
          </div>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="onCommand" popper-class="admin-menu-popper">
            <span class="user-info">
              <el-avatar :size="36" class="user-avatar">
                {{ avatarText }}
              </el-avatar>
              <span class="user-details">
                <span class="user-name">{{ userStore.displayName }}</span>
                <span class="user-role">系统管理员</span>
              </span>
              <el-tag size="small" type="danger" effect="dark" round class="admin-badge">ADMIN</el-tag>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
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

      <el-main class="main-content">
        <component :is="currentView" :key="route.fullPath + '-' + globalRefreshTick" />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { authApi } from '@/api/auth'
import AdminDashboard from '@/views/admin/Dashboard.vue'
import AdminMembers from '@/views/admin/Members.vue'
import AdminPackages from '@/views/admin/Packages.vue'
import AdminAppointments from '@/views/admin/Appointments.vue'
import AdminActivities from '@/views/admin/Activities.vue'
import AdminAssessments from '@/views/admin/Assessments.vue'
import AdminHealthRecords from '@/views/admin/HealthRecords.vue'
import AdminOperationLogs from '@/views/admin/OperationLogs.vue'
import AdminConfig from '@/views/admin/Config.vue'
import AdminProfile from '@/views/admin/Profile.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const globalRefreshTick = ref(0)

window.addEventListener('storage', (e) => {
  if (e.key && (e.key.startsWith('img:') || e.key.startsWith('imgcache:'))) {
    globalRefreshTick.value++
  }
})

window.addEventListener('image-cache-ready', () => {
  globalRefreshTick.value++
})

const menuItems = computed(() => {
  const adminRoute = router.options.routes.find((r) => r.path === '/admin')
  return (adminRoute?.children || []).map((c) => ({
    path: `/admin/${c.path}`,
    title: c.meta?.title,
    icon: c.meta?.icon || 'Menu'
  }))
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')
const avatarText = computed(() => (userStore.displayName || 'A').charAt(0).toUpperCase())

const currentView = computed(() => {
  const p = route.path
  if (p.startsWith('/admin/dashboard')) return AdminDashboard
  if (p.startsWith('/admin/members')) return AdminMembers
  if (p.startsWith('/admin/packages')) return AdminPackages
  if (p.startsWith('/admin/appointments')) return AdminAppointments
  if (p.startsWith('/admin/activities')) return AdminActivities
  if (p.startsWith('/admin/assessments')) return AdminAssessments
  if (p.startsWith('/admin/health')) return AdminHealthRecords
  if (p.startsWith('/admin/oper-logs')) return AdminOperationLogs
  if (p.startsWith('/admin/config')) return AdminConfig
  if (p.startsWith('/admin/profile')) return AdminProfile
  return AdminDashboard
})

async function onCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/admin/profile')
    return
  }
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
.layout-wrapper { height: 100%; }

.sidebar {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
  border-right: 1px solid rgba(0, 0, 0, 0.04);
  box-shadow: 4px 0 24px rgba(74, 144, 226, 0.06);
}

.logo {
  height: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--text-main);
  font-weight: 700;
  font-size: 18px;
  white-space: nowrap;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  position: relative;
  z-index: 1;
  padding: 0 16px;
}

.logo-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 50%, #f093fb 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.45);
  flex-shrink: 0;
}

.logo-svg {
  width: 26px;
  height: 26px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.15));
}

.logo-text {
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 2px;
}

.menu-item {
  margin: 4px 10px;
  border-radius: 12px;
  height: 46px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.menu-item:hover { background: rgba(74, 144, 226, 0.08) !important; }

.menu-icon {
  font-size: 19px;
  margin-right: 10px;
  color: #5a607a;
  transition: color 0.3s;
}

:deep(.el-menu--collapse) .menu-item {
  margin: 4px auto;
  padding: 0 !important;
  width: 46px;
  display: flex;
  align-items: center;
  justify-content: center;
}
:deep(.el-menu--collapse) .menu-icon {
  margin-right: 0;
}

:deep(.el-menu-item.is-active) {
  background: var(--primary-gradient) !important;
  border-radius: 12px;
  box-shadow: 0 6px 20px rgba(74, 144, 226, 0.35);
  border: none;
}

:deep(.el-menu-item.is-active) .menu-icon { color: #fff; }
:deep(.el-menu-item.is-active) { color: #fff !important; }

.sidebar-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
}

.footer-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 0, 0, 0.1), transparent);
  margin-bottom: 12px;
}

.footer-text {
  text-align: center;
  color: var(--text-muted);
  font-size: 11px;
  letter-spacing: 3px;
  font-weight: 500;
}

.header {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px !important;
}

.header-left { display: flex; align-items: center; gap: 20px; }

.collapse-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  cursor: pointer;
  color: var(--text-sub);
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.collapse-btn:hover {
  color: var(--primary-color);
  background: rgba(74, 144, 226, 0.08);
  border-color: rgba(74, 144, 226, 0.25);
  transform: scale(1.05);
}

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

.user-role {
  font-size: 11px;
  color: var(--text-muted);
}

.admin-badge {
  background: linear-gradient(135deg, #ff5c72, #ff8a5c) !important;
  border: none !important;
  color: #fff !important;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.arrow-icon { font-size: 12px; color: var(--text-muted); }

.main-content {
  padding: 0;
  overflow: auto;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.fade-enter-from { opacity: 0; transform: translateY(10px); }
.fade-leave-to { opacity: 0; transform: translateY(-5px); }
</style>

<style>
.page-subtitle {
  display: none !important;
}

.admin-menu-popper {
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  border-radius: 14px !important;
  padding: 6px !important;
  box-shadow: 0 16px 48px rgba(74, 144, 226, 0.15) !important;
}

.admin-menu-popper .el-dropdown-menu__item {
  color: var(--text-sub) !important;
  border-radius: 10px;
  padding: 8px 14px !important;
  font-size: 14px;
  transition: all 0.2s ease;
}

.admin-menu-popper .el-dropdown-menu__item:hover {
  background: rgba(74, 144, 226, 0.08) !important;
  color: var(--primary-color) !important;
}

.admin-menu-popper .el-dropdown-menu__item.is-divided {
  margin-top: 4px !important;
  border-top: 1px solid rgba(0, 0, 0, 0.06) !important;
  padding-top: 12px !important;
}

.admin-menu-popper .el-dropdown-menu__item .el-icon {
  margin-right: 8px;
  font-size: 16px;
  color: var(--text-muted);
}

.admin-menu-popper .el-dropdown-menu__item:hover .el-icon { color: var(--primary-color); }
</style>
