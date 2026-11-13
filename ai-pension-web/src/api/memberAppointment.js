import request from './request'

/**
 * 会员端 - 预约服务 —— 对应 AppointmentController（/api/member/appointment）
 */
export const memberAppointmentApi = {
  /** GET /api/member/appointment/packages  params:{ sortBy }
   *  sortBy: price_asc | price_desc | 空（默认最新）
   *  data: AppointmentPackage 实体列表（字段下划线：cover_url/suitable_people/items...） */
  listPackages(sortBy) {
    return request.get('/member/appointment/packages', {
      params: sortBy ? { sortBy } : {}
    })
  },

  /** GET /api/member/appointment/slots/{packageId}
   *  data: [{ date, slots:[{ id,packageId,appointDate,timeRange,maxCount,
   *          currentCount,status,available }] }]（按日期分组） */
  listSlots(packageId) {
    return request.get(`/member/appointment/slots/${packageId}`)
  },

  /** POST /api/member/appointment/book  —— 后端为 @RequestParam，走 query 参数
   *  params: { slotId, packageId } */
  bookAppointment(slotId, packageId) {
    return request.post('/member/appointment/book', null, {
      params: { slotId, packageId }
    })
  },

  /** POST /api/member/appointment/cancel —— @RequestParam，params:{ appointmentId } */
  cancelAppointment(appointmentId) {
    return request.post('/member/appointment/cancel', null, {
      params: { appointmentId }
    })
  },

  /** GET /api/member/appointment/list 我的预约
   *  data: [{ id,status,reportUrl,createTime, package:{ id,name,coverUrl,price } }] */
  listMyAppointments() {
    return request.get('/member/appointment/list')
  }
}