import request from './request'

export const adminProfileApi = {
  getProfile() {
    return request.get('/member/profile')
  },
  updateProfile(data) {
    return request.put('/member/profile', data)
  },
  changePassword(data) {
    return request.post('/member/profile/change-password', data)
  }
}