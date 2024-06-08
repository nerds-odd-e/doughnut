Feature: Bazaar generate
  As a trainer, I want to generate to assessment questions in the Bazaar so that I can
  print the questions for notebook.

  Background:
    Given there are some notes for existing user "old_learner"
      | topicConstructor | testingParent |
      | Countries        |               |
      | Singapore        | Countries     |
      | Vietnam          | Countries     |
      | Japan            | Countries     |
      | Korea            | Countries     |
      | China            | Countries     |
      | Mars             | Countries     |
    And notebook "Countries" is shared to the Bazaar

  Scenario: open pop up for log in if the user is not logged in and generate assessment
    Given I haven't login
    When I go to the bazaar
    And I generate assessment questions on notebook "Countries"
    Then I should see message that says "Please login first"

  Scenario: display assessment questions from notebook
    Given there are questions for the note:
      | noteTopic | question                         | answer | oneWrongChoice |
      | Singapore | Where in the world is Singapore? | Asia   | euro           |
      | Vietnam   | Most famous food of Vietnam?     | Pho    | bread          |
    And I am logged in as an existing user
    When I go to the bazaar
    And I generate assessment questions on notebook "Countries"
    Then I should see message that says "Insufficient notes to create assessment!"
    And I should see 0 questions

  Scenario: display assessment questions from notebook
    Given there are questions for the note:
      | noteTopic | question                           | answer  | oneWrongChoice |
      | Singapore | Where in the world is Singapore?   | Asia    | euro           |
      | Vietnam   | Most famous food of Vietnam?       | Pho     | bread          |
      | Japan     | What is the capital city of Japan? | Tokyo   | Kyoto          |
      | Korea     | What is the capital city of Korea? | Seoul   | Busan          |
      | China     | What is the capital city of China? | Beijing | Shanghai       |
      | Mars      | What is the capital city of Mars?  | X       | Y              |
    And I am logged in as an existing user
    When I go to the bazaar
    And I generate assessment questions on notebook "Countries"
    Then I should see message that says "Assessment For Countries"
    And I should see 5 questions
