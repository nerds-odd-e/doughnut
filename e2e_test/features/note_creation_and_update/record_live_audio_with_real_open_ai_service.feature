@usingRealOpenAiService
@skipOptimizationDueToKnownNecessarySlowness
Feature: Record live audio with real OpenAI service
  As a learner, I want to record live audio and append the transcription to a note

  Background:
    Given I am logged in as an existing user

  Scenario: Append live recording transcription with real OpenAI
    Given I have a notebook "DS lecture" with a note "Data Structure Lecture" and content "Let's start"
    And the browser is mocked to give permission to record audio
    And I start recording audio for the note "Data Structure Lecture"
    And the browser records audio input from the microphone as in "lecture.wav"
    When I stop recording audio
    Then the note content on the current page should be "Please be quiet." within 20 seconds
