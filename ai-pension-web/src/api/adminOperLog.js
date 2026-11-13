import request from './request'

/**
 * 管理端 - 操作日志
 * 对应 OperLogController（/api/admin/oper-logs）
 */
export const adminOperLogApi = {
  list(params) {
    return request.get('/admin/oper-logs', { params })
  },

  detail(id) {
    return request.get(`/admin/oper-logs/${id}`)
  }
}
