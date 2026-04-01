package com.medical.web.api.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.exception.ServiceException;
import com.medical.common.pagination.PageResult;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.DeptSaveDto;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Nurse;
import com.medical.domain.entity.SysDept;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.DeptListVo;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.NurseMapper;
import com.medical.mapper.SysDeptMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.StaffDeptAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 科室管理（与开发文档 /api/admin/dept 一致）
 */
@RestController
@RequestMapping("/api/admin/dept")
@RequiredArgsConstructor
public class AdminDeptController {

    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;
    private final DoctorMapper doctorMapper;
    private final NurseMapper nurseMapper;
    private final StaffDeptAssignmentService staffDeptAssignmentService;

    @GetMapping("/page")
    public ResultVo<PageResult<DeptListVo>> page(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SysDept::getName, kw).or().like(SysDept::getCode, kw));
        }
        if (status != null) {
            wrapper.eq(SysDept::getStatus, status);
        }
        wrapper.orderByAsc(SysDept::getSortOrder).orderByAsc(SysDept::getDeptId);
        Page<SysDept> page = sysDeptMapper.selectPage(new Page<>(current, size), wrapper);
        Map<Long, String> parentNames = new HashMap<>();
        List<Long> parentIds = page.getRecords().stream()
                .map(SysDept::getParentId)
                .filter(pid -> pid != null && pid > 0)
                .distinct()
                .collect(Collectors.toList());
        if (!parentIds.isEmpty()) {
            for (SysDept p : sysDeptMapper.selectBatchIds(parentIds)) {
                if (p != null) {
                    parentNames.put(p.getDeptId(), p.getName());
                }
            }
        }
        List<DeptListVo> list = page.getRecords().stream().map(d -> {
            DeptListVo vo = new DeptListVo();
            BeanUtils.copyProperties(d, vo);
            if (d.getParentId() != null && d.getParentId() > 0) {
                vo.setParentName(parentNames.get(d.getParentId()));
            }
            return vo;
        }).collect(Collectors.toList());
        PageResult<DeptListVo> result = new PageResult<>();
        result.setCurrentPage(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setList(list);
        return ResultVo.ok(result);
    }

    @GetMapping("/tree")
    public ResultVo<List<DeptListVo>> tree() {
        List<SysDept> all = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getStatus, 1)
                        .orderByAsc(SysDept::getSortOrder)
                        .orderByAsc(SysDept::getDeptId));
        Map<Long, String> idToName = all.stream()
                .collect(Collectors.toMap(SysDept::getDeptId, SysDept::getName, (a, b) -> a));
        List<DeptListVo> vos = all.stream().map(d -> {
            DeptListVo vo = new DeptListVo();
            BeanUtils.copyProperties(d, vo);
            if (d.getParentId() != null && d.getParentId() > 0) {
                vo.setParentName(idToName.get(d.getParentId()));
            }
            return vo;
        }).collect(Collectors.toList());
        return ResultVo.ok(buildTree(vos, 0L));
    }

    private List<DeptListVo> buildTree(List<DeptListVo> flat, long parentId) {
        List<DeptListVo> children = new ArrayList<>();
        for (DeptListVo vo : flat) {
            long pid = vo.getParentId() == null ? 0L : vo.getParentId();
            if (pid == parentId) {
                children.add(vo);
            }
        }
        for (DeptListVo vo : children) {
            List<DeptListVo> sub = buildTree(flat, vo.getDeptId());
            if (!sub.isEmpty()) {
                vo.setChildren(sub);
            }
        }
        return children;
    }

    @GetMapping("/options")
    public ResultVo<List<DeptListVo>> options() {
        List<SysDept> all = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getStatus, 1)
                        .orderByAsc(SysDept::getSortOrder)
                        .orderByAsc(SysDept::getDeptId));
        List<DeptListVo> vos = all.stream().map(d -> {
            DeptListVo vo = new DeptListVo();
            vo.setDeptId(d.getDeptId());
            vo.setParentId(d.getParentId());
            vo.setName(d.getName());
            vo.setCode(d.getCode());
            return vo;
        }).collect(Collectors.toList());
        return ResultVo.ok(vos);
    }

    @GetMapping("/{id}")
    public ResultVo<DeptListVo> detail(@PathVariable Long id) {
        SysDept d = sysDeptMapper.selectById(id);
        if (d == null) {
            throw new ServiceException("科室不存在");
        }
        DeptListVo vo = new DeptListVo();
        BeanUtils.copyProperties(d, vo);
        if (d.getParentId() != null && d.getParentId() > 0) {
            SysDept p = sysDeptMapper.selectById(d.getParentId());
            if (p != null) {
                vo.setParentName(p.getName());
            }
        }
        return ResultVo.ok(vo);
    }

    @PostMapping
    public ResultVo<Void> create(@Valid @RequestBody DeptSaveDto dto) {
        long dup = sysDeptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getName, dto.getName().trim()));
        if (dup > 0) {
            throw new BusinessWarningException("科室名称已存在");
        }
        if (StringUtils.hasText(dto.getCode())) {
            long codeDup = sysDeptMapper.selectCount(
                    new LambdaQueryWrapper<SysDept>()
                            .eq(SysDept::getCode, dto.getCode().trim()));
            if (codeDup > 0) {
                throw new BusinessWarningException("科室代码已存在");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        SysDept d = new SysDept();
        d.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        d.setName(dto.getName().trim());
        d.setCode(StringUtils.hasText(dto.getCode()) ? dto.getCode().trim() : null);
        d.setManager(StringUtils.hasText(dto.getManager()) ? dto.getManager().trim() : null);
        d.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        d.setAddress(StringUtils.hasText(dto.getAddress()) ? dto.getAddress().trim() : null);
        d.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        d.setStatus(dto.getStatus() != null && dto.getStatus() == 0 ? 0 : 1);
        d.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        d.setCreatedTime(now);
        d.setUpdatedTime(now);
        sysDeptMapper.insert(d);
        return ResultVo.ok();
    }

    @PutMapping("/{id}")
    public ResultVo<Void> update(@PathVariable Long id, @Valid @RequestBody DeptSaveDto dto) {
        SysDept exist = sysDeptMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("科室不存在");
        }
        long dup = sysDeptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getName, dto.getName().trim())
                        .ne(SysDept::getDeptId, id));
        if (dup > 0) {
            throw new BusinessWarningException("科室名称已存在");
        }
        if (StringUtils.hasText(dto.getCode())) {
            long codeDup = sysDeptMapper.selectCount(
                    new LambdaQueryWrapper<SysDept>()
                            .eq(SysDept::getCode, dto.getCode().trim())
                            .ne(SysDept::getDeptId, id));
            if (codeDup > 0) {
                throw new BusinessWarningException("科室代码已存在");
            }
        }
        Long pid = dto.getParentId() != null ? dto.getParentId() : 0L;
        if (pid.equals(id)) {
            throw new BusinessWarningException("不能将上级科室设为自己");
        }
        exist.setParentId(pid);
        exist.setName(dto.getName().trim());
        exist.setCode(StringUtils.hasText(dto.getCode()) ? dto.getCode().trim() : null);
        exist.setManager(StringUtils.hasText(dto.getManager()) ? dto.getManager().trim() : null);
        exist.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        exist.setAddress(StringUtils.hasText(dto.getAddress()) ? dto.getAddress().trim() : null);
        exist.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        if (dto.getStatus() != null) {
            exist.setStatus(dto.getStatus());
        }
        exist.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        exist.setUpdatedTime(LocalDateTime.now());
        sysDeptMapper.updateById(exist);
        return ResultVo.ok();
    }

    @DeleteMapping("/{id}")
    public ResultVo<Void> delete(@PathVariable Long id) {
        SysDept exist = sysDeptMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("科室不存在");
        }
        long children = sysDeptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new BusinessWarningException("请先删除或移走下级科室");
        }
        long doctors = doctorMapper.selectCount(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getDeptId, id));
        long nurses = nurseMapper.selectCount(
                new LambdaQueryWrapper<Nurse>().eq(Nurse::getDeptId, id));
        long users = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (doctors > 0 || nurses > 0 || users > 0) {
            throw new BusinessWarningException("科室下仍有用户或医护档案，无法删除");
        }
        sysDeptMapper.deleteById(id);
        return ResultVo.ok();
    }

    /**
     * 按角色将用户归属科室并同步 doctor/nurse 表（依赖 sys_dept.code 与角色代码对应，见 v8 脚本）
     */
    @PostMapping("/sync-staff")
    public ResultVo<Integer> syncStaff() {
        int n = staffDeptAssignmentService.syncAllUsersByRole();
        return ResultVo.ok(n);
    }
}
