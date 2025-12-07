-- Rename topic_constructor column to title_constructor
ALTER TABLE note CHANGE COLUMN topic_constructor title_constructor varchar(150) DEFAULT NULL;

-- Rename description column to details
ALTER TABLE note CHANGE COLUMN description details text;

