DELIMITER //

CREATE PROCEDURE MigrateLinksToNotes()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE l_id, l_source_id, l_target_id, new_note_id, source_notebook_id INT;
    DECLARE l_type_id SMALLINT;  -- Adjust the data type as needed
    DECLARE l_type_label VARCHAR(255);
    DECLARE l_level TINYINT;
    DECLARE source_updated_at TIMESTAMP;
    DECLARE cur CURSOR FOR SELECT id, source_id, target_id, type_id FROM link;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO l_id, l_source_id, l_target_id, l_type_id;
        IF done THEN
            LEAVE read_loop;
        END IF;

        -- Get notebook_id and updated_at from source note
        SELECT notebook_id, updated_at INTO source_notebook_id, source_updated_at
        FROM note WHERE id = l_source_id;

        -- Mapping link_type_id to link_type_label
        SET l_type_label = CASE l_type_id
            WHEN 0 THEN 'no link'
            WHEN 1 THEN 'related to'
            WHEN 2 THEN 'a specialization of'
            WHEN 3 THEN 'an application of'
            WHEN 4 THEN 'an instance of'
            WHEN 6 THEN 'a part of'
            WHEN 8 THEN 'tagged by'
            WHEN 10 THEN 'an attribute of'
            WHEN 12 THEN 'the opposite of'
            WHEN 14 THEN 'author of'
            WHEN 15 THEN 'using'
            WHEN 17 THEN 'an example of'
            WHEN 19 THEN 'before'
            WHEN 22 THEN 'similar to'
            WHEN 23 THEN 'confused with'
            ELSE 'unknown link type'
        END;

        -- Determine the greater level between source and target
        SELECT GREATEST(n1.level, n2.level) INTO l_level
        FROM note n1 JOIN note n2 ON n1.id = l_source_id AND n2.id = l_target_id;

        -- Insert new note and get its ID
        INSERT INTO note (topic_constructor, level, parent_id, target_note_id, notebook_id, updated_at)
        VALUES (CONCAT(':', l_type_label), l_level, l_source_id, l_target_id, source_notebook_id, source_updated_at);

        SET new_note_id = LAST_INSERT_ID();

        -- Update thing table
        UPDATE thing SET note_id = new_note_id, link_id = NULL WHERE link_id = l_id;

    END LOOP;

    CLOSE cur;
END;

//
DELIMITER ;

-- Execute the stored procedure
CALL MigrateLinksToNotes();

-- Drop the stored procedure
DROP PROCEDURE IF EXISTS MigrateLinksToNotes;
