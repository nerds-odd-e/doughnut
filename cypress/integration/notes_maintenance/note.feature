Feature: Note maintenance
    As a learner, I want to maintain my newly acquired knowledge in
    notes, so that I can review them in the future.

    Background:
        Given I've logged in as an existing user

    Scenario: Create a new note
        When I create note with:
            | note-title      | note-description  |
            | Sedation        | Put to sleep      |
        And I create note with:
            | note-title      | note-description  | note-picture          |
            | Sedition        | Incite violence   | a_photo_of_slide.jpg  |
        Then I should see these notes belonging to the user
            | note-title      |
            | Sedation        |
            | Sedition        |

