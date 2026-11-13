import request from './request'

/**
 * 管理端 - 活动与报名管理 —— 对应 AdminActivityController（/api/admin/activity）
 */
export const adminActivityApi = {
  // ---------- 活动管理 ----------
  /** GET /api/admin/activity/list
   *  data: [{ id,title,coverUrl,content,registrationStart,registrationEnd,
   *           activityStart,activityEnd,maxParticipants,currentParticipants,
   *           status,createTime,updateTime }] */
  listActivities() {
    return request.get('/admin/activity/list')
  },

  /** POST /api/admin/activity
   *  body: { title, coverUrl, content, registrationStart, registrationEnd,
   *          activityStart, activityEnd, maxParticipants, status }
   *  时间为 ISO 字符串（如 2026-09-01T10:00），后端 LocalDateTime.parse 解析 */
  createActivity(data) {
    return request.post('/admin/activity', data)
  },

  /** PUT /api/admin/activity/{id} */
  updateActivity(id, data) {
    return request.put(`/admin/activity/${id}`, data)
  },

  /** DELETE /api/admin/activity/{id} */
  deleteActivity(id) {
    return request.delete(`/admin/activity/${id}`)
  },

  // ---------- 报名管理 ----------
  /** GET /api/admin/activity/{activityId}/registrations */
  listRegistrations(activityId) {
    if (activityId) {
      return request.get(`/admin/activity/${activityId}/registrations`)
    }
    return request.get('/admin/activity/registrations')
  },

  /** GET /api/admin/activity/registration/{id} */
  getRegistrationDetail(id) {
    return request.get(`/admin/activity/registration/${id}`)
  },

  /** POST /api/admin/activity/registration/check-in/{id} 报名签到 */
  checkIn(id) {
    return request.post(`/admin/activity/registration/check-in/${id}`)
  },

  /** DELETE /api/admin/activity/registration/{id} 取消报名 */
  cancelRegistration(id) {
    return request.delete(`/admin/activity/registration/${id}`)
  }
}