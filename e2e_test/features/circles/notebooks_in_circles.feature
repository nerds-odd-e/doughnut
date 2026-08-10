Feature: Notebooks in circles
  As a learner, I want notebooks in circles so that circle members can share and subscribe to notes.

  Background:
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members

  Scenario: Circle member adds a note under a circle notebook
    Given There is a notebook "Team agreement" in circle "Odd-e SG Team" by "old_learner"
    When "another_old_learner" adds a note "Keep it complex" under notebook "Team agreement"
    Then the note title should be "Keep it complex"

  Scenario: Subscribing to a circle notebook
    Given I am logged in as "old_learner"
    And There is a notebook "Team agreement" in circle "Odd-e SG Team" by "old_learner"
    When I subscribe to notebook "Team agreement" in the circle "Odd-e SG Team", with daily assimilation target of 1 notes per day
    Then I should be able to edit the subscription to notebook "Team agreement"

  Scenario: Moving a notebook to another circle
    Given I am logged in as "old_learner"
    And There is a circle "Odd-e Thai Team" with "old_learner" members
    And There is a notebook "Family gathering guidelines" in circle "Odd-e SG Team" by "old_learner"
    When I move the notebook "Family gathering guidelines" from "Odd-e SG Team" to "Odd-e Thai Team"
    Then I should see the notebook "Family gathering guidelines" in circle "Odd-e Thai Team"

  Scenario: Creating a notebook group from the circle catalog
    Given I am logged in as "old_learner"
    When I create a notebook "Circle catalog nb" in circle "Odd-e SG Team"
    And I am on "Odd-e SG Team" circle page
    And I create a notebook group named "Circle E2E Group" by moving notebook "Circle catalog nb" from the circle catalog
    Then I should see notebook group "Circle E2E Group" with a hint including "Circle catalog nb" on the circle page
