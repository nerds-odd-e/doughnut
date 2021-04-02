ALTER TABLE note
ADD COLUMN hide_title_in_article BOOLEAN;
ALTER TABLE note
ADD COLUMN show_as_bullet_in_article BOOLEAN;

UPDATE note SET hide_title_in_article = 0, show_as_bullet_in_article = 0;

ALTER TABLE note
    MODIFY hide_title_in_article BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE note
    MODIFY show_as_bullet_in_article BOOLEAN DEFAULT FALSE NOT NULL;
