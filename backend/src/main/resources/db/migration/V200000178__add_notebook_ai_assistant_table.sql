CREATE TABLE notebook_ai_assistant (
    id SERIAL PRIMARY KEY,
    notebook_id INTEGER NOT NULL UNIQUE REFERENCES notebook(id),
    additional_instructions_to_ai TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
