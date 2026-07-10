-- Full application schema (CREATE statements only, no data). flyway_schema_history omitted.

-- MySQL dump 10.13  Distrib 8.4.9, for macos26.5 (arm64)
--
-- Host: 127.0.0.1    Database: doughnut_test
-- ------------------------------------------------------
-- Server version	8.4.9

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
-- Table structure for table `admin_data_migration_progress`
--

DROP TABLE IF EXISTS `admin_data_migration_progress`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_data_migration_progress` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `step_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_count` int NOT NULL DEFAULT '0',
  `processed_count` int NOT NULL DEFAULT '0',
  `last_processed_note_id` int unsigned DEFAULT NULL,
  `last_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_admin_data_migration_progress_step` (`step_name`),
  KEY `idx_admin_data_migration_progress_status` (`status`),
  KEY `fk_admin_data_migration_progress_last_note` (`last_processed_note_id`),
  CONSTRAINT `fk_admin_data_migration_progress_last_note` FOREIGN KEY (`last_processed_note_id`) REFERENCES `note` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=841 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=5373 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=1492 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book`
--

DROP TABLE IF EXISTS `book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int unsigned NOT NULL,
  `book_name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `format` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_file_ref` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_book_notebook_id` (`notebook_id`),
  KEY `idx_book_notebook_id` (`notebook_id`),
  CONSTRAINT `fk_book_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=4609 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book_block`
--

DROP TABLE IF EXISTS `book_block`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_block` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `book_id` int unsigned NOT NULL,
  `structural_title` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `layout_sequence` int NOT NULL,
  `depth` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_book_block_book_id` (`book_id`),
  CONSTRAINT `fk_book_block_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=8977 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book_block_reading_record`
--

DROP TABLE IF EXISTS `book_block_reading_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_block_reading_record` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `book_block_id` int unsigned NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `completed_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_book_block_reading_user_block` (`user_id`,`book_block_id`),
  KEY `idx_book_block_reading_block` (`book_block_id`),
  CONSTRAINT `fk_book_block_reading_block` FOREIGN KEY (`book_block_id`) REFERENCES `book_block` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_book_block_reading_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=337 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book_content_block`
--

DROP TABLE IF EXISTS `book_content_block`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_content_block` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `book_block_id` int unsigned NOT NULL,
  `sibling_order` int NOT NULL,
  `type` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `page_idx` int DEFAULT NULL,
  `raw_data` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_bcb_book_block` (`book_block_id`),
  CONSTRAINT `fk_bcb_book_block` FOREIGN KEY (`book_block_id`) REFERENCES `book_block` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2689 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `book_user_last_read_position`
--

DROP TABLE IF EXISTS `book_user_last_read_position`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `book_user_last_read_position` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `book_id` int unsigned NOT NULL,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `selected_book_block_id` int unsigned DEFAULT NULL,
  `reading_position_locator_json` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_book_user_last_read` (`user_id`,`book_id`),
  KEY `idx_book_user_last_read_book` (`book_id`),
  KEY `fk_book_user_last_read_selected_block` (`selected_book_block_id`),
  CONSTRAINT `fk_book_user_last_read_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_book_user_last_read_selected_block` FOREIGN KEY (`selected_book_block_id`) REFERENCES `book_block` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_book_user_last_read_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=625 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `circle`
--

