Feature: New questions assessment
  As a trainer, I want to create a notebook with knowledge and questions
  and share it in the Bazaar, so that people can use it to assess their knowledge

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
    And notebook "Countries" is shared to the Bazaar
    And there are questions for the note:
      | noteTopic | question                           | answer  | oneWrongChoice | approved |
      | Singapore | Where in the world is Singapore?   | Asia    | euro           | true     |
      | Vietnam   | Most famous food of Vietnam?       | Pho     | bread          | true     |
      | Japan     | What is the capital city of Japan? | Tokyo   | Kyoto          | true     |
      | Korea     | What is the capital city of Korea? | Seoul   | Busan          | true     |
      | China     | What is the capital city of China? | Beijing | Shanghai       | true     |


  Scenario: Complete an assessment with 5 approved questions
    Given I set the number of questions per assessment of the notebook "Countries" to 5
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I answer the question "Where in the world is Singapore?" with "Asia"
    And I answer the question "Most famous food of Vietnam?" with "Pho"
    And I answer the question "What is the capital city of Japan?" with "Kyoto"
    And I answer the question "What is the capital city of Korea?" with "Busan"
    And I answer the question "What is the capital city of China?" with "Shanghai"
    And I should see the score "Yours score: 2 / 5" at the end of assessment

  Scenario: Perform an assessment with all correct answers
    Given I set the number of questions per assessment of the notebook "Countries" to 5
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I answer the question "Where in the world is Singapore?" with "Asia"
    And I answer the question "Most famous food of Vietnam?" with "Pho"
    And I answer the question "What is the capital city of Japan?" with "Tokyo"
    And I answer the question "What is the capital city of Korea?" with "Seoul"
    And I answer the question "What is the capital city of China?" with "Beijing"
    And I should see the score "Yours score: 5 / 5" at the end of assessment

  @focus
  Scenario: Perform an assessment with all wrong answers
    Given I set the number of questions per assessment of the notebook "Countries" to 1
    When I start the assessment on the "Countries" notebook in the bazaar
    And I answer with the following answers:
      | question                         | answer |
      | Where in the world is Singapore? | euro   |
    Then I should see the score "Yours score: 0 / 1" at the end of assessment
    And I should see a link to the "Singapore" notebook

  @ignore
  Scenario: Questions vary from attempt to attempt
    Given I set the number of questions per assessment of the notebook "Countries" to 1
    Then 10 subsequent attempts of assessment on the "Countries" notebook should not have the same questions each time

  Scenario: Fail to start assessment not enough approve questions
    Given I toggle the approval of the question "What is the capital city of China?" of the topic "China"
    And I toggle the approval of the question "Most famous food of Vietnam?" of the topic "Vietnam"
    And I set the number of questions per assessment of the notebook "Countries" to 4
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I should see error message Not enough questions

  Scenario: Cannot start assessment with 0 questions
    Given I set the number of questions per assessment of the notebook "Countries" to 0
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I should see error message The assessment is not available

  Scenario: Must login to generate assessment
    Given I haven't login
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I should see message that says "Please login first"
