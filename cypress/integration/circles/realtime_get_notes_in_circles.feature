Feature: Notes in circles
  As a learner, I want to create circles so that I can own notes together with other people.

  Background:
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I've logged in as "old_learner"
    And  I create a notebook "Butter Deleted Note" in circle "Odd-e SG Team"


  Scenario: Realtime view when note is deleted in circle page
    When I am on "Odd-e SG Team" circle page
    And A member "another_old_learner" of my circle delete the "Butter Deleted Note"
    Then I should not see "Butter Deleted Note" in the circle page within 5 seconds after deletion
