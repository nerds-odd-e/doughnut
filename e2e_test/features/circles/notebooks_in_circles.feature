Feature: Notes in circles
  As a learner, I want to create circles so that I can own notes together with other people.

  Background:
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I am logged in as "old_learner"

  Scenario: Creating note that belongs to the circle
    When I create a notebook "Team agreement" in circle "Odd-e SG Team"
    Then I should see "Odd-e SG Team" in breadcrumb
    When I am logged in as "another_old_learner"
    Then I should see the notebook "Team agreement" in circle "Odd-e SG Team"
    When I add a note "Keep it complex" under "Team agreement"

  Scenario: subscribe to a note and review
    Given  There is a notebook "Team agreement" in circle "Odd-e SG Team"
    When I subscribe to notebook "Team agreement" in the circle "Odd-e SG Team", with target of learning 1 notes per day
    Then  I should be able to edit the subscription to notebook "Team agreement"

  @mockBrowserTime
  Scenario: Realtime view when note is created and deleted in circle page
    Given I am on "Odd-e SG Team" circle page
    When  There is a notebook "Shared info" in circle "Odd-e SG Team"
    Then I should see "Shared info" in the circle page within 5 seconds
    When someone of my circle deletes the "Shared info" notebook
    Then I should not see "Shared info" in the circle page within 5 seconds
