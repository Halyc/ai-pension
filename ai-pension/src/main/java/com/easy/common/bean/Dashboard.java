package com.easy.common.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dashboard {

    private Long totalMembers;
    private Long todayNewMembers;
    private Long totalAppointments;
    private Long todayAppointments;
    private Long totalActivities;
    private Long ongoingActivities;
    private Long aiChatCount;

}
