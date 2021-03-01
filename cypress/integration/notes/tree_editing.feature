Feature: Tree editing
    As a learner, I want to reorder my note and move it up in the tree.

    Background:
        Given I've logged in as an existing user
        And there are some notes for the current user
            | title                | testingLinkTo |
            | Shape                |               |
            | Rectangle            | Shape         |
            | Square               | Rectangle     |
            | Triangle             | Shape         |
            | Equilateral triangle | Triangle       |
            | Circle               | Shape         |

    Scenario: Move left right
        When I move note "Shape/Triangle" left
        Then I should see "Triangle" is before "Rectangle" in "Shape"
        When I move note "Shape/Triangle" right
        Then I should see "Rectangle" is before "Triangle" in "Shape"
