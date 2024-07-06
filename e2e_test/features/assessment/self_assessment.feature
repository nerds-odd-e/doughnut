Feature: Self assessment
  As a trainer, I want to create a notebook with knowledge and questions
  and share it in the Bazaar, so that people can use it to assess their own skill level and knowledge on the topic

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Countries" and notes:
      | Topic     | Parent Topic |
      | Singapore | Countries    |
      | Vietnam   | Countries    |
      | Japan     | Countries    |
      | Korea     | Countries    |
      | China     | Countries    |
    And notebook "Countries" is shared to the Bazaar
    And there are questions for the note:
      | noteTopic | question                                       | answer   | oneWrongChoice | approved |
      | Singapore | Where in the world is Singapore?               | Asia     | euro           | true     |
      | Vietnam   | Most famous food of Vietnam?                   | Pho      | bread          | true     |
      | Japan     | What is the capital city of Japan?             | Tokyo    | Kyoto          | true     |
      | Japan     | What is the largest city in the Kyushu island? | Fukuoka  | Nagasaki       | true     |
      | Korea     | What is the capital city of Korea?             | Seoul    | Busan          | true     |
      | China     | What is the capital city of China?             | Beijing  | Shanghai       | true     |
      | China     | What is the largest city of China?             | Shanghai | Beijing        | true     |

  Scenario Outline: Perform an assessment with variable outcomes counts correct scores
    Given I set the number of questions per assessment of the notebook "Countries" to <QuestionsPerAssessment>
    When I start the assessment on the "Countries" notebook in the bazaar
    And I answer with the following answers:
      | question                                       | answer                   |
      | Where in the world is Singapore?               | <SingaporeAnswer>        |
      | Most famous food of Vietnam?                   | <VietnamAnswer>          |
      | What is the capital city of Japan?             | <JapanAnswer>            |
      | What is the capital city of Korea?             | <KoreaAnswer>            |
      | What is the capital city of China?             | <ChinaAnswer>            |
      | What is the largest city in the Kyushu island? | <KyushuAnswer>           |
      | What is the largest city of China?             | <LargestChinaCityAnswer> |
    Then I should see the score "Your score: <ExpectedScore> / <QuestionsPerAssessment>" at the end of assessment

    Examples:
      | QuestionsPerAssessment | SingaporeAnswer | VietnamAnswer | JapanAnswer | KoreaAnswer | ChinaAnswer | KyushuAnswer | LargestChinaCityAnswer | ExpectedScore |
      | 5                      | Asia            | Pho           | Tokyo       | Seoul       | Beijing     | Fukuoka      | Shanghai               | 5             |
      | 5                      | euro            | bread         | Kyoto       | Busan       | Shanghai    | Nagasaki     | Beijing                | 0             |
      | 5                      | Asia            | Pho           | Kyoto       | Busan       | Shanghai    | Nagasaki     | Beijing                | 2             |

  Scenario: Perform an assessment with all wrong answers
    Given I set the number of questions per assessment of the notebook "Countries" to 2
    When I start the assessment on the "Countries" notebook in the bazaar
    And I answer with the following answers:
      | question                                       | answer   |
      | Where in the world is Singapore?               | euro     |
      | What is the capital city of Japan?             | Kyoto    |
      | Most famous food of Vietnam?                   | bread    |
      | What is the capital city of Korea?             | Busan    |
      | What is the capital city of China?             | Shanghai |
      | What is the largest city in the Kyushu island? | Nagasaki |
      | What is the largest city of China?             | Beijing  |
    Then I should see the score "Your score: 0 / 2" at the end of assessment
    And I should see a link to the "Singapore" notebook

  Scenario: Perform multiple assesments on the same notebook and questions vary from attempt to attempt
    Given I set the number of questions per assessment of the notebook "Countries" to 1
    Then 5 subsequent attempts of assessment on the "Countries" notebook should use more than 1 different questions

  Scenario Outline: Cannot start assessment with 0 questions or not enough approved questions
    Given I set the number of questions per assessment of the notebook "Countries" to <Questions Per Assessment>
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I should see error message <Message>

    Examples:
      | Questions Per Assessment | Message                         |
      | 0                        | The assessment is not available |
      | 10                       | Not enough questions            |

  Scenario: Must login to generate assessment
    Given I haven't login
    When I start the assessment on the "Countries" notebook in the bazaar
    Then I should see message that says "Please login first"
