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
    And I continue typing <further word>
    Then the note details are <complete note details>
    

  Examples:
      | action                       | final note details                                                    | further word          | complete note details |
      | accept the AI suggestion     | Schroedinger-Team: Scrum is a popular Software Development Framework. | " The term comes from" | "Schroedinger-Team: Scrum is a popular Software Development Framework. The term comes from" |
      | continue typing "Master"     | Schroedinger-Team: Scrum Master                                       | " is"                 | "Schroedinger-Team: Scrum Master is" |