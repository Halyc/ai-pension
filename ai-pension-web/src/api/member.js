import request from './request'

/**
 * 管理端 - 会员管理 —— 对应 MemberManagementController（/api/admin/members）
 */
export const memberApi = {
  /** GET /api/admin/members/list  分页+条件查询
   *  params: { page, size, phone, name, status }
   *  data: { records:[{id,phone,real_name,member_level,points,status,create_time}],
   *          total, size, current, pages } */
  getMemberList(params) {
    return request.get('/admin/members/list', { params })
  },

  /** GET /api/admin/members/detail/{userId}
   *  data: { member:{id,phone,real_name,gender,birthdate,member_level,points,status},
   *          healthStats:{recentRecordsCount,lastRecordTime},
   *          appointmentStats:{totalCount,completedCount} } */
  getMemberDetail(userId) {
    return request.get(`/admin/members/detail/${userId}`)
  },

  /** POST /api/admin/members/enable/{userId} 启用会员 */
  enableMember(userId) {
    return request.post(`/admin/members/enable/${userId}`)
  },

  /** POST /api/admin/members/disable/{userId} 禁用会员 */
  disableMember(userId) {
    return request.post(`/admin/members/disable/${userId}`)
  },

  /** GET /api/admin/members/levels  data: [{code,name}] */
  getMemberLevels() {
    return request.get('/admin/members/levels')
  },

  /** POST /api/admin/members/{userId}/level  body: { levelCode } */
  updateMemberLevel(userId, levelCode) {
    return request.post(`/admin/members/${userId}/level`, { levelCode })
  },

  /** POST /api/admin/members/{userId}/points  body: { points }（积分变更量，正数增加/负数扣减） */
  updateMemberPoints(userId, points) {
    return request.post(`/admin/members/${userId}/points`, { points })
  },

  /** POST /api/admin/members/{userId}/reset-password 重置会员密码为 123456 */
  resetMemberPassword(userId) {
    return request.post(`/admin/members/${userId}/reset-password`)
  },

  /** POST /api/admin/members/add  新增用户
   *  body: { phone, password, role }  role: ADMIN / MEMBER */
  addMember(data) {
    return request.post('/admin/members/add', data)
  },

  /** DELETE /api/admin/members/delete/{userId} 逻辑删除用户（deleted=1） */
  deleteMember(userId) {
    return request.delete(`/admin/members/delete/${userId}`)
  }
}