Feature: AI asks clarifying questions when auto-generating note details
  To have better auto-generated note details, I want to answer clarifying questions from the AI.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic   | details        | testingParent |
      | Taiwan  |                |               |
      | Taipei  | It is a        | Taiwan        |
      | People  | The people of  | Taipei        |

  @ignore
  @usingMockedOpenAiService
  Scenario: AI needs to ask a clarifying question
    Given I update note "People" details from "The people of " to become "The people of Taipei are great"
    When I request to complete the details for the note "People"
    Then OpenAI assistant will ask the question "Do you mean great as in big, or as in wonderful?" and generate no note details

  @ignore
  @usingMockedOpenAiService
  Scenario: User supplies an answer to a clarifying question
    Given I update note "People" details from "The people of " to become "The people of Taipei are great"
    When I request to complete the details for the note "People"
    Then OpenAI assistant asks the question "Do you mean great as in big, or as in wonderful?" and generate no note details
    When I answer with "wonderful"
    Then the note details on the current page should be "The people of Taipei are wonderful individuals."

  @ignore
  @usingMockedOpenAiService
  Scenario: AI doesn't need to ask a clarifying question
    Given I update note "People" details from "The people of " to become "The people of Taipei are wonderful"
    When I request to complete the details for the note "People"
    Then the note details on the current page should be "The people of Taipei are wonderful individuals."
