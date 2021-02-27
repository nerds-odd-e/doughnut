Feature: Note maintenance
    As a learner, I want to maintain my newly acquired knowledge in
    notes, so that I can review them in the future.

    Background:
        Given I've logged in as an existing user

    Scenario: Create a new note
        When I create top level note with:
            | note-title      | note-description  |
            | Sedation        | Put to sleep      |
        And I create top level note with:
            | note-title      | note-description  | note-picture |
            | Sedition        | Incite violence   | a_slide.jpg  |
        Then I should see these notes belonging to the user at the top level of all my notes
            | note-title      |
            | Sedation        |
            | Sedition        |

    Scenario: Create a new note with invalid information
        When I create top level note with:
            | note-title      | note-description  |
            |                 | Put to sleep      |
        Then I should see that the note creation is not successful

    Scenario: Create a new note belonging to another node
        Given there are some notes for the current user
            | title           | description          |
            | LeSS in Action  | An awesome training  |
        When I create note belonging to "LeSS in Action":
            | note-title      | note-description                     |
            | Re-quirement    | Re-think the way we do requirement   |
        Then I should not see note "Re-quirement" at the top level of all my notes
        When I open "LeSS in Action" note at top level
        Then I should see "LeSS in Action" in note title
        And I should see these notes belonging to the user
            | note-title      |
            | Re-quirement    |
        When I am creating note under "LeSS in Action > Re-quirement"
        Then I should see "LeSS in Action, Re-quirement" in breadcrumb

    Scenario: Create a new sibling note
        Given there are some notes for the current user
            | title           | description          |
            | LeSS in Action  | An awesome training  |
        And I create note belonging to "LeSS in Action":
            | note-title      | note-description                     |
            | Re-quirement    | Re-think the way we do requirement   |
        When I create a sibling note of "Re-quirement":
            | note-title      | note-description                     |
            | Re-Design    | Re-think the way we do design   |
        When I open "LeSS in Action" note at top level
        And I should see these notes belonging to the user
            | note-title      |
            | Re-quirement    |
            | Re-Design       |

    Scenario: Delete a note
        Given there are some notes for the current user
            | title           | description          |
            | LeSS in Action  | An awesome training  |
        When I delete top level note "LeSS in Action"
        Then I should not see note "LeSS in Action" at the top level of all my notes
