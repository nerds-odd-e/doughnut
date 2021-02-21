Feature: link note

@clean_db @login_as_new_user @seed_notes @link_note
    Scenario: View all linkable notes when no links exist
        When I navigate to the notes page
        Then I should see 3 notes belonging to the user
            | note-title       |
            | Sedition        |
            | Sedation        |
            | Sedative        |
        When I click Create Link button on Sedition
        Then I should be navigated to the linking page
        And I should see the source note as Sedition
        And I should see below notes
        | note-title      |   note-description  |
        | Sedation        |   Put to sleep      |
        | Sedative        |   sleep medicine    |

    @login_as_existing_user
    Scenario: Search note for linking
        When I navigate to the notes page
        And I click Create Link button on Sedition
        Then I should be navigated to the linking page
        And  I should see below notes
            | note-title      |   note-description  |
            | Sedation        |   Put to sleep      |
            | Sedative        |   sleep medicine    |
        When I search for notes with title "Sedatio"
        Then I should see only "Sedation"

    @login_as_existing_user
    Scenario: Create link for note
        When I navigate to the notes page
        And I click Create Link button on Sedition
        Then I should be navigated to the linking page
        And I should be able to see the buttons for linking note
        When I select a Sedation note
        Then I should be redirected to review page
        And I should see the Sedition note linked to Sedation


@ignore @clean_db @login_as_new_user
    Scenario Outline: View all linkable notes when there are existing links
        Given link exist form Sedition to sedation
        When I navigate to the notes page
        Then I should see all notes belonging to the user
        When I click Create Link button on Sedition note card
        Then I should be navigated to the linking page
        And I should see below notes

        Examples:
        | note-title      |   note-description       |
        | Sedative        |   sleep medicine    |