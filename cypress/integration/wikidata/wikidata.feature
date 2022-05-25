Feature: Validate Wikidata link

  Background:
    Given I've logged in as an existing user

  @ignore
  Scenario: Associate wikidata's id with note when wikidata's title is different from note's title
    Given there are some notes for the current user
      | title | description |
      | TDD   |             |
    And I visit note "TDD"
    And I associate the note "TDD" with wikidata id "Q12345"
    When I confirm the association with different title "XXXX"
    Then I should see the icon beside title linking to wikidata url with id "Q12345"

