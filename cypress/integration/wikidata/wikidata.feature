Feature: Validate Wikidata link

  @ignore
  Scenario: Fetch a wikidata record
    Given I have a note with title TDD
    When I associate the note TDD with wikidata Q123
    Then I will see the wikidata link for Q123 for note TDD
