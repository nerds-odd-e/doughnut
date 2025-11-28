/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `assessment_attempt`
--

DROP TABLE IF EXISTS `assessment_attempt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assessment_attempt` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `notebook_id` int unsigned NOT NULL,
  `submitted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `answers_total` int unsigned DEFAULT NULL,
  `answers_correct` int unsigned DEFAULT NULL,
  `certificate_expires_at` timestamp NOT NULL DEFAULT ((now() + interval 1 year)),
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `notebook_id` (`notebook_id`),
  CONSTRAINT `assessment_attempt_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `assessment_attempt_ibfk_2` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `assessment_question_instance`
--

DROP TABLE IF EXISTS `assessment_question_instance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `assessment_question_instance` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `assessment_attempt_id` int unsigned NOT NULL,
  `quiz_answer_id` int unsigned DEFAULT NULL,
  `predefined_question_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `assessment_attempt_id` (`assessment_attempt_id`),
  KEY `fk_assess_question_quiz_answer` (`quiz_answer_id`),
  KEY `fk_assess_question_predefined` (`predefined_question_id`),
  CONSTRAINT `assessment_question_instance_ibfk_1` FOREIGN KEY (`assessment_attempt_id`) REFERENCES `assessment_attempt` (`id`),
  CONSTRAINT `fk_assess_question_predefined` FOREIGN KEY (`predefined_question_id`) REFERENCES `predefined_question` (`id`),
  CONSTRAINT `fk_assess_question_quiz_answer` FOREIGN KEY (`quiz_answer_id`) REFERENCES `quiz_answer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `attachment_blob`
--

DROP TABLE IF EXISTS `attachment_blob`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attachment_blob` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `data` longblob,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bazaar_notebook`
--

DROP TABLE IF EXISTS `bazaar_notebook`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bazaar_notebook` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_bazaar_notebook_notebook_id` (`notebook_id`),
  CONSTRAINT `fk_bazaar_notebook_notebook_id` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `certificate`
--

DROP TABLE IF EXISTS `certificate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certificate` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `notebook_id` int unsigned NOT NULL,
  `start_date` timestamp NOT NULL,
  `expiry_date` timestamp NOT NULL,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  KEY `notebook_id` (`notebook_id`),
  CONSTRAINT `certificate_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `certificate_ibfk_2` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `circle`
--

DROP TABLE IF EXISTS `circle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `circle` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `invitation_code` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `invitation_code` (`invitation_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `circle_user`
--

DROP TABLE IF EXISTS `circle_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `circle_user` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `circle_id` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `circle_user_circle_user_id` (`circle_id`,`user_id`),
  KEY `fk_circle_user_user_id` (`user_id`),
  CONSTRAINT `fk_circle_user_circle_id` FOREIGN KEY (`circle_id`) REFERENCES `circle` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_circle_user_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conversation`
--

DROP TABLE IF EXISTS `conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `subject_ownership_id` int unsigned NOT NULL,
  `conversation_initiator_id` int unsigned NOT NULL,
  `assessment_question_instance_id` int unsigned DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `note_id` int unsigned DEFAULT NULL,
  `recall_prompt_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `subject_ownership_id` (`subject_ownership_id`),
  KEY `conversation_initiator_id` (`conversation_initiator_id`),
  KEY `fk_conversation_assessment_question` (`assessment_question_instance_id`),
  KEY `note_id` (`note_id`),
  KEY `review_question_instance_id` (`recall_prompt_id`),
  CONSTRAINT `conversation_ibfk_1` FOREIGN KEY (`subject_ownership_id`) REFERENCES `ownership` (`id`),
  CONSTRAINT `conversation_ibfk_2` FOREIGN KEY (`conversation_initiator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `conversation_ibfk_3` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`),
  CONSTRAINT `conversation_ibfk_4` FOREIGN KEY (`recall_prompt_id`) REFERENCES `recall_prompt` (`id`),
  CONSTRAINT `fk_conversation_assessment_question` FOREIGN KEY (`assessment_question_instance_id`) REFERENCES `assessment_question_instance` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `conversation_message`
--

