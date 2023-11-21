@ignore @usingMockedOpenAiService
Feature: Details Inline Auto Completion

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic   | details        | testingParent |
      | Scrum   |                |               |
    When I open the note "Scrum"
     And I type in the details the word "Scrum" followed by a space


  Scenario: A sentence is suggested
    Then I see after "Scrum " the suggestion from AI: "is a popular Software Development Framework."
    And the cursor is after "Scrum "

  Scenario: A sentence is suggested and accepted
    When I press "Tab"
    Then I should see "Scrum is a popular Software Development Framework." in the page
     And the cursor is after "Software Development Framework."

  Scenario: A sentence is suggested and declined by the user by continue typing
    When I continue typing "Master "
    Then I see after "Scrum Master " the suggestion from AI: "is a role in the Scrum Framework."
     And the cursor is after "Scrum Master "

