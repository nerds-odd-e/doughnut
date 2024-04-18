@ignore
Feature: Note Upload Audio File
    As a learner, I want to be able to upload an audio file for a note

    Background:
        Given I am logged in as an existing user
        And I have a note with the topic "lunar"

    Scenario Outline: Supported file types
        Given I have a file named <File Name>
        When I upload <File Name> to note "lunar"
        Then I must see the message <Expected Message>
        Examples:
            | File Name     | Expected Message   |
            | youtube.mp4   | Wrong audio format |
            | something.txt | Wrong audio format |
            | podcast1.mp3  | Upload successful  |
            | podcast2.m4a  | Upload successful  |
            | podcast3.wav  | Upload successful  |
@focus 
    Scenario: Upload audio into a note
#        Given I have an audio file named "podcast.mp3"
        When I upload an audio-file "Alison.mp3" to the note "lunar"
        Then I must be able to download the "Alison.mp3" from the note "lunar"

    Scenario: Upload audio into a note with big file size
        Given I have a 2 GB audio file named "big_podcast.mp3"
        When I upload "big_podcast.mp3" to note "lunar"
        Then I must see the error message "Size limit exceeded"

    Scenario: Upload audio file into a note with existing audio file attached
        Given I have an audio file named "another_podcast.mp3" attached to it
        When I upload "another_podcast.mp3" to note "lunar"
        Then I must see a message "Upload successful"
        And I must be able to download the "another_podcast.mp3" from the note