/*
Navicat MySQL Data Transfer

Source Server         : py
Source Server Version : 80040
Source Host           : localhost:3306
Source Database       : campus_paw_track

Target Server Type    : MYSQL
Target Server Version : 80040
File Encoding         : 65001

Date: 2026-05-29 01:26:38
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `ai_predictions`
-- ----------------------------
DROP TABLE IF EXISTS `ai_predictions`;
CREATE TABLE `ai_predictions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `animal_id` bigint NOT NULL,
  `predicted_behavior` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `confidence_score` decimal(5,4) DEFAULT NULL COMMENT '预测置信度',
  `predicted_at` datetime DEFAULT NULL COMMENT '预测生效时间',
  `reasoning_text` text COLLATE utf8mb4_unicode_ci COMMENT 'AI 推理的逻辑过程描述',
  PRIMARY KEY (`id`),
  KEY `fk_predictions_animal` (`animal_id`),
  CONSTRAINT `ai_predictions_ibfk_1` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`),
  CONSTRAINT `fk_predictions_animal` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of ai_predictions
-- ----------------------------

-- ----------------------------
-- Table structure for `animals`
-- ----------------------------
DROP TABLE IF EXISTS `animals`;
CREATE TABLE `animals` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '给小动物起的名字',
  `breed` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '品种/特征描述',
  `qr_code_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '对应线下扫描二维码的唯一标识',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '档案建立时间',
  `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ai_summary` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `qr_code_id` (`qr_code_id`),
  KEY `idx_animals_breed` (`breed`),
  KEY `idx_animals_name_breed` (`name`,`breed`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animals
-- ----------------------------
INSERT INTO `animals` VALUES ('12', '大橙', 'Cat', 'Cat-12', '2026-05-28 21:39:57', '它全身覆盖着均匀的橙色短毛，面部有深浅相间的条纹，耳朵完整。在略显疲惫的眼神下，粉红色的鼻头分外醒目，嘴角还生着细长而清晰的胡须。', '/images/Cat-12/eating/Cat-12_20260528_214054.jpg', 'TA是一只长着粉红鼻头、眼神略显疲惫的橙色条纹猫。TA最大的爱好就是干饭，常在深夜出没于校园投喂点大吃大喝。虽然顶着一张高冷打工人的疲惫脸，却有着最敬业的干饭魂，真是一位深夜食堂的VIP食客。');
INSERT INTO `animals` VALUES ('16', '花花', 'Cat', 'Cat-16', '2026-05-29 00:33:38', '三色毛发，黑、橙、白相间，面部有白色斑块，耳朵完整，眼神明亮', '/images/Cat-16/Cat-16_20260529_003338.jpg', '花花身披黑橙白三色拼色大衣，脸上还戴着标志性的白色“面罩”，眼睛亮得像两颗探照灯。它最爱在深更半夜开展单猫狂欢，零点三十三分准时在校园各个角落上演跑酷与空气搏击。它的出没地点是个谜——毕竟它的定位记录和自拍描述一模一样，堪称行为艺术大师，总在你以为看花眼时闪亮登场。');

-- ----------------------------
-- Table structure for `animal_comments`
-- ----------------------------
DROP TABLE IF EXISTS `animal_comments`;
CREATE TABLE `animal_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `animal_id` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `like_count` int NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `user_nickname` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comment_animal_created` (`animal_id`,`created_at` DESC),
  KEY `idx_comment_animal_likes` (`animal_id`,`like_count` DESC),
  KEY `idx_comment_user` (`user_id`),
  CONSTRAINT `fk_comments_animal` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_comments
-- ----------------------------
INSERT INTO `animal_comments` VALUES ('11', '16', '666还会发光', '2026-05-29 00:34:09.692661', '1', '1', '游客1');

-- ----------------------------
-- Table structure for `animal_life_narratives`
-- ----------------------------
DROP TABLE IF EXISTS `animal_life_narratives`;
CREATE TABLE `animal_life_narratives` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `animal_id` bigint NOT NULL COMMENT '关联动物ID',
  `narrative_content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'AI生成的行为描述(如：在教学楼A睡觉...)',
  `start_time` datetime NOT NULL COMMENT '该叙事覆盖的时间区间开始',
  `end_time` datetime NOT NULL COMMENT '该叙事覆盖的时间区间结束',
  `summary_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'DAILY' COMMENT '类型: DAILY, WEEKLY, INSIGHT',
  `model_version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '生成该内容的AI模型版本(方便后续优化比较)',
  `token_usage` int DEFAULT NULL COMMENT '记录消耗的Token数(方便统计成本)',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (`id`),
  KEY `idx_animal_time` (`animal_id`,`start_time`,`end_time`),
  KEY `idx_narrative_animal_time` (`animal_id`,`start_time` DESC),
  CONSTRAINT `animal_life_narratives_ibfk_1` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`),
  CONSTRAINT `fk_narratives_animal` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_life_narratives
-- ----------------------------
INSERT INTO `animal_life_narratives` VALUES ('5', '12', '大橙正低头享用着今晚的自助大餐，它那一身匀称的漂亮橘毛和清晰的胡须十分帅气，就是眼神里透着一丝疲惫。刚才它大概是去后山巡视领地了吧，现在终于能下班干饭，毕竟当一整天神气的校园守护者也是体力活呢。', '2026-05-28 19:39:57', '2026-05-28 19:39:57', 'DAILY', 'gemini-flash-latest', '0', '2026-05-28 21:40:03');
INSERT INTO `animal_life_narratives` VALUES ('6', '12', '大橙在两个小时后居然又开启了干饭模式，它那粉红的小鼻头不停耸动，脸上深浅相间的条纹写满了认真。看来刚才下班后的那一顿只是前菜，辛勤巡逻了一整天的橘色警长，高低得再来顿宵夜才能彻底充好电呢。', '2026-05-28 19:39:57', '2026-05-28 21:40:54', 'DAILY', 'gemini-flash-latest', '0', '2026-05-28 21:41:01');
INSERT INTO `animal_life_narratives` VALUES ('11', '16', '花花这会儿正在路灯下追着一片落叶打转，三色毛发在夜里晃成小彩虹，上次撞见它还在花坛边打盹，说不定是刚做了个捕猎的梦，醒来就忙着练习新招数呢。', '2026-05-29 00:33:39', '2026-05-29 00:33:39', 'DAILY', 'deepseek-v4-pro', '0', '2026-05-29 00:33:46');

-- ----------------------------
-- Table structure for `animal_logs`
-- ----------------------------
DROP TABLE IF EXISTS `animal_logs`;
CREATE TABLE `animal_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `animal_id` bigint NOT NULL COMMENT '关联动物ID',
  `user_id` bigint DEFAULT NULL COMMENT '记录者ID (可为空，支持游客模式)',
  `location` point NOT NULL /*!80003 SRID 4326 */ COMMENT '地理坐标(经纬度)',
  `behavior_tag` enum('EATING','SLEEPING','PLAYING','SUNBATHING','WALKING','OTHER') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '行为标记',
  `photo_url` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '现场照片路径',
  `description` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '补充描述',
  `scene_description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前行为整体画面描述',
  `recorded_at` datetime NOT NULL COMMENT '实际拍摄/观察时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据录入时间',
  PRIMARY KEY (`id`),
  SPATIAL KEY `location` (`location`),
  KEY `fk_logs_animal` (`animal_id`),
  KEY `fk_logs_user` (`user_id`),
  CONSTRAINT `animal_logs_ibfk_1` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`),
  CONSTRAINT `fk_logs_animal` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_logs
