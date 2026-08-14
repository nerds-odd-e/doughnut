# Database ERD

Entity-relationship view of the application database: foreign keys as relationships (edge labels include `ON DELETE` rules), and key columns (PK, UK, FK) per table. The `flyway_schema_history` table is omitted.

```mermaid
erDiagram
    attachment_blob ||--o{ image : "attachment_blob_id ON DELETE CASCADE"
    book ||--o{ book_block : "book_id ON DELETE CASCADE"
    book ||--o{ book_user_last_read_position : "book_id ON DELETE CASCADE"
    book_block ||--o{ book_block_reading_record : "book_block_id ON DELETE CASCADE"
    book_block ||--o{ book_content_block : "book_block_id ON DELETE NO ACTION"
    book_block ||--o{ book_user_last_read_position : "selected_book_block_id ON DELETE SET NULL"
    circle ||--o{ circle_user : "circle_id ON DELETE CASCADE"
    circle ||--o{ ownership : "circle_id ON DELETE CASCADE"
    conversation ||--o{ conversation_message : "conversation_id ON DELETE CASCADE"
    folder ||--o{ folder : "parent_folder_id ON DELETE CASCADE"
    folder ||--o{ "note" : "folder_id ON DELETE SET NULL"
    image ||--o{ "note" : "image_id ON DELETE CASCADE"
    learning_session ||--o{ session_item : "learning_session_id ON DELETE CASCADE"
    memory_tracker ||--o{ question_generation_batch_request : "memory_tracker_id ON DELETE CASCADE"
    memory_tracker ||--o{ recall_prompt : "memory_tracker_id ON DELETE CASCADE"
    memory_tracker ||--o{ session_item : "memory_tracker_id ON DELETE CASCADE"
    "note" ||--o{ admin_data_migration_progress : "last_processed_note_id ON DELETE SET NULL"
    "note" ||--o{ assimilation_sequence_skip : "note_id ON DELETE CASCADE"
    "note" ||--o{ conversation : "note_id ON DELETE NO ACTION"
    "note" ||--o{ image : "note_id ON DELETE SET NULL"
    "note" ||--o{ memory_tracker : "note_id ON DELETE CASCADE"
    "note" ||--o{ note_alias_index : "note_id ON DELETE CASCADE"
    "note" ||--o{ note_creator : "note_id ON DELETE CASCADE"
    "note" ||--o{ note_property_index : "note_id ON DELETE CASCADE"
    "note" ||--o{ note_property_index : "target_note_id ON DELETE SET NULL"
    "note" ||--o{ note_wiki_title_cache : "note_id ON DELETE CASCADE"
    "note" ||--o{ note_wiki_title_cache : "target_note_id ON DELETE CASCADE"
    "note" ||--o{ predefined_question : "note_id ON DELETE CASCADE"
    notebook ||--o{ bazaar_notebook : "notebook_id ON DELETE NO ACTION"
    notebook ||--o{ book : "notebook_id ON DELETE CASCADE"
    notebook ||--o{ folder : "notebook_id ON DELETE CASCADE"
    notebook ||--o{ learning_session : "notebook_id ON DELETE CASCADE"
    notebook ||--o{ "note" : "notebook_id ON DELETE NO ACTION"
    notebook ||--o{ subscription : "notebook_id ON DELETE NO ACTION"
    notebook_group ||--o{ notebook : "notebook_group_id ON DELETE SET NULL"
    notebook_group ||--o{ subscription : "notebook_group_id ON DELETE SET NULL"
    ownership ||--o{ conversation : "subject_ownership_id ON DELETE NO ACTION"
    ownership ||--o{ notebook : "ownership_id ON DELETE CASCADE"
    ownership ||--o{ notebook_group : "ownership_id ON DELETE CASCADE"
    predefined_question ||--o{ recall_prompt : "predefined_question_id ON DELETE NO ACTION"
    question_generation_batch ||--o{ question_generation_batch_request : "batch_id ON DELETE CASCADE"
    quiz_answer ||--o{ recall_prompt : "quiz_answer_id ON DELETE NO ACTION"
    recall_prompt ||--o{ conversation : "recall_prompt_id ON DELETE SET NULL"
    "user" ||--o{ assimilation_sequence_skip : "user_id ON DELETE CASCADE"
    "user" ||--o{ book_block_reading_record : "user_id ON DELETE CASCADE"
    "user" ||--o{ book_user_last_read_position : "user_id ON DELETE CASCADE"
    "user" ||--o{ circle_user : "user_id ON DELETE CASCADE"
    "user" ||--o{ conversation : "conversation_initiator_id ON DELETE NO ACTION"
    "user" ||--o{ conversation_message : "sender ON DELETE CASCADE"
    "user" ||--o{ image : "user_id ON DELETE CASCADE"
    "user" ||--o{ learning_session : "user_id ON DELETE CASCADE"
    "user" ||--o{ memory_tracker : "user_id ON DELETE CASCADE"
    "user" ||--o{ note_creator : "user_id ON DELETE CASCADE"
    "user" ||--o{ notebook : "creator_id ON DELETE CASCADE"
    "user" ||--o{ ownership : "user_id ON DELETE CASCADE"
    "user" ||--o{ question_generation_batch : "user_id ON DELETE CASCADE"
    "user" ||--o{ subscription : "user_id ON DELETE CASCADE"
    "user" ||--o{ user_token : "user_id ON DELETE CASCADE"
    admin_data_migration_progress {
        int id PK
        string step_name UK
        int last_processed_note_id FK
    }
    assimilation_sequence_skip {
        int id PK
        int user_id FK
        int note_id FK
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
        int note_id FK
    }
    learning_session {
        int id PK
        int user_id FK
        int notebook_id FK
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
    }
    note_alias_index {
        int id PK
        int note_id FK
    }
    note_creator {
        int note_id PK FK
        int user_id FK
    }
    note_embeddings {
        bigint id PK
    }
    note_property_index {
        int id PK
        int note_id FK
        int target_note_id FK
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
    question_generation_batch {
        int id PK
        int user_id FK
    }
    question_generation_batch_maintenance_run {
        int id PK
    }
    question_generation_batch_request {
        int id PK
        int batch_id FK
        int memory_tracker_id FK
        string custom_id UK
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
    session_item {
        int id PK
        int learning_session_id FK
        int memory_tracker_id FK
    }
    shedlock {
        string name PK
    }
    subscription {
        int id PK
        int user_id FK
        int notebook_id FK
        int notebook_group_id FK
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
```

