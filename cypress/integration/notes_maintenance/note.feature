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
            | note-title      | note-description  | note-picture |
            | Sedition        | Incite violence   | a_slide.jpg  |
        Then I should see these notes belonging to the user at the top level of all my notes
            | note-title      |
            | Sedation        |
            | Sedition        |

    Scenario: Create a new note belonging to another node
        Given I create note with:
            | note-title      | note-description     |
            | LeSS in Action  | An awesome training  |
        When I create note belonging to "LeSS in Action":
            | note-title      | note-description                     |
            | Re-quirement    | Re-think the way we do requirement   |
        Then I should not see note "Re-quirement" at the top level of all my notes
        When I open "LeSS in Action" note
        Then I should see "LeSS in Action" in note title
        And I should see these notes belonging to the user
            | note-title      |
            | Re-quirement    |
