package com.medical.web.api.patient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.response.ResultVo;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.PrescriptionVo;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 患者端 — 我的处方（只读，按当前登录患者隔离）
 */
@RestController
@RequestMapping("/api/patient/prescription")
@RequiredArgsConstructor
public class PatientPrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PatientMapper patientMapper;
    private final SysUserMapper sysUserMapper;

    @GetMapping("/my")
    public ResultVo<List<PrescriptionVo>> myPrescriptions() {
        Long patientId = getCurrentPatientId();
        return ResultVo.ok(prescriptionService.getPrescriptionsByPatientId(patientId));
    }

    @GetMapping("/{prescriptionId}")
    public ResultVo<PrescriptionVo> detail(@PathVariable Long prescriptionId) {
        Long patientId = getCurrentPatientId();
        return ResultVo.ok(prescriptionService.getPrescriptionDetailForPatient(prescriptionId, patientId));
    }

    private Long getCurrentPatientId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessWarningException("请先登录");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, auth.getName()));
        if (user == null) {
            throw new BusinessWarningException("用户不存在");
        }
        Patient patient = patientMapper.selectOne(
                new LambdaQueryWrapper<Patient>().eq(Patient::getUserId, user.getUserId()));
        if (patient == null) {
            throw new BusinessWarningException("患者档案不存在");
        }
        return patient.getPatientId();
    }
}
