@mockBrowserTime
Feature: Reify a property
  As a learner, I want to turn a property that links to another note into a relationship note
  so that the relationship can carry its own content while I keep recalling it.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Space topics" with notes:
      | Title |
      | Mars  |
    And I have a note "Moon" under notebook "Space topics" with content:
      """
      ---
      related: '[[Mars]]'
      ---

      Moon body.
      """
    And It's day 1, 8 hour
    And the note "Moon" has assimilated property "related"

  Scenario: Reifying a tracked property keeps its recall due on the relationship note
    When It's day 2, 9 hour
    Then I should see that I have 1 notes to recall
    When I visit note "Moon"
    And I reify the rich note property "related"
    Then I should be on the relationship note page from "Moon" with relation "related" to "Mars"
    And I should see that I have 1 notes to recall
    When I open the assimilation panel
    Then the note memory tracker should have recall count 0
