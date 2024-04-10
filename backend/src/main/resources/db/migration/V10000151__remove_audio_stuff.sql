-- Setting the delimiter to create the stored procedure
DELIMITER $$

CREATE PROCEDURE DropColumnAndConstraintIfExists()
BEGIN
    -- Variable to hold the name of the foreign key constraint
    DECLARE fk_name VARCHAR(64);

    -- Find the name of the foreign key constraint on the `audio_id` column
    SELECT CONSTRAINT_NAME INTO fk_name
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE() -- Uses the default database
    AND TABLE_NAME = 'note'
    AND COLUMN_NAME = 'audio_id';

    -- If a constraint name was found, then drop the foreign key constraint
    IF fk_name IS NOT NULL THEN
        SET @sql = CONCAT('ALTER TABLE note DROP FOREIGN KEY ', fk_name);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;

    -- After removing the foreign key constraint, check and drop the column if it exists
    IF EXISTS (SELECT *
               FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'note'
               AND COLUMN_NAME = 'audio_id') THEN
        ALTER TABLE note DROP COLUMN audio_id;
    END IF;
END$$

DELIMITER ;

-- Execute the stored procedure
CALL DropColumnAndConstraintIfExists();

-- Drop the stored procedure after execution
DROP PROCEDURE IF EXISTS DropColumnAndConstraintIfExists;

-- Drop `audio` table if it exists
DROP TABLE IF EXISTS audio;

-- Drop `audio_blob` table if it exists
DROP TABLE IF EXISTS audio_blob;
