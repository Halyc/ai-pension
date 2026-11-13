import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

/**
 * 路由表：
 *  - /login                 登录/注册
 *  - /admin/**              管理后台（ADMIN 令牌，对应 /api/admin/** 与认证接口）
 *  - /member/**             会员服务门户（MEMBER 令牌，对应 /api/member/**）
 * 侧边菜单根据此处路由 meta 自动生成（见 Layout）。
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { requiresAdmin: true },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: { title: '数据看板', icon: 'Odometer' }
      },
      {
        path: 'members',
        name: 'AdminMembers',
        component: () => import('@/views/admin/Members.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'packages',
        name: 'AdminPackages',
        component: () => import('@/views/admin/Packages.vue'),
        meta: { title: '套餐与时段', icon: 'Calendar' }
      },
      {
        path: 'appointments',
        name: 'AdminAppointments',
        component: () => import('@/views/admin/Appointments.vue'),
        meta: { title: '预约记录', icon: 'Tickets' }
      },
      {
        path: 'activities',
        name: 'AdminActivities',
        component: () => import('@/views/admin/Activities.vue'),
        meta: { title: '活动与报名', icon: 'Flag' }
      },
      {
        path: 'assessments',
        name: 'AdminAssessments',
        component: () => import('@/views/admin/Assessments.vue'),
        meta: { title: '问卷评测', icon: 'Document' }
      },
      {
        path: 'health',
        name: 'AdminHealthRecords',
        component: () => import('@/views/admin/HealthRecords.vue'),
        meta: { title: '健康档案', icon: 'FirstAidKit' }
      },
      {
        path: 'oper-logs',
        name: 'AdminOperationLogs',
        component: () => import('@/views/admin/OperationLogs.vue'),
        meta: { title: '操作日志', icon: 'Document' }
      },
      {
        path: 'config',
        name: 'AdminConfig',
        component: () => import('@/views/admin/Config.vue'),
        meta: { title: '系统配置', icon: 'Setting' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/admin/Profile.vue'),
        meta: { title: '个人中心', icon: 'UserFilled' }
      }
    ]
  },
  {
    path: '/member',
    component: () => import('@/layout/MemberLayout.vue'),
    redirect: '/member/home',
    meta: { requiresMember: true },
    children: [
      {
        path: 'home',
        name: 'MemberHome',
        component: () => import('@/views/member/Home.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'appointments',
        name: 'MemberAppointments',
        component: () => import('@/views/member/Appointments.vue'),
        meta: { title: '预约服务', icon: 'Calendar' }
      },
      {
        path: 'activities',
        name: 'MemberActivities',
        component: () => import('@/views/member/Activities.vue'),
        meta: { title: '社区活动', icon: 'Flag' }
      },
      {
        path: 'health',
        name: 'MemberHealth',
        component: () => import('@/views/member/Health.vue'),
        meta: { title: '健康档案', icon: 'FirstAidKit' }
      },
      {
        path: 'assessment',
        name: 'MemberAssessment',
        component: () => import('@/views/member/Assessment.vue'),
        meta: { title: '健康评测', icon: 'EditPen' }
      },
      {
        path: 'chat',
        name: 'MemberChat',
        component: () => import('@/views/member/Chat.vue'),
        meta: { title: 'AI 健康助手', icon: 'ChatDotRound' }
      },
      {
        path: 'profile',
        name: 'MemberProfile',
        component: () => import('@/views/member/Profile.vue'),
        meta: { title: '个人中心', icon: 'UserFilled' }
      }
    ]
  },
  { path: '/', redirect: '/login' },
  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.public) {
    if (userStore.isLogin && to.path === '/login') {
      return next(userStore.isAdmin ? '/admin/dashboard' : '/member/home')
    }
    return next()
  }

  if (!userStore.isLogin) {
    return next(`/login?redirect=${encodeURIComponent(to.fullPath)}`)
  }

  if (to.matched.some((r) => r.meta.requiresAdmin) && !userStore.isAdmin) {
    return next('/member/home')
  }
  if (to.matched.some((r) => r.meta.requiresMember) && !userStore.isMember) {
    return next('/admin/dashboard')
  }

  next()
})

export default router
