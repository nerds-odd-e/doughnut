Feature: Bazaar generate
  As a trainer, I want to generate an assessment for a notebook in the Bazaar so that I can
  print the questions for notebook to test my training participants.

  Background:
    Given there are some notes for existing user "old_learner"
      | topicConstructor | parentTopic |
      | Countries        |             |
      | Singapore        | Countries   |
      | Vietnam          | Countries   |
      | Japan            | Countries   |
      | Korea            | Countries   |
      | China            | Countries   |
      | Mars             | Countries   |
    Given there are questions for the note:
      | noteTopic | question                         | answer | oneWrongChoice |
      | Singapore | Where in the world is Singapore? | Asia   | euro           |
      | Vietnam   | Most famous food of Vietnam?     | Pho    | bread          |
    And notebook "Countries" is shared to the Bazaar

  Scenario: Must login to generate assessment
    Given I haven't login
    When I generate assessment questions on notebook "Countries" in the bazaar
    Then I should see message that says "Please login first"

  Scenario: Cannot do assessment if notebook has no enough questions
    And I am logged in as an existing user
    When I generate assessment questions on notebook "Countries" in the bazaar
    Then I should see message that says "Insufficient notes to create assessment!"
    And I should see 0 questions

  Scenario: Get assessment for notebook successfully
    Given there are questions for the note:
      | noteTopic | question                           | answer  | oneWrongChoice |
      | Japan     | What is the capital city of Japan? | Tokyo   | Kyoto          |
      | Korea     | What is the capital city of Korea? | Seoul   | Busan          |
      | China     | What is the capital city of China? | Beijing | Shanghai       |
      | Mars      | What is the capital city of Mars?  | X       | Y              |
    And I am logged in as an existing user
    When I generate assessment questions on notebook "Countries" in the bazaar
    Then I should see message that says "Assessment For Countries"
    And I should see 5 questions
