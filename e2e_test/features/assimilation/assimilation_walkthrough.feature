Feature: Assimilation walkthrough
  As a learner, I want to walk through assimilation from the menu or note page,
  seeing what Understanding added before I choose Next item, with skip
  leaving only a note's Understanding out of the sequence until I return it,
  toasts past the daily cap, and clear feedback when nothing remains.

  Background:
    Given I am logged in as an existing user
    And my daily new notes to assimilate is set to 2
    And there are notes from Note 1 to Note 5

  Scenario: Menu shows assimilation progress midway through daily plan
    Given It's day 1
    When I start assimilation from the menu
    And I assimilate on the assimilation panel
    Then I should see assimilation menu progress

  Scenario: Starting assimilation shows blocking loading for the next note
    Given It's day 1
    When I start assimilation from the menu while the next note loads slowly
    Then I should be assimilating the note "Note 1"
    And I should see assimilation progress "2/5"

  Scenario: Assimilating waits at its result until Next item, with progress and daily goal toast
    Given It's day 1
    When I start assimilation from the menu
    Then I should be assimilating the note "Note 1"
    And I should see assimilation progress "2/5"
    When I assimilate on the assimilation panel
    Then I should see Understanding added to recall for "Note 1"
    When I choose Next item
    Then I should be assimilating the note "Note 2"
    And I should see assimilation progress "1/4"
    When I assimilate on the assimilation panel
    And I choose Next item
    Then I should see the daily assimilation goal toast
    And I should be assimilating the note "Note 3"

  Scenario: Skip and continue until no more notes to assimilate
    Given It's day 1
    And the note "Note 1" was assimilated on day 1
    And the note "Note 2" was assimilated on day 1
    When I start assimilation from the menu
    Then I should be assimilating the note "Note 3"
    When I skip on the assimilation panel
    Then I should be assimilating the note "Note 4"
    When I assimilate on the assimilation panel
    And I choose Next item
    Then I should be assimilating the note "Note 5"
    When I assimilate on the assimilation panel
    And I assimilate it as commissioned
    Then I should see Understanding added to recall for "Note 5"
    When I choose Next item
    Then I should see the no more notes to assimilate toast
    And I should still be on the note page for "Note 5"

  Scenario: Already assimilated note cannot be assimilated again
    Given the note "Note 1" was assimilated on day 1
    When I jump to the note page of "Note 1"
    And I open the assimilation panel
    Then assimilate should be disabled

  Scenario: Skip leaves only the note's Understanding out until Return to sequence
    Given It's day 1
    And I have a note "Vitamins" under notebook "Nutrition" with content:
      """
      ---
      topic: micronutrients
      ---

      Vitamin notes body.
      """
    When I start assimilation from the menu
    Then I should be assimilating the note "Vitamins"
    When I skip on the assimilation panel
    Then I should be at property "topic" of note "Vitamins"
    When I jump to the note page of "Vitamins"
    And I open the assimilation panel
    And I assimilate it as commissioned
    Then I should see a commissioned memory tracker
    And I should see Return to sequence on the assimilation panel
    And assimilate should be enabled
    When I return to sequence on the assimilation panel
    Then I should see Skip on the assimilation panel
    When I start assimilation from the menu
    Then I should be assimilating the note "Vitamins"

  Scenario: Assimilating a skipped note creates understanding and leaves the sequence
    Given It's day 1
    When I start assimilation from the menu
    Then I should be assimilating the note "Note 1"
    When I skip on the assimilation panel
    Then I should be assimilating the note "Note 2"
    When I jump to the note page of "Note 1"
    And I open the assimilation panel
    Then assimilate should be enabled
    When I assimilate on the assimilation panel
    Then I should see Understanding added to recall for "Note 1"
    When I choose Next item
    Then I should be assimilating the note "Note 2"
    And I should see assimilation progress "1/4"
    When I jump to the note page of "Note 1"
    And I open the assimilation panel
    Then assimilate should be disabled

  Scenario: Walkthrough does not offer notes from a Skip Memory Tracking notebook
    Given It's day 1
    And I have a notebook "Private archive" with a note "Archive memo"
    And I change notebook "Private archive" to skip memory tracking
    When I start assimilation from the menu
    Then I should be assimilating the note "Note 1"
    And I should see assimilation progress "2/5"
