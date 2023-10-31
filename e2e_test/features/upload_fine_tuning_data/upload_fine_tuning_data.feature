@usingMockedOpenAiService
Feature: Upload fine tuning data

  @ignore
  Scenario: Block upload fine tuning data
    Given I have 9 positive feedbacks
    When I upload the feedbacks
    Then I should see the error message "Feedback less than 10."

  @ignore
  Scenario: Open AI fail
    Given Open AI is not ready
    Given I have 10 positive feedbacks and 1 negative feedback
    When I upload the feedbacks
    Then I should see the error message "Something wrong with Open AI service."

  @ignore
  Scenario: Upload fine tuning data to Open AI service
    Given I have 10 positive feedbacks and 1 negative feedback
    When I upload the feedbacks
    Then I should see the success message "Upload successfully."




