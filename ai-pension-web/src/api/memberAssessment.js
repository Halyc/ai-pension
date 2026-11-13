import request from './request'

/**
 * 会员端 - 健康评测 —— 对应 MemberAssessmentController（/api/member/assessment）
 */
export const memberAssessmentApi = {
  /** GET /api/member/assessment/questionnaires 已发布问卷
   *  data: [{ id, title, description, createTime }] */
  questionnaires() {
    return request.get('/member/assessment/questionnaires')
  },

  /** GET /api/member/assessment/questions/{questionnaireId}
   *  data: [{ id, questionnaireId, content, type, options, sortOrder }] */
  questions(questionnaireId) {
    return request.get(`/member/assessment/questions/${questionnaireId}`)
  },

  /** POST /api/member/assessment/submit  body:{ questionnaireId, answers } */
  submit(data) {
    return request.post('/member/assessment/submit', data)
  },

  /** GET /api/member/assessment/history 我的评测历史
   *  data: [{ id, questionnaireId, questionnaireTitle, aiScore, createTime }] */
  history() {
    return request.get('/member/assessment/history')
  },

  /** GET /api/member/assessment/detail/{resultId}
   *  data: { id, questionnaireId, questionnaireTitle,
   *          questions:[{id,content,type,options}], answers, aiScore, aiSuggestion, createTime } */
  detail(resultId) {
    return request.get(`/member/assessment/detail/${resultId}`)
  }
}
