package com.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.domain.entity.Appointment;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.Schedule;
import com.medical.domain.entity.SysDept;
import com.medical.domain.vo.AppointmentVo;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final SysDeptMapper sysDeptMapper;
    private final ScheduleService scheduleService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 创建预约
     */
    @Transactional(rollbackFor = Exception.class)
    public Appointment createAppointment(Long patientId, Long scheduleId) {
        Schedule schedule = scheduleService.getScheduleById(scheduleId);
        if (schedule == null) {
            throw new BusinessWarningException("排班不存在");
        }
        if (schedule.getStatus() != 1) {
            throw new BusinessWarningException("该时段已停诊");
        }
        if (schedule.getBookedSlots() >= schedule.getTotalSlots()) {
            throw new BusinessWarningException("该时段号源已满");
        }

        LambdaQueryWrapper<Appointment> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Appointment::getPatientId, patientId)
                .eq(Appointment::getScheduleId, scheduleId)
                .in(Appointment::getStatus, List.of(1, 2));
        if (appointmentMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessWarningException("您已预约过该时段，请勿重复预约");
        }

        Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
        if (doctor == null) {
            throw new BusinessWarningException("医生信息不存在");
        }

        boolean locked = scheduleService.lockSlot(scheduleId);
        if (!locked) {
            throw new BusinessWarningException("号源已被抢完，请重新选择");
        }

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo("APT" + System.currentTimeMillis());
        appointment.setPatientId(patientId);
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setScheduleId(scheduleId);
        appointment.setAppointmentDate(schedule.getScheduleDate());
        appointment.setTimeSlot(schedule.getTimeSlot());
        appointment.setStatus(1);
        appointment.setFeeAmount(doctor.getConsultationFee());
        appointment.setPaid(0);
        appointment.setCreatedTime(LocalDateTime.now());
        appointment.setUpdatedTime(LocalDateTime.now());
        appointmentMapper.insert(appointment);

        return appointment;
    }

    /**
     * 取消预约
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessWarningException("预约记录不存在");
        }
        if (!appointment.getPatientId().equals(patientId)) {
            throw new BusinessWarningException("无权操作此预约");
        }
        if (appointment.getStatus() != 1) {
            throw new BusinessWarningException("当前状态无法取消");
        }

        appointment.setStatus(3);
        appointment.setUpdatedTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        scheduleService.releaseSlot(appointment.getScheduleId());
    }

    /**
     * 获取患者的预约列表
     */
    public List<AppointmentVo> getPatientAppointments(Long patientId) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getPatientId, patientId)
                .orderByDesc(Appointment::getAppointmentDate)
                .orderByAsc(Appointment::getTimeSlot);
        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        return appointments.stream().map(a -> {
            AppointmentVo vo = new AppointmentVo();
            vo.setAppointmentId(a.getAppointmentId());
            vo.setAppointmentNo(a.getAppointmentNo());
            vo.setPatientId(a.getPatientId());
            vo.setAppointmentDate(a.getAppointmentDate().format(DATE_FORMATTER));
            vo.setTimeSlot(a.getTimeSlot());
            vo.setQueueNo(a.getQueueNo());
            vo.setStatus(a.getStatus());
            vo.setStatusText(getStatusText(a.getStatus()));
            vo.setFeeAmount(a.getFeeAmount());
            vo.setPaid(a.getPaid());
            vo.setCreatedTime(a.getCreatedTime());

            Doctor doctor = doctorMapper.selectById(a.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setDoctorTitle(doctor.getTitle());
                SysDept dept = sysDeptMapper.selectById(doctor.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }

            Patient patient = patientMapper.selectById(a.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }

            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 获取预约详情
     */
    public AppointmentVo getAppointmentDetail(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessWarningException("预约记录不存在");
        }
        if (!appointment.getPatientId().equals(patientId)) {
            throw new BusinessWarningException("无权查看此预约");
        }

        AppointmentVo vo = new AppointmentVo();
        vo.setAppointmentId(appointment.getAppointmentId());
        vo.setAppointmentNo(appointment.getAppointmentNo());
        vo.setPatientId(appointment.getPatientId());
        vo.setAppointmentDate(appointment.getAppointmentDate().format(DATE_FORMATTER));
        vo.setTimeSlot(appointment.getTimeSlot());
        vo.setQueueNo(appointment.getQueueNo());
        vo.setStatus(appointment.getStatus());
        vo.setStatusText(getStatusText(appointment.getStatus()));
        vo.setFeeAmount(appointment.getFeeAmount());
        vo.setPaid(appointment.getPaid());
        vo.setCreatedTime(appointment.getCreatedTime());

        Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
        if (doctor != null) {
            vo.setDoctorName(doctor.getName());
            vo.setDoctorTitle(doctor.getTitle());
            SysDept dept = sysDeptMapper.selectById(doctor.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }

        Patient patient = patientMapper.selectById(appointment.getPatientId());
        if (patient != null) {
            vo.setPatientName(patient.getName());
        }

        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待就诊";
            case 2: return "已就诊";
            case 3: return "已取消";
            case 4: return "爽约";
            default: return "未知";
        }
    }
}