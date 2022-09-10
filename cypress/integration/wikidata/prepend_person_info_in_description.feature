Feature: Prepend Person Info in Description
  As a user, when I associate my note of a person with their Wikidata,
  i want to automatically add their person information

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | testingParent | description    |
      | People  |               | Awesome people |

  @usingMockedWikidataService @mockBrowserTime @ignore
  Scenario: Add person with single birth date and country of citizenship
    Given Wikidata.org has an entity "Q36970" with "Jackie Chan"
    And "Jackie Chan" has "01-Jan-1955" in field "birth date" # not implemented yet
    And "Jackie Chan" has "Hong Kong" in field "country of citizenship" # also not implemented
    When I create a note belonging to "People":
      | Title       | Wikidata Id  |
      | Jackie Chan | Q36970       |
    And I should see the icon beside title linking to "https://www.wikidata.org/wiki/Q2102"
    Then I should see "Born 01-Jan-1955 \nCitizen of Hong Kong" in the page

