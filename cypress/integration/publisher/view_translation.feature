Feature: View Translate on Notes
  As a book editor I want to see translation of the Note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title   | titleIDN  | description      | descriptionIDN   |
      | English | Indonesia | English Language | Bahasa Indonesia |
    And I open the "article" view of note "English"

  @featureToggle
  Scenario: Note detail view default language and translation
    Then Note title on the page should be "English"
    When I switch language to "ID"
    Then Note title on the page should be "Indonesia"
    When I switch language to "EN"
    Then Note title on the page should be "English"

  @featureToggle
  Scenario Outline: Cards and Article view have language same with parent
    Given I switch language to "ID"
    When I switch to "<View>" view
    Then Note title on the page should be "<Title>"
    And Note description on the page should be "<Description>"

    Examples:
      | View         | Title     | Description      |
      | article view | Indonesia | Bahasa Indonesia |
      | cards view   | Indonesia | Bahasa Indonesia |