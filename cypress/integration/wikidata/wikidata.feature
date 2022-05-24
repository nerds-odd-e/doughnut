Feature: Validate Wikidata link

  Background:
    Given I've logged in as an existing user

  Scenario: Fetch a wikidata record
    Given there is a wikidata record "Q123"
    When I fetch for "Q123" from Wikidata
    Then I can get a payload "OK"

  @ignore
  Scenario: Validate wikidata link
    Given there are some notes for the current user
    | title | description |
    | TDD   |             |

    When I associate "TDD" with wikidata id "Q123"
    Then I should see association confirmation message "Link to WikiData Successful"

