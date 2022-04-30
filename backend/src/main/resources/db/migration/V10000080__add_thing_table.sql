CREATE TABLE IF NOT EXISTS `thing` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` timestamp DEFAULT NULL,
  `note_id` int unsigned NOT NULL,
  `link_id` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `fk_thing_note_source` (`note_id`),
  UNIQUE KEY `fk_thing_link_target` (`link_id`),
  KEY `FK_thing_user_id` (`user_id`),
  KEY `FK_created_at` (`created_at`),
  CONSTRAINT `fk_thing_note_id` FOREIGN KEY (`note_id`) REFERENCES `note` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_thing_link_id` FOREIGN KEY (`link_id`) REFERENCES `link` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_thing_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `thing` MODIFY `note_id` int unsigned;
ALTER TABLE `thing` MODIFY `link_id` int unsigned;

INSERT INTO thing (user_id, created_at, deleted_at, note_id)
SELECT user_id, created_at, deleted_at, id FROM note;

INSERT INTO thing (user_id, created_at, link_id)
SELECT user_id, created_at, id FROM link;

INSERT INTO thing (user_id, created_at, deleted_at, note_id)
SELECT user_id, created_at, deleted_at, id FROM note;

INSERT INTO thing (user_id, created_at, link_id)
SELECT user_id, created_at, id FROM link;

ALTER TABLE review_point ADD `thing_id` int unsigned DEFAULT NULL;

UPDATE review_point
INNER JOIN thing ON thing.note_id = review_point.note_id AND thing.link_id = review_point.link_id
SET thing_id = thing.id;

ALTER TABLE `review_point` MODIFY `thing_id` int unsigned NOT NULL;
ALTER TABLE review_point ADD FOREIGN KEY (thing_id) REFERENCES thing(id) ON DELETE CASCADE;

ALTER TABLE review_point DROP FOREIGN KEY FK_review_point_user_id;
ALTER TABLE review_point DROP INDEX user_id;
ALTER TABLE review_point ADD UNIQUE INDEX user_thing (user_id, thing_id);
ALTER TABLE thing ADD UNIQUE INDEX link_note (link_id, note_id);
ALTER TABLE review_point ADD FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;
