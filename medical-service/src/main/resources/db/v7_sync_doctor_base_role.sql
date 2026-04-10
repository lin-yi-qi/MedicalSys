-- ============================================================
-- 智能医疗服务管理系统 - 为专科医师补授「医生」角色 + 自动映射科室
-- 版本：v8
-- 说明：
--   1. 为拥有专科角色的用户补授 DOCTOR 基础角色
--   2. 根据用户的最高优先级角色，自动设置 sys_user.dept_id
-- ============================================================

SET NAMES utf8mb4;
USE `medical_service`;

-- ============================================================
-- 第一部分：补授 DOCTOR 角色
-- ============================================================
INSERT IGNORE INTO sys_user_role (user_id, role_id, created_time)
SELECT DISTINCT ur.user_id, dr.role_id, NOW()
FROM sys_user_role ur
         INNER JOIN sys_role sr ON ur.role_id = sr.role_id AND sr.status = 1
         INNER JOIN sys_role dr ON dr.role_code = 'DOCTOR' AND dr.status = 1
WHERE sr.role_code IN (
                       'ER_DOCTOR', 'PEDIATRICIAN', 'INTERNIST', 'SURGEON',
                       'GYNECOLOGIST', 'ORTHOPEDIST', 'DERMATOLOGIST',
                       'OPHTHALMOLOGIST', 'ENT_DOCTOR', 'CARDIOLOGIST',
                       'NEUROLOGIST', 'ONCOLOGIST', 'PSYCHIATRIST',
                       'TCM_DOCTOR', 'REHAB_DOCTOR', 'NUTRITIONIST',
                       'ANESTHESIOLOGIST', 'PATHOLOGIST'
    )
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur2
                      INNER JOIN sys_role r2 ON ur2.role_id = r2.role_id
    WHERE r2.role_code = 'DOCTOR' AND ur2.user_id = ur.user_id
);

-- ============================================================
-- 第二部分：根据角色自动映射科室（填充 sys_user.dept_id）
-- 医生角色优先级顺序
-- ============================================================

-- 2.1 先为有专科角色的医生设置科室
UPDATE sys_user u
    INNER JOIN (
    SELECT
    ur.user_id,
    MIN(CASE
    -- 按优先级排序，数值越小优先级越高
    WHEN r.role_code = 'ER_DOCTOR' THEN 1
    WHEN r.role_code = 'PEDIATRICIAN' THEN 2
    WHEN r.role_code = 'INTERNIST' THEN 3
    WHEN r.role_code = 'SURGEON' THEN 4
    WHEN r.role_code = 'GYNECOLOGIST' THEN 5
    WHEN r.role_code = 'ORTHOPEDIST' THEN 6
    WHEN r.role_code = 'DERMATOLOGIST' THEN 7
    WHEN r.role_code = 'OPHTHALMOLOGIST' THEN 8
    WHEN r.role_code = 'ENT_DOCTOR' THEN 9
    WHEN r.role_code = 'CARDIOLOGIST' THEN 10
    WHEN r.role_code = 'NEUROLOGIST' THEN 11
    WHEN r.role_code = 'ONCOLOGIST' THEN 12
    WHEN r.role_code = 'PSYCHIATRIST' THEN 13
    WHEN r.role_code = 'TCM_DOCTOR' THEN 14
    WHEN r.role_code = 'REHAB_DOCTOR' THEN 15
    WHEN r.role_code = 'NUTRITIONIST' THEN 16
    WHEN r.role_code = 'ANESTHESIOLOGIST' THEN 17
    WHEN r.role_code = 'PATHOLOGIST' THEN 18
    WHEN r.role_code = 'DOCTOR' THEN 19
    ELSE 99
    END) as priority,
    -- 获取优先级最高的角色代码
    SUBSTRING_INDEX(
    GROUP_CONCAT(r.role_code ORDER BY
    CASE
    WHEN r.role_code = 'ER_DOCTOR' THEN 1
    WHEN r.role_code = 'PEDIATRICIAN' THEN 2
    WHEN r.role_code = 'INTERNIST' THEN 3
    WHEN r.role_code = 'SURGEON' THEN 4
    WHEN r.role_code = 'GYNECOLOGIST' THEN 5
    WHEN r.role_code = 'ORTHOPEDIST' THEN 6
    WHEN r.role_code = 'DERMATOLOGIST' THEN 7
    WHEN r.role_code = 'OPHTHALMOLOGIST' THEN 8
    WHEN r.role_code = 'ENT_DOCTOR' THEN 9
    WHEN r.role_code = 'CARDIOLOGIST' THEN 10
    WHEN r.role_code = 'NEUROLOGIST' THEN 11
    WHEN r.role_code = 'ONCOLOGIST' THEN 12
    WHEN r.role_code = 'PSYCHIATRIST' THEN 13
    WHEN r.role_code = 'TCM_DOCTOR' THEN 14
    WHEN r.role_code = 'REHAB_DOCTOR' THEN 15
    WHEN r.role_code = 'NUTRITIONIST' THEN 16
    WHEN r.role_code = 'ANESTHESIOLOGIST' THEN 17
    WHEN r.role_code = 'PATHOLOGIST' THEN 18
    WHEN r.role_code = 'DOCTOR' THEN 19
    ELSE 99
    END
    ), ',', 1
    ) as highest_role_code
    FROM sys_user_role ur
    INNER JOIN sys_role r ON ur.role_id = r.role_id
    WHERE r.role_code IN (
    'ER_DOCTOR', 'PEDIATRICIAN', 'INTERNIST', 'SURGEON',
    'GYNECOLOGIST', 'ORTHOPEDIST', 'DERMATOLOGIST',
    'OPHTHALMOLOGIST', 'ENT_DOCTOR', 'CARDIOLOGIST',
    'NEUROLOGIST', 'ONCOLOGIST', 'PSYCHIATRIST',
    'TCM_DOCTOR', 'REHAB_DOCTOR', 'NUTRITIONIST',
    'ANESTHESIOLOGIST', 'PATHOLOGIST', 'DOCTOR'
    )
    GROUP BY ur.user_id
    ) AS user_role_priority ON u.user_id = user_role_priority.user_id
    INNER JOIN sys_dept d ON d.code = user_role_priority.highest_role_code AND d.status = 1
    SET u.dept_id = d.dept_id,
        u.updated_time = NOW()
