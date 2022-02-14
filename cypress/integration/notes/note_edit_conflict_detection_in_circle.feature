Feature: Note edit conflict detection
  As a learner, I want to be notified when other user in the same circle
  is editing the same field of the same note

  Background:
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I've logged in as "old_learner"
    And I create a notebook "Team agreement" in circle "Odd-e SG Team"
    And I add a note "Keep it complex" under "Team agreement"

  @ignore
  Scenario: Edit a note title
    When Other update note "Keep it complex" to become
      | old_title        | title              | description       |
      | Keep it complex  | Keep it simple     | Our best training |
    And I edit note "Keep it complex" to become:
      | Title               | Description         |
      | Keep it complex     | Keep it not complex |
    Then I should see alert "Conflict detected, change cannot be saved" in the page
