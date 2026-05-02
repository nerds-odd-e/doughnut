Feature: Self assessment with random notes and questions
  As a trainer, I want the assessment to be more dynamic by using random notes and questions

  Background:
    Given I am logged in as an existing user
    And I have a notebook "World facts" with a note "Countries" and notes:
      | Title     |
      | Singapore |
      | Vietnam   |
    And notebook "World facts" is shared to the Bazaar
    And there are questions in the notebook "World facts" for the note:
      | Note Title | Question                         | Answer | One Wrong Choice | Approved |
      | Singapore  | Where in the world is Singapore? | Asia   | euro             | true     |
      | Vietnam    | Most famous food of Vietnam?     | Pho    | bread            | true     |
    And I set the number of questions per assessment of the notebook "World facts" to 1

  @randomizerWithFixedSeed
  Scenario: Perform multiple assesments on the same notebook and questions vary from attempt to attempt
    Then 4 subsequent attempts of assessment on the "World facts" notebook should use 2 questions

  @randomizerAlwaysInAscendOrder
  Scenario: Should always use the same questions when randomizer is set to always in ascending order
    Then 4 subsequent attempts of assessment on the "World facts" notebook should use 1 questions
