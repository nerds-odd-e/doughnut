-- Normalize and remove empty names (including whitespace-only)
UPDATE notebook
SET name = TRIM(name);

UPDATE notebook
SET name = CONCAT('Notebook-', id)
WHERE name IS NULL OR name = '';

-- Within each (ownership_id, name) group, keep the lowest id; rename others to name-2, name-3, ...
UPDATE notebook n
INNER JOIN (
  SELECT id,
         rn,
         base_name
  FROM (
    SELECT id,
           ROW_NUMBER() OVER (
             PARTITION BY ownership_id, name
             ORDER BY id
           ) AS rn,
           name AS base_name
    FROM notebook
  ) ranked
  WHERE ranked.rn > 1
) r ON n.id = r.id
SET n.name = LEFT(CONCAT(r.base_name, '-', r.rn), 150);

-- Collision repair (e.g. existing "Foo-2" vs renamed duplicate). Repeat for truncation edge cases.
UPDATE notebook n
INNER JOIN (
  SELECT n1.id
  FROM notebook n1
  INNER JOIN notebook n2
    ON n1.ownership_id = n2.ownership_id
   AND n1.name = n2.name
   AND n1.id > n2.id
) d ON n.id = d.id
SET n.name = CONCAT(
  LEFT(TRIM(n.name), GREATEST(0, 150 - CHAR_LENGTH(CAST(n.id AS CHAR)) - 1)),
  '-',
  CAST(n.id AS CHAR)
);

UPDATE notebook n
INNER JOIN (
  SELECT n1.id
  FROM notebook n1
  INNER JOIN notebook n2
    ON n1.ownership_id = n2.ownership_id
   AND n1.name = n2.name
   AND n1.id > n2.id
) d ON n.id = d.id
SET n.name = CONCAT(
  LEFT(TRIM(n.name), GREATEST(0, 150 - CHAR_LENGTH(CAST(n.id AS CHAR)) - 1)),
  '-',
  CAST(n.id AS CHAR)
);

UPDATE notebook n
INNER JOIN (
  SELECT n1.id
  FROM notebook n1
  INNER JOIN notebook n2
    ON n1.ownership_id = n2.ownership_id
   AND n1.name = n2.name
   AND n1.id > n2.id
) d ON n.id = d.id
SET n.name = CONCAT(
  LEFT(TRIM(n.name), GREATEST(0, 150 - CHAR_LENGTH(CAST(n.id AS CHAR)) - 1)),
  '-',
  CAST(n.id AS CHAR)
);

ALTER TABLE notebook
  MODIFY COLUMN name VARCHAR(150) NOT NULL,
  ADD CONSTRAINT chk_notebook_name_nonempty CHECK (CHAR_LENGTH(TRIM(`name`)) > 0),
  ADD UNIQUE KEY uk_notebook_ownership_name (ownership_id, name);
