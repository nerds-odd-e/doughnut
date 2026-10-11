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
    And the place where my speech will arrive should be marked in the note content
    When I stop recording audio
    Then the note content on the current page should be "This is class 1. its talk about dada struct day."
    And no place should be marked for arriving speech
    And the note "DS lecture/Data Structure Lecture" in Donut should have content "This is class 1. its talk about dada struct day."

  @mockBrowserTime
  Scenario: Retry the kept recording from the Voice input button after the transcription failed
    Given the OpenAI transcription service fails
    And I start recording audio for the note "Data Structure Lecture"
    And the browser records audio input from the microphone as in "lecture.wav"
    When I stop recording audio
    Then the note content on the current page should be "This is class 1."
    And I should see an error toast containing "Could not turn your speech into text"
    Given the OpenAI transcription service now returns the text "its talk about dada struct day."
    When I retry converting my speech
    Then the note content on the current page should be "This is class 1. its talk about dada struct day."
    And the note "DS lecture/Data Structure Lecture" in Donut should have content "This is class 1. its talk about dada struct day."

  Scenario: Create a note by speaking the title
    Given the OpenAI transcription service will return the text "Photosynthesis in desert plants." when I stop
    When I am creating a note in the notebook "DS lecture"
    And I speak the title
    Then the place where my spoken title will arrive should be marked in the Title field
    When the browser records audio input from the microphone as in "lecture.wav"
    And I stop speaking the title
    Then the Title field should read "Photosynthesis in desert plants."
    And no place should be marked in the Title field
    When I submit the new note
    Then I should see the note "Photosynthesis in desert plants."

  @mockBrowserTime
  Scenario: Insert spoken words at the title caret from the toolbar
    Given I have a notebook "Orchard" with a note "Orchard notes" and content "This is class 1."
    And the OpenAI transcription service will return the text "harvest" when I stop
    When I visit note "Orchard notes"
    And I place the note title caret after "Orchard"
    And I start voice input from the visible toolbar
    And the browser records audio input from the microphone as in "lecture.wav"
    And I stop recording audio
    Then the note title should be "Orchard harvest notes"
    When it is 1 minutes later in the browser
    And I track the current note title as "Orchard harvest notes"
    And I reload the current page for note "Orchard harvest notes"
    Then the note title should be "Orchard harvest notes"
    And the note "Orchard/Orchard harvest notes" in Donut should have content "This is class 1."

  Scenario: Rename a linked note from the toolbar, keeping visible reference text
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
    And I select the whole note title
    And I start voice input from the visible toolbar
    And the browser records audio input from the microphone as in "lecture.wav"
    And I stop recording audio
    Then the note title should be "WikiLinks CI Renamed"
    When I keep visible reference text for the title "WikiLinks CI Renamed"
    And I reload the current page for note "WikiLinks CI Renamed"
    Then the note title should be "WikiLinks CI Renamed"
    When I route to the note "WikiLinks Tech"
    Then I should see the note content rendered as:
      | Kind      | Text           |
      | wiki link | WikiLinks CI   |
    And the wiki link "WikiLinks CI" should open the note titled "WikiLinks CI Renamed"
