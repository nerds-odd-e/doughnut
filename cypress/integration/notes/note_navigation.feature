Feature: Note navigation
    As a learner, I want to browse my notes.

    Background:
        Given I've logged in as an existing user

    Scenario Outline: Navigation
        Given there are some notes for the current user
            | title           | description                | testingLinkTo |
            | Shape           | The form of something      |               |
            | Square          | four equal straight sides  | Shape         |
            | Triangle        | three sides shape          | Shape         |
            | My next focus   | Deciding                   |               |
        When I open "<starting note>" note at top level
        Then I should not see these buttons: "<no buttons>"
        But I should be able to go to the "<button>" note "<expected title>"

        Examples:
        | starting note |  no buttons                              | button     |  expected title  |
        | Shape         | next sibling, previous sibling, previous | next       |  Square          |
