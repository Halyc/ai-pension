import request from './request'

/**
 * 管理端 - 系统配置 —— 对应 ConfigController（/api/admin/config）
 */
export const configApi = {
  /** GET /api/admin/config/list
   *  data: [{ id, configKey, configValue, description }] */
  listConfigs() {
    return request.get('/admin/config/list')
  },

  /** GET /api/admin/config/{key} */
  getConfigByKey(key) {
    return request.get(`/admin/config/${encodeURIComponent(key)}`)
  },

  /** PUT /api/admin/config/{key}  body:{ value, description? }（不存在则新建） */
  updateConfig(key, data) {
    return request.put(`/admin/config/${encodeURIComponent(key)}`, data)
  }
}
