/*
Navicat MySQL Data Transfer

Source Server         : py
Source Server Version : 80040
Source Host           : localhost:3306
Source Database       : campus_paw_track

Target Server Type    : MYSQL
Target Server Version : 80040
File Encoding         : 65001

Date: 2026-05-29 14:33:28
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
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animals
-- ----------------------------
INSERT INTO `animals` VALUES ('12', '大橙', 'Cat', 'Cat-12', '2026-05-28 21:39:57', '它全身覆盖着均匀的橙色短毛，面部有深浅相间的条纹，耳朵完整。在略显疲惫的眼神下，粉红色的鼻头分外醒目，嘴角还生着细长而清晰的胡须。', '/images/Cat-12/eating/Cat-12_20260528_214054.jpg', 'TA是一只长着粉红鼻头、眼神略显疲惫的橙色条纹猫。TA最大的爱好就是干饭，常在深夜出没于校园投喂点大吃大喝。虽然顶着一张高冷打工人的疲惫脸，却有着最敬业的干饭魂，真是一位深夜食堂的VIP食客。');
INSERT INTO `animals` VALUES ('18', '三花', 'Cat', 'Cat-18', '2026-05-29 02:56:34', '这只校园小动物披着白底、缀满黑橙斑块的三色毛发，面部花纹清晰，醒目的白色斑块衬出俏皮神态。淡黄色眼睛明亮机敏，耳朵完整而灵动，模样可爱至极。', '/images/Cat-18/Cat-18_20260529_025634.jpg', '三花是校园里穿定制皮草的时髦精，白底黑橙斑块像打翻的调色盘，面部留白恰似敷着面膜就出了门。淡黄色眼睛总闪着“我全都要”的机灵劲儿，最爱凌晨散步兼白日补觉，完美践行“猫界时差”。教室墙角、花坛边都可能撞见这位时间管理大师在补美容觉，不愧是用脚步丈量校园的混色小旋风。');

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
  `parent_id` bigint DEFAULT NULL,
  `reply_to_user_nickname` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_comment_animal_created` (`animal_id`,`created_at` DESC),
  KEY `idx_comment_animal_likes` (`animal_id`,`like_count` DESC),
  KEY `idx_comment_user` (`user_id`),
  CONSTRAINT `fk_comments_animal` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_comments_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_comments
-- ----------------------------
INSERT INTO `animal_comments` VALUES ('12', '12', '盛世美颜啊', '2026-05-29 03:36:26.744725', '0', '4', 'h', null, null);
INSERT INTO `animal_comments` VALUES ('15', '12', '不敢苟同', '2026-05-29 14:19:01.009940', '0', '4', 'h', '12', null);

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
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_life_narratives
-- ----------------------------
INSERT INTO `animal_life_narratives` VALUES ('5', '12', '大橙正低头享用着今晚的自助大餐，它那一身匀称的漂亮橘毛和清晰的胡须十分帅气，就是眼神里透着一丝疲惫。刚才它大概是去后山巡视领地了吧，现在终于能下班干饭，毕竟当一整天神气的校园守护者也是体力活呢。', '2026-05-28 19:39:57', '2026-05-28 19:39:57', 'DAILY', 'gemini-flash-latest', '0', '2026-05-28 21:40:03');
INSERT INTO `animal_life_narratives` VALUES ('6', '12', '大橙在两个小时后居然又开启了干饭模式，它那粉红的小鼻头不停耸动，脸上深浅相间的条纹写满了认真。看来刚才下班后的那一顿只是前菜，辛勤巡逻了一整天的橘色警长，高低得再来顿宵夜才能彻底充好电呢。', '2026-05-28 19:39:57', '2026-05-28 21:40:54', 'DAILY', 'gemini-flash-latest', '0', '2026-05-28 21:41:01');
INSERT INTO `animal_life_narratives` VALUES ('14', '18', '午夜时分，三花正沿着石阶缓缓踱步，仿佛在巡视自己静谧的王国。夜色里栏杆和绿植投下斑驳的影子，或许它刚结束一场神秘的校园探险，正踏着猫步回窝，回味刚才草丛里的小小奇遇呢。', '2026-05-29 00:56:35', '2026-05-29 00:56:35', 'DAILY', 'deepseek-v4-pro', '0', '2026-05-29 02:56:39');
INSERT INTO `animal_life_narratives` VALUES ('15', '18', '凌晨快三点，三花终于结束了它的王国巡视，困得直接蜷在地砖上打起了盹。看来两小时前那场石阶上的神秘探险还挺消耗体力，现在它把地砖当成了临时王座，正睡得香甜，大概梦里还在回味草丛里的奇遇呢。', '2026-05-29 00:56:35', '2026-05-29 02:57:47', 'DAILY', 'deepseek-v4-pro', '0', '2026-05-29 02:57:52');
INSERT INTO `animal_life_narratives` VALUES ('16', '18', '凌晨三点多，刚刚还蜷在地砖上打盹的三花，这会儿悄悄挪到了石栏上。大概是觉得高处更有巡视完王国后的成就感，它把自己蜷成一个柔软的花团，在绿树环抱里沉沉睡着。也不知道在梦里，是不是又开启了一场新的草丛大冒险呢。', '2026-05-29 00:56:35', '2026-05-29 03:20:00', 'DAILY', 'deepseek-v4-pro', '0', '2026-05-29 03:20:06');
INSERT INTO `animal_life_narratives` VALUES ('17', '18', '经过一个长长的上午好眠，三花总算从石栏上跳下来开始新一轮领地巡查，这会儿正慢悠悠地踏过午后的台阶，竖着耳朵盘算昨晚那场草丛大冒险的续集该从哪丛灌木开场呢。', '2026-05-29 00:56:35', '2026-05-29 13:42:31', 'DAILY', 'deepseek-v4-pro', '0', '2026-05-29 13:42:36');

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
  `scene_description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前行为整体画面描述',
  `recorded_at` datetime NOT NULL COMMENT '实际拍摄/观察时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '数据录入时间',
  PRIMARY KEY (`id`),
  SPATIAL KEY `location` (`location`),
  KEY `fk_logs_animal` (`animal_id`),
  KEY `fk_logs_user` (`user_id`),
  CONSTRAINT `animal_logs_ibfk_1` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`),
  CONSTRAINT `fk_logs_animal` FOREIGN KEY (`animal_id`) REFERENCES `animals` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_logs
-- ----------------------------
INSERT INTO `animal_logs` VALUES ('10', '12', null, 0xE6100000010100000042AF3F89CFDC5D406D72F8A413013A40, 'EATING', '/images/Cat-12/Cat-12_20260528_213957.jpg', '橙色毛发，全身均匀，耳朵完整，眼神略显疲惫，胡须清晰', null, '2026-05-28 19:39:57', '2026-05-28 21:39:57');
INSERT INTO `animal_logs` VALUES ('11', '12', null, 0xE61000000101000000D7BE805EB8DC5D402600FF942A013A40, 'EATING', '/images/Cat-12/eating/Cat-12_20260528_214054.jpg', '橙色短毛，面部有深浅相间条纹，耳朵完整，胡须细长，鼻头粉红。', null, '2026-05-28 21:40:54', '2026-05-28 21:40:54');
INSERT INTO `animal_logs` VALUES ('19', '18', null, 0xE61000000101000000F18288D4B4DC5D40ECDE8AC404013A40, 'WALKING', '/images/Cat-18/Cat-18_20260529_025634.jpg', '黑、橙、白三色毛发，面部有白色斑块，耳朵完整，眼睛呈淡黄色。', '一只三色猫在石阶上缓缓行走，背景为校园栏杆和绿植。', '2026-05-29 00:56:35', '2026-05-29 02:56:34');
INSERT INTO `animal_logs` VALUES ('20', '18', null, 0xE610000001010000001A19E42EC2DC5D403123BC3D08013A40, 'SLEEPING', '/images/Cat-18/sleeping/Cat-18_20260529_025747.jpg', '三色毛发，黑、橙、白相间，面部白色，耳朵完整，眼神明亮', '夜晚的校园地砖上，一只三花猫安静地蜷坐着，似乎在打盹', '2026-05-29 02:57:47', '2026-05-29 02:57:47');
INSERT INTO `animal_logs` VALUES ('21', '18', null, 0xE61000000101000000A6D24F38BBDC5D40A017EE5C18013A40, 'SLEEPING', '/images/Cat-18/sleeping/Cat-18_20260529_031959.jpg', '三色毛发，白底夹杂黑色与橙色斑块，耳朵完整，面部有清晰花纹', '一只三花猫趴在石栏上，周围绿树环绕，正安静地睡觉', '2026-05-29 03:20:00', '2026-05-29 03:19:59');
INSERT INTO `animal_logs` VALUES ('22', '18', null, 0xE61000000101000000E3A6069ACFDC5D4087C267EBE0003A40, 'WALKING', '/images/Cat-18/walking/Cat-18_20260529_134231.jpg', '黑、橙、白三色毛发，面部有明显斑纹，耳朵完整，眼睛明亮', '一只三色猫在校园台阶上缓慢行走，背景为石栏和绿植', '2026-05-29 13:42:31', '2026-05-29 13:42:31');

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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of animal_ratings
-- ----------------------------
INSERT INTO `animal_ratings` VALUES ('3', '18', '7', '6', '2026-05-29 03:24:21.832034', '6', null, '10');
INSERT INTO `animal_ratings` VALUES ('4', '12', '7', '10', '2026-05-29 03:36:05.645298', '6', '4', '7');

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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES ('1', '2026-05-28 03:53:38.580044', 'device_z7os9sc38k_1779911618545', '游客1', null, 'GUEST', null, null);
INSERT INTO `users` VALUES ('2', '2026-05-28 16:37:55.274490', 'device_g7nr5poiu3j_1779953252419', '游客2', null, 'GUEST', null, null);
INSERT INTO `users` VALUES ('3', '2026-05-28 19:33:29.011378', null, '超级管理员', 'aL71D1M+67exPuWJoMRV193R59wBGzRfsrJSkTC7QXE=', 'ADMIN', 'RlwdSL+UufLRJIYVmG/OpA==', 'admin');
INSERT INTO `users` VALUES ('4', '2026-05-29 03:35:56.487410', null, 'h', 'a867vN+k3Q0BmzPKRAGIOuvDNCZfXijavJs179Jp6cc=', 'USER', 'yXzuHwHwyK5sDf5nFAWraQ==', 'h');
