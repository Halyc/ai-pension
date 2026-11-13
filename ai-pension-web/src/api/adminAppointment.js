import request from './request'

/**
 * 管理端 - 预约管理（套餐 / 时段 / 预约记录）
 * 对应 AppointmentManageController（/api/admin/appointment）
 */
export const adminAppointmentApi = {
  // ---------- 体检套餐 ----------
  /** GET /api/admin/appointment/packages  params:{page,size,keyword}
   *  data: { total, page, size, list:[AppointmentPackage 实体，字段下划线] } */
  listPackages(params) {
    return request.get('/admin/appointment/packages', { params })
  },

  /** POST /api/admin/appointment/package
   *  body: { name, coverUrl, description, price, suitablePeople, items, status }
   *  （后端按驼峰 key 读取；items 可为数组） */
  createPackage(data) {
    return request.post('/admin/appointment/package', data)
  },

  /** PUT /api/admin/appointment/package/{id} */
  updatePackage(id, data) {
    return request.put(`/admin/appointment/package/${id}`, data)
  },

  /** DELETE /api/admin/appointment/package/{id} */
  deletePackage(id) {
    return request.delete(`/admin/appointment/package/${id}`)
  },

  // ---------- 预约时段 ----------
  /** GET /api/admin/appointment/slots/{packageId}
   *  data: [{ id, packageId, appointDate, timeRange, maxCount, currentCount, status, createTime }] */
  listSlots(packageId) {
    return request.get(`/admin/appointment/slots/${packageId}`)
  },

  /** POST /api/admin/appointment/slots/batch
   *  body: { packageId, startDate:'yyyy-MM-dd', endDate:'yyyy-MM-dd', timeRanges:[], maxCount } */
  batchCreateSlots(data) {
    return request.post('/admin/appointment/slots/batch', data)
  },

  /** DELETE /api/admin/appointment/slot/{id} */
  deleteSlot(id) {
    return request.delete(`/admin/appointment/slot/${id}`)
  },

  /** DELETE /api/admin/appointment/slots/batch  body: { ids:[] } */
  batchDeleteSlots(ids) {
    return request.delete('/admin/appointment/slots/batch', { data: { ids } })
  },

  // ---------- 预约记录 ----------
  /** GET /api/admin/appointment/list  params:{page,size,status,date(yyyy-MM-dd)}
   *  data: { total, page, size, list:[{id,userId,slotId,packageId,status,reportUrl,createTime,updateTime}] } */
  listAppointments(params) {
    return request.get('/admin/appointment/list', { params })
  },

  /** POST /api/admin/appointment/uploadReport/{id} multipart —— 管理员上传体检报告 */
  uploadReport(id, file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post(`/admin/appointment/uploadReport/${id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  /** POST /api/admin/appointment/complete/{id} */
  completeAppointment(id) {
    return request.post(`/admin/appointment/complete/${id}`)
  }
}