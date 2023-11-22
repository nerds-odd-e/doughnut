@usingMockedOpenAiService
Feature: Details Inline Auto Completion

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic | details | testingParent |
      | Scrum |         |               |
    When I visit note "Scrum"
    And I type in the details the word "Schroedinger-Team: Scrum " followed by a space

  @ignore
  Scenario Outline: Inline autocomplete note details
    Given OpenAI will complete the phrase "Schroedinger-Team: Scrum " with "is a popular Software Development Framework."
    Then I see after "Schroedinger-Team: Scrum " the suggestion from AI: "is a popular Software Development Framework."
    When I <action>
    Then the note details are "<final note details>"

  Examples:
      | action                     | final note details                                  |
      | accept the suggestion      | Schroedinger-Team: Scrum is a popular Software Development Framework. |

  @ignore
  Scenario: A sentence is suggested and declined by the user by continue typing
    When I continue typing "Master "
    Then I see after "Scrum Master " the suggestion from AI: "is a role in the Scrum Framework."
