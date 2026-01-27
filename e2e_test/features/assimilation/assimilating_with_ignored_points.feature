Feature: Assimilation With Ignored Points
  As a learner, I want to selected points to be ignored in the checklist when generating questions for my note.
  So that I can focus on the important points and not be distracted by the irrelevant points.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Animals" which skips review
    And there are some notes:
      | Title | Details                                                      | Skip Memory Tracking | Parent Title |
      | Lion  | Lion is a large cat native to Africa and also called the king of the jungle | false                | Animals      |

  @usingMockedOpenAiService
  Scenario Outline: AI generated question - ignore checklist topic in question options
    Given OpenAI generates understanding checklist with points:
      | Lion is a large cat native to Africa |
      | also called the king of the jungle   |
    And AI will generate a question when prompt include "Ignore the topic 'also called the king of the jungle'":
      | Question Stem                    | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | Where are lions native to?       | Africa         | Asia               | Europe             |
    And AI will generate a question when prompt doesn't include "Ignore the topic 'also called the king of the jungle'":
      | Question Stem                      | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | What is the lion also known as?    | king of the jungle | tiger            | leopard            |
    When I am assimilating the note "Lion"
    And one of the checklist topic is selected to ignore "<ignored_point>" and assimilate the note
    And I go to the recalls page
    Then I should be asked "<expected_question_stem>"

    Examples:
      | ignored_point                        | expected_question_stem              |
      | also called the king of the jungle   | Where are lions native to?          |
      | Lion is a large cat native to Africa | What is the lion also known as?     |
