Feature: Validate Wikidata link

  Background:
    Given I've logged in as an existing user

  Scenario: Associate wikidata's id with note when wikidata's title is different from note's title
    Given there are some notes for the current user
      | title | description |
      | TDD   |             |

    And I associate "TDD" with wikidata id "Q123"
    When I confirm the association with different title
    Then I should see association icon