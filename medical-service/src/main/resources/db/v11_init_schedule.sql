-- ============================================================
-- 智能医疗服务管理系统 - 排班数据填充脚本
-- 版本：v11
-- 说明：为拥有 DOCTOR 角色的医生生成未来7天的排班数据
--       依赖：v12 医生档案数据已执行
-- 数据库：MySQL 8.x utf8mb4
-- 可重复执行：会先清理旧排班数据再插入
-- ============================================================

SET NAMES utf8mb4;
USE `medical_service`;

-- 清理旧排班数据
DELETE FROM `schedule`;

-- 为 doctor 表中的所有医生生成未来7天的排班
INSERT INTO `schedule` (
    doctor_id, dept_id, schedule_date, time_slot,
    total_slots, booked_slots, status,
    created_time, updated_time
)
SELECT
    d.doctor_id,
    d.dept_id,
    DATE_ADD(CURDATE(), INTERVAL t.n DAY) AS schedule_date,
    ts.time_slot,
    20 AS total_slots,
    0 AS booked_slots,
    1 AS status,
    NOW(),
    NOW()
FROM doctor d
         CROSS JOIN (
    SELECT 0 AS n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6
) t
         CROSS JOIN (
    SELECT '08:00-09:00' AS time_slot
    UNION SELECT '09:00-10:00'
    UNION SELECT '10:00-11:00'
    UNION SELECT '14:00-15:00'
    UNION SELECT '15:00-16:00'
) ts
WHERE d.status = 1
  AND NOT EXISTS (
    SELECT 1 FROM schedule s
    WHERE s.doctor_id = d.doctor_id
      AND s.schedule_date = DATE_ADD(CURDATE(), INTERVAL t.n DAY)
      AND s.time_slot = ts.time_slot
);