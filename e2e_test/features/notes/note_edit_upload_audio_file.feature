Feature: Note Upload Audio File
    As a learner, I want to be able to upload an audio file for a note

    Background:
        Given I am logged in as an existing user
        And I have a note with the topic "lunar"


    Scenario: Upload audio into a note
        When I upload an audio-file "Alison.mp3" to the note "lunar"
        Then I must be able to download the "Alison.mp3" from the note "lunar"

    @ignore
    Scenario: I must not be able to upload an audio file with an invalid format
        When I try to upload an audio-file "alison.txt" to the note "lunar"
        Then I must see an error message "Invalid file format"

    Scenario: Upload audio file into a note with existing audio file attached
        Given I upload an audio-file "Alison.mp3" to the note "lunar"
        When I upload an audio-file "harvard.wav" to the note "lunar"
        Then I must be able to download the "harvard.wav" from the note "lunar"
