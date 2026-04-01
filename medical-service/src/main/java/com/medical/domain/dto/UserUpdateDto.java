package com.medical.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户信息更新 DTO
 */
@Data
public class UserUpdateDto {
    private String name;
    private String email;
    private String mobilePhone;
    /** 传入则整体替换用户角色；不传表示不修改角色 */
    private List<Long> roleIds;

    /** 所属科室；与 clearDept 配合使用 */
    private Long deptId;

    /** 为 true 时清空科室（不修改请传 false 或不传） */
    private Boolean clearDept;
}
