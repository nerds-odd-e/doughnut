Feature: Note creation should have description if wikidata is a person
  As a learner, I want to to create a note. If the note is a person I want the birthday and country
  to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And Wikidata.org has an entity "Q22502" with title "Taiwan"
    And Wikidata.org has an entity "Q736936" with title "Lu"
    And Wikidata.org has an entity "Q706446" with title "Wang Chien-ming"
    And Wikidata.org has an entity "Q4604" with title "Confucius"
    And Wikidata.org entity "Q706446" is a person from "Q22502" and birthday is "+1980-03-31T00:00:00Z"
    And Wikidata.org entity "Q4604" is a person from "Q736936" and birthday is "-0552-10-09T00:00:00Z"
    And there are some notes for the current user
      | title  | testingParent |
      | People |               |
      | Taiwan | People        |

  @usingMockedWikidataService
  Scenario Outline: Create a note for a person with wikidata should auto fill the description
    When I create a note belonging to "People":
      | Title         | Wikidata Id  |
      | <person name> | <wikidataId> |
    Then I should see the note description on current page becomes "<expected description>"

    Examples:
      | person name     | wikidataId | expected description     |
      | Wang Chien-ming | Q706446    | Taiwan, 31 March 1980    |
      | Confucius       | Q4604      | Lu, 09 October 0552 B.C. |


  @usingMockedWikidataService
  Scenario: Create a note for the country of origin when the person is created
    When I create a note belonging to "People":
      | Title     | Wikidata Id  |
      | Confucius | Q4604        |
    Then On the current page, I should see "Confucius" has link "related to" "Lu"
    And I should see "My Notes/People/Confucius" with these children
      | note-title |
      | Lu         |

  @usingMockedWikidataService
  Scenario: link to the country of orgin note if it already exists
    When I create a note belonging to "People":
      | Title           | Wikidata Id  |
      | Wang Chien-ming | Q706446      |
    Then On the current page, I should see "Wang Chien-ming" has link "related to" "Taiwan"
