import request from './request'

export const healthApi = {
  record(data) {
    return request.post('/member/health/record', data)
  },

  history(months = 12) {
    return request.post('/member/health/history', null, { params: { months } })
  },

  trend(months = 12) {
    return request.get('/member/health/trend', { params: { months } })
  },

  delete(id) {
    return request.delete(`/member/health/delete/${id}`)
  },

  guidances() {
    return request.get('/member/health/guidances')
  },

  markGuidanceRead(id) {
    return request.post(`/member/health/guidance/read/${id}`)
  }
}