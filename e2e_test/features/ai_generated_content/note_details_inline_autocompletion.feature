@usingMockedOpenAiService
Feature: Details Inline Auto Completion

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic | details | testingParent |
      | Scrum |         |               |
    When I visit note "Scrum"

  Scenario Outline: Inline autocomplete note details
    Given OpenAI will complete the phrase "Scrum" with " is a popular Software Development Framework."
    And I type in the details the word "Scrum " followed by a space
    Then I see after "Scrum " the suggestion from AI: " is a popular Software Development Framework."
    When I <action>
    Then the note details are "<final note details>"
    And I continue typing <further word>
    Then the note details are <complete note details>

    Examples:
      | action                   | final note details                                 | further word  | complete note details                                                                       |
      | accept the AI suggestion | Scrum is a popular Software Development Framework. | "It"          | "Scrum is a popular Software Development Framework." |
      | continue typing "Master" | Scrum Master                                       | "s"           | "Scrum Masters"                                                        |
