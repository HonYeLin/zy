/*
Navicat MySQL Data Transfer

Source Server         : py
Source Server Version : 80040
Source Host           : localhost:3306
Source Database       : campus_paw_track

Target Server Type    : MYSQL
Target Server Version : 80040
File Encoding         : 65001

Date: 2026-05-27 17:07:22
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `ai_predictions`
-- ----------------------------
DROP TABLE IF EXISTS `ai_predictions`;
CREATE TABLE `ai_predictions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `animal_id` bigint NOT NULL,
  `predicted_behavior` varchar(50) NOT NULL,
  `confidence_score` decimal(5,4) DEFAULT NULL COMMENT '预测置信度',
  `predicted_at` datetime DEFAULT NULL COMMENT '预测生效时间',
  `reasoning_text` text COMMENT 'AI 推理的逻辑过程描述',
  PRIMARY KEY (`id`),
  KEY `animal_id` (`animal_id`),
  CONSTRAINT `ai_predictions_ibfk_1` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of ai_predictions
-- ----------------------------

-- ----------------------------
-- Table structure for `animals`
-- ----------------------------
DROP TABLE IF EXISTS `animals`;
CREATE TABLE `animals` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '给小动物起的名字',
  `breed` varchar(50) DEFAULT NULL COMMENT '品种/特征描述',
  `qr_code_id` varchar(100) DEFAULT NULL COMMENT '对应线下扫描二维码的唯一标识',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '档案建立时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `qr_code_id` (`qr_code_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of animals
-- ----------------------------
INSERT INTO `animals` VALUES ('1', '大橘', 'Cat', null, '2026-05-27 02:18:03');
INSERT INTO `animals` VALUES ('2', 'M01', 'Cat', null, '2026-05-27 02:19:20');
INSERT INTO `animals` VALUES ('3', 'D01', 'Dog', null, '2026-05-27 02:31:43');
INSERT INTO `animals` VALUES ('4', 'D02', 'Dog', null, '2026-05-27 02:32:00');
INSERT INTO `animals` VALUES ('5', 'D01', 'Cat', null, '2026-05-27 02:32:22');

-- ----------------------------
-- Table structure for `animal_life_narratives`
-- ----------------------------
DROP TABLE IF EXISTS `animal_life_narratives`;
CREATE TABLE `animal_life_narratives` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `animal_id` bigint NOT NULL COMMENT '关联动物ID',
  `narrative_content` text NOT NULL COMMENT 'AI生成的行为描述(如：在教学楼A睡觉...)',
  `start_time` datetime NOT NULL COMMENT '该叙事覆盖的时间区间开始',
  `end_time` datetime NOT NULL COMMENT '该叙事覆盖的时间区间结束',
  `summary_type` varchar(20) DEFAULT 'DAILY' COMMENT '类型: DAILY, WEEKLY, INSIGHT',
  `model_version` varchar(50) DEFAULT NULL COMMENT '生成该内容的AI模型版本(方便后续优化比较)',
  `token_usage` int DEFAULT NULL COMMENT '记录消耗的Token数(方便统计成本)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_animal_time` (`animal_id`,`start_time`,`end_time`),
  CONSTRAINT `animal_life_narratives_ibfk_1` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of animal_life_narratives
-- ----------------------------

-- ----------------------------
-- Table structure for `animal_logs`
-- ----------------------------
DROP TABLE IF EXISTS `animal_logs`;
CREATE TABLE `animal_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `animal_id` bigint NOT NULL COMMENT '关联动物ID',
  `user_id` bigint DEFAULT NULL COMMENT '记录者ID (可为空，支持游客模式)',
  `location` point NOT NULL /*!80003 SRID 4326 */ COMMENT '地理坐标(经纬度)',
  `behavior_tag` enum('EATING','SLEEPING','PLAYING','SUNBATHING','WALKING','OTHER') NOT NULL COMMENT '行为标记',
  `photo_url` varchar(255) DEFAULT NULL COMMENT '现场照片路径',
  `description` varchar(200) DEFAULT NULL COMMENT '补充描述',
  `recorded_at` datetime NOT NULL COMMENT '实际拍摄/观察时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据录入时间',
  PRIMARY KEY (`id`),
  SPATIAL KEY `location` (`location`),
  KEY `animal_id` (`animal_id`),
  CONSTRAINT `animal_logs_ibfk_1` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------
-- Records of animal_logs
-- ----------------------------
INSERT INTO `animal_logs` VALUES ('1', '2', null, 0xE610000001010000003579CA6ABADC5D40C55565DF15013A40, 'EATING', '', '大', '2026-05-27 02:19:21', '2026-05-27 02:19:20');
INSERT INTO `animal_logs` VALUES ('2', '3', null, 0xE61000000101000000D940BAD8B4DC5D40DFDFA0BDFA003A40, 'SLEEPING', '', '黄', '2026-05-27 02:31:43', '2026-05-27 02:31:43');
INSERT INTO `animal_logs` VALUES ('3', '4', null, 0xE61000000101000000E542E55FCBDC5D40A702EE79FE003A40, 'EATING', '', '黄', '2026-05-27 02:32:00', '2026-05-27 02:32:00');
INSERT INTO `animal_logs` VALUES ('4', '5', null, 0xE61000000101000000077AA86DC3DC5D4059F78F85E8003A40, 'EATING', '', '黄', '2026-05-27 02:32:23', '2026-05-27 02:32:22');
INSERT INTO `animal_logs` VALUES ('5', '3', null, 0xE61000000101000000FE7BF0DAA5DC5D40F17EDC7EF9003A40, 'EATING', '', '黄', '2026-05-27 02:32:36', '2026-05-27 02:32:35');

-- ----------------------------
-- Table structure for `prediction_feedbacks`
-- ----------------------------
DROP TABLE IF EXISTS `prediction_feedbacks`;
CREATE TABLE `prediction_feedbacks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actual_behavior` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `animal_id` bigint NOT NULL,
  `feedback_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `predicted_behavior` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `recorded_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of prediction_feedbacks
-- ----------------------------
INSERT INTO `prediction_feedbacks` VALUES ('1', '睡觉/休息', '5', 'CONFIRMED', '睡觉/休息', '2026-05-27 16:26:36.271101');
