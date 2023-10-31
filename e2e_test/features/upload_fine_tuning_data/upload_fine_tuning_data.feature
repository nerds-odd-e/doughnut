@usingMockedOpenAiService
Feature: Upload fine tuning data

  @ignore
  Scenario: Block upload fine tuning data
    Given I have 9 good feedbacks
    When I upload the feedbacks
    Then I should see the error message "Feedback less than 10."

  @ignore
  Scenario: Open AI fail
    Given Open AI is not ready
    When I upload 10 feedbacks
    Then I should see the error message "Something wrong with Open AI service."





