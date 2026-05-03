# Database ERD

Entity-relationship view of the application database: foreign keys as relationships, and key columns (PK, UK, FK) per table. The `flyway_schema_history` table is omitted.

```mermaid
erDiagram
    assessment_attempt ||--o{ assessment_question_instance : "assessment_attempt_id"
    assessment_question_instance ||--o{ conversation : "assessment_question_instance_id"
    attachment_blob ||--o{ image : "attachment_blob_id"
    book ||--o{ book_block : "book_id"
    book ||--o{ book_user_last_read_position : "book_id"
    book_block ||--o{ book_block_reading_record : "book_block_id"
    book_block ||--o{ book_content_block : "book_block_id"
    book_block ||--o{ book_user_last_read_position : "selected_book_block_id"
    circle ||--o{ circle_user : "circle_id"
    circle ||--o{ ownership : "circle_id"
    conversation ||--o{ conversation_message : "conversation_id"
    folder ||--o{ folder : "parent_folder_id"
    folder ||--o{ "note" : "folder_id"
    image ||--o{ "note" : "image_id"
    image ||--o{ note_accessory : "image_id"
    memory_tracker ||--o{ recall_prompt : "memory_tracker_id"
    "note" ||--o{ conversation : "note_id"
    "note" ||--o{ memory_tracker : "note_id"
    "note" ||--o{ note_accessory : "note_id"
    "note" ||--o{ note_wiki_title_cache : "note_id"
    "note" ||--o{ note_wiki_title_cache : "target_note_id"
    "note" ||--o{ predefined_question : "note_id"
    "note" ||--o{ wiki_reference_migration_progress : "last_processed_note_id"
    notebook ||--o{ assessment_attempt : "notebook_id"
    notebook ||--o{ bazaar_notebook : "notebook_id"
    notebook ||--o{ book : "notebook_id"
    notebook ||--o{ certificate : "notebook_id"
    notebook ||--o{ folder : "notebook_id"
    notebook ||--o{ "note" : "notebook_id"
    notebook ||--o{ notebook_certificate_approval : "notebook_id"
    notebook ||--o{ subscription : "notebook_id"
    notebook_group ||--o{ notebook : "notebook_group_id"
    notebook_group ||--o{ subscription : "notebook_group_id"
    ownership ||--o{ conversation : "subject_ownership_id"
    ownership ||--o{ notebook : "ownership_id"
    ownership ||--o{ notebook_group : "ownership_id"
    predefined_question ||--o{ assessment_question_instance : "predefined_question_id"
    predefined_question ||--o{ recall_prompt : "predefined_question_id"
    quiz_answer ||--o{ assessment_question_instance : "quiz_answer_id"
    quiz_answer ||--o{ recall_prompt : "quiz_answer_id"
    recall_prompt ||--o{ conversation : "recall_prompt_id"
    "user" ||--o{ assessment_attempt : "user_id"
    "user" ||--o{ book_block_reading_record : "user_id"
    "user" ||--o{ book_user_last_read_position : "user_id"
    "user" ||--o{ certificate : "user_id"
    "user" ||--o{ circle_user : "user_id"
    "user" ||--o{ conversation : "conversation_initiator_id"
    "user" ||--o{ conversation_message : "sender"
    "user" ||--o{ image : "user_id"
    "user" ||--o{ memory_tracker : "user_id"
    "user" ||--o{ "note" : "creator_id"
    "user" ||--o{ notebook : "creator_id"
    "user" ||--o{ ownership : "user_id"
    "user" ||--o{ subscription : "user_id"
    "user" ||--o{ suggested_question_for_fine_tuning : "user_id"
    "user" ||--o{ user_token : "user_id"
    assessment_attempt {
        int id PK
        int user_id FK
        int notebook_id FK
    }
    assessment_question_instance {
        int id PK
        int assessment_attempt_id FK
        int predefined_question_id FK
        int quiz_answer_id FK
    }
    attachment_blob {
        int id PK
    }
    bazaar_notebook {
        int id PK
        int notebook_id FK
    }
    book {
        int id PK
        int notebook_id UK FK
    }
    book_block {
        int id PK
        int book_id FK
    }
    book_block_reading_record {
        int id PK
        int user_id FK
        int book_block_id FK
    }
    book_content_block {
        int id PK
        int book_block_id FK
    }
    book_user_last_read_position {
        int id PK
        int user_id FK
        int book_id FK
        int selected_book_block_id FK
    }
    certificate {
        int id PK
        int user_id FK
        int notebook_id FK
    }
    circle {
        int id PK
        string invitation_code UK
    }
    circle_user {
        int id PK
        int user_id FK
        int circle_id FK
    }
    conversation {
        int id PK
        int subject_ownership_id FK
        int conversation_initiator_id FK
        int assessment_question_instance_id FK
        int note_id FK
        int recall_prompt_id FK
    }
    conversation_message {
        int id PK
        int conversation_id FK
        int sender FK
    }
    failure_report {
        int id PK
    }
    folder {
        int id PK
        int notebook_id FK
        int parent_folder_id FK
    }
    global_settings {
        int id PK
    }
    image {
        int id PK
        int user_id FK
        int attachment_blob_id FK
    }
    memory_tracker {
        int id PK
        int user_id FK
        int note_id FK
    }
    "note" {
        int id PK
        int image_id FK
        int notebook_id FK
        int folder_id FK
        int creator_id FK
    }
    note_accessory {
        int id PK
        int note_id UK FK
        int image_id FK
    }
    note_embeddings {
        bigint id PK
    }
    note_wiki_title_cache {
        int id PK
        int note_id FK
        int target_note_id FK
    }
    notebook {
        int id PK
        int ownership_id FK
        int creator_id FK
        int notebook_group_id FK
    }
    notebook_ai_assistant {
        bigint id PK
        int notebook_id UK
    }
    notebook_certificate_approval {
        int id PK
        int notebook_id UK FK
    }
    notebook_group {
        int id PK
        int ownership_id FK
    }
    ownership {
        int id PK
        int user_id UK FK
        int circle_id UK FK
    }
    predefined_question {
        int id PK
        int note_id FK
    }
    quiz_answer {
        int id PK
    }
    recall_prompt {
        int id PK
        int memory_tracker_id FK
        int predefined_question_id FK
        int quiz_answer_id FK
    }
    subscription {
        int id PK
        int user_id FK
        int notebook_id FK
        int notebook_group_id FK
    }
    suggested_question_for_fine_tuning {
        int id PK
        int user_id FK
    }
    "user" {
        int id PK
        string external_identifier UK
    }
    user_token {
        int id PK
        int user_id FK
        string token UK
    }
    wiki_reference_migration_progress {
        int id PK
        string step_name UK
        int last_processed_note_id FK
    }
```

