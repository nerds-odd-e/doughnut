CREATE TABLE note_accessory (
    id INT PRIMARY KEY AUTO_INCREMENT,
    note_id INT unsigned UNIQUE,
    `url` varchar(1024) DEFAULT NULL,
    `picture_url` varchar(1024) DEFAULT NULL,
    `use_parent_picture` tinyint NOT NULL DEFAULT '0',
    `picture_mask` varchar(1024) DEFAULT NULL,
    `image_id` int unsigned DEFAULT NULL,
    `audio_id` int unsigned DEFAULT NULL,
    FOREIGN KEY (note_id) REFERENCES note(id),
    FOREIGN KEY (image_id) REFERENCES image(id),
    FOREIGN KEY (audio_id) REFERENCES audio(id)
);

INSERT INTO note_accessory (note_id, url, picture_url, picture_mask, image_id, use_parent_picture, audio_id)
SELECT id, url, picture_url, picture_mask, image_id, use_parent_picture, audio_id FROM note;
