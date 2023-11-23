Feature: AI asks clarifying questions when auto-generating note details
  To have better auto-generated note details, I want to answer clarifying questions from the AI.

  @usingMockedOpenAiService
  Scenario Outline: User supplies an answer to a clarifying question
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic   | details                        |
      | Sports  | Football                       |
    And I visit note "Sports"
    When I request to complete the details for the note "Sports"
    Then OpenAI assistant will ask the question "Do you mean American Football or European Football?" and generate no note details
    And I <react> to the clarifying question "Do you mean American Football or European Football?"
    Then The clarification question dialog is invisible
    And the note details on the current page should be "<note details>"

    Examples:
        | react                   | note details                                    | 
        | answer with "European"  | European football origins from England.         |
        | answer with "American"  | American football origins from the USA.         |
        | cancel                  | Football                                        |

