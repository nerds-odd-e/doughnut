@ignore
Feature: Note Convert Audio File to SRT
  As a learner, I want to be able to convert an audio-file to SRT

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "podcast"

  Scenario: Convert audio-file to SRT only
    When I upload an audio-file "harvard.wav"
    And I select "Convert to SRT file"
    And I click "Convert only"
    Then I should see the extracted SRT content in a pop-up

  Scenario: Save audio-file and convert to SRT
    When I upload an audio-file "harvard.wav"
    And I select "Convert to SRT file"
    And I click "Save & Convert"
    Then I should see the extracted SRT content in a pop-up
