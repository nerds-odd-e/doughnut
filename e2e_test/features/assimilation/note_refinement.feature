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
    And OpenAI reloads refinement layout after removal:
      | id   | text | parent | alreadyExtracted |
      | p1   | A    |        |                  |
      | p1-2 | C    | p1     | true             |
      | p3   | E    |        |                  |
    When I am assimilating the note "Sample"
    And I remove refinement layout items 1 and 3
    Then the note content on the current page should be "A. C. E."
    And no refinement layout points should be selected
    And I should see the refinement layout:
      | text | level | alreadyExtracted |
      | A    | 1     |                  |
      | C    | 2     | true             |
      | E    | 1     |                  |

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
    And I open extraction preview for refinement layout points "B" and "D"
    And I create the note from the extraction preview
    Then the note title should be "Point B and D"
    And I should see folder "Sample tree/Context" containing these notes:
      | note-title    |
      | Sample        |
      | Point B and D |

  Scenario: Save edited extraction preview content
    Given OpenAI will extract layout points "B and D" to a new note with title "Point B and D" and content "Combined B and D" and updated parent content "A. C. E."
    When I am assimilating the note "Sample"
    And I open extraction preview for refinement layout points "B" and "D"
    And I edit the extraction preview to title "Edited B and D" and content "Edited combined content" and updated parent content "A. C. E. edited"
    And I create the note from the extraction preview
    Then the note title should be "Edited B and D"
    And I should see note "Sample tree/Context/Sample" has content "A. C. E. edited"

  Scenario: Retry extraction preview before creating note
    Given OpenAI will extract layout points "B and D" with retry producing title "Retry B and D" and content "Retry combined content" and updated parent content "A. C. E. retry"
    When I am assimilating the note "Sample"
    And I open extraction preview for refinement layout points "B" and "D"
    And I retry the extraction preview
    And I create the note from the extraction preview
    Then the note title should be "Retry B and D"
    And I should see folder "Sample tree/Context" containing these notes:
      | note-title    |
      | Sample        |
      | Retry B and D |

  Scenario: Cannot create note with blank title from extraction preview
    Given OpenAI will extract layout points "B and D" to a new note with title "Point B and D" and content "Combined B and D" and updated parent content "A. C. E."
    When I am assimilating the note "Sample"
    And I open extraction preview for refinement layout points "B" and "D"
    And I clear the extraction preview new note title
    Then the extraction preview create note button should be disabled
    And I should see folder "Sample tree/Context" containing these notes:
      | note-title |
      | Sample     |

  Scenario: Export extract request shows AI request JSON
    When I am assimilating the note "Sample"
    And I export the extract request for refinement layout points "B" and "D"
    Then the export request dialog should show AI request JSON

  Scenario: Export breakdown request shows AI request JSON
    When I am assimilating the note "Sample"
    And I export the breakdown request from refinement layout
    Then the export request dialog should show AI request JSON
