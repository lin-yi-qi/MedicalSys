-- ============================================================
-- 智能医疗服务管理系统 - 护士扩展表补齐脚本
-- 版本：v10
-- 说明：历史库缺少 nurse 表时执行；可重复执行（DROP + CREATE）。
-- ============================================================

SET NAMES utf8mb4;
USE `medical_service`;

DROP TABLE IF EXISTS `nurse`;
CREATE TABLE `nurse` (
  `nurse_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `nurse_no` varchar(50) NOT NULL,
  `name` varchar(50) NOT NULL,
  `dept_id` bigint NOT NULL,
  `title` varchar(50) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `status` tinyint DEFAULT 1,
  `sort_order` int DEFAULT 0,
  `remark` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT NULL,
  `updated_time` datetime DEFAULT NULL,
  `created_by` varchar(50) DEFAULT NULL,
  `updated_by` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`nurse_id`),
  UNIQUE KEY `uk_nurse_no` (`nurse_no`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
