Feature: Retrieve Realtime Updates for Comments

  Background:
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I've logged in as "old_learner"
    And I create a notebook "Shapes" in circle "Odd-e SG Team"
    And I add a note "Triangle" under "Shapes"

  @ignore
  Scenario: New comment is added by another user
    When "another_old_learner" adds a comment with description "Needs a description"
    Then I should see a comment with content "Needs a description"

  @ignore
  Scenario: Existing comment is edited by another user
    Given "another_old_learner" adds a comment for note "Triangle" with description "Needs a description"
    When "another_old_learner" edits the comment with description "Please add description for triangle"
    Then I should see a comment with content "Please add description for triangle"