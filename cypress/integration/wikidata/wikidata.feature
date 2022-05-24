Feature: Validate Wikidata link

  @ignore
  Scenario: Fetch a wikidata record
    Given there is a wikidata record Q123
    When I fetch for Q123 from Wikidata
    Then I can get a payload ABCD
