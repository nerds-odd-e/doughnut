Feature: Note creation should have description if wikidata is a human
  As a learner, I want to to create a note. If the note is a human I want the birthday and country
  to be included in the description of the new note.


  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title | description |
      | human | some desc   |
    And Wikidata.org has an entity "Q706446" with title "Wang Chien-ming"
    And Wikidata.org entity "Q706446" is a human with date on birthday "31 March 1980" and country of citizenship "Taiwan"

  Scenario: New Note creation and wikidata is selected by user
    When I create a note belonging to "human":
      | Title           | Wikidata Id |
      | Wang Chien-ming | Q706446     |
    Then I should see the description becomes "31 March 1980"
