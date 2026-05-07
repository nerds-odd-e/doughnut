DROP TABLE IF EXISTS `certificate`;
DROP TABLE IF EXISTS `notebook_certificate_approval`;
ALTER TABLE `notebook` DROP COLUMN `certificate_expiry`;
ALTER TABLE `assessment_attempt` DROP COLUMN `certificate_expires_at`;
