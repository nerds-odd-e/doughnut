Feature: AI asks clarifying questions when auto-generating note details
  To have better auto-generated note details, I want to answer clarifying questions from the AI.

  @ignore
  @usingMockedOpenAiService
  Scenario Outline: User supplies an answer to a clarifying question
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic   | details                        |
      | People  | The people of Taipei are great |
    And I visit note "People"
    And OpenAI assistant will ask the question "Do you mean great as in big, or as in wonderful?" and generate no note details
    When I request to complete the details for the note "People"
    And I <react> to the clarifying question "Do you mean great as in big, or as in wonderful?"
    Then the note details on the current page should be "<note details>"

    Examples:
        | react                   | note details                                    | 
        | answer with "wonderful" | The people of Taipei are wonderful individuals. |
        | answer with "tall"      | The average height of Taipeier is 1.7m.         |
        | cancel                  | The people of Taipei are great                  |

