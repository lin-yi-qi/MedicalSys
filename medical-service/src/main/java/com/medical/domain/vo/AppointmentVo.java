package com.medical.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约信息 VO
 */
@Data
public class AppointmentVo {
    private Long appointmentId;
    private String appointmentNo;
    private Long patientId;
    private String patientName;
    private String doctorName;
    private String doctorTitle;
    private String deptName;
    private String appointmentDate;
    private String timeSlot;
    private Integer queueNo;
    private Integer status;
    private String statusText;
    private BigDecimal feeAmount;
    private Integer paid;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}