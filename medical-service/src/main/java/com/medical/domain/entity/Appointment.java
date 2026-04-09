package com.medical.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预约
 */
@Data
@TableName("appointment")
public class Appointment {
    @TableId(type = IdType.AUTO)
    private Long appointmentId;
    private String appointmentNo;
    private Long patientId;
    private Long doctorId;
    private Long scheduleId;
    private LocalDate appointmentDate;
    private String timeSlot;
    private Integer queueNo;
    private Integer status;
    private BigDecimal feeAmount;
    private Integer paid;
    private LocalDateTime paidTime;
    private LocalDateTime checkInTime;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private String createdBy;
    private String updatedBy;
}