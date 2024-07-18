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
      | Note Topic | Question                           | Answer | One Wrong Choice | Approved |
      | Singapore  | Where in the world is Singapore?   | Asia   | europe           | true     |
      | Vietnam    | Most famous food of Vietnam?       | Pho    | bread            | true     |
      | Japan      | What is the capital city of Japan? | Tokyo  | kyoto            | true     |
    And I have a notebook with head note "Foods" and notes:
      | Topic     | Parent Topic |
      | Malaysia  | Foods        |
      | England   | Foods        |
      | USA       | Foods        |
    And notebook "Foods" is shared to the Bazaar
    And there are questions for the note:
      | Note Topic | Question                           | Answer         | One Wrong Choice | Approved |
      | Malaysia   | Which food is from Malaysia?       | Nasi lemak     | Pad thai         | true     |
      | England    | Which food is from England?        | Fish and chips | Pad thai         | true     |
      | USA        | Which food is from USA?            | Burger king    | Pad thai         | true     |

  Scenario Outline: Perform an assessment with variable outcomes counts correct scores
    Given I set the number of questions per assessment of the notebook "Countries" to 3
    When I do the assessment on "Countries" in the bazaar with the following answers:
      | Question                           | Answer            |
      | Where in the world is Singapore?   | <SingaporeAnswer> |
      | Most famous food of Vietnam?       | <VietnamAnswer>   |
      | What is the capital city of Japan? | <JapanAnswer>     |
    Then I should see the score "Your score: <ExpectedScore> / 3" at the end of assessment

    Examples:
      | SingaporeAnswer | VietnamAnswer | JapanAnswer | ExpectedScore |
      | Asia            | Pho           | Tokyo       | 3             |
      | europe          | bread         | kyoto       | 0             |
      | Asia            | Pho           | kyoto       | 2             |

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

  Scenario: Perform an assessment more than the limit per day
    Given There is a notebook "Countries"
    And The notebook owner set the number of questions in assessment of the notebook "Countries" to 1
    And The notebook owner set the number of maximum attempt per day of the notebook "Countries" to 3
    And I have done the assessment of the notebook "Countries" 3 times
    When I try to do assessment of the notebook "Countries" again
    Then I should not be able to do anymore assessment of the notebook "Countries" today

  @ignore
  Scenario: Perform another assessment after one reached limit per day
    Given There is a notebook "Countries"
    And The notebook owner set the number of questions in assessment of the notebook "Countries" to 1
    And The notebook owner set the number of maximum attempt per day of the notebook "Countries" to 3
    And There is a notebook "Foods"
    And The notebook owner set the number of questions in assessment of the notebook "Foods" to 1
    And The notebook owner set the number of maximum attempt per day of the notebook "Foods" to 3
    And I have done the assessment of the notebook "Countries" 3 times
    When I start the assessment on the "Foods" notebook in the bazaar
    Then I should be able to do the assessment of the notebook "Foods"
