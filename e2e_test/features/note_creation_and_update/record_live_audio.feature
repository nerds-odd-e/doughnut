@usingMockedOpenAiService
Feature: Record live audio onto a note
  As a learner, I want to record live audio and append the transcription to a note

  Background:
    Given I am logged in as an existing user
    And I have a notebook "DS lecture" with a note "Data Structure Lecture" and content "This is class 1."
    And the browser is mocked to give permission to record audio

  @mockBrowserTime
  Scenario: A lone transcription segment waits until stop before it is appended
    Given the OpenAI transcription service will return the following srt transcript while I am speaking:
      """
      00:00:00,000 --> 00:00:01,000
      its talk about dada struct day.

      """
    And the OpenAI transcription service will return the text "its talk about dada struct day." when I stop
    And I start recording audio for the note "Data Structure Lecture"
    And the browser records audio input from the microphone as in "lecture.wav"
    When it is 2 minutes later in the browser
    Then the note content on the current page should be "This is class 1."
    When I stop recording audio
    Then the note content on the current page should be "This is class 1. its talk about dada struct day."
    And the note "DS lecture/Data Structure Lecture" in Donut should have content "This is class 1. its talk about dada struct day."

  @mockBrowserTime
  Scenario: The kept recording joins the next recording after the transcription failed
    Given the OpenAI transcription service fails
    And I start recording audio for the note "Data Structure Lecture"
    And the browser records audio input from the microphone as in "lecture.wav"
    When I stop recording audio
    Then the note content on the current page should be "This is class 1."
    And I should see an error toast containing "Could not turn your speech into text"
    Given the OpenAI transcription service now returns the text "its talk about dada struct day."
    When I start recording audio for the note "Data Structure Lecture"
    And I stop recording audio
    Then the note content on the current page should be "This is class 1. its talk about dada struct day."
    And the note "DS lecture/Data Structure Lecture" in Donut should have content "This is class 1. its talk about dada struct day."

  Scenario: Create a note by speaking the title
    Given the OpenAI transcription service will return the text "Photosynthesis in desert plants." when I stop
    When I am creating a note in the notebook "DS lecture"
    And I speak the title
    And the browser records audio input from the microphone as in "lecture.wav"
    And I stop speaking the title
    Then the Title field should read "Photosynthesis in desert plants."
    When I submit the new note
    Then I should see the note "Photosynthesis in desert plants."

  @mockBrowserTime
  Scenario: Rename a note by speaking the title
    Given the OpenAI transcription service will return the text "Apple orchard care" when I stop
    When I visit note "Data Structure Lecture"
    And I speak the title
    And the browser records audio input from the microphone as in "lecture.wav"
    And I stop speaking the title
    Then the note title should be "Apple orchard care"
    When it is 1 minutes later in the browser
    Then I should see the note tree in the sidebar
      | note-title         |
      | Apple orchard care |
    When I track the current note title as "Apple orchard care"
    And I visit note "Apple orchard care"
    Then the note title should be "Apple orchard care"
    And the note "DS lecture/Apple orchard care" in Donut should have content "This is class 1."

  Scenario: Rename a linked note by speaking the title, keeping visible reference text
    Given I have a notebook "WikiLinks Speak NB" with notes:
      | Title          | Folder         |
      | WikiLinks Tech | WikiLinks Root |
      | WikiLinks CI   | WikiLinks Root |
    And the OpenAI transcription service will return the text "WikiLinks CI Renamed" when I stop
    When I update note "WikiLinks Tech" content using markdown to become:
      """
      See [[WikiLinks CI]] for process.
      """
    And I route to the note "WikiLinks CI"
    And I speak the title
    And the browser records audio input from the microphone as in "lecture.wav"
    And I stop speaking the title
    Then the note title should be "WikiLinks CI Renamed"
    When I keep visible reference text for the title "WikiLinks CI Renamed"
    And I route to the note "WikiLinks Tech"
    Then I should see the note content rendered as:
      | Kind      | Text           |
      | wiki link | WikiLinks CI   |
    And the wiki link "WikiLinks CI" should open the note titled "WikiLinks CI Renamed"
