@usingMockedOpenAiService
@startWithEmptyDownloadsFolder
Feature: Refine Training Examples for OpenAI Fine-Tuning

  As an admin,
  I want to edit or duplicate example questions suggested by users
  So that I can correct or enhance these examples for OpenAI's fine-tuning process,
  Ultimately improving question generation and evaluation accuracy.

  Background:
    Given I am logged in as an existing user

  Scenario: Admin modifies a positively rated question
    Given I have the true false question "Fire is hot" rated as a good example
    When the admin modifies the question suggested "Fire is hot" to:
      | Question Stem | Choice A                                    |
      | Is fire hot?  | The coldest fire is still too hot for human |
    Then an admin can retrieve the training data for question generation containing:
      | Question Stem | Choices                                     |
      | Is fire hot?  | The coldest fire is still too hot for human |

  Scenario: Admin duplicates a negatively rated question
    Given I have the true false question "Fire is cold" rated as a bad example
    When an admin duplicates the question "Fire is cold"
    Then there should be 2 examples containing "Fire is cold"
