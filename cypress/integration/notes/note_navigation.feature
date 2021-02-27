Feature: Note navigation
    As a learner, I want to browse my notes.

    Background:
        Given I've logged in as an existing user

        @ignore
    Scenario: Navigation
        Given there are some notes for the current user
            | title           | description                | linkTo        |
            | Shape           | The form of something      |               |
            | Square          | four equal straight sides  | Shape         |
            | Triangle        | three sides shape          | Shape         |
            | My next focus   | Deciding                   |               |
        When I open "Shape" note at top level
        Then I should not be able to go to next sibling
        But I should be able to go to next note "Square"
        When I open "Shape" note at top level
        Then I should be able to go to next note "Square"
        When I open "Shape/Square" note at top level
        Then I should be able to go to next note "Triangle"
        When I open "Shape/Square" note at top level
        Then I should not be able to go to next sibling
        When I open "Shape/Triangle" note at top level
        Then I should not be able to go to next sibling

