@usingMockedOpenAiService
Feature: Note refinement
  As a learner, when I start assimilating a note, I want to open Refine note
  to see AI refinement suggestions for decomposing and improving the note,
  remove selected suggestions to have AI remove related content from note content,
  and extract a suggestion to a new note.
  So that I can refine long notes while assimilating them.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Sample tree" with notes:
      | Title  | Folder  | Content |
      | Parent |         | |
      | Sample | Context | A. B. C. D. E. |
    And OpenAI generates refinement suggestions:
      | A |
      | B |
      | C |
      | D |
      | E |

  Scenario: Generate refinement suggestions for a note
    When I am assimilating the note "Sample"
    Then I should see refinement suggestions with 5 items

  Scenario: Remove selected refinement suggestions
    Given OpenAI returns the following content when requested to remove suggestions:
      | B. D. E. |
    When I am assimilating the note "Sample"
    And I remove refinement suggestions 0 and 2
    Then the note content on the current page should be "B. D. E."

  Scenario: Extract a suggestion to a new note
    Given OpenAI will extract suggestion "B" to a new note with title "Point B" and content "Extracted" and updated parent content "A. C. D. E."
    When I am assimilating the note "Sample"
    And I should see refinement suggestions with 5 items
    And I extract the suggestion "B" to a new note
    Then the note title should be "Point B"
    And I should see folder "Sample tree/Context" containing these notes:
      | note-title |
      | Sample     |
      | Point B    |
