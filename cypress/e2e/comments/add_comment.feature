@ignore
Feature: Add comment to note

  Background:
    Given I've logged in as an existing user
    And there are some notes for the current user
      | title  | description | parentTitle |
      | Taiwan | Taiwan best | N.A.        |
    # And there are some existing comments
    #   | text      |
    #   | Taiwan #1 |

  Scenario: Existing comments are displayed under the notes
    Given I visit note "Taiwan"
    Then I should see the text "Taiwan #1"

  @ignore
  Scenario: One or many comments can be added to a note
    Given I visit note "Taiwan"
    When I add a comment with "Taiwan #2"
    Then there are some existing comments
      | text      |
      | Taiwan #1 |
      | Taiwan #2 |
