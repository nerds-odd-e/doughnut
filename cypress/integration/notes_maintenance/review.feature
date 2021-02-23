Feature: Reviewing
    As a learner, I want to review my notes so that I have fresh memory.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
            | title      |   description       |
            | Sedition   |   Incite violence   |
            | Sedation   |   Put to sleep      |
        And I am creating link for note "Sedition"
        And I link to note "Sedation"

    @clean_db
    Scenario: Review notes
        Then Reviews should include single note pages:
            | Sedition   |   Incite violence   |
            | Sedation   |   Put to sleep      |

    @clean_db
    Scenario: Review linked notes
        Then Reviews should include related notes page from "Sedition" to "Sedation"