DROP TABLE IF EXISTS `circle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `circle` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `invitation_code` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `invitation_code` (`invitation_code`)
) ENGINE=InnoDB AUTO_INCREMENT=917 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=629 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `note_id` int unsigned DEFAULT NULL,
  `recall_prompt_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `subject_ownership_id` (`subject_ownership_id`),
  KEY `conversation_initiator_id` (`conversation_initiator_id`),
  KEY `note_id` (`note_id`),
  KEY `recall_prompt_id` (`recall_prompt_id`),
  CONSTRAINT `conversation_ibfk_1` FOREIGN KEY (`subject_ownership_id`) REFERENCES `ownership` (`id`),
  CONSTRAINT `conversation_ibfk_2` FOREIGN KEY (`conversation_initiator_id`) REFERENCES `user` (`id`),
  CONSTRAINT `conversation_ibfk_3` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`),
  CONSTRAINT `conversation_ibfk_4` FOREIGN KEY (`recall_prompt_id`) REFERENCES `recall_prompt` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4371 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=1201 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=529 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;


--
-- Table structure for table `folder`
--

DROP TABLE IF EXISTS `folder`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `folder` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int unsigned NOT NULL,
  `parent_folder_id` int unsigned DEFAULT NULL,
  `name` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `index_content` mediumtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_folder_notebook_parent_name` (`notebook_id`,(ifnull(`parent_folder_id`,0)),`name`),
  KEY `idx_folder_notebook_id` (`notebook_id`),
  KEY `idx_folder_parent_folder_id` (`parent_folder_id`),
  CONSTRAINT `fk_folder_notebook` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_folder_parent` FOREIGN KEY (`parent_folder_id`) REFERENCES `folder` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11200 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=2504 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `note_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_image_image_blob_id` (`attachment_blob_id`),
  KEY `fk_image_user_id` (`user_id`),
  KEY `fk_image_note_id` (`note_id`),
  CONSTRAINT `fk_image_image_blob_id` FOREIGN KEY (`attachment_blob_id`) REFERENCES `attachment_blob` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_image_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_image_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=429 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `recall_count` int NOT NULL DEFAULT '100',
  `forgetting_curve_index` float NOT NULL DEFAULT '100',
  `last_recalled_at` timestamp NOT NULL,
  `assimilated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `next_recall_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `removed_from_tracking` tinyint NOT NULL DEFAULT '0',
  `note_id` int unsigned DEFAULT NULL,
  `spelling` tinyint(1) NOT NULL DEFAULT '0',
  `deleted_at` timestamp NULL DEFAULT NULL,
  `property_key` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_note_spelling_active` (`user_id`,`note_id`,`spelling`,`property_key`,(if((`deleted_at` is null),1,NULL))),
  KEY `last_recalled_at` (`last_recalled_at`),
  KEY `FK_memory_tracker_user_id` (`user_id`),
  KEY `review_point_fk_note_id` (`note_id`),
  CONSTRAINT `memory_tracker_fk_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `memory_tracker_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=17858 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `image_id` int unsigned DEFAULT NULL,
  `notebook_id` int unsigned DEFAULT NULL,
  `folder_id` int unsigned DEFAULT NULL,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted_at` timestamp(3) NULL DEFAULT NULL,
  `title` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `content` mediumtext,
  `remember_spelling` tinyint NOT NULL DEFAULT '0',
  `skip_memory_tracking` tinyint NOT NULL DEFAULT '0',
  `level` tinyint NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_notebook_folder_title` ((ifnull(`notebook_id`,0)),(ifnull(`folder_id`,0)),(lower(`title`))),
  KEY `fk_note_image_id` (`image_id`),
  KEY `fk_note_notebook_id` (`notebook_id`),
  KEY `idx_note_folder_id` (`folder_id`),
  KEY `idx_note_structural_peer` (`notebook_id`,`folder_id`,`deleted_at`,`id`),
  KEY `idx_note_notebook_id_title` (`notebook_id`,(lower(`title`))),
  CONSTRAINT `fk_note_folder` FOREIGN KEY (`folder_id`) REFERENCES `folder` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_note_image_id` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_note_notebook_id` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=85359 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_alias_index`
--

DROP TABLE IF EXISTS `note_alias_index`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `note_alias_index` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned NOT NULL,
  `alias_display` varchar(767) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `alias_lookup_key` varchar(767) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_note_alias_index_note_lookup` (`note_id`,`alias_lookup_key`),
  KEY `idx_note_alias_index_note_id` (`note_id`),
  KEY `idx_note_alias_index_lookup_note` (`alias_lookup_key`,`note_id`),
  CONSTRAINT `fk_note_alias_index_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7493 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_creator`
