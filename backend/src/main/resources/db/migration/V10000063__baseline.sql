--
-- Table structure for table `bazaar_notebook`
--

CREATE TABLE `bazaar_notebook` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_bazaar_notebook_notebook_id` (`notebook_id`),
  CONSTRAINT `fk_bazaar_notebook_notebook_id` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=67 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `circle`
--

CREATE TABLE `circle` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `invitation_code` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `invitation_code` (`invitation_code`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `circle_user`
--

CREATE TABLE `circle_user` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `circle_id` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `circle_user_circle_user_id` (`circle_id`,`user_id`),
  KEY `fk_circle_user_user_id` (`user_id`),
  CONSTRAINT `fk_circle_user_circle_id` FOREIGN KEY (`circle_id`) REFERENCES `circle` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_circle_user_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `failure_report`
--

CREATE TABLE `failure_report` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `error_name` varchar(1000) NOT NULL,
  `error_detail` text NOT NULL,
  `created_datetime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `issue_number` decimal(10,0) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `flyway_schema_history`
--

CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `script` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Table structure for table `image`
--

CREATE TABLE `image` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `storage_type` varchar(20) NOT NULL,
  `image_blob_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_image_image_blob_id` (`image_blob_id`),
  KEY `fk_image_user_id` (`user_id`),
  CONSTRAINT `fk_image_image_blob_id` FOREIGN KEY (`image_blob_id`) REFERENCES `image_blob` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_image_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `image_blob`
--

CREATE TABLE `image_blob` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `data` mediumblob,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `link`
--

CREATE TABLE `link` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `source_id` int unsigned NOT NULL,
  `target_id` int unsigned NOT NULL,
  `user_id` int unsigned NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `type_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_link_note_source` (`source_id`),
  KEY `fk_link_note_target` (`target_id`),
  KEY `FK_link_user_id` (`user_id`),
  CONSTRAINT `fk_link_note_source` FOREIGN KEY (`source_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_link_note_target` FOREIGN KEY (`target_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_link_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=799 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `note`
--

CREATE TABLE `note` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL,
  `description` text,
  `user_id` int unsigned DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `picture_url` varchar(1024) DEFAULT NULL,
  `url` varchar(1024) DEFAULT NULL,
  `url_is_video` tinyint(1) NOT NULL DEFAULT '0',
  `sibling_order` bigint NOT NULL DEFAULT '1',
  `skip_review` tinyint(1) NOT NULL DEFAULT '0',
  `master_review_setting_id` int unsigned DEFAULT NULL,
  `image_id` int unsigned DEFAULT NULL,
  `use_parent_picture` tinyint(1) NOT NULL DEFAULT '0',
  `picture_mask` varchar(1024) DEFAULT NULL,
  `hide_title_in_article` tinyint(1) NOT NULL DEFAULT '0',
  `show_as_bullet_in_article` tinyint(1) NOT NULL DEFAULT '0',
  `notebook_id` int unsigned DEFAULT NULL,
  `title_idn` varchar(100) DEFAULT NULL,
  `description_idn` text,
  `version` int DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `fk_user_id` (`user_id`),
  KEY `parent_id` (`sibling_order`),
  KEY `fk_note_master_review_setting_id` (`master_review_setting_id`),
  KEY `fk_note_image_id` (`image_id`),
  KEY `fk_note_notebook_id` (`notebook_id`),
  CONSTRAINT `fk_note_image_id` FOREIGN KEY (`image_id`) REFERENCES `image` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_note_master_review_setting_id` FOREIGN KEY (`master_review_setting_id`) REFERENCES `review_setting` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_note_notebook_id` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON UPDATE RESTRICT,
  CONSTRAINT `fk_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=2798 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `notebook`
--

CREATE TABLE `notebook` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `ownership_id` int unsigned NOT NULL,
  `creator_id` int unsigned NOT NULL,
  `skip_review_entirely` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `fk_notes_book_creator_id` (`creator_id`),
  KEY `fk_notes_book_ownership_id` (`ownership_id`),
  CONSTRAINT `fk_notes_book_creator_id` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_notes_book_ownership_id` FOREIGN KEY (`ownership_id`) REFERENCES `ownership` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1241 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `notebook_head_note`
--

CREATE TABLE `notebook_head_note` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `notebook_id` int unsigned NOT NULL,
  `head_note_id` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_notebook_head_note_head_note_id` (`head_note_id`),
  KEY `fk_notebook_head_note_notebook_id` (`notebook_id`),
  CONSTRAINT `fk_notebook_head_note_head_note_id` FOREIGN KEY (`head_note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_notebook_head_note_notebook_id` FOREIGN KEY (`notebook_id`) REFERENCES `notebook` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=539 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `notes_closure`
--

CREATE TABLE `notes_closure` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned NOT NULL,
  `ancestor_id` int unsigned NOT NULL,
  `depth` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `note_id` (`note_id`,`ancestor_id`),
  UNIQUE KEY `note_id_2` (`note_id`,`depth`),
  KEY `fk_notes_closure_ancestor_id` (`ancestor_id`),
  CONSTRAINT `fk_notes_closure_ancestor_id` FOREIGN KEY (`ancestor_id`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_notes_closure_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1814 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `ownership`
--

CREATE TABLE `ownership` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned DEFAULT NULL,
  `circle_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ownership_user_id` (`user_id`),
  UNIQUE KEY `ownership_circle_id` (`circle_id`),
  CONSTRAINT `fk_ownership_circle_id` FOREIGN KEY (`circle_id`) REFERENCES `circle` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_ownership_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=1811 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `review_point`
--

CREATE TABLE `review_point` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `note_id` int unsigned DEFAULT NULL,
  `repetition_count` int NOT NULL DEFAULT '100',
  `forgetting_curve_index` int NOT NULL DEFAULT '100',
  `last_reviewed_at` timestamp NOT NULL,
  `initial_reviewed_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `next_review_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `removed_from_review` tinyint(1) NOT NULL DEFAULT '0',
  `link_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`,`note_id`,`link_id`),
  KEY `fk_review_point_note_id` (`note_id`),
  KEY `last_reviewed_at` (`last_reviewed_at`),
  KEY `FK_review_point_link_id` (`link_id`),
  CONSTRAINT `FK_review_point_link_id` FOREIGN KEY (`link_id`) REFERENCES `link` (`id`),
  CONSTRAINT `fk_review_point_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON UPDATE RESTRICT,
  CONSTRAINT `FK_review_point_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=373 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `review_setting`
--

CREATE TABLE `review_setting` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `remember_spelling` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `subscription`
--

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
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Table structure for table `tree_node`
--

CREATE TABLE `tree_node` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `note_id` int unsigned NOT NULL,
  `sibling_order` bigint NOT NULL DEFAULT '1',
  `parent_id` int unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_tree_node_note_id` (`note_id`),
  KEY `parent_id` (`parent_id`,`sibling_order`),
  CONSTRAINT `fk_tree_node_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_tree_node_parent_id` FOREIGN KEY (`parent_id`) REFERENCES `tree_node` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
--
-- Table structure for table `user`
--

CREATE TABLE `user` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `external_identifier` varchar(100) NOT NULL,
  `daily_new_notes_count` int unsigned NOT NULL DEFAULT '10',
  `space_intervals` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_external_identifier` (`external_identifier`)
) ENGINE=InnoDB AUTO_INCREMENT=1763 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
