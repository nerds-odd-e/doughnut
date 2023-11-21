@ignore
Feature: Details Inline Auto Completion

  Scenario: A sentence is suggested
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic   | details        | testingParent |
      | Scrum   |                |               |
    When I open the note "Scrum"
    And I type in the details the word "Scrum" followed by a space
    Then a suggestion from AI is shown after "Scrum "
     And the suggestion is "is a popular Software Development Framework"