--

DROP TABLE IF EXISTS `note_creator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `note_creator` (
  `note_id` int unsigned NOT NULL,
  `user_id` int unsigned NOT NULL,
  PRIMARY KEY (`note_id`),
  UNIQUE KEY `uk_note_creator_note_user` (`note_id`,`user_id`),
  KEY `fk_note_creator_user` (`user_id`),
  CONSTRAINT `fk_note_creator_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_creator_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=265 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_property_index`
--

DROP TABLE IF EXISTS `note_property_index`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `note_property_index` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned NOT NULL,
  `property_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `item_index` int unsigned NOT NULL DEFAULT '0',
  `target_note_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_note_property_index_note_key_item` (`note_id`,`property_key`,`item_index`),
  KEY `idx_note_property_index_property_key` (`property_key`),
  KEY `idx_note_property_index_target_note` (`target_note_id`),
  KEY `idx_note_property_index_note_key` (`note_id`,`property_key`),
  CONSTRAINT `fk_note_property_index_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_property_index_target_note` FOREIGN KEY (`target_note_id`) REFERENCES `note` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=4217 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `note_wiki_title_cache`
--

DROP TABLE IF EXISTS `note_wiki_title_cache`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `note_wiki_title_cache` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned NOT NULL,
  `target_note_id` int unsigned NOT NULL,
  `link_text` varchar(767) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_note_wiki_title_cache_note_link` (`note_id`,`link_text`),
  KEY `idx_note_wiki_title_cache_note_id` (`note_id`),
  KEY `idx_note_wiki_title_cache_target_note_id` (`target_note_id`),
  KEY `idx_note_wiki_title_cache_target_note_id_note_id` (`target_note_id`,`note_id`,`id`),
  CONSTRAINT `fk_note_wiki_title_cache_note` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_note_wiki_title_cache_target` FOREIGN KEY (`target_note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=10213 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `notebook_group_id` int unsigned DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `name` varchar(150) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `index_content` mediumtext,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notebook_ownership_name` (`ownership_id`,`name`),
  KEY `fk_notes_book_creator_id` (`creator_id`),
  KEY `fk_notes_book_ownership_id` (`ownership_id`),
  KEY `fk_notebook_notebook_group` (`notebook_group_id`),
  CONSTRAINT `fk_notebook_notebook_group` FOREIGN KEY (`notebook_group_id`) REFERENCES `notebook_group` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_notes_book_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_notes_book_ownership_id` FOREIGN KEY (`ownership_id`) REFERENCES `ownership` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_notebook_name_nonempty` CHECK ((char_length(trim(`name`)) > 0))
) ENGINE=InnoDB AUTO_INCREMENT=66871 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `notebook_group`
--

DROP TABLE IF EXISTS `notebook_group`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notebook_group` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `ownership_id` int unsigned NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_notebook_group_ownership` (`ownership_id`),
  CONSTRAINT `fk_notebook_group_ownership` FOREIGN KEY (`ownership_id`) REFERENCES `ownership` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1105 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=89815 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `raw_json_question` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `image_url` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `image_mask` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_contested` tinyint(1) NOT NULL DEFAULT '0',
  `context_seed` bigint DEFAULT NULL,
  `tested_focus` text COLLATE utf8mb4_unicode_ci,
  `validation_rationale` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `quiz_question_fk_note_id` (`note_id`),
  CONSTRAINT `quiz_question_fk_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7596 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `question_generation_batch`
--

DROP TABLE IF EXISTS `question_generation_batch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_generation_batch` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `planned_at` timestamp(3) NOT NULL,
  `openai_input_file_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `openai_batch_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `submitted_at` timestamp(3) NULL DEFAULT NULL,
  `openai_output_file_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `openai_error_file_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `output_collected_at` timestamp(3) NULL DEFAULT NULL,
  `imported_at` timestamp(3) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_question_generation_batch_user_status` (`user_id`,`status`),
  CONSTRAINT `fk_question_generation_batch_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3601 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `question_generation_batch_maintenance_run`
