@usingMockedOpenAiService
Feature: User Contests Question generation by AI
  As a learner, I want to contest the question generated by AI, so that I can get a better question for my note.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Scuba Diving"
    And OpenAI assistant will create these thread ids in sequence: "thread-first-question, thread-evaluate, thread-second-question"
    Given OpenAI generates this question for assistant thread "thread-first-question":
      | Question Stem                                       | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the most common scuba diving certification? | Rescue Diver   | Divemaster         | Open Water Diver   |

  Scenario Outline: I should be able to regenerate the question when the question and choices do not make sense relating to the note
    Given OpenAI evaluates the question as <Legitimate Question> for assistant thread "thread-evaluate"
    And OpenAI generates this question for assistant thread "thread-second-question":
      | Question Stem         | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is scuba diving? | Rescue Diver   | Divemaster         | Open Water Diver   |
    When I test myself for the note "Scuba Diving"
    And I contest the question
    Then I should see the question "What is the most common scuba diving certification?" is <Old Question Status>
    And I should be asked "<Current Question>"

    Examples:
    | Legitimate Question | Old Question Status | Current Question                                    |
    # | legitamate          | enabled             | What is the most common scuba diving certification? |
    | not legitamate      | disabled            | What is scuba diving?                               |
