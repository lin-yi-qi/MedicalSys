package com.medical.web.api.patient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.AppointmentCreateDto;
import com.medical.domain.entity.Appointment;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.AppointmentVo;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 患者端 - 预约管理 API
 */
@Slf4j
@RestController
@RequestMapping("/api/patient/appointment")
@RequiredArgsConstructor
public class PatientAppointmentController {

    private final AppointmentService appointmentService;
    private final PatientMapper patientMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 获取当前登录患者的患者ID
     */
    private Long getCurrentPatientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessWarningException("请先登录");
        }
        String username = auth.getName();

        // 先查 sys_user 获取 user_id
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessWarningException("用户不存在");
        }

        // 再查 patient 表
        Patient patient = patientMapper.selectOne(
                new LambdaQueryWrapper<Patient>().eq(Patient::getUserId, user.getUserId())
        );
        if (patient == null) {
            throw new BusinessWarningException("患者档案不存在");
        }
        return patient.getPatientId();
    }

    /**
     * 创建预约
     */
    @PostMapping("/create")
    public ResultVo<String> create(@Valid @RequestBody AppointmentCreateDto dto) {
        Long patientId = getCurrentPatientId();
        Appointment appointment = appointmentService.createAppointment(patientId, dto.getScheduleId());
        return ResultVo.ok(appointment.getAppointmentNo());
    }

    /**
     * 取消预约
     */
    @PutMapping("/cancel/{appointmentId}")
    public ResultVo<Void> cancel(@PathVariable Long appointmentId) {
        Long patientId = getCurrentPatientId();
        appointmentService.cancelAppointment(appointmentId, patientId);
        return ResultVo.ok();
    }

    /**
     * 获取我的预约列表
     */
    @GetMapping("/my")
    public ResultVo<List<AppointmentVo>> myAppointments() {
        Long patientId = getCurrentPatientId();
        List<AppointmentVo> list = appointmentService.getPatientAppointments(patientId);
        return ResultVo.ok(list);
    }

    /**
     * 获取预约详情
     */
    @GetMapping("/{appointmentId}")
    public ResultVo<AppointmentVo> detail(@PathVariable Long appointmentId) {
        Long patientId = getCurrentPatientId();
        AppointmentVo vo = appointmentService.getAppointmentDetail(appointmentId, patientId);
        return ResultVo.ok(vo);
    }
}