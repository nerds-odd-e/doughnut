Feature: Reviewing
    As a learner, I want to review my notes so that I have fresh memory.

    Background:
        Given I've logged in as an existing user

    @clean_db
    Scenario: New user review multiple notes
        Given there are some notes for the current user
            | title      |   description       |
            | Sedition   |   Incite violence   |
            | Sedation   |   Put to sleep      |
        When I review my notes
        Then Reviews should include note page:
            | Sedition   |   Incite violence   |
            | Sedation   |   Put to sleep      |

    @clean_db
    Scenario: New user review note with link
        Given I link Sedition to Sedation
        | note-title      |   note-description  |
        | Sedition        |   Incite violence   |
        | Sedation        |   Put to sleep      |
        When I review my notes
        Then I should see following note with links on the review page
        | note-title      |   note-links |
        | Sedition        |   Sedation   |
        And I click on next note
        Then I should see the note with title and description on the review page
        | note-title      |   note-description  |
        | Sedition        |   Incite violence   |
        And I click on next note
        Then I should see the note with title and description on the review page
        | note-title      |   note-description  |
        | Sedation        |   Put to sleep      |