--

DROP TABLE IF EXISTS `question_generation_batch_maintenance_run`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_generation_batch_maintenance_run` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `trigger_source` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `started_at` timestamp(3) NOT NULL,
  `finished_at` timestamp(3) NULL DEFAULT NULL,
  `error` text COLLATE utf8mb4_unicode_ci,
  `considered_user_count` int DEFAULT NULL,
  `submitted_count` int DEFAULT NULL,
  `failed_count` int DEFAULT NULL,
  `skipped_count` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_question_generation_batch_maintenance_run_trigger_started` (`trigger_source`,`started_at`)
) ENGINE=InnoDB AUTO_INCREMENT=97 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `question_generation_batch_request`
--

DROP TABLE IF EXISTS `question_generation_batch_request`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_generation_batch_request` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `batch_id` int unsigned NOT NULL,
  `memory_tracker_id` int unsigned NOT NULL,
  `custom_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `context_seed` bigint NOT NULL,
  `status` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `raw_success_payload` longtext COLLATE utf8mb4_unicode_ci,
  `raw_error_payload` longtext COLLATE utf8mb4_unicode_ci,
  `error_detail` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_question_generation_batch_request_custom_id` (`custom_id`),
  UNIQUE KEY `uq_question_generation_batch_request_batch_tracker` (`batch_id`,`memory_tracker_id`),
  KEY `fk_question_generation_batch_request_tracker` (`memory_tracker_id`),
  CONSTRAINT `fk_question_generation_batch_request_batch` FOREIGN KEY (`batch_id`) REFERENCES `question_generation_batch` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_question_generation_batch_request_tracker` FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3361 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
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
  `thinking_time_ms` int DEFAULT NULL,
  `spelling_answer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_quiz_answer_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=5038 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `recall_prompt`
--

DROP TABLE IF EXISTS `recall_prompt`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `recall_prompt` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `memory_tracker_id` int unsigned NOT NULL,
  `question_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `predefined_question_id` int unsigned DEFAULT NULL,
  `quiz_answer_id` int unsigned DEFAULT NULL,
  `created_at` timestamp(3) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_recall_prompt_memory_tracker` (`memory_tracker_id`),
  KEY `fk_recall_prompt_predefined_question` (`predefined_question_id`),
  KEY `fk_recall_prompt_quiz_answer` (`quiz_answer_id`),
  CONSTRAINT `fk_recall_prompt_memory_tracker` FOREIGN KEY (`memory_tracker_id`) REFERENCES `memory_tracker` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_recall_prompt_predefined_question` FOREIGN KEY (`predefined_question_id`) REFERENCES `predefined_question` (`id`),
  CONSTRAINT `fk_recall_prompt_quiz_answer` FOREIGN KEY (`quiz_answer_id`) REFERENCES `quiz_answer` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7699 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `shedlock`
--

DROP TABLE IF EXISTS `shedlock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `shedlock` (
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `lock_until` timestamp(3) NOT NULL,
  `locked_at` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `locked_by` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`name`)
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
  `notebook_group_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `subscription_user_notebook` (`user_id`,`notebook_id`),
  KEY `fk_subscription_notebook_id` (`notebook_id`),
  KEY `fk_subscription_notebook_group` (`notebook_group_id`),
  CONSTRAINT `fk_subscription_notebook_group` FOREIGN KEY (`notebook_group_id`) REFERENCES `notebook_group` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_subscription_notebook_id` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON UPDATE RESTRICT,
  CONSTRAINT `fk_subscription_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1437 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB AUTO_INCREMENT=88905 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `label` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_used_at` timestamp(3) NULL DEFAULT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `token` (`token`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_token_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=337 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-10 12:10:27
