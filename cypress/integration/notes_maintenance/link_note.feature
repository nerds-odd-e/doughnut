Feature: link note
    As a learner, I want to maintain my newly acquired knowledge in
    notes that linking to each other, so that I can review them in the
    future.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user

    @clean_db
    Scenario: View all linkable notes when no links exist
        Then I should see these notes belonging to the user
            | note-title      |
            | Sedition        |
            | Sedation        |
            | Sedative        |
        When I create link for note "Sedition"
        And I should see the source note as Sedition
        And I should see below notes
        | note-title      |   note-description  |
        | Sedation        |   Put to sleep      |
        | Sedative        |   sleep medicine    |

    @clean_db
    Scenario: Search note for linking
        Given I create link for note "Sedition"
        And  I should see below notes
            | note-title      |   note-description  |
            | Sedation        |   Put to sleep      |
            | Sedative        |   sleep medicine    |
        When I search for notes with title "Sedatio"
        Then I should see only "Sedation"

    @clean_db
    Scenario: Create link for note
        Given I create link for note "Sedition"
        And I should be able to see the buttons for linking note
        When I select a Sedation note
        Then I should be redirected to review page
        And I should see the Sedition note linked to Sedation