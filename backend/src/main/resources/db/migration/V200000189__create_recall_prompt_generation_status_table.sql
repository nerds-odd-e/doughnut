CREATE TABLE recall_prompt_generation_status (
    memory_tracker_id INT PRIMARY KEY,
    last_attempt_time TIMESTAMP,
    attempt_count INT NOT NULL DEFAULT 0,
    successful BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    in_progress BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (memory_tracker_id) REFERENCES memory_tracker(id) ON DELETE CASCADE
);

CREATE INDEX idx_rpgs_status ON recall_prompt_generation_status(successful, attempt_count, in_progress);
CREATE INDEX idx_rpgs_last_attempt ON recall_prompt_generation_status(last_attempt_time);
