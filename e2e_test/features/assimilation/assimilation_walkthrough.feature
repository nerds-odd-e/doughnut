Feature: Assimilation walkthrough from menu
  As a learner, I want the Assimilate menu action to walk me through notes
  with clear feedback when my daily goal is met or nothing remains.

  Background:
    Given I am logged in as an existing user
    And my daily new notes to assimilate is set to 2
    And there are notes from Note 1 to Note 5

  Scenario: Past daily cap shows goal toast and loads next note
    Given the note "Note 1" was assimilated on day 1
    And the note "Note 2" was assimilated on day 1
    When I jump to the note page of "Note 2"
    And I start assimilation from the menu
    Then I should see the daily assimilation goal toast
    And I should be assimilating the note "Note 3"

  Scenario: Nothing left shows no-more toast and stays on current note
    Given the note "Note 1" was assimilated on day 1
    And the note "Note 2" was assimilated on day 1
    And the note "Note 3" was assimilated on day 1
    And the note "Note 4" was assimilated on day 1
    And the note "Note 5" was assimilated on day 1
    When I jump to the note page of "Note 5"
    And I start assimilation from the menu
    Then I should see the no more notes to assimilate toast
    And I should still be on the note page for "Note 5"
