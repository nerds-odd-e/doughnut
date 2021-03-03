Feature: Note display
    As a learner, I want to read my note or show my note
    to other people.

    Background:
        Given I've logged in as an existing user

    Scenario: Long description is abbreviated in card view
        Given there are some notes for the current user
            | title                                    | description          |
            | Potentially shippable product increment  | The output of every Sprint is called a Potentially Shippable Product Increment. The work of all the teams must be integrated before the end of every Sprint—the integration must be done during the Sprint.  |
        Then I should see these notes belonging to the user at the top level of all my notes
            | note-title                               | note-description          |
            | Potentially shippable product increment  | The output of every Sprint is called a Potentia... |

    Scenario: The note display page should contain the total descendant cout
        Given there are some notes for the current user
            | title                | testingLinkTo |
            | Shape                |               |
            | Rectangle            | Shape         |
            | Square               | Rectangle     |
            | Triangle             | Shape         |
            | Equilateral triangle | Triangle      |
            | Circle               | Shape         |
        When I open "Shape" note from top level
        Then I should see there are 5 descendants

