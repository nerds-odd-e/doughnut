Feature: Recording a live audio and append to note details
  As a learner, I want to create a note to capture the audio of a live event and append it to the note details

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Data Structure Lecture"

  @ignore
  Scenario: Record audio of a live event
    Given the browser is mocked to give permission to record audio and receive audio input as in "lecture.wav"
    When I start recording audio for the note "Data Structure Lecture"
    Then the note "Data Structure Lecture" should have details "Let's talk about data structure today."
