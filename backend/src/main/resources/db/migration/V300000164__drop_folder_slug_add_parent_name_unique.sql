UPDATE folder f
  JOIN (
    SELECT id
    FROM (
           SELECT id,
                  ROW_NUMBER() OVER (
                    PARTITION BY notebook_id, IFNULL(parent_folder_id, 0), name
                    ORDER BY id
                    ) AS rn
           FROM folder
         ) ranked
    WHERE ranked.rn > 1
  ) dup ON dup.id = f.id
SET f.name = CONCAT(TRIM(f.name), ' (#', CAST(f.id AS CHAR), ')');

ALTER TABLE folder
  DROP INDEX uk_folder_notebook_slug;

ALTER TABLE folder
  ADD UNIQUE KEY uk_folder_notebook_parent_name (notebook_id,
    (IFNULL(parent_folder_id, 0)), name);

ALTER TABLE folder
  DROP COLUMN slug;
