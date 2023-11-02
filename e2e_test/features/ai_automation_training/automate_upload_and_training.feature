@ignore
@usingMockedOpenAiService
Feature: Automatic upload and training

  As a admin when i click on the "Upload and Training" button
  I want to upload the note and train the AI model automatically

  Background:
    Given I am logged in as an existing user

  Scenario Outline: make upload and training progress
    Given I have <Feedback Count> feedbacks
    And OpenAi Upload progress should <Upload result>
    And OpenAi training progress should <Training result>
    When I click on the "Upload and Training" button
    Then I should see the message <Message>
    Examples:
      | Feedback Count | Message                                                | Upload result | Traning result |
      | 9              | "You need at least 10 feedbacks to train the AI model" | success       | success        |
      | 10             | "Upload failed"                                        | failed        | success        |
      | 10             | "Training failed"                                      | success       | failed         |
      | 10             | "Training is in progress"                              | success       | success        |