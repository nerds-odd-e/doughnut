Feature: Notes in circles
  As a learner, I want to create circles so that I can own notes together with other people.

  Scenario: Creating note that belongs to the circle
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
    And I've logged in as "old_learner"
    When I create a note "Team agreement" in circle "Odd-e SG Team"
    Then I should see "Circles, Odd-e SG Team, Team agreement" in breadcrumb
    When I've logged in as "another_old_learner"
    Then I should see the note "Team agreement" in circle "Odd-e SG Team"

  Scenario: Sharing my note to the circle
  Scenario: Sharing circle note to bazaar
