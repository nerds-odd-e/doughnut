Feature: Notes in circles
  As a learner, I want to create circles so that I can own notes together with other people.

  Background:
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I am logged in as "old_learner"

  Scenario: Creating note that belongs to the circle
    When I create a notebook "Team agreement" in circle "Odd-e SG Team"
    Then I should see "Odd-e SG Team" in breadcrumb
    When I am re-logged in as "another_old_learner"
    Then I should see the notebook "Team agreement" in circle "Odd-e SG Team"
    When I add a note "Keep it complex" under "Team agreement"

  Scenario: subscribe to a note and view
    Given  There is a notebook "Team agreement" in circle "Odd-e SG Team" by "old_learner"
    When I subscribe to notebook "Team agreement" in the circle "Odd-e SG Team", with target of learning 1 notes per day
    Then  I should be able to edit the subscription to notebook "Team agreement"

  Scenario: move notebook to another circle
    Given There is a circle "Odd-e Thai Team" with "old_learner" members
    And  There is a notebook "Family gathering guidelines" in circle "Odd-e SG Team" by "old_learner"
    When I move the notebook "Family gathering guidelines" from "Odd-e SG Team" to "Odd-e Thai Team"
    Then I should see the notebook "Family gathering guidelines" in circle "Odd-e Thai Team"
