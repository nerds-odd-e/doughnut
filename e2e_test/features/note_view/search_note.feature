Feature: search note
  As a learner, I want to maintain my newly acquired knowledge in
  notes that linking to each other, so that I can review them in the
  future.

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Sedation" and details "Put to sleep"
    And I have a notebook with the head note "Sedative" and details "Sleep medicine"
    And there are some notes:
      | Title    | Parent Title |
      | Physical | Sedation     |
      | Magical  | Sedation     |

  @mockBrowserTime
  Scenario Outline: Search at the top level
    When I start searching from all my notebooks page
    Then I should see "<targets>" as targets only when searching "<search key>"
    Examples:
      | search key | targets            |
      | Sed        | Sedation, Sedative |
      | Sedatio    | Sedation           |

  @mockBrowserTime
  @ignore
  Scenario: Show exact match first when searching
      Given there are some notes:
      | Title      | Parent Title |
      | Diazepam   | Sedative     |
      | Lorazepam  | Sedative     |
      | Clonazepam | Sedative     |
      | Pam        | Sedative     |
    When I start searching from all my notebooks page
    Then I should see "Pam, Diazepam, Lorazepam, Clonazepam" as targets only when searching "pam"

  @mockBrowserTime
  Scenario: Search when adding new note
    Given I am creating a note under "Sedation"
    When I type "ph" in the title
    Then I should see "Physical" as the possible duplicate
