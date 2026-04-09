package com.medical.domain.vo;

import lombok.Data;

/**
 * 排班时段 VO
 */
@Data
public class ScheduleSlotVo {
    private Long scheduleId;
    private String timeSlot;
    private Integer total;
    private Integer booked;
    private Integer remaining;
    private Boolean available;
}