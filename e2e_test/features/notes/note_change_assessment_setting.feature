Feature: Nested Note creation
  As a learner, I want to set number of quiz questions in the assessment.

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | parentTopic |
      | Countries        |             |
      | Singapore        | Countries   |
      | Vietnam          | Countries   |
      | Japan            | Countries   |
      | Korea            | Countries   |
      | China            | Countries   |
    And there are questions for the note:
      | noteTopic | question                           | answer  | oneWrongChoice | approved |
      | Singapore | Where in the world is Singapore?   | Asia    | euro           | true     |
      | Vietnam   | Most famous food of Vietnam?       | Pho     | bread          | true     |
      | Japan     | What is the capital city of Japan? | Tokyo   | Kyoto          | true     |
      | Korea     | What is the capital city of Korea? | Seoul   | Busan          | true     |
      | China     | What is the capital city of China? | Beijing | Shanghai       | true     |
    And notebook "Countries" is shared to the Bazaar

  Scenario: Set 0 question for the assessment
    Given I set the number of question for the "Countries" note is "0"
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I should see error message The assessment is not available

  Scenario: Set 6 questions for the assessment
    Given I set the number of question for the "Countries" note is "6"
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I should see error message Not enough questions

  Scenario: Set 2 questions for the assessment
    Given I set the number of question for the "Countries" note is "2"
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I answer the question "Where in the world is Singapore?" with "Asia"
    And I answer the question "Most famous food of Vietnam?" with "Pho"
    And I should see the score "Yours score: 2 / 2" at the end of assessment
