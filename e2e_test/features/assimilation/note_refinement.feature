@usingMockedOpenAiService
Feature: Note refinement
  As a learner, when I start assimilating a note, I want to open Refine note
  to see an AI-generated refinement layout for decomposing and improving the note,
  remove selected refinement layout items to have AI remove related content from note content,
  and extract refinement layout items to a new note.
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
    And I am assimilating the note "Sample"

  Scenario: Remove selected refinement layout items
    Given OpenAI returns the following content when requested to remove refinement layout items:
      | A. C. E. |
    And OpenAI reloads refinement layout after removal:
      | id   | text | parent | alreadyExtracted |
      | p1   | A    |        |                  |
      | p1-2 | C    | p1     | true             |
      | p3   | E    |        |                  |
    Then I should see the refinement layout:
      | text | level | alreadyExtracted |
      | A    | 1     |                  |
      | B    | 2     |                  |
      | C    | 2     | true             |
      | D    | 1     |                  |
      | E    | 1     |                  |
    When I remove refinement layout items 1 and 3
    Then the note content on the current page should be "A. C. E."
    And no refinement layout items should be selected
    And I should see the refinement layout:
      | text | level | alreadyExtracted |
      | A    | 1     |                  |
      | C    | 2     | true             |
      | E    | 1     |                  |

  Scenario: Extract selected refinement layout items to one new note
    Given OpenAI will extract refinement layout items "B and D" to a new note with title "Point B and D" and content "Combined B and D" and updated parent content "A. C. E."
    When I open extraction preview for refinement layout items "B" and "D"
    Then the extraction preview should show original content "A. C. E."
    When I view the extraction preview original as a diff
    Then the extraction preview original diff should show original "A. B. C. D. E." and updated "A. C. E."
    When I create the note from the extraction preview
    Then the note title should be "Point B and D"
    And I should see folder "Sample tree/Context" containing these notes:
      | note-title    |
      | Sample        |
      | Point B and D |

  Scenario: Save edited extraction preview content
    Given OpenAI will extract refinement layout items "B and D" to a new note with title "Point B and D" and content "Combined B and D" and updated parent content "A. C. E."
    When I open extraction preview for refinement layout items "B" and "D"
    And I edit the extraction preview to title "Edited B and D" and content "Edited combined content" and updated parent content "A. C. E. edited"
    And I create the note from the extraction preview
    Then the note title should be "Edited B and D"
    And note "Sample" should have content "A. C. E. edited"

  Scenario: Retry extraction preview before creating note
    Given OpenAI will extract refinement layout items "B and D" with retry producing title "Retry B and D" and content "Retry combined content" and updated parent content "A. C. E. retry"
    When I open extraction preview for refinement layout items "B" and "D"
    And I retry the extraction preview
    And I create the note from the extraction preview
    Then the note title should be "Retry B and D"
    And note "Sample" should have content "A. C. E. retry"
