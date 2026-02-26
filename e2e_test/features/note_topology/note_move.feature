Feature: note move
  As a learner, I want to move a note to become a child of another note,   so that I can recall them in the
  future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Sedition" and details "Incite violence"
    And I have a notebook with the head note "Sedation" and details "Put to sleep"
    And I have a notebook with the head note "Sedative" and details "Sleep medicine"

  @mockBrowserTime
  Scenario: link and move
    Given I move note "Sedition" to be under note "Sedation"
    When I visit all my notebooks
    Then I should not see note "Sedition" at the top level of all my notes
