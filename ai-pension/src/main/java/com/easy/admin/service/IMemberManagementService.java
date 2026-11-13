package com.easy.admin.service;

import com.easy.common.util.Result;

public interface IMemberManagementService {

    Result getMemberList(Integer page, Integer size, String phone, String name, String status);

    Result getMemberDetail(Long userId);

    Result enableMember(Long userId);

    Result disableMember(Long userId);

    Result getMemberLevels();

    Result updateMemberLevel(Long userId, String levelCode);

    Result updateMemberPoints(Long userId, Integer points);

    Result resetMemberPassword(Long userId);

    Result addMember(String phone, String password, String role, Integer points);

    Result deleteMember(Long userId);
}