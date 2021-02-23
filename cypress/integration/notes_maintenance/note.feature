Feature: Note maintenance
    As a learner, I want to maintain my newly acquired knowledge in
    notes that linking to each other, so that I can review them in the
    future.

    Background:
        Given I've logged in as an existing user

    @clean_db
    Scenario: New user create notes after login
        When I create note with:
            | note-title      | note-description  |
            | Sedation        | Put to sleep      |
        Then Reviews should include single note pages:
            | Sedation        | Put to sleep      |
