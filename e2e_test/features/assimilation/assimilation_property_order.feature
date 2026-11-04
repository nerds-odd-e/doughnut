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
