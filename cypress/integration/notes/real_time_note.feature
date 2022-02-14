Feature: real time note
  As a learner, I want to see note in a circle that I currently view to 
  get a real time update, so that I won't miss any update that other people make.

  Background: 
    Given There is a circle "Odd-e SG Team" with "old_learner, another_old_learner" members
      And I've logged in as "old_learner"

  @ignore
  Scenario: Creating note that belongs to the circle
    When I create a notebook "Team agreement" in circle "Odd-e SG Team"
    When I've logged in as "another_old_learner"
    Then I should see the note "Team agreement" in circle "Odd-e SG Team"
    When I add a note "Keep it complex" under "Team agreement" 
    When I've logged in as "old_learner"
    Then I update note title "Team agreement" to become "New team agreement"
    When I've logged in as "another_old_learner"
    Then I should see the note "New team agreement" in circle "Odd-e SG Team"