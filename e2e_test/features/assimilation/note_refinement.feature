@usingMockedOpenAiService
Feature: Note refinement
  As a learner, when I start assimilating a note, I want to open Refine note
  to see an AI-generated layout for decomposing and improving the note,
  remove selected layout points to have AI remove related content from note content,
  and extract layout points to a new note.
  So that I can refine long notes while assimilating them.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Sample tree" with notes:
      | Title  | Folder  | Content |
      | Parent |         | |
      | Sample | Context | A. B. C. D. E. |
    And OpenAI generates refinement layout:
      | id   | text | parent | alreadyExtracted |
      | p1   | A    |        |                  |
      | p1-1 | B    | p1     |                  |
      | p1-2 | C    | p1     | true             |
      | p2   | D    |        |                  |
      | p3   | E    |        |                  |

  Scenario: Generate a refinement layout for a note
    When I am assimilating the note "Sample"
    Then I should see the refinement layout:
      | text | level | alreadyExtracted |
      | A    | 1     |                  |
      | B    | 2     |                  |
      | C    | 2     | true             |
      | D    | 1     |                  |
      | E    | 1     |                  |

  Scenario: Remove selected refinement layout points
    Given OpenAI returns the following content when requested to remove layout points:
      | A. C. E. |
    When I am assimilating the note "Sample"
    And I remove refinement layout items 1 and 3
    Then the note content on the current page should be "A. C. E."

  Scenario: Extract selected layout points to one new note
    Given OpenAI will extract layout points "B and D" to a new note with title "Point B and D" and content "Combined B and D" and updated parent content "A. C. E."
    When I am assimilating the note "Sample"
    And I should see the refinement layout:
      | text | level | alreadyExtracted |
      | A    | 1     |                  |
      | B    | 2     |                  |
      | C    | 2     | true             |
      | D    | 1     |                  |
      | E    | 1     |                  |
    And I extract refinement layout points "B" and "D" to a new note
    Then the note title should be "Point B and D"
    And I should see folder "Sample tree/Context" containing these notes:
      | note-title    |
      | Sample        |
      | Point B and D |
