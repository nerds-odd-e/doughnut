@ignore
@usingMockedOpenAiService
Feature: Automatic upload and training

  As a admin when i click on the "Upload and Training" button
  I want to upload the note and train the AI model automatically

  Background:
    Given I am logged in as an existing user

  Scenario: Feedback less than 10
    Given I have 9 feedbacks
    When I click on the "Upload and Training" button
    Then I should see the message "You need at least 10 feedbacks to train the AI model"

  Scenario: Feedback more than 10 but upload progress failed
    Given I have 10 feedbacks
    When I click on the "Upload and Training" button
    And the upload progress failed
    Then I should see the message "Upload failed"

  Scenario: Feedback more than 10 and upload progress success but training progress failed
    Given I have 10 feedbacks
    When I click on the "Upload and Training" button
    And the upload progress success
    And the training progress failed
    Then I should see the message "Training failed"

  Scenario: Feedback more than 10 and upload progress success and training progress success
    Given I have 10 feedbacks
    When I click on the "Upload and Training" button
    Then I should see the message "Training is in progress"
