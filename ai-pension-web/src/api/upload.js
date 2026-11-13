import request from './request'

function buildFormData(file, extra) {
  const formData = new FormData()
  formData.append('file', file)
  if (extra) {
    Object.keys(extra).forEach((key) => formData.append(key, extra[key]))
  }
  return formData
}

export const uploadApi = {
  /**
   * 管理端图片上传（活动封面、套餐封面、管理员头像）
   * POST /api/admin/upload/image，参数 file + dir
   */
  uploadImage(file, dir = 'covers') {
    return request.post('/admin/upload/image', buildFormData(file, { dir }))
  },

  /**
   * 会员端头像上传
   * POST /api/member/upload/avatar，参数 file
   */
  uploadAvatar(file) {
    return request.post('/member/upload/avatar', buildFormData(file))
  }
}
