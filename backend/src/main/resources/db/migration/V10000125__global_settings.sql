DROP TABLE IF EXISTS `global_settings`;
CREATE TABLE `global_settings` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `key` varchar(512) NOT NULL,
  `value` varchar(512) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
);
