package com.medical.web.api.patient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.response.ResultVo;
import com.medical.domain.entity.Doctor;
import com.medical.domain.vo.ScheduleSlotVo;
import com.medical.mapper.DoctorMapper;
import com.medical.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班查询 API
 */
@Slf4j
@RestController
@RequestMapping("/api/patient/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final DoctorMapper doctorMapper;

    @GetMapping("/available-dates")
    public ResultVo<List<LocalDate>> getAvailableDates(
            @RequestParam("userId") Long userId) {
        // 根据 userId 查询 doctorId
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getUserId, userId);
        Doctor doctor = doctorMapper.selectOne(wrapper);
        if (doctor == null) {
            return ResultVo.ok(List.of());
        }
        List<LocalDate> dates = scheduleService.getAvailableDates(doctor.getDoctorId());
        return ResultVo.ok(dates);
    }

    @GetMapping("/slots")
    public ResultVo<List<ScheduleSlotVo>> getScheduleSlots(
            @RequestParam("userId") Long userId,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        // 根据 userId 查询 doctorId
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getUserId, userId);
        Doctor doctor = doctorMapper.selectOne(wrapper);
        if (doctor == null) {
            return ResultVo.ok(List.of());
        }
        List<ScheduleSlotVo> slots = scheduleService.getScheduleSlots(doctor.getDoctorId(), date);
        return ResultVo.ok(slots);
    }
}