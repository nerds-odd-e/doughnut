@disableOpenAiService
Feature: Assimilate With Remembering Spelling
  As a learner, I want to keep notes for recall with spelling verification.
  Spelling is only available for notes with content.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title   | Skip Memory Tracking |
      | English | true                 |

  Scenario Outline: Remembering spelling availability depends on note content
    Given I have a notebook "English practice" with notes:
      | Title | Content |
      | Word  | <content> |
    When I am assimilating the note "Word"
    Then remembering spelling should be <availability>

    Examples:
      | case                     | content                 | availability |
      | note has no content      |                         | unavailable  |
      | note has definition      | Definition content      | available    |

  Scenario Outline: Verify spelling proceeds with keep for recall
    Given I have a notebook "English practice" with notes:
      | Title        | Content |
      | <note_title> | Non-empty body text |
    And I am assimilating the note "<note_title>"
    And I keep for recall with remembering spelling
    When I verify spelling with "<spelling_input>"
    Then the spelling verification result for note "<note_title>" should be <expected_result>

    Examples:
      | note_title | spelling_input | expected_result         |
      | sedition   | sedition       | "success"               |
      | sedition   | wrong answer   | "error: wrong spelling" |

  Scenario: Already assimilated note reappears in to-be-assimilated list when remember spelling is added later
    Given I have a notebook "English practice" with notes:
      | Title   | Content |
      | Relearn | Non-empty body text |
    And I assimilated one note "Relearn" on day 1
    And I add remember spelling to the note "Relearn"
    When I reload the current page for note "Relearn"
    Then I should see 1 due for assimilation

  Scenario: Add only spelling memory tracker when note already has trackers
    Given I have a notebook "English practice" with notes:
      | Title | Content |
      | Word  | Non-empty body text |
    And I assimilated one note "Word" on day 1
    When I am assimilating the note "Word"
    And I keep for recall with remembering spelling
    When I verify spelling with "Word"
    Then the spelling verification result for note "Word" should be "success"
