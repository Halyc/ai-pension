import request from './request'

/**
 * 会员个人资料 —— 对应 UserController 中 /api/member/profile/**
 */
export const profileApi = {
  /** GET /api/member/profile 获取当前登录会员资料（User 实体，字段下划线命名） */
  getProfile() {
    return request.get('/member/profile')
  },

  /** PUT /api/member/profile 更新资料 body: User（可改 real_name/gender/birth_date/avatar/member_level/phone） */
  updateProfile(data) {
    return request.put('/member/profile', data)
  },

  /** POST /api/member/profile/change-password body: {oldPassword,newPassword} */
  changePassword(data) {
    return request.post('/member/profile/change-password', data)
  }
}