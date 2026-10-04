@usingMockedOpenAiService
Feature: Record live audio onto a note
  As a learner, I want to record live audio and append the transcription to a note

  Background:
    Given I am logged in as an existing user
    And I have a notebook "DS lecture" with a note "Data Structure Lecture" and content "This is class 1."
    And the OpenAI transcription service will return the following srt transcript:
      """
      00:00:00,000 --> 00:00:01,000
      its talk about dada struct day.

      """
    And the OpenAI completion service will return the following response for the transcription to text request:
      | request contains                | response                                                          |
      | its talk about dada struct day. | Let's talk about data structure today.                           |
    And the browser is mocked to give permission to record audio

  @mockBrowserTime
  Scenario: A lone transcription segment waits until stop before it is appended
    Given I start recording audio for the note "Data Structure Lecture"
    And the browser records audio input from the microphone as in "lecture.wav"
    When it is 2 minutes later in the browser
    Then the note content on the current page should be "This is class 1."
    When I stop recording audio
    Then the note content on the current page should be "This is class 1. Let's talk about data structure today."
    And the note "DS lecture/Data Structure Lecture" in Donut should have content "This is class 1. Let's talk about data structure today."
