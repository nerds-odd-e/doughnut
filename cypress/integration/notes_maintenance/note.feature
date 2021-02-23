Feature: Note maintenance
    As a learner, I want to maintain my newly acquired knowledge in
    notes that linking to each other, so that I can review them in the
    future.

    Background:
        Given I've logged in as an existing user

    @clean_db
    Scenario: Create a new note
        When I create note with:
            | note-title      | note-description  |
            | Sedation        | Put to sleep      |
        Then Reviews should have review pages in sequence:
            | review type   | title      |   additional info   |
            | single note   | Sedation   |   Put to sleep      |
