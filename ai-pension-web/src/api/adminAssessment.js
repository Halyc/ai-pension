import request from './request'

/**
 * 管理端 - 问卷评测管理 —— 对应 AssessmentController（/api/admin/assessment）
 */
export const adminAssessmentApi = {
  // ---------- 13.1 问卷管理 ----------
  /** GET /api/admin/assessment/questionnaires
   *  data: [{ id,title,description,status,createTime,updateTime }] */
  listQuestionnaires() {
    return request.get('/admin/assessment/questionnaires')
  },

  /** POST /api/admin/assessment/questionnaires  body:{ title, description }（新建为 DRAFT 草稿） */
  createQuestionnaire(data) {
    return request.post('/admin/assessment/questionnaires', data)
  },

  /** PUT /api/admin/assessment/questionnaires/{id}  body:{ title?, description? } */
  updateQuestionnaire(id, data) {
    return request.put(`/admin/assessment/questionnaires/${id}`, data)
  },

  /** DELETE /api/admin/assessment/questionnaires/{id} */
  deleteQuestionnaire(id) {
    return request.delete(`/admin/assessment/questionnaires/${id}`)
  },

  /** POST /api/admin/assessment/questionnaires/{id}/publish 发布问卷（DRAFT → PUBLISHED） */
  publishQuestionnaire(id) {
    return request.post(`/admin/assessment/questionnaires/${id}/publish`)
  },

  // ---------- 13.2 题目管理 ----------
  /** GET /api/admin/assessment/questions  params:{ questionnaireId }（必填）
   *  data: [{ id, questionnaireId, content, type, options, sortOrder }] */
  listQuestions(questionnaireId) {
    const params = questionnaireId ? { questionnaireId } : {}
    return request.get('/admin/assessment/questions', { params })
  },

  /** POST /api/admin/assessment/questions
   *  body: { questionnaireId, content, type, options, sortOrder } */
  createQuestion(data) {
    return request.post('/admin/assessment/questions', data)
  },

  /** PUT /api/admin/assessment/questions/{id} */
  updateQuestion(id, data) {
    return request.put(`/admin/assessment/questions/${id}`, data)
  },

  /** DELETE /api/admin/assessment/questions/{id} */
  deleteQuestion(id) {
    return request.delete(`/admin/assessment/questions/${id}`)
  },

  // ---------- 13.3 评测结果管理 ----------
  /** GET /api/admin/assessment/results
   *  data: [{ id,userId,questionnaireId,answers,aiScore,aiSuggestion,createTime }] */
  listResults() {
    return request.get('/admin/assessment/results')
  },

  /** GET /api/admin/assessment/results/{id} */
  getResultDetail(id) {
    return request.get(`/admin/assessment/results/${id}`)
  },

  /** DELETE /api/admin/assessment/results/{id} */
  deleteResult(id) {
    return request.delete(`/admin/assessment/results/${id}`)
  }
}