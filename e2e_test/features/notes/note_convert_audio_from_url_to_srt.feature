@ignore
Feature: Note Convert Audio File from URL to SRT
  As a learner, I want to be able to convert an audio-file from URL to SRT

  Background:
    Given I am logged in as an existing user
    And I have a note with the topic "podcast"

  Scenario: Convert audio-file from URL to SRT
    When I provide a URL to an audio-file "harvard.wav" to the note "podcast"
    And I convert the audio-file from URL to SRT
    Then I should see the extracted SRT content 
    """
    1
    00:00:00,000 --> 00:00:02,000
    Hello, this is a test.
    """
