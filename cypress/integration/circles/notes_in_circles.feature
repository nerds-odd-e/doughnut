Feature: Notes in circles
  As a learner, I want to create circles so that I can own notes together with other people.

  Background:
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I've logged in as "old_learner"

  Scenario: Creating note that belongs to the circle
    When I create a note "Team agreement" in circle "Odd-e SG Team"
    Then I should see "Circles, Odd-e SG Team" in breadcrumb
    When I've logged in as "another_old_learner"
    Then I should see the note "Team agreement" in circle "Odd-e SG Team"
    When I add a note "Keep it complex" under "Team agreement"

  Scenario: subscribe to a note and review
    When I create a note "Team agreement" in circle "Odd-e SG Team"
    Then I subscribe to note "Team agreement" in the circle "Odd-e SG Team", with target of learning 1 notes per day
    And  I should be able to edit the subscription to notebook "Team agreement"

