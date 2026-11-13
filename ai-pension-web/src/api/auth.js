import request from './request'

/**
 * 认证模块 —— 对应 UserController（/api/auth/**）
 */
export const authApi = {
  /** 会员注册 POST /api/auth/register  body: User{phone,password,smsCode,real_name?} */
  register(data) {
    return request.post('/auth/register', data)
  },

  /** 发送短信验证码 POST /api/auth/sms/code  body: {phone} */
  sendSmsCode(data) {
    return request.post('/auth/sms/code', data)
  },

  /** 登录 POST /api/auth/login  body: {phone,password} 或 {phone,smsCode}
   *  返回 data: { tokenPair:{accessToken,refreshToken}, user } */
  login(data) {
    return request.post('/auth/login', data)
  },

  /**
   * 退出登录 POST /api/auth/logout
   * 后端签名：@RequestHeader("Authorization") String accesstoken, String refreshtoken
   * refreshtoken 无注解 → 按 query 参数传递
   */
  logout(refreshToken) {
    return request.post('/auth/logout', null, {
      params: { refreshtoken: refreshToken || '' }
    })
  },

  /** 重置密码 POST /api/auth/reset-password  body: {phone, newPassword, smsCode} */
  resetPassword(data) {
    return request.post('/auth/reset-password', data)
  }
}