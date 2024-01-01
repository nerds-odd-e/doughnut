Feature: AI Asks Clarifying Questions When Auto-Generating Note Details
  To obtain better auto-generated note details, I want to answer clarifying questions from the AI.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topic   | details            |
      | Sports  | Football is        |
    And OpenAI service can create thread and run with id "thread-111" when requested
    And the OpenAI assistant is set to ask "Do you mean American Football or European Football?" for unclarified request on "Football is" in thread "thread-111"

  @usingMockedOpenAiService
  Scenario Outline: Responding to AI's Clarification Question
    Given the OpenAI assistant in thread "thread-111" is set to:
      | response | arguments                                           |
      | ask      | Do you mean American Football or European Football? |
      | complete | " originated from England."                         |
    And the OpenAI assistant will complete the details with " originated from England." if the clarifying answer contains "European"
    When I request to complete the details for the note "Sports"
    And I <respond> to the clarifying question "Do you mean American Football or European Football?"
    Then the note details on the current page should be "<note details>"

    Examples:
      | respond               | note details                                   |
      | answer "European"     | Football is originated from England.           |
      | respond with "cancel" | Football is                                    |

  @usingMockedOpenAiService
  Scenario: Managing Extended Clarification Dialogue
    Given the OpenAI assistant in thread "thread-111" is set to:
      | response | arguments                                           |
      | ask      | Do you mean American Football or European Football? |
      | ask      | Do you mean the American version?                   |
    Given the OpenAI assistant is set to ask "Do you mean American Football or European Football?" for unclarified request on "Football is" in thread "thread-111"
    And the OpenAI assistant will ask "Do you mean the American version?" following an unclear response like "Ameriland"
    When I request to complete the details for the note "Sports"
    And I answer "Ameriland" to the clarifying question "Do you mean American Football or European Football?"
    Then I should see a follow-up question "Do you mean the American version?"
    And the initial clarifying question with the response "Ameriland" should be visible
