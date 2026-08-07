Feature: Search notes
  As a learner, I want to search my notes by title,
  so that I can find the notes I need.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Sedation care" with notes:
      | Title    | Content      |
      | Sedation | Put to sleep |
      | Physical |              |
      | Magical  |              |
    And I have a notebook "Sedative drugs" with notes:
      | Title      | Content         |
      | Sedative   | Sleep medicine  |
      | Diazepam   |                 |
      | Lorazepam  |                 |
      | Clonazepam |                 |
      | Pam        |                 |

  @mockBrowserTime
  Scenario Outline: Search finds matching note titles
    When I start searching notes
    Then I should see "<targets>" as targets only when searching "<search key>"
    Examples:
      | search key | targets            |
      | Sed        | Sedation, Sedative |
      | Sedatio    | Sedation           |

  @mockBrowserTime
  Scenario: Exact title match appears first in search results
    When I start searching notes
    Then I should see "Pam, Diazepam, Lorazepam, Clonazepam" as targets only when searching "pam"

  @mockBrowserTime
  Scenario: Creating a note warns about a possible duplicate title
    Given I am creating a note in the notebook "Sedation care"
    When I type "ph" in the title
    Then I should see "Physical" as the possible duplicate
