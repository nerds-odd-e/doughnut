Feature: Note Upload Audio File
    As a learner, I want to be able to upload an audio file for a note

    Background:
        Given I am logged in as an existing user
        And I have a note with the topic "podcast"


    Scenario: Upload audio into a note
        When I upload an audio-file "Alison.mp3" to the note "podcast"
        Then I must be able to download the "Alison.mp3" from the note "podcast"

    Scenario: I must not be able to upload an audio file with an invalid format
        When I upload an audio-file "moon.jpg" to the note "podcast"
        Then I should see an error "Invalid file type: image/jpeg. Allowed types are: audio/mpeg, audio/wav, audio/mp4" on "Upload Audio File"

    Scenario: Upload audio file into a note with existing audio file attached
        Given I upload an audio-file "Alison.mp3" to the note "podcast"
        When I upload an audio-file "harvard.wav" to the note "podcast"
        Then I must be able to download the "harvard.wav" from the note "podcast"

    # this scenario uses real Open AI service. It costs less than $0.01 per 20 runs.
    Scenario: Convert audio-file to SRT without saving
      When I try to upload an audio-file "sample-3s.mp3" to the note "podcast"
      And I convert the audio-file to SRT without saving
      Then I should see the extracted SRT content
      """
      1
      00:00:00,000 --> 00:00:02,000
      You



      """

