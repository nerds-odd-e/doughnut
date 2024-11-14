@longerTimeout
Feature: Record live audio with real OpenAI service
    As a learner, I want to record audio of a live event and get the transcription

    Background:
        Given I am logged in as an existing user

    # this scenario uses real Open AI service.
    Scenario: Record audio of a live event with real OpenAI service
      Given I have a notebook with the head note "Data Structure Lecture" and details "Let's start"
      And the browser is mocked to give permission to record audio
      And I start recording audio for the note "Data Structure Lecture"
      And the browser records audio input from the microphone as in "lecture.wav"
      When I stop recording audio
      Then the note details on the current page should be "Please be very quiet."

