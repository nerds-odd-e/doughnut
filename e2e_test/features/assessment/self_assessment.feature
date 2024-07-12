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
    And notebook "Countries" is shared to the Bazaar
    And there are questions for the note:
      | noteTopic | question                           | answer | oneWrongChoice | approved |
      | Singapore | Where in the world is Singapore?   | Asia   | euro           | true     |
      | Vietnam   | Most famous food of Vietnam?       | Pho    | bread          | true     |
      | Japan     | What is the capital city of Japan? | Tokyo  | Kyoto          | true     |

  Scenario Outline: Perform an assessment with variable outcomes counts correct scores
    Given I set the number of questions per assessment of the notebook "Countries" to <QuestionsPerAssessment>
    When I start the assessment on the "Countries" notebook in the bazaar
    And I answer with the following answers:
      | question                           | answer            |
      | Where in the world is Singapore?   | <SingaporeAnswer> |
      | Most famous food of Vietnam?       | <VietnamAnswer>   |
      | What is the capital city of Japan? | <JapanAnswer>     |
    Then I should see the score "Your score: <ExpectedScore> / <QuestionsPerAssessment>" at the end of assessment

    Examples:
      | QuestionsPerAssessment | SingaporeAnswer | VietnamAnswer | JapanAnswer | ExpectedScore |
      | 3                      | Asia            | Pho           | Tokyo       | 3             |
      | 3                      | euro            | bread         | Kyoto       | 0             |
      | 3                      | Asia            | Pho           | Kyoto       | 2             |

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
