Feature: Retrieve Realtime Updates for Comments

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title    |
      | Triangle |

  @ignore
  Scenario: New comment is added by another user
    Given there is no comments for the note "Triangle"
    When another user adds a comment for note "Triangle" with description "Needs a description"
    Then I should see a comment with content "Needs a description"

  @ignore
  Scenario: Existing comment is edited by another user

    Given there is a comment in "Triangle" note with description "Needs a description"
    When another user edits the comment with description "Please add description for triangle"
    Then I should see a comment with content "Please add description for triangle"