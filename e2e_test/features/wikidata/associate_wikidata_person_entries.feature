Feature: Note creation should have details if wikidata is a person
  As a learner, I want to to create a note. If the note is a person I want the birthday and country
  to be included in the details of the new note.

  Background:
    Given I am logged in as an existing user
    And Wikidata.org has an entity "Q22502" with label "Taiwan"
    And Wikidata.org has an entity "Q736936" with label "Lu"
    And Wikidata.org has an entity "Q706446" with label "Wang Chien-ming"
    And Wikidata.org has an entity "Q4604" with label "Confucius"
    And Wikidata.org entity "Q706446" is a person from "Q22502" and birthday is "+1980-03-31T00:00:00Z"
    And Wikidata.org entity "Q4604" is a person from "Q736936" and birthday is "-0552-10-09T00:00:00Z"
    And there are some notes for the current user:
      | topic  | testingParent | wikidataId |
      | People |               |            |
      | Taiwan | People        | Q22502     |

  @usingMockedWikidataService
  Scenario Outline: Create a note for a person with wikidata should auto fill the details
    When I create a note belonging to "People":
      | Topic         | Wikidata Id  |
      | <person name> | <wikidataId> |
    Then the note details on the current page should be "<expected details>"

    Examples:
      | person name     | wikidataId | expected details           |
      | Wang Chien-ming | Q706446    | Taiwan, 31 March 1980    |
      | Confucius       | Q4604      | Lu, 09 October 0552 B.C. |


  @usingMockedWikidataService
  Scenario: Create a note for the country of origin when the person is created
    When I create a note belonging to "People":
      | Topic     | Wikidata Id  |
      | Confucius | Q4604        |
    Then On the current page, I should see "Confucius" has link "related to" "Lu"
    And I should see "My Notes/People/Confucius" with these children
      | note-topic |
      | Lu         |

  @usingMockedWikidataService
  Scenario: link to the country of orgin note if it already exists
    When I create a note belonging to "People":
      | Topic           | Wikidata Id  |
      | Wang Chien-ming | Q706446      |
    Then On the current page, I should see "Wang Chien-ming" has link "related to" "Taiwan"
    # this check is not sufficient, should check new note is not create for taiwan