WHERE (u.dept_id IS NULL OR u.dept_id != d.dept_id)
  AND u.user_id IN (
-- 只更新医生角色用户
    SELECT DISTINCT ur2.user_id
    FROM sys_user_role ur2
    INNER JOIN sys_role r2 ON ur2.role_id = r2.role_id
    WHERE r2.role_code IN ('DOCTOR', 'ER_DOCTOR', 'PEDIATRICIAN', 'SURGEON')
    );

-- ============================================================
-- 第三部分：为护士设置科室（如果有护士角色）
-- ============================================================
UPDATE sys_user u
    INNER JOIN (
    SELECT
    ur.user_id,
    MIN(CASE
    WHEN r.role_code = 'NURSE_HEAD' THEN 1
    WHEN r.role_code = 'ICU_NURSE' THEN 2
    WHEN r.role_code = 'OR_NURSE' THEN 3
    WHEN r.role_code = 'EMERGENCY_NURSE' THEN 4
    WHEN r.role_code = 'NURSE' THEN 5
    ELSE 99
    END) as priority,
    SUBSTRING_INDEX(
    GROUP_CONCAT(r.role_code ORDER BY
    CASE
    WHEN r.role_code = 'NURSE_HEAD' THEN 1
    WHEN r.role_code = 'ICU_NURSE' THEN 2
    WHEN r.role_code = 'OR_NURSE' THEN 3
    WHEN r.role_code = 'EMERGENCY_NURSE' THEN 4
    WHEN r.role_code = 'NURSE' THEN 5
    ELSE 99
    END
    ), ',', 1
    ) as highest_role_code
    FROM sys_user_role ur
    INNER JOIN sys_role r ON ur.role_id = r.role_id
    WHERE r.role_code IN ('NURSE_HEAD', 'ICU_NURSE', 'OR_NURSE', 'EMERGENCY_NURSE', 'NURSE')
    GROUP BY ur.user_id
    ) AS user_nurse_priority ON u.user_id = user_nurse_priority.user_id
    INNER JOIN sys_dept d ON d.code = user_nurse_priority.highest_role_code AND d.status = 1
    SET u.dept_id = d.dept_id,
        u.updated_time = NOW()
WHERE (u.dept_id IS NULL OR u.dept_id != d.dept_id);

-- ============================================================
-- 验证结果
-- ============================================================
SELECT
    u.user_id,
    u.username,
    u.name,
    u.dept_id,
    d.name as dept_name,
    d.code as dept_code,
    GROUP_CONCAT(DISTINCT r.role_code ORDER BY r.role_code) as roles
FROM sys_user u
         LEFT JOIN sys_dept d ON u.dept_id = d.dept_id
         LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
         LEFT JOIN sys_role r ON ur.role_id = r.role_id
WHERE r.role_code IN ('DOCTOR', 'ER_DOCTOR', 'PEDIATRICIAN', 'SURGEON', 'NURSE')
GROUP BY u.user_id
ORDER BY u.user_id
    LIMIT 20;