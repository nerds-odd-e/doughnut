Feature: Notes in circles
  As a learner, I want to create circles so that I can own notes together with other people.

  Background:
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I've logged in as "old_learner"
    And  I create a notebook "Shared info" in circle "Odd-e SG Team"


  Scenario: Realtime view when note is deleted in circle page
    Given I am on "Odd-e SG Team" circle page
    When someone of my circle deletes the "Shared info" notebook
    Then I should not see "Shared info" in the circle page within 5 seconds after deletion
