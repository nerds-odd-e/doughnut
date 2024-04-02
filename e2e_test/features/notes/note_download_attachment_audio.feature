@ignore
Feature: Download attachment
  As a learner, I want to download an attachment audio

  Background:
    Given I am logged in as an existing user
    And there are some notes for the current user:
      | topicConstructor | details             |
      | LeSS in Action   | An awesome training |
    And there is an attachment audio for current note

  Scenario: Download attachment audio file from note details successful
    Then I should download the attachment from my note details

  Scenario: Download attachment audio file from note details with error the attachment audio cannot find from server
    Then I should see the error message when attachment not found from server