Feature: Note creation should have description if wikidata is a person
  As a learner, I want to to create a note. If the note is a person I want the birthday and country
  to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And Wikidata.org has an entity "Q706446" with title "Wang Chien-ming"
    And Wikidata.org has an entity "Q4604" with title "Confucius"
    And there are some notes for the current user
      | title  | testingParent |
      | People |               |
      | Taiwan | People        |

  @usingMockedWikidataService
  Scenario Outline: Create a note for a person with wikidata should auto fill the description
    Given Wikidata.org has an entity "Q12345" with title "<country of origin>"
    And Wikidata.org entity "<wikidataId>" is a person from "Q12345" and birthday is "<birthday>"
    When I create a note belonging to "People":
      | Title         | Wikidata Id  |
      | <person name> | <wikidataId> |
    Then I should see the note description on current page becomes "<description>"

    Examples:
      | person name     | wikidataId | country of origin | birthday              | description              |
      | Wang Chien-ming | Q706446    | Taiwan            | +1980-03-31T00:00:00Z | Taiwan, 31 March 1980    |
      | Confucius       | Q4604      | Lu                | -0552-10-09T00:00:00Z | Lu, 09 October 0552 B.C. |


  @usingMockedWikidataService
  Scenario Outline: New Note creation and person wikidata is selected
    Given Wikidata.org has an entity "Q12345" with title "<country of origin>"
    And Wikidata.org entity "<wikidataId>" is a person from "Q12345" and birthday is "<birthday>"
    When I create a note belonging to "People":
      | Title         | Wikidata Id  |
      | <person name> | <wikidataId> |
    Then I should see the note description on current page becomes "<description>"
    And I should see "My Notes/People/<person name>" with these children
      | note-title          |
      | <country of origin> |

    Examples:
      | person name     | wikidataId | country of origin | birthday              | description              |
      | Confucius       | Q4604      | Lu                | -0552-10-09T00:00:00Z | Lu, 09 October 0552 B.C. |


  @usingMockedWikidataService
  Scenario Outline: New Note creation and person wikidata is selected and country of orgin note already exists
    Given Wikidata.org has an entity "Q12345" with title "<country of origin>"
    And Wikidata.org entity "<wikidataId>" is a person from "Q12345" and birthday is "<birthday>"
    When I create a note belonging to "People":
      | Title         | Wikidata Id  |
      | <person name> | <wikidataId> |
    Then I should see the note description on current page becomes "<description>"
    Then On the current page, I should see "<person name>" has link "related to" "<country of origin>"

    Examples:
      | person name     | wikidataId | country of origin | birthday              | description              |
      | Wang Chien-ming | Q706446    | Taiwan            | +1980-03-31T00:00:00Z | Taiwan, 31 March 1980    |
