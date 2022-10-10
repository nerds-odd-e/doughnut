Feature: Note creation should have description if wikidata is a person
  As a learner, I want to to create a note. If the note is a person I want the birthday and country
  to be included in the description of the new note.

  Background:
    Given I've logged in as an existing user
    And I create a notebook with title

  @usingMockedWikidataService
  Scenario Outline: New Note creation and person wikidata is selected
    Given Wikidata.org has an entity "<wikidataId>" with title "<person name>"
    # And Wikidata.org has an entity "Q12345" with title "<country of origin>"
    And Wikidata.org entity "<wikidataId>" is a person from "Q12345" and birthday is "<birthday>"
    When I create a note with title "<person name>" and wiki id "<wikidataId>"
    When I should see the note description on current page becomes "<description>"
    Examples:
      | person name    | wikidataId | contry of origin | birthday              | description |
      | Wang Chien-ming | Q706446    | Taiwan           | +1980-03-31T00:00:00Z | 31 March 1980 |
      | Confucius       | Q4604      | Lu               | -0552-10-09T00:00:00Z | 09 October 0552 B.C. |

  Scenario Outline: New Note creation and person wikidata is selected
    When I create a note with title "<person name>" and wiki id "<wikidataId>"
    When I should see the note description on current page becomes "<description>"
    Examples:
      | person name    | wikidataId | contry of origin | birthday              | description |
      | Wang Chien-ming | Q706446    | Taiwan           | +1980-03-31T00:00:00Z | Taiwan, 31 March 1980 |
      | Confucius       | Q4604      | Lu               | +1980-03-31T00:00:00Z | Lu, 09 October 0552 B.C. |
