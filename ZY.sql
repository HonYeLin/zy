/*
Navicat MySQL Data Transfer

Source Server         : py
Source Server Version : 80040
Source Host           : localhost:3306
Source Database       : campus_paw_track

Target Server Type    : MYSQL
Target Server Version : 80040
File Encoding         : 65001

Date: 2026-05-28 12:19:15
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
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animals
-- ----------------------------
INSERT INTO `animals` VALUES ('9', '大橙', 'Cat', 'Cat-9', '2026-05-27 22:33:12', '橙色短毛，全身均匀橙色，耳朵完整，眼睛微眯，胡须清晰', '/images/Cat-10_20260527_223414.jpg', '大橙是一只全身橙黄、总是眯着眼、胡须倍儿精神的小猫。作为校级睡眠艺术家，TA最爱在深夜的校园角落呼呼大睡，主打一个随遇而安。只要有地儿躺就是TA的专属休息室，真是将“大橘为重”贯彻到底的慵懒小胖。');
INSERT INTO `animals` VALUES ('11', '三花', 'Cat', 'Cat-11', '2026-05-28 01:16:44', '白底黑棕相间毛发，头部有黑色斑块，耳朵完整，尾巴卷曲', '/images/Cat-11/Cat-11_20260528_011644.jpg', 'TA是一只白底黑棕相间的小猫，头顶黑斑，尾巴微卷，双耳完好。TA平生最爱随时随地开启省电模式，深夜常在校园角落里深度昏迷，能安稳睡成这样，真是一位雷打不动的学术圈特困生。');

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
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_comments
-- ----------------------------
INSERT INTO `animal_comments` VALUES ('1', '11', '123', '2026-05-28 03:41:00.387956', '1', null, '游客');
INSERT INTO `animal_comments` VALUES ('2', '11', '好懒', '2026-05-28 03:41:11.012653', '2', null, '游客');
INSERT INTO `animal_comments` VALUES ('3', '11', '6', '2026-05-28 03:47:48.488875', '0', null, '游客1');
INSERT INTO `animal_comments` VALUES ('4', '11', '65', '2026-05-28 03:47:51.701257', '0', null, '游客1');
INSERT INTO `animal_comments` VALUES ('5', '11', '这是一条优化后数据库的测试评论！', '2026-05-28 04:00:24.655003', '0', '1', '游客1');

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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_life_narratives
-- ----------------------------
INSERT INTO `animal_life_narratives` VALUES ('2', '9', '大橙现在正趴在路灯下的石椅上打盹呢，它微微眯着眼，胡须偶尔抖动一下。傍晚那会儿它还在食堂门口熟练地讨要小鱼干，现在估计是吃饱喝足，在这儿吹着晚风做着关于罐头的美梦吧。', '2026-05-27 20:33:13', '2026-05-27 20:33:13', 'DAILY', 'gemini-flash-latest', '0', '2026-05-27 22:33:21');
INSERT INTO `animal_life_narratives` VALUES ('3', '9', '夜深人静的大橙正蜷缩在暖和的廊道角落里睡觉，粉红的小鼻头随着呼吸微微颤动。看它舒展的胡须和放松的姿态，大概是傍晚巡视完领地又和学生们讨到了不少零食，累了一整天，此刻正做着满是小鱼干的美梦呢。', '2026-05-27 22:34:14', '2026-05-27 22:34:14', 'DAILY', 'gemini-flash-latest', '0', '2026-05-27 22:34:21');
INSERT INTO `animal_life_narratives` VALUES ('4', '11', '夜深了，有着黑斑小脑瓜和卷曲尾巴的三花正缩成一团甜甜地睡着，作为校园新面孔，它白天可能在灌木丛里巡视了很久，现在终于能放下防备，在安静的深夜里枕着满天星光做个关于小鱼干的美梦啦。', '2026-05-27 23:16:44', '2026-05-27 23:16:44', 'DAILY', 'gemini-flash-latest', '0', '2026-05-28 01:16:50');

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
  `recorded_at` datetime NOT NULL COMMENT '实际拍摄/观察时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据录入时间',
  PRIMARY KEY (`id`),
  SPATIAL KEY `location` (`location`),
  KEY `fk_logs_animal` (`animal_id`),
  KEY `fk_logs_user` (`user_id`),
  CONSTRAINT `animal_logs_ibfk_1` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`),
  CONSTRAINT `fk_logs_animal` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_logs
-- ----------------------------
INSERT INTO `animal_logs` VALUES ('7', '9', null, 0xE610000001010000006BBA9EE8BADC5D40DDB1D82615013A40, 'SLEEPING', '/images/Cat-9_20260527_223312.jpg', '橙色短毛，全身均匀橙色，耳朵完整，眼睛微眯，胡须清晰', '2026-05-27 20:33:13', '2026-05-27 22:33:12');
INSERT INTO `animal_logs` VALUES ('8', '9', null, 0xE610000001010000006FF59CF4BEDC5D40E695EB6D33013A40, 'SLEEPING', '/images/Cat-10_20260527_223414.jpg', '橙色毛发，面部有深色斑纹，耳朵完整，胡须细长，鼻头粉红。', '2026-05-27 22:34:14', '2026-05-27 22:34:14');
INSERT INTO `animal_logs` VALUES ('9', '11', null, 0xE61000000101000000FBCBEEC9C3DC5D406A6803B001013A40, 'SLEEPING', '/images/Cat-11/Cat-11_20260528_011644.jpg', '白底黑棕相间毛发，头部有黑色斑块，耳朵完整，尾巴卷曲', '2026-05-27 23:16:44', '2026-05-28 01:16:44');

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
INSERT INTO `animal_ratings` VALUES ('1', '11', '10', '10', '2026-05-28 04:00:24.672469', '9', '1', '8');
INSERT INTO `animal_ratings` VALUES ('2', '11', '10', '10', '2026-05-28 09:12:50.911122', '9', '1', '8');

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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of prediction_feedbacks
-- ----------------------------
INSERT INTO `prediction_feedbacks` VALUES ('2', '睡觉/休息', '9', 'CONFIRMED', '睡觉/休息', '2026-05-27 23:43:06.053176');

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
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES ('1', '2026-05-28 03:53:38.580044', 'device_z7os9sc38k_1779911618545', '游客1', null, 'GUEST', null, null);
