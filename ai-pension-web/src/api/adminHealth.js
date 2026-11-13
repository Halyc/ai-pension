import request from './request'

export const adminHealthApi = {
  listRecords(params) {
    return request.get('/admin/health/records', { params })
  },
  listGuidances(params) {
    return request.get('/admin/health/guidances', { params })
  },
  getRecordDetail(id) {
    return request.get(`/admin/health/record/${id}`)
  },
  deleteRecord(id) {
    return request.delete(`/admin/health/record/${id}`)
  },
  deleteGuidance(id) {
    return request.delete(`/admin/health/guidance/${id}`)
  }
}