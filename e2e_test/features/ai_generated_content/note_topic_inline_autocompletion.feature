@ignore
@usingMockedOpenAiService
Feature: Note Topic Inline Auto Completion

  Background:
    Given I am logged in as an existing user
    And OpenAI for topic will complete the phrase "Scr" with "um"

  @ignore
  Scenario Outline: note topic auto suggestion
    When I create a new note and type <Typed characters>
    Then I see the suggestion <Suggestion>

    Examples:
      | Typed characters | Suggestion | Comment                                        |
      | S                |            |                                                |
      | Sc               |            |                                                |
      | Scr              | um         |                                                |
      | Scrum            |  Process   | Watch out for the space in front of " Process" |
      | Scrum Ma         | ster       |                                                |

  Scenario: note topic auto suggestion is accepted
    When I create a new note and type "Scr"
    Then I see the suggestion "um"
    When I accept the topic suggestion
    Then I see the topic "Scrum"
