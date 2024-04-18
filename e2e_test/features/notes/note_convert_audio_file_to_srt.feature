
Feature: Note Convert Audio File to SRT
  As a learner, I want to be able to convert an audio-file to SRT

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "podcast"

  Scenario: Convert audio-file to SRT without saving
    When I upload an audio-file "harvard.wav" to the note "podcast"
    And I convert the audio-file to SRT without saving
    Then I should see the extracted SRT content 
    """
    1
    00:00:00,000 --> 00:00:02,000
    Hello, this is a test.
    """

  Scenario: Convert audio-file to SRT
    When I upload an audio-file "harvard.wav" to the note "podcast"
    And I save and convert the audio-file to SRT
    Then I should see the extracted SRT content 
    """
    1
    00:00:00,000 --> 00:00:02,000
    Hello, this is a test.
    """
