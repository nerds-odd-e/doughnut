@ignore
Feature: Add comment to note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description | parentTitle |
      | Taiwan | 台北 best     | N.A.        |

  @ignore
  Scenario: One or many comments can be added to a note
    When I add a comment with "This is a new comment"
    Then I should see a comment added with "This is a new comment"
