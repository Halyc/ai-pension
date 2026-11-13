import request from './request'

/**
 * 数据看板 —— 对应 DashboardController（/api/admin/dashboard）
 */
export const dashboardApi = {
  /** GET /api/admin/dashboard/summary
   *  data: { totalMembers, todayNewMembers, totalAppointments, todayAppointments,
   *          totalActivities, ongoingActivities, aiChatCount } */
  getSummary() {
    return request.get('/admin/dashboard/summary')
  }
}
