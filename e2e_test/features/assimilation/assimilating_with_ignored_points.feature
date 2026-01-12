Feature: Assimilation With Ignored Points
  As a learner, I want to selected points to be ignored in the checklist when generating questions for my note.
  So that I can focus on the important points and not be distracted by the irrelevant points.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Countries" which skips review
    And there are some notes:
      | Title       | Details                                                        | Skip Memory Tracking | Parent Title |
      | Netherlands | The Netherlands is a country in Europe and also called Holland | false                | Countries    |

  @usingMockedOpenAiService
  Scenario Outline: AI generated question - ignore checklist topic in question options
    Given OpenAI generates understanding checklist with points:
      | The Netherlands is a country in Europe |
      | also called Holland                    |
    And AI will generate a question when prompt include "Ignore the topic 'also called Holland'":
      | Question Stem               | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | Netherlands is also called? | Low country    | Germany            | France             |
    And AI will generate a question when prompt doesn't include "Ignore the topic 'also called Holland'":
      | Question Stem               | Correct Choice | Incorrect Choice 1 | Incorrect Choice 2 |
      | Netherlands is also called? | Holland        | Germany            | France             |
    When I am assimilating new note on day 1
    And one of the checklist topic is selected to ignore "<ignored_point>" and assimilate the note
    Then the question generated for the note "Netherlands" should not include "<excluded_text>"

    Examples:
      | ignored_point                          | excluded_text |
      | also called Holland                    | Holland       |
      | The Netherlands is a country in Europe | Low country   |