-- ----------------------------
INSERT INTO `animal_logs` VALUES ('10', '12', null, 0xE6100000010100000042AF3F89CFDC5D406D72F8A413013A40, 'EATING', '/images/Cat-12/Cat-12_20260528_213957.jpg', '橙色毛发，全身均匀，耳朵完整，眼神略显疲惫，胡须清晰', '2026-05-28 19:39:57', '2026-05-28 21:39:57');
INSERT INTO `animal_logs` VALUES ('11', '12', null, 0xE61000000101000000D7BE805EB8DC5D402600FF942A013A40, 'EATING', '/images/Cat-12/eating/Cat-12_20260528_214054.jpg', '橙色短毛，面部有深浅相间条纹，耳朵完整，胡须细长，鼻头粉红。', '2026-05-28 21:40:54', '2026-05-28 21:40:54');
INSERT INTO `animal_logs` VALUES ('16', '16', null, 0xE610000001010000002CB81FF0C0DC5D40043BFE0B04013A40, 'PLAYING', '/images/Cat-16/Cat-16_20260529_003338.jpg', '三色毛发，黑、橙、白相间，面部有白色斑块，耳朵完整，眼神明亮', '2026-05-29 00:33:39', '2026-05-29 00:33:38');

-- ----------------------------
-- Table structure for `animal_ratings`
-- ----------------------------
DROP TABLE IF EXISTS `animal_ratings`;
CREATE TABLE `animal_ratings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `animal_id` bigint NOT NULL,
  `appearance_score` int NOT NULL,
  `clinginess_score` int NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `temper_score` int NOT NULL,
  `user_id` bigint DEFAULT NULL,
  `visibility_score` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ratings_animal` (`animal_id`),
  KEY `idx_ratings_user` (`user_id`),
  CONSTRAINT `fk_ratings_animal` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ratings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_ratings
-- ----------------------------

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
  PRIMARY KEY (`id`),
  KEY `idx_feedback_animal` (`animal_id`),
  CONSTRAINT `fk_feedback_animal` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of prediction_feedbacks
-- ----------------------------
INSERT INTO `prediction_feedbacks` VALUES ('4', '它最近这个点两次都在埋头干饭，看来现在大', '12', 'CONFIRMED', '它最近这个点两次都在埋头干饭，看来现在大', '2026-05-28 23:04:49.082922');

-- ----------------------------
-- Table structure for `users`
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `guest_device_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `salt` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `username` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_dbo9ce5sf2rbnj8ur756eewjv` (`guest_device_id`),
  UNIQUE KEY `UK_r43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES ('1', '2026-05-28 03:53:38.580044', 'device_z7os9sc38k_1779911618545', '游客1', null, 'GUEST', null, null);
INSERT INTO `users` VALUES ('2', '2026-05-28 16:37:55.274490', 'device_g7nr5poiu3j_1779953252419', '游客2', null, 'GUEST', null, null);
INSERT INTO `users` VALUES ('3', '2026-05-28 19:33:29.011378', null, '超级管理员', 'aL71D1M+67exPuWJoMRV193R59wBGzRfsrJSkTC7QXE=', 'ADMIN', 'RlwdSL+UufLRJIYVmG/OpA==', 'admin');
