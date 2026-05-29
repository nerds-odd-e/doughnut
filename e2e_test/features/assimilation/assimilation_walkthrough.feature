Feature: Assimilation walkthrough
  As a learner, I want to walk through assimilation from the menu or note page
  with keep/skip advancing to the next note, toasts past the daily cap, and
  clear feedback when nothing remains.

  Background:
    Given I am logged in as an existing user
    And my daily new notes to assimilate is set to 2
    And there are notes from Note 1 to Note 5

  Scenario: Menu shows assimilation progress midway through daily plan
    Given the note "Note 1" was assimilated on day 1
    When I jump to the note page of "Note 2"
    Then I should see assimilation menu progress

  Scenario: Walk through notes with menu, keep, skip, toasts, and panel on note page
    Given It's day 1
    When I start assimilation from the menu
    Then I should be assimilating the note "Note 1"
    And I should see assimilation progress "0/2/5"
    When I keep for recall on the assimilation panel
    Then I should be assimilating the note "Note 2"
    And I should see assimilation progress "1/2/5"
    When I keep for recall on the assimilation panel
    Then I should see the daily assimilation goal toast
    And I should be assimilating the note "Note 3"
    When I skip recall on the assimilation panel
    Then I should be assimilating the note "Note 4"
    When I keep for recall on the assimilation panel
    Then I should be assimilating the note "Note 5"
    When I keep for recall on the assimilation panel
    Then I should see the no more notes to assimilate toast
    And I should still be on the note page for "Note 5"
    When I jump to the note page of "Note 1"
    And I open assimilation settings from more options
    Then the keep for recall button should be disabled
