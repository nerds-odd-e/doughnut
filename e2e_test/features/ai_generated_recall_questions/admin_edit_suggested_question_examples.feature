@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Generate Training examples for fine-tuning OpenAI
  As an admin,
  I want to edit the example questions suggested by the users,
  So that I can fix the questions or create good example by modifying bad ones,
  so that I can use these data for OpenAI fine-tuning to improve question generation and question generation vevaluation.

  Background:
    Given I am logged in as an existing user


  Scenario: Admin should be able to edit the suggested question and its choices
    Given I have the true false question "Fire is hot" rated as a good example
    When an admin edit the question and choices "Fire is hot" with a different question:
      | Question Stem | Choice A                                    |
      | Is fire hot?  | The coldest fire is still too hot for human |
    Then an admin can retrieve the training data for question generation containing:
      | Question Stem | Choices                                     |
      | Is fire hot?  | The coldest fire is still too hot for human |

  Scenario: Admin should be able to duplicate negative feedback
    Given I have the true false question "Fire is cold" rated as a bad example
    When an admin can duplicate the question "Fire is cold"
    Then an admin should be able to see 2 examples containing "Fire is cold"
    And an admin should be able to identify the duplicated record

  Scenario: Admin should not be able to duplicate positive feedback
    Given I have the true false question "Fire is hot" rated as a good example
    Then an admin should not be able to duplicate this feedback to the question "Fire is hot"
