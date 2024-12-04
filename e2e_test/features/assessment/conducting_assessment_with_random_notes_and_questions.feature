Feature: Self assessment with random notes and questions
  As a trainer, I want the assessment to be more dynamic by using random notes and questions

  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Countries" and notes:
      | Title     | Parent Title |
      | Singapore | Countries    |
      | Vietnam   | Countries    |
    And notebook "Countries" is shared to the Bazaar
    And there are questions in the notebook "Countries" for the note:
      | Note Topic | Question                         | Answer | One Wrong Choice | Approved |
      | Singapore  | Where in the world is Singapore? | Asia   | euro             | true     |
      | Vietnam    | Most famous food of Vietnam?     | Pho    | bread            | true     |
    And I set the number of questions per assessment of the notebook "Countries" to 1

  @randomizerWithFixedSeed
  Scenario: Perform multiple assesments on the same notebook and questions vary from attempt to attempt
    Then 4 subsequent attempts of assessment on the "Countries" notebook should use 2 questions

  @randomizerAlwaysInAscendOrder
  Scenario: Should always use the same questions when randomizer is set to always in ascending order
    Then 4 subsequent attempts of assessment on the "Countries" notebook should use 1 questions
