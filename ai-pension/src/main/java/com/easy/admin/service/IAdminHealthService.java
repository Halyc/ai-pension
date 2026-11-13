package com.easy.admin.service;

import com.easy.common.util.Result;

public interface IAdminHealthService {

    Result listRecords(Integer page, Integer size, Long userId, String keyword);

    Result listGuidances(Integer page, Integer size, Long userId);

    Result getRecordDetail(Long id);

    Result deleteRecord(Long id);

    Result deleteGuidance(Long id);
}