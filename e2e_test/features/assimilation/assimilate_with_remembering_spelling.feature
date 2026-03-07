@disableOpenAiService
Feature: Assimilate With Remembering Spelling
  As a learner, I want to keep notes for recall with spelling verification.
  Spelling is only available for notes with details.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "English" which skips memory tracking

  Scenario Outline: Remembering spelling availability depends on note details
    Given there are some notes:
      | Title | Details        | Parent Title |
      | Word  | <details>      | English      |
    When I am assimilating the note "Word"
    Then remembering spelling should be <availability>

    Examples:
      | case                     | details                 | availability |
      | note has no details      |                         | unavailable  |
      | note has definition      | Definition content      | available    |

  Scenario Outline: Verify spelling proceeds with keep for recall
    Given there are some notes:
      | Title        | Details           | Parent Title |
      | <note_title> | Non-empty details | English      |
    And I am assimilating the note "<note_title>"
    And I keep for recall with remembering spelling
    When I verify spelling with "<spelling_input>"
    Then the spelling verification result for note "<note_title>" should be <expected_result>

    Examples:
      | note_title     | spelling_input | expected_result         |
      | sedition       | sedition       | "success"               |
      | colour / color | colour         | "success"               |
      | sedition       | wrong answer   | "error: wrong spelling" |

  Scenario: Already assimilated note reappears in to-be-assimilated list when remember spelling is added later
    Given there are some notes:
      | Title   | Details           | Parent Title |
      | Relearn | Non-empty details | English      |
    And I assimilate the note "Relearn"
    And I add remember spelling to the note "Relearn"
    When I navigate to the assimilation page
    Then I should see 1 due for assimilation
    
  Scenario: Add only spelling memory tracker when note already has trackers
    Given there are some notes:
      | Title | Details           | Parent Title |
      | Word  | Non-empty details | English      |
    And I assimilated one note "Word" on day 1
    When I am assimilating the note "Word"
    And I keep for recall with remembering spelling
    When I verify spelling with "Word"
    Then the spelling verification result for note "Word" should be "success"

  Scenario: Keep for recall disabled when note already has memory trackers
    Given there are some notes:
      | Title | Details           | Parent Title |
      | Word  | Non-empty details | English      |
    And I assimilated one note "Word" on day 1
    When I am assimilating the note "Word"
    Then the keep for recall button should be disabled
