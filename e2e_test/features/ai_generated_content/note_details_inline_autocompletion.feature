@usingMockedOpenAiService
Feature: Details Inline Auto Completion

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic | details | testingParent |
      | Scrum |         |               |
    When I visit note "Scrum"
    And I type in the details the word "Schroedinger-Team: Scrum " followed by a space

  Scenario Outline: Inline autocomplete note details
    Given OpenAI will complete the phrase "Schroedinger-Team: Scrum " with "is a popular Software Development Framework."
    Then I see after "Schroedinger-Team: Scrum " the suggestion from AI: "is a popular Software Development Framework."
    When I <action>
    Then the note details are "<final note details>"

  Examples:
      | action                    | final note details                                  |
      | accept the AI suggestion     | Schroedinger-Team: Scrum is a popular Software Development Framework. |
      | continue typing "Master"  | Schroedinger-Team: Scrum Master |

