Feature: New questions assessment

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | testingParent |
      | Countries        |               |
      | Singapore        | Countries     |
      | Vietnam          | Countries     |
      | Japan            | Countries     |
      | Korea            | Countries     |
      | China            | Countries     |
    And notebook "Countries" is shared to the Bazaar
    When I go to the bazaar

  Scenario: Start an assessment with 5 approved questions
    Given there are questions for the note:
      | topicConstructor | question                           | answer  | option   |
      | Singapore        | Where in the world is Singapore?   | Asia    | euro     | 
      | Vietnam          | Most famous food of Vietnam?       | Pho     | bread    |
      | Japan            | What is the capital city of Japan? | Tokyo   | Kyoto    |
      | Korea            | What is the capital city of Korea? | Seoul   | Busan    |
      | China            | What is the capital city of China? | Beijing | Shanghai |
    When I start the assessment on the "Countries" notebook
    Then I answer the question "Where in the world is Singapore?" with "Asia"
    And I answer the question "Most famous food of Vietnam?" with "Pho"
    And I answer the question "What is the capital city of Japan?" with "Kyoto"
    And I answer the question "What is the capital city of Korea?" with "Busan"
    And I answer the question "What is the capital city of China?" with "Shanghai"
    And I should see end of questions in the end

  @ignore
  Scenario: Fail to start assessment with 4 approved questions
    Given there are questions for the note:
      | topicConstructor | question                           | answer  | option   |
      | Vietnam          | Most famous food of Vietnam?       | Pho     | bread    |
      | Japan            | What is the capital city of Japan? | Tokyo   | Kyoto    |
      | Korea            | What is the capital city of Korea? | Seoul   | Busan    |
      | China            | What is the capital city of China? | Beijing | Shanghai |
    When I start the assessment on the "Countries" notebook
    Then I see error message Not enough approved questions