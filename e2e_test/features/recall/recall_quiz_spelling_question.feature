@disableOpenAiService
Feature: Spelling recall quiz
  As a learner, I want spelling quizzes in recall to practice writing note titles from their content.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title      | Content                                    | Skip Memory Tracking | Remember Spelling |
      | English    |                                            | true                 |                   |
      | sedition   | Sedition means incite violence             |                      | true              |
      | LinkTarget | A note linked from spelling content        |                      |                   |
      | Wikistudy  | Wikistudy uses [[LinkTarget]] for practice |                      | true              |
    And It's day 1

  Scenario: Spelling quiz accepts a correct answer
    Given the note "sedition" was assimilated on day 1
    When I visit recall for a due quiz question on day 2
    Then I should be asked spelling question "means incite violence" from notebook "English practice"
    When I type my answer "Sedition"
    Then I should see that my last answer to spelling question is correct

  Scenario: Spelling quiz stem shows wikilink display text without brackets
    Given I assimilate the note "Wikistudy" with the option of remembering spelling
    When I visit recall for a due quiz question on day 2
    Then I should be asked spelling question "uses LinkTarget for practice" from notebook "English practice"
