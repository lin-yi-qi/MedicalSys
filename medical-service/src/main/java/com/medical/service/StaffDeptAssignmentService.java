package com.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Nurse;
import com.medical.domain.entity.SysDept;
import com.medical.domain.entity.SysRole;
import com.medical.domain.entity.SysUser;
import com.medical.domain.entity.SysUserRole;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.NurseMapper;
import com.medical.mapper.SysDeptMapper;
import com.medical.mapper.SysRoleMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 按用户角色将医生/护士归属到对应科室（sys_dept.code 与 role_code 对齐），并维护 doctor / nurse 业务表。
 */
@Service
@RequiredArgsConstructor
public class StaffDeptAssignmentService {

    private static final List<String> PHYSICIAN_ROLE_PRIORITY = Arrays.asList(
            "ER_DOCTOR", "PEDIATRICIAN", "INTERNIST", "SURGEON", "GYNECOLOGIST",
            "ORTHOPEDIST", "DERMATOLOGIST", "OPHTHALMOLOGIST", "ENT_DOCTOR", "CARDIOLOGIST",
            "NEUROLOGIST", "ONCOLOGIST", "PSYCHIATRIST", "TCM_DOCTOR", "REHAB_DOCTOR",
            "NUTRITIONIST", "ANESTHESIOLOGIST", "PATHOLOGIST", "DOCTOR"
    );

    private static final List<String> NURSE_ROLE_PRIORITY = Arrays.asList(
            "NURSE_HEAD", "ICU_NURSE", "OR_NURSE", "EMERGENCY_NURSE", "NURSE"
    );

    private static final Set<String> PHYSICIAN_ROLES = new HashSet<>(PHYSICIAN_ROLE_PRIORITY);
    private static final Set<String> NURSE_ROLES = new HashSet<>(NURSE_ROLE_PRIORITY);

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final DoctorMapper doctorMapper;
    private final NurseMapper nurseMapper;

    /**
     * 为所有用户按角色批量同步科室，并 upsert doctor/nurse 表。
     *
     * @return 成功同步的用户数
     */
    @Transactional(rollbackFor = Exception.class)
    public int syncAllUsersByRole() {
        Map<String, Long> deptCodeToId = loadDeptCodeToId();
        List<SysUser> users = sysUserMapper.selectList(null);
        int n = 0;
        for (SysUser u : users) {
            if (syncUserInternal(u.getUserId(), deptCodeToId)) {
                n++;
            }
        }
        return n;
    }

    /**
     * 同步单个用户：根据角色写入 sys_user.dept_id，并维护 doctor / nurse 行。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean syncUser(Long userId) {
        return syncUserInternal(userId, loadDeptCodeToId());
    }

    private Map<String, Long> loadDeptCodeToId() {
        List<SysDept> depts = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().isNotNull(SysDept::getCode));
        Map<String, Long> map = new HashMap<>();
        for (SysDept d : depts) {
            if (StringUtils.hasText(d.getCode())) {
                map.put(d.getCode().trim(), d.getDeptId());
            }
        }
        return map;
    }

    private boolean syncUserInternal(Long userId, Map<String, Long> deptCodeToId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        List<Long> roleIds = sysUserRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return false;
        }
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        List<String> codes = roles.stream()
                .map(SysRole::getRoleCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        boolean isPhysician = codes.stream().anyMatch(PHYSICIAN_ROLES::contains);
        boolean isNurse = codes.stream().anyMatch(NURSE_ROLES::contains);

        boolean changed = false;
        if (isPhysician) {
            Long deptId = pickDeptId(codes, PHYSICIAN_ROLE_PRIORITY, deptCodeToId);
            if (deptId != null) {
                user.setDeptId(deptId);
                sysUserMapper.updateById(user);
                upsertDoctor(user, deptId);
                changed = true;
            }
        } else if (isNurse) {
            Long deptId = pickDeptId(codes, NURSE_ROLE_PRIORITY, deptCodeToId);
            if (deptId == null) {
                deptId = deptCodeToId.get("NURSE");
            }
            if (deptId != null) {
                user.setDeptId(deptId);
                sysUserMapper.updateById(user);
                upsertNurse(user, deptId);
                changed = true;
            }
        }
        return changed;
    }

    private Long pickDeptId(List<String> userRoles, List<String> priority, Map<String, Long> deptCodeToId) {
        for (String code : priority) {
            if (userRoles.contains(code)) {
                Long id = deptCodeToId.get(code);
                if (id != null) {
                    return id;
                }
            }
        }
        return null;
    }

    private void upsertDoctor(SysUser user, Long deptId) {
        Doctor exist = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, user.getUserId()));
        LocalDateTime now = LocalDateTime.now();
        String name = StringUtils.hasText(user.getName()) ? user.getName().trim() : user.getUsername();
        if (exist == null) {
            Doctor d = new Doctor();
            d.setUserId(user.getUserId());
            d.setDoctorNo("D" + user.getUserId());
            d.setName(name);
            d.setDeptId(deptId);
            d.setStatus(1);
            d.setSortOrder(0);
            d.setCreatedTime(now);
            d.setUpdatedTime(now);
            doctorMapper.insert(d);
        } else {
            exist.setDeptId(deptId);
            exist.setName(name);
            exist.setUpdatedTime(now);
            doctorMapper.updateById(exist);
        }
    }

    private void upsertNurse(SysUser user, Long deptId) {
        Nurse exist = nurseMapper.selectOne(
                new LambdaQueryWrapper<Nurse>().eq(Nurse::getUserId, user.getUserId()));
        LocalDateTime now = LocalDateTime.now();
        String name = StringUtils.hasText(user.getName()) ? user.getName().trim() : user.getUsername();
        if (exist == null) {
            Nurse n = new Nurse();
            n.setUserId(user.getUserId());
            n.setNurseNo("N" + user.getUserId());
            n.setName(name);
            n.setDeptId(deptId);
            n.setStatus(1);
            n.setSortOrder(0);
            n.setCreatedTime(now);
            n.setUpdatedTime(now);
            nurseMapper.insert(n);
        } else {
            exist.setDeptId(deptId);
            exist.setName(name);
            exist.setUpdatedTime(now);
            nurseMapper.updateById(exist);
        }
    }

    /**
     * 用户已指定 deptId 时，仅同步 doctor/nurse 行（不覆盖用户手选科室）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void upsertStaffRecordsForUser(Long userId, Long deptId, List<String> roleCodes) {
        if (deptId == null) {
            return;
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            return;
        }
        boolean isPhysician = roleCodes.stream().anyMatch(PHYSICIAN_ROLES::contains);
        boolean isNurse = roleCodes.stream().anyMatch(NURSE_ROLES::contains);
        if (isPhysician) {
            upsertDoctor(user, deptId);
        }
        if (isNurse) {
            upsertNurse(user, deptId);
        }
    }
}
