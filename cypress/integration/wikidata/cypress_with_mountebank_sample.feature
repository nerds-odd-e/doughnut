Feature: I can fetch some data from an external service

  Scenario: Fetch a record from wikidata dummy service
    Given I have a record on "Mountebank" on the external service
    When I ask for the "Mountebank" record
    Then I should get the expected payload
