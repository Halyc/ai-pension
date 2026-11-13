import request from './request'

/**
 * 会员端 - 社区活动 —— 对应 ActivityController（/api/member/activity）
 */
export const memberActivityApi = {
  /** GET /api/member/activity/list  params:{ status }
   *  data: [{ id,title,coverUrl,content,registrationStart,registrationEnd,
   *           activityStart,activityEnd,maxParticipants,currentParticipants,status }] */
  listActivities(status) {
    return request.get('/member/activity/list', {
      params: status ? { status } : {}
    })
  },

  /** GET /api/member/activity/detail/{activityId} */
  getActivityDetail(activityId) {
    return request.get(`/member/activity/detail/${activityId}`)
  },

  /** POST /api/member/activity/register/{activityId} 报名活动 */
  registerActivity(activityId) {
    return request.post(`/member/activity/register/${activityId}`)
  },

  /** GET /api/member/activity/my 我的报名
   *  data: [{ registrationId, activityId, checkInStatus, createTime,
   *           activity:{ id,title,coverUrl,activityStart,activityEnd,status } }] */
  listMyActivities() {
    return request.get('/member/activity/my')
  },

  /** GET /api/member/activity/is-registered/{activityId}
   *  data: { registered, registrationId?, checkInStatus? } */
  checkRegistered(activityId) {
    return request.get(`/member/activity/is-registered/${activityId}`)
  },

  /** GET /api/member/activity/check-in-status/{activityId}
   *  data: { registrationId, checkInStatus, activityId } */
  getCheckInStatus(activityId) {
    return request.get(`/member/activity/check-in-status/${activityId}`)
  },

  /** POST /api/member/activity/check-in/{activityId} 活动签到 */
  checkIn(activityId) {
    return request.post(`/member/activity/check-in/${activityId}`)
  },

  /** DELETE /api/member/activity/cancel/{activityId} 取消报名 */
  cancelRegistration(activityId) {
    return request.delete(`/member/activity/cancel/${activityId}`)
  }
}