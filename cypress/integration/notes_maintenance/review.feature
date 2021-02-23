Feature: Reviewing
    As a learner, I want to review my notes so that I have fresh memory.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
            | title      | description       |
            | Sedition   | Incite violence   |
            | Sedation   | Put to sleep      |
            | Sedative   | Sleep medicine    |
        And I am creating link for note "Sedition"
        And I link to note "Sedation"
        And I am creating link for note "Sedition"
        And I link to note "Sedative"

    @clean_db
    Scenario: Review notes
        Then Reviews should have review pages in sequence:
            | review type   | title      |   additional info    |
            | related notes | Sedition   |   Sedation, Sedative |
            | single note   | Sedition   |   Incite violence    |
            | single note   | Sedation   |   Put to sleep       |

