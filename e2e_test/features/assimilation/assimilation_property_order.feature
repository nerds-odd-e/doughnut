Feature: Assimilation property order
  As a learner, I want a property I add to an old note to be offered after
  newer material, so additions to old notes do not jump to the front.

  Background:
    Given I am logged in as an existing user
    And It's day 1
    And there are notes from Note 1 to Note 2
    And the note "Note 1" was assimilated on day 1

  Scenario: A property added to an old note comes after a newer note
    Given It's day 2
    And I visit note "Note 1"
    When I add a rich note property with key "status" and value "draft"
    And I start assimilation from the menu
    Then I should be assimilating the note "Note 2"


  Scenario: Assimilating a list value continues at the next exact value
    Given the note "Note 2" was assimilated on day 1
    And I have a notebook "Item assimilation"
    And I have a note "Movement" under notebook "Item assimilation" with content:
      """
      ---
      activity: [walk, run, run]
      ---

      Ways to move.
      """
    And the note "Movement" was assimilated on day 1
    When I visit note "Movement"
    And I assimilate the value "walk" of property "activity"
    Then occurrence 1 of item "run" in property "activity" should be highlighted