DROP TABLE IF EXISTS `conversation_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `conversation_message` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `conversation_id` int unsigned NOT NULL,
  `message` text NOT NULL,
  `created_at` timestamp(3) NULL DEFAULT NULL,
  `sender` int unsigned DEFAULT NULL,
  `read_by_receiver` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_conversation_id` (`conversation_id`),
  KEY `fk_conversation_detail_initiator_id` (`sender`),
  CONSTRAINT `fk_conversation_detail_initiator_id` FOREIGN KEY (`sender`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_conversation_id` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `failure_report`
--

DROP TABLE IF EXISTS `failure_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `failure_report` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `error_name` varchar(1000) NOT NULL,
  `error_detail` text NOT NULL,
  `created_datetime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `issue_number` decimal(10,0) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `global_settings`
--

DROP TABLE IF EXISTS `global_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `global_settings` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `key_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `image`
--

DROP TABLE IF EXISTS `image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `image` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `attachment_blob_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_image_image_blob_id` (`attachment_blob_id`),
  KEY `fk_image_user_id` (`user_id`),
  CONSTRAINT `fk_image_image_blob_id` FOREIGN KEY (`attachment_blob_id`) REFERENCES `attachment_blob` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_image_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `memory_tracker`
--

DROP TABLE IF EXISTS `memory_tracker`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `memory_tracker` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `repetition_count` int NOT NULL DEFAULT '100',
  `forgetting_curve_index` int NOT NULL DEFAULT '100',
  `last_recalled_at` timestamp NOT NULL,
  `assimilated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `next_recall_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `removed_from_tracking` tinyint NOT NULL DEFAULT '0',
  `note_id` int unsigned DEFAULT NULL,
  `spelling` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_note_spelling` (`user_id`,`note_id`,`spelling`),
  KEY `last_reviewed_at` (`last_recalled_at`),
  KEY `FK_review_point_user_id` (`user_id`),
  KEY `review_point_fk_note_id` (`note_id`),
  CONSTRAINT `memory_tracker_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
  CONSTRAINT `review_point_fk_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note`
--

DROP TABLE IF EXISTS `note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `note` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `picture_url` varchar(1024) DEFAULT NULL,
  `url` varchar(1024) DEFAULT NULL,
  `sibling_order` bigint NOT NULL DEFAULT '1',
  `image_id` int unsigned DEFAULT NULL,
  `use_parent_picture` tinyint NOT NULL DEFAULT '0',
  `picture_mask` varchar(1024) DEFAULT NULL,
  `notebook_id` int unsigned DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp(3) NULL DEFAULT NULL,
  `wikidata_id` varchar(50) DEFAULT NULL,
  `topic_constructor` varchar(150) DEFAULT NULL,
  `description` text,
  `target_note_id` int unsigned DEFAULT NULL,
  `construct_type_id` int DEFAULT NULL,
  `parent_id` int unsigned DEFAULT NULL,
  `remember_spelling` tinyint NOT NULL DEFAULT '0',
  `skip_memory_tracking` tinyint NOT NULL DEFAULT '0',
  `level` tinyint NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `creator_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `parent_id` (`sibling_order`),
  KEY `fk_note_image_id` (`image_id`),
  KEY `fk_note_notebook_id` (`notebook_id`),
  KEY `target_note_id` (`target_note_id`),
  KEY `parent_id_2` (`parent_id`),
  KEY `FK_note_creator_id` (`creator_id`),
  CONSTRAINT `FK_note_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_note_image_id` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_note_notebook_id` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON UPDATE RESTRICT,
  CONSTRAINT `note_ibfk_1` FOREIGN KEY (`target_note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `note_ibfk_2` FOREIGN KEY (`parent_id`) REFERENCES `note` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_accessory`
--

DROP TABLE IF EXISTS `note_accessory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `note_accessory` (
  `id` int NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned DEFAULT NULL,
  `url` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_url` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `use_parent_image` tinyint NOT NULL DEFAULT '0',
  `image_mask` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `note_id` (`note_id`),
  KEY `image_id` (`image_id`),
  CONSTRAINT `note_accessory_ibfk_1` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`),
  CONSTRAINT `note_accessory_ibfk_2` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_embeddings`
--

DROP TABLE IF EXISTS `note_embeddings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `note_embeddings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `note_id` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `embedding_raw` varbinary(6144) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_note_embeddings_note_id` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notebook`
--

DROP TABLE IF EXISTS `notebook`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notebook` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `ownership_id` int unsigned NOT NULL,
  `creator_id` int unsigned NOT NULL,
  `skip_memory_tracking_entirely` tinyint NOT NULL DEFAULT '0',
  `deleted_at` timestamp NULL DEFAULT NULL,
  `number_of_questions_in_assessment` int unsigned DEFAULT '5',
  `certificate_expiry` varchar(64) DEFAULT 'P1Y',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_notes_book_creator_id` (`creator_id`),
  KEY `fk_notes_book_ownership_id` (`ownership_id`),
  CONSTRAINT `fk_notes_book_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_notes_book_ownership_id` FOREIGN KEY (`ownership_id`) REFERENCES `ownership` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notebook_ai_assistant`
--

DROP TABLE IF EXISTS `notebook_ai_assistant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notebook_ai_assistant` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int NOT NULL,
  `additional_instructions_to_ai` text COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `notebook_id` (`notebook_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notebook_certificate_approval`
--

DROP TABLE IF EXISTS `notebook_certificate_approval`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notebook_certificate_approval` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int unsigned NOT NULL,
  `last_approval_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `notebook_id` (`notebook_id`),
  CONSTRAINT `notebook_certificate_approval_ibfk_1` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notebook_head_note`
--

DROP TABLE IF EXISTS `notebook_head_note`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notebook_head_note` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int unsigned NOT NULL,
  `head_note_id` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_notebook_head_note_head_note_id` (`head_note_id`),
  KEY `fk_notebook_head_note_notebook_id` (`notebook_id`),
  CONSTRAINT `fk_notebook_head_note_head_note_id` FOREIGN KEY (`head_note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_notebook_head_note_notebook_id` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ownership`
--

DROP TABLE IF EXISTS `ownership`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ownership` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned DEFAULT NULL,
  `circle_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ownership_user_id` (`user_id`),
  UNIQUE KEY `ownership_circle_id` (`circle_id`),
  CONSTRAINT `fk_ownership_circle_id` FOREIGN KEY (`circle_id`) REFERENCES `circle` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_ownership_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `predefined_question`
--

DROP TABLE IF EXISTS `predefined_question`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `predefined_question` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `correct_answer_index` tinyint DEFAULT NULL,
  `note_id` int unsigned DEFAULT NULL,
  `is_approved` tinyint(1) DEFAULT '0',
  `raw_json_question` text COLLATE utf8mb4_unicode_ci,
  `image_url` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_mask` varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_contested` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `quiz_question_fk_note_id` (`note_id`),
  CONSTRAINT `quiz_question_fk_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `quiz_answer`
--

DROP TABLE IF EXISTS `quiz_answer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `quiz_answer` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `result` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `choice_index` tinyint DEFAULT NULL,
  `correct` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recall_prompt`
--

DROP TABLE IF EXISTS `recall_prompt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recall_prompt` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `predefined_question_id` int unsigned DEFAULT NULL,
  `quiz_answer_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_question_and_answer` (`predefined_question_id`),
  KEY `fk_quiz_answer` (`quiz_answer_id`),
  CONSTRAINT `fk_question_and_answer` FOREIGN KEY (`predefined_question_id`) REFERENCES `predefined_question` (`id`),
  CONSTRAINT `fk_quiz_answer` FOREIGN KEY (`quiz_answer_id`) REFERENCES `quiz_answer` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recall_prompt_generation_status`
--

DROP TABLE IF EXISTS `recall_prompt_generation_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recall_prompt_generation_status` (
  `memory_tracker_id` int unsigned NOT NULL,
  `last_attempt_time` timestamp NULL DEFAULT NULL,
  `attempt_count` int NOT NULL DEFAULT '0',
  `successful` tinyint(1) NOT NULL DEFAULT '0',
  `error_message` text COLLATE utf8mb4_unicode_ci,
  `in_progress` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`memory_tracker_id`),
  KEY `idx_rpgs_status` (`successful`,`attempt_count`,`in_progress`),
  KEY `idx_rpgs_last_attempt` (`last_attempt_time`),
  CONSTRAINT `recall_prompt_generation_status_ibfk_1` FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `subscription`
--

DROP TABLE IF EXISTS `subscription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `subscription` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `daily_target_of_new_notes` int unsigned NOT NULL DEFAULT '5',
  `notebook_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `subscription_user_notebook` (`user_id`,`notebook_id`),
  KEY `fk_subscription_notebook_id` (`notebook_id`),
  CONSTRAINT `fk_subscription_notebook_id` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON UPDATE RESTRICT,
  CONSTRAINT `fk_subscription_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `suggested_question_for_fine_tuning`
--

DROP TABLE IF EXISTS `suggested_question_for_fine_tuning`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `suggested_question_for_fine_tuning` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `preserved_question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `preserved_note_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `approved` tinyint(1) NOT NULL DEFAULT '0',
  `is_positive_feedback` tinyint(1) NOT NULL DEFAULT '0',
  `real_correct_answers` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `fk_user_id` (`user_id`),
  CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `external_identifier` varchar(100) NOT NULL,
  `daily_assimilation_count` int unsigned NOT NULL DEFAULT '10',
  `space_intervals` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_external_identifier` (`external_identifier`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `user_token`
--

DROP TABLE IF EXISTS `user_token`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_token` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `label` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_used_at` timestamp(3) NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_token_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
