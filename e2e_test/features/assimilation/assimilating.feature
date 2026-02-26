Feature: Assimilating
    As a learner, I want to recall my notes. When user's memory of a note is tracked for the first time,
    there should be a daily max and I should be able to give some of my initial impression
    of the note, and/or choose to skip it for any further recalls.

  Background:
    Given I am logged in as an existing user
    And my daily new notes to assimilate is set to 2
    And there are notes from Note 1 to Note 5

  Scenario: First day of assimilation
    Given It's day 1, 8 hour
    Then I assimilate these in sequence:
      | Assimilation Type | Title  |
      | single note       | Note 1 |
      | single note       | Note 2 |
      | assimilation done |        |
    Given It's day 1, 9 hour
    Then I assimilate these in sequence:
      | Assimilation Type | Title |
      | assimilation done |       |

  Scenario: First day assimilation - only part of the daily number
    Given It's day 1, 8 hour
    Then I assimilate these in sequence:
      | Assimilation Type | Title  |
      | single note | Note 1 |
    Given It's day 1, 9 hour
    Then I assimilate these in sequence:
      | Assimilation Type | Title  |
      | single note       | Note 2 |
      | assimilation done |        |

  Scenario: Skip assimilation
    Given It's day 1, 8 hour
    When I assimilate these in sequence:
      | Assimilation Type | Title  | Skip |
      | single note       | Note 1 | yes  |
      | single note       | Note 2 | no   |
      | single note       | Note 3 | no   |
      | assimilation done |        |      |
