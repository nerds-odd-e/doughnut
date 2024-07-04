
Feature: Questions order in assessment is random
  As a user taking an assessment
  I want to have random questions in the assessment
  so that I get a new test each time and learn something new

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | parentTopic |
      | Countries        |             |
      | Singapore        | Countries   |

    And notebook "Countries" is shared to the Bazaar
    And there are questions for the note:
      | noteTopic | question                         | answer  | oneWrongChoice | approved |
      | Singapore | Where in the world is Singapore? | Asia    | euro           | true     |
      | Singapore | Most famous food of Singapore?   | Noodles | bread          | true     |


  Scenario: Questions vary from attempt to attempt
    Given I set the number of questions per assessment of the notebook "Countries" to 1
    Then 10 subsequent attempts of assessment on the "Countries" notebook should be random meaning it should not have the same questions each time
