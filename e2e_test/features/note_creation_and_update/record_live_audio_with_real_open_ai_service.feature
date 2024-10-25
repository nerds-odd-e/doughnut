Feature: Note Upload Audio File
    As a learner, I want to be able to upload an audio file for a note

    Background:
        Given I am logged in as an existing user
        And I have a notebook with the head note "podcast"


    # this scenario uses real Open AI service. It costs less than $0.01 per 20 runs.
    Scenario: Convert audio-file to SRT without saving
      When I try to upload an audio-file "sample-3s.mp3" to the note "podcast"
      And I convert the audio-file to SRT without saving
      And the note details on the current page should be "You"


    Scenario: Record audio of a live event with real OpenAI service
      Given I have a notebook with the head note "Data Structure Lecture"
      And the browser is mocked to give permission to record audio
      And I start recording audio for the note "Data Structure Lecture"
      And the browser records audio input from the microphone as in "lecture.wav"
      When I stop recording audio
      Then the note details on the current page should be "Please be very quiet."

