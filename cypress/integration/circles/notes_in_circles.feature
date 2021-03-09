Feature: Notes in circles
  As a learner, I want to create circles so that I can own notes together with other people.

  @ignore
  Scenario: Creating note that belongs to the circle
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I've logged in as "old_learner"
    And I create a note "Team agreement" in circle "Odd-e SG Team"
    When I've logged in as "another_old_learner"
    Then I should see the note "Team agreement" in circle "Odd-e SG Team"

  Scenario: Sharing my note to the circle
  Scenario: Sharing circle note to bazaar
